#!/bin/bash
set -e

echo "----------------------------------------------------------------"
echo "STARTING MERGED TEST RUNNER"
echo "----------------------------------------------------------------"

# Ensure state directory exists
mkdir -p .state

# Track exit codes
UNIT_EXIT_CODE=0
ANDROID_EXIT_CODE=0

# --- PHASE 1: UNIT TESTS ---
# Run unit tests, but capture exit code to allow Android tests to run
echo ""
echo ">> [1/2] Running ALL Unit Tests..."
set +e  # Temporarily disable exit on error
./gradlew testDebugUnitTest --build-cache --configuration-cache
UNIT_EXIT_CODE=$?
set -e  # Re-enable exit on error

if [ $UNIT_EXIT_CODE -eq 0 ]; then
    echo ">> ✅ Unit Tests passed."
else
    echo ">> ❌ Unit Tests failed with exit code $UNIT_EXIT_CODE"
fi

# Update Unit Test State (Checkpoint 2)
echo ""
echo ">> Updating Unit Test State..."
if [ -d "app/build/test-results/testDebugUnitTest" ]; then
    set +e
    python3 .github/scripts/manage_test_state.py update_state \
      --state-file .state/unit-test-status.json \
      --xml-dir app/build/test-results/testDebugUnitTest \
      --suite unit
    UNIT_STATE_UPDATE_EXIT=$?
    set -e

    if [ $UNIT_STATE_UPDATE_EXIT -eq 0 ]; then
        echo ">> ✅ Unit test state updated."
    else
        echo ">> ⚠️ Unit test state update encountered issues (exit code $UNIT_STATE_UPDATE_EXIT)."
    fi
else
    echo ">> ⚠️ No unit test results found to parse."
fi

# Continue to Android tests regardless of unit test result.

# --- PHASE 2: ANDROID TESTS ---
echo ""
echo ">> [2/2] Calculating ANDROID Filter..."

# Check if state file exists
if [ ! -f .state/test-status.json ]; then
    echo ">> No previous state found. Defaulting to RUN_ALL."
    ANDROID_FILTER="RUN_ALL"
else
    # Calculate filter based on previous Android results
    ANDROID_FILTER=$(python3 .github/scripts/manage_test_state.py get_args --state-file .state/test-status.json)
fi

echo ">> Android Filter: $ANDROID_FILTER"

if [ "$ANDROID_FILTER" == "NONE" ]; then
  echo ">> ✅ All Android Tests previously passed. Skipping."
  ANDROID_EXIT_CODE=0
elif [ "$ANDROID_FILTER" == "RUN_ALL" ]; then
  echo ">> 🔄 Running ALL Android Tests..."
  set +e  # Temporarily disable exit on error
  ./gradlew connectedCheck --parallel --build-cache --configuration-cache
  ANDROID_EXIT_CODE=$?
  set -e  # Re-enable exit on error
else
  echo ">> ⚠️ Rerunning FAILED Android Tests..."
  # Pass comma-separated list directly
  set +e  # Temporarily disable exit on error
  ./gradlew connectedCheck -Pandroid.testInstrumentationRunnerArguments.class="$ANDROID_FILTER" --parallel --build-cache --configuration-cache
  ANDROID_EXIT_CODE=$?
  set -e  # Re-enable exit on error
fi

if [ $ANDROID_EXIT_CODE -eq 0 ]; then
    echo ">> ✅ Android Tests passed."
else
    echo ">> ❌ Android Tests failed with exit code $ANDROID_EXIT_CODE"
fi

# Update State for Android Tests
# Always attempt to update state, even if tests failed
echo ""
echo ">> Updating Android Test State..."
if [ -d "app/build/outputs/androidTest-results/connected" ]; then
    set +e  # Don't fail if state update has issues
    python3 .github/scripts/manage_test_state.py update_state \
      --state-file .state/test-status.json \
      --xml-dir app/build/outputs/androidTest-results/connected
    STATE_UPDATE_EXIT=$?
    set -e

    if [ $STATE_UPDATE_EXIT -eq 0 ]; then
        echo ">> ✅ State updated successfully."
    else
        echo ">> ⚠️ State update encountered issues (exit code $STATE_UPDATE_EXIT)."
    fi
else
    echo ">> ⚠️ No Android Test results found to parse."
fi

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
