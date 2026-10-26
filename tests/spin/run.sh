#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
test_output=$(mktemp -d)
trap 'rm -rf "$test_output"' EXIT
javac -d "$test_output" common/src/main/java/net/hydra/jojomod/item/SpinCharge.java common/src/main/java/net/hydra/jojomod/fates/powers/SpinProgression.java tests/spin/SpinChargeTest.java tests/spin/SpinProgressionTest.java common/src/main/java/net/hydra/jojomod/entity/projectile/SpinProjectileRules.java tests/spin/SpinProjectileRulesTest.java
java -cp "$test_output" SpinChargeTest
java -cp "$test_output" SpinProgressionTest
java -cp "$test_output" SpinProjectileRulesTest
