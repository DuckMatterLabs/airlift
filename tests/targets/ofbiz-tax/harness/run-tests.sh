#!/usr/bin/env bash
# Runs one Airlift test suite inside the OFBiz container.
#   run-tests.sh <suitename>          (airliftsmoke | airliftbackfill | airliftblind)
# Exit code: 0 = suite green, 1 = failures/errors, 2 = infrastructure failure.
# JUnit XML report: $TEST_RESULTS_DIR/<suitename>.xml
set -uo pipefail

HARNESS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HARNESS_DIR/../target.env"

SUITE="$1"
REPORT="$TEST_RESULTS_DIR/$SUITE.xml"
rm -f "$REPORT"
mkdir -p "$TEST_RESULTS_DIR"

cd "$TARGET_REPO"
./gradlew "ofbiz --test component=accounting --test suitename=$SUITE" --console=plain
GRADLE_EXIT=$?

if [ ! -f "$REPORT" ]; then
  echo "run-tests: no report produced at $REPORT (boot/compile failure?)" >&2
  exit 2
fi

python3 - "$REPORT" <<'EOF'
import sys
import xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
tests = int(root.get("tests", 0))
failures = int(root.get("failures", 0))
errors = int(root.get("errors", 0))
print(f"suite={sys.argv[1].rsplit('/',1)[-1]} tests={tests} failures={failures} errors={errors}")
sys.exit(0 if failures == 0 and errors == 0 and tests > 0 else 1)
EOF
PARSE_EXIT=$?
if [ "$PARSE_EXIT" -ne 0 ] || [ "$GRADLE_EXIT" -ne 0 ]; then
  exit 1
fi
exit 0
