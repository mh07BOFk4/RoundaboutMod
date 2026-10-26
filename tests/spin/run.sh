#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
test_output=$(mktemp -d)
trap 'rm -rf "$test_output"' EXIT
javac -d "$test_output" common/src/main/java/net/hydra/jojomod/item/SpinCharge.java tests/spin/SpinChargeTest.java
java -cp "$test_output" SpinChargeTest
