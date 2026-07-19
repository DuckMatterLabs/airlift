#!/usr/bin/env python3
"""Airlift claim-status promotion (deterministic, no LLM) — the M7 spine's seed.

Moves claim statuses forward along `extracted -> pinned -> verified` from test
evidence (IR-SCHEMA.md section 12) and records an attestation on each promoted
claim. Demotion is out of scope (M7); attestation records are shaped so the
future demotion pass can read them.

Usage:
  promote.py --ir <ir-dir> [--pinned-from <spine.yaml>]... [--verified-from <spine.yaml>]...
             [--dry-run]

Spine files are the machine-readable spine reports of IR-SCHEMA.md section 8
(emitted target-side, e.g. by report-claims.py --spine-out): suite, results_file,
sha, timestamp, tests: [{test, claim, outcome}], unbound: [...].

Rules:
  * claim bound to a green test in a --pinned-from spine  => at least `pinned`
  * claim bound to a green test in a --verified-from spine => `verified`
  * any red bound test anywhere in the supplied evidence  => NO promotion, reported
  * forward-only; idempotent (same inputs => no-op); --dry-run prints the diff

Exit codes: 0 = ok (with or without promotions), 1 = structural error,
2 = conflicts present (non-conflicted claims were still promoted).

File edits are line-surgical: only the claim's `status:` line changes and an
`attestations:` block is appended/merged at end of file — atom files stay
git-diffable, comments and layout untouched.
"""
import argparse
import os
import sys

import yaml

STATUS_ORDER = {"extracted": 0, "pinned": 1, "verified": 2}


def load_yaml(path):
    with open(path, encoding="utf-8") as fh:
        return yaml.safe_load(fh)


def read_lines(path):
    with open(path, encoding="utf-8") as fh:
        return fh.readlines()


def write_lines(path, lines):
    text = "".join(lines)
    yaml.safe_load(text)  # must still parse — fail loudly before writing otherwise
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(text)


def load_spine(path, tier):
    doc = load_yaml(path)
    problems = []
    for key in ("results_file", "sha", "timestamp", "tests"):
        if not doc.get(key):
            problems.append(f"{path}: spine missing {key}")
    for entry in doc.get("unbound") or []:
        problems.append(f"{path}: unbound test {entry} (spine must bind every test)")
    doc["tier"] = tier
    doc["path"] = path
    return doc, problems


def plan(ir_dir, spines):
    """Return (per-claim plan, conflicts, warnings). A plan entry:
    {claim, target, attestations: [att...]}."""
    manifest = load_yaml(os.path.join(ir_dir, "ir-manifest.yaml"))
    known = {c["id"] for c in manifest.get("claims", [])}
    green, red, warnings = {}, {}, []
    for spine in spines:
        att = {"tier": spine["tier"], "evidence": spine["results_file"],
               "sha": spine["sha"], "date": str(spine["timestamp"])}
        for t in spine["tests"]:
            claim = t.get("claim")
            if claim not in known:
                warnings.append(f"{spine['path']}: unknown claim {claim} (test {t.get('test')})")
                continue
            if t.get("outcome") == "passed":
                green.setdefault(claim, []).append(att)
            else:
                red.setdefault(claim, []).append(
                    f"{t.get('test')} {t.get('outcome')} in {spine.get('suite')} ({spine['path']})")
    conflicts = {c: red[c] for c in red}
    entries = []
    for claim, atts in sorted(green.items()):
        if claim in conflicts:
            continue
        # dedup attestations while keeping order; target = strongest green tier
        unique = []
        for a in atts:
            if a not in unique:
                unique.append(a)
        target = max((a["tier"] for a in unique), key=lambda t: STATUS_ORDER[t])
        entries.append({"claim": claim, "target": target, "attestations": unique})
    return entries, conflicts, warnings


def set_claim_status_line(lines, status):
    for i, line in enumerate(lines):
        if line.startswith("status:"):
            lines[i] = f"status: {status}\n"
            return True
    return False


def strip_top_level_block(lines, key):
    """Remove the top-level `key:` block. Comment lines (any indent) inside the
    block continue it; they are returned separately so callers can re-emit them
    (atom files are human-reviewed — comments must survive a rewrite)."""
    out, comments, skipping = [], [], False
    for line in lines:
        if line.startswith(f"{key}:"):
            skipping = True
            continue
        if skipping and line[:1] not in (" ", "\t", "\n", "", "#"):
            skipping = False
        if skipping:
            if line.lstrip().startswith("#"):
                comments.append(line)
        else:
            out.append(line)
    return out, comments


def attestation_block(atts):
    block = ["attestations:\n"]
    for a in atts:
        block.append(f"  - tier: {a['tier']}\n")
        block.append(f"    evidence: {a['evidence']}\n")
        block.append(f"    sha: {a['sha']}\n")
        block.append(f"    date: \"{a['date']}\"\n")
    return block


def apply_claim(ir_dir, entry, dry_run):
    """Returns (change descriptions, post-promotion status) — the status is the
    one the claim file will carry, so dry-run can report manifest edits too."""
    cid = entry["claim"]
    path = os.path.join(ir_dir, "claims", cid + ".yaml")
    claim = load_yaml(path)
    current = claim.get("status", "extracted")
    changes = []
    new_status = current
    if STATUS_ORDER[entry["target"]] > STATUS_ORDER.get(current, 0):
        new_status = entry["target"]
        changes.append(f"{cid}: status {current} -> {new_status}")
    existing = claim.get("attestations") or []
    to_add = [a for a in entry["attestations"] if a not in existing]
    for a in to_add:
        changes.append(f"{cid}: + attestation tier={a['tier']} sha={a['sha'][:12]} "
                       f"date={a['date']} evidence={a['evidence']}")
    if not changes or dry_run:
        return changes, new_status
    lines = read_lines(path)
    if new_status != current and not set_claim_status_line(lines, new_status):
        raise RuntimeError(f"{cid}: no top-level status: line found")
    if to_add:
        lines, comments = strip_top_level_block(lines, "attestations")
        while lines and lines[-1].strip() == "":
            lines.pop()
        if lines and not lines[-1].endswith("\n"):
            lines[-1] += "\n"
        lines += ["attestations:\n"] + comments + attestation_block(existing + to_add)[1:]
    write_lines(path, lines)
    check = load_yaml(path)
    assert check.get("status") == new_status and (check.get("attestations") or []) == existing + to_add, \
        f"{cid}: post-write verification failed"
    return changes, new_status


def sync_manifest(ir_dir, dry_run, planned=None):
    """Make the manifest claim-index status lines match the claim files.
    `planned` maps claim id -> post-promotion status, so a dry-run reports the
    manifest edits the real run would make (claim files are untouched then)."""
    planned = planned or {}
    manifest_path = os.path.join(ir_dir, "ir-manifest.yaml")
    lines = read_lines(manifest_path)
    changes = []
    in_claims = in_target = False
    target_status = cid = None
    for i, line in enumerate(lines):
        if line.startswith("claims:"):
            in_claims = True
            continue
        if in_claims and line[:1] not in (" ", "\n", "#", ""):
            in_claims = False
        if not in_claims:
            continue
        stripped = line.strip()
        if stripped.startswith("- id: "):
            cid = stripped.removeprefix("- id: ").strip()
            if cid in planned:
                target_status = planned[cid]
            else:
                claim = load_yaml(os.path.join(ir_dir, "claims", cid + ".yaml"))
                target_status = claim.get("status")
            in_target = True
            continue
        if in_target and stripped.startswith("status: "):
            current = stripped.removeprefix("status: ").strip()
            if target_status and current != target_status:
                indent = line[:len(line) - len(line.lstrip())]
                changes.append(f"manifest: {cid}: {current} -> {target_status} "
                               f"(claim file is authoritative)")
                if not dry_run:
                    lines[i] = f"{indent}status: {target_status}\n"
            in_target = False
    if changes and not dry_run:
        write_lines(manifest_path, lines)
    return changes


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--ir", required=True)
    parser.add_argument("--pinned-from", action="append", default=[])
    parser.add_argument("--verified-from", action="append", default=[])
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    spines, problems = [], []
    for path in args.pinned_from:
        spine, errs = load_spine(path, "pinned")
        spines.append(spine)
        problems += errs
    for path in args.verified_from:
        spine, errs = load_spine(path, "verified")
        spines.append(spine)
        problems += errs
    if problems:
        for p in problems:
            print(f"ERROR {p}")
        return 1
    if not spines:
        print("ERROR no evidence supplied (--pinned-from/--verified-from)")
        return 1

    entries, conflicts, warnings = plan(args.ir, spines)
    for w in warnings:
        print(f"WARN {w}")

    total_changes, planned = [], {}
    for entry in entries:
        changes, new_status = apply_claim(args.ir, entry, args.dry_run)
        total_changes += changes
        planned[entry["claim"]] = new_status
    total_changes += sync_manifest(args.ir, args.dry_run, planned)

    label = "DRY-RUN " if args.dry_run else ""
    if total_changes:
        print(f"=== {label}promotion changes ({len(total_changes)}) ===")
        for c in total_changes:
            print(f"  {c}")
    else:
        print(f"=== {label}no changes (already promoted — idempotent no-op) ===")

    if conflicts:
        print(f"=== NOT promoted — red evidence ({len(conflicts)}) ===")
        for claim in sorted(conflicts):
            for reason in conflicts[claim]:
                print(f"  {claim}: {reason}")
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
