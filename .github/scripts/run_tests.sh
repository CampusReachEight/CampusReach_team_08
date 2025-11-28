#!/bin/bash
set -e

echo "----------------------------------------------------------------"
echo "STARTING MERGED TEST RUNNER"
echo "----------------------------------------------------------------"

# Ensure state directory exists
mkdir -p .state

# --- PHASE 1: UNIT TESTS ---
# We treat Unit Tests as a single block. Always run them.
# If they fail, the job fails here, and we fix them.
echo ""
echo ">> [1/2] Running ALL Unit Tests..."
./gradlew testDebugUnitTest --build-cache --configuration-cache

# Note: We do NOT update the JSON state for unit tests.
# If unit tests pass, we proceed to Android.

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
elif [ "$ANDROID_FILTER" == "RUN_ALL" ]; then
  echo ">> 🔄 Running ALL Android Tests..."
  ./gradlew connectedCheck --parallel --build-cache --configuration-cache
else
  echo ">> ⚠️ Rerunning FAILED Android Tests..."
  # Pass comma-separated list directly
  ./gradlew connectedCheck -Pandroid.testInstrumentationRunnerArguments.class="$ANDROID_FILTER" --parallel --build-cache --configuration-cache
fi

# Update State for Android Tests
# We run this even if tests failed (because 'set -e' might stop the script,
# but the CI workflow 'if: always()' step will handle the final upload/update if we split it out.
# Ideally, we want this script to update state if it ran.
if [ -d "app/build/outputs/androidTest-results/connected" ]; then
    python3 .github/scripts/manage_test_state.py update_state \
      --state-file .state/test-status.json \
      --xml-dir app/build/outputs/androidTest-results/connected
else
    echo ">> ⚠️ No Android Test results found to parse."
fi

echo "----------------------------------------------------------------"
echo "TEST RUNNER COMPLETE"
echo "----------------------------------------------------------------"