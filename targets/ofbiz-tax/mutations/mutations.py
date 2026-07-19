#!/usr/bin/env python3
"""Deliberate bug kit for exit criterion E2 (and E4).

Each mutation is a small, realistic defect applied to the seam's primary source file via
exact-string replacement. Revert restores the file from git (requires a committed baseline).

Usage:
  mutations.py list
  mutations.py apply <ID>
  mutations.py revert
"""
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))


def target_env():
    env = {}
    with open(os.path.join(HERE, "..", "target.env"), encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, _, value = line.partition("=")
                env[key] = os.path.expandvars(value.strip('"').strip("'"))
    env["SEAM_PRIMARY_FILE"] = env["SEAM_PRIMARY_FILE"].replace(
        "$TARGET_REPO", env["TARGET_REPO"])
    return env


# id -> (old, new, description, expected violated claim area)
MUTATIONS = {
    "M1-exempt-flip": (
        'if ("Y".equals(partyTaxInfo.getString("isExempt"))) {',
        'if ("N".equals(partyTaxInfo.getString("isExempt"))) {',
        "Exemption test inverted: exempt parties charged, non-exempt zeroed",
        "TAX.EXEMPT",
    ),
    "M2-min-price-boundary": (
        "EntityOperator.LESS_THAN_EQUAL_TO, itemPrice)));",
        "EntityOperator.LESS_THAN, itemPrice)));",
        "Minimum-item-price threshold made exclusive: item exactly at minimum no longer taxed",
        "TAX.RATE",
    ),
    "M3-item-amount-untaxed": (
        "taxable = taxable.add(itemAmount);",
        "taxable = taxable.add(BigDecimal.ZERO);",
        "Item amount dropped from taxable base",
        "TAX.BASE",
    ),
    "M4-shipping-untaxed": (
        "taxable = taxable.add(shippingAmount);",
        "taxable = taxable.add(BigDecimal.ZERO);",
        "Shipping never added to taxable base",
        "TAX.SHIP",
    ),
    "M5-rollup-direction": (
        'billToPartyIdSet.add(partyRelationship.getString("partyIdFrom"));',
        'billToPartyIdSet.add(partyRelationship.getString("partyIdTo"));',
        "Group-membership rollup walks the wrong direction: group exemptions ignored",
        "TAX.EXEMPT",
    ),
    "M6-inheritance-cut": (
        '"taxAuthorityAssocTypeId", "EXEMPT_INHER")',
        '"taxAuthorityAssocTypeId", "NO_SUCH_ASSOC")',
        "Exemption inheritance association never found: parent-jurisdiction exemptions ignored",
        "TAX.EXEMPT",
    ),
    "M7-exempt-amount-zero": (
        'adjValue.set("exemptAmount", taxAmount);',
        'adjValue.set("exemptAmount", BigDecimal.ZERO);',
        "Forgone tax no longer recorded on exempt adjustments",
        "TAX.EXEMPT",
    ),
}


def main():
    cmd = sys.argv[1]
    env = target_env()
    path = env["SEAM_PRIMARY_FILE"]
    repo = env["TARGET_REPO"]

    if cmd == "list":
        for mid, (_o, _n, desc, area) in MUTATIONS.items():
            print(f"{mid}: {desc} [expect: {area}]")
        return 0

    if cmd == "revert":
        subprocess.run(["git", "-C", repo, "checkout", "--", path], check=True)
        print("reverted")
        return 0

    if cmd == "apply":
        mid = sys.argv[2]
        old, new, desc, area = MUTATIONS[mid]
        with open(path, encoding="utf-8") as fh:
            text = fh.read()
        count = text.count(old)
        if count != 1:
            print(f"FATAL: mutation {mid} target string occurs {count} times (need exactly 1)",
                  file=sys.stderr)
            return 2
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(text.replace(old, new))
        print(f"applied {mid}: {desc} [expect: {area}]")
        return 0

    print(f"unknown command {cmd}", file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main())
