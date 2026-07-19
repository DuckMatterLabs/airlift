#!/usr/bin/env bash
# Installs the Airlift tax harness into the OFBiz working copy (idempotent).
set -euo pipefail

HARNESS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$HARNESS_DIR/../target.env"

ACC="$TARGET_REPO/applications/accounting"

# 1. Harness sources -> test sourceset
mkdir -p "$ACC/src/test/java"
rsync -a --delete "$HARNESS_DIR/src/org/apache/ofbiz/accounting/tax/test/" \
    "$ACC/src/test/java/org/apache/ofbiz/accounting/tax/test/" \
    --exclude "backfill/" --exclude "blind/"
mkdir -p "$ACC/src/test/java/org/apache/ofbiz/accounting/tax/test/backfill" \
         "$ACC/src/test/java/org/apache/ofbiz/accounting/tax/test/blind"

# 2. Test suite definitions
cp "$HARNESS_DIR/testdef/"airlift*.xml "$ACC/testdef/"

# 3. Register suites in ofbiz-component.xml (idempotent)
COMPONENT="$ACC/ofbiz-component.xml"
for suite in airliftsmoketests airliftbackfilltests airliftblindtests; do
  if ! grep -q "testdef/$suite.xml" "$COMPONENT"; then
    sed -i '' "s|<test-suite loader=\"main\" location=\"testdef/ratetests.xml\"/>|<test-suite loader=\"main\" location=\"testdef/ratetests.xml\"/>\n    <test-suite loader=\"main\" location=\"testdef/$suite.xml\"/>|" "$COMPONENT"
  fi
done

echo "harness installed into $ACC"
