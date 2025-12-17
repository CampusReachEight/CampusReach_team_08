#!/bin/bash
set -e

echo "----------------------------------------------------------------"
echo "STARTING TEST RUNNER"
echo "----------------------------------------------------------------"

# Track exit codes
UNIT_EXIT_CODE=0
ANDROID_EXIT_CODE=0

# --- PHASE 1: UNIT TESTS ---
echo ""
echo ">> [1/2] Running Unit Tests..."

set +e  # Temporarily disable exit on error
./gradlew testDebugUnitTest --build-cache --configuration-cache --stacktrace
UNIT_EXIT_CODE=$?
set -e  # Re-enable exit on error

if [ $UNIT_EXIT_CODE -eq 0 ]; then
    echo ">> ✅ Unit Tests passed."
else
    echo ">> ❌ Unit Tests failed with exit code $UNIT_EXIT_CODE"
fi

# --- PHASE 2: ANDROID TESTS ---
echo ""
echo ">> [2/2] Running Android Tests..."

set +e  # Temporarily disable exit on error
./gradlew createDebugCoverageReport --build-cache --configuration-cache --stacktrace --parallel
ANDROID_EXIT_CODE=$?
set -e  # Re-enable exit on error

if [ $ANDROID_EXIT_CODE -eq 0 ]; then
    echo ">> ✅ Android Tests passed."
else
    echo ">> ❌ Android Tests failed with exit code $ANDROID_EXIT_CODE"
fi

# Debug: Show where coverage files were generated
echo ""
echo ">> 📊 Coverage Files Generated:"
echo ">> Unit Test Coverage (.exec):"
find app/build -name "*.exec" -type f 2>/dev/null || echo "   No .exec files found"
echo ">> Android Test Coverage (.ec):"
find app/build -name "*.ec" -type f 2>/dev/null || echo "   No .ec files found"
echo ""

# Exit with appropriate code
echo ""
echo "----------------------------------------------------------------"
echo "TEST RUNNER COMPLETE"
echo "----------------------------------------------------------------"
echo ">> Unit Tests Exit Code: $UNIT_EXIT_CODE"
echo ">> Android Tests Exit Code: $ANDROID_EXIT_CODE"
echo "----------------------------------------------------------------"

# Fail if either test suite failed
if [ $UNIT_EXIT_CODE -ne 0 ]; then
    echo ">> ❌ FAILED: Unit tests failed"
    exit $UNIT_EXIT_CODE
fi

if [ $ANDROID_EXIT_CODE -ne 0 ]; then
    echo ">> ❌ FAILED: Android tests failed"
    exit $ANDROID_EXIT_CODE
fi

echo ">> ✅ SUCCESS: All tests passed"
exit 0
