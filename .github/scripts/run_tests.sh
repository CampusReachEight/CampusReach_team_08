#!/bin/bash
set -e

echo "----------------------------------------------------------------"
echo "STARTING MERGED TEST RUNNER"
echo "----------------------------------------------------------------"

# Ensure state directory exists
mkdir -p .state

# --- PHASE 1: UNIT TESTS ---
echo ""
echo ">> [1/2] Calculating UNIT TEST Filter..."

# Check if state file exists, if not, it's a fresh run -> RUN_ALL
if [ ! -f .state/test-status.json ]; then
    echo ">> No previous state found. Defaulting to RUN_ALL."
    UNIT_FILTER="RUN_ALL"
else
    UNIT_FILTER=$(python3 .github/scripts/manage_test_state.py get_args --state-file .state/test-status.json --suite unit)
fi

echo ">> Unit Filter: $UNIT_FILTER"

if [ "$UNIT_FILTER" == "NONE" ]; then
  echo ">> ✅ All Unit Tests previously passed. Skipping."
elif [ "$UNIT_FILTER" == "RUN_ALL" ]; then
  echo ">> 🔄 Running ALL Unit Tests..."
  ./gradlew testDebugUnitTest --build-cache --configuration-cache
else
  echo ">> ⚠️ Rerunning FAILED Unit Tests..."
  # Use eval to handle quotes in --tests arguments
  eval "./gradlew testDebugUnitTest $UNIT_FILTER --build-cache --configuration-cache"
fi

# Update State for Unit Tests
if [ -d "app/build/test-results/testDebugUnitTest" ]; then
    python3 .github/scripts/manage_test_state.py update_state \
      --state-file .state/test-status.json \
      --xml-dir app/build/test-results/testDebugUnitTest \
      --suite unit
else
    echo ">> ⚠️ No Unit Test results found to parse."
fi


# --- PHASE 2: ANDROID TESTS ---
echo ""
echo ">> [2/2] Calculating ANDROID Filter..."

# Re-check state file (it might have been created by Unit phase)
if [ ! -f .state/test-status.json ]; then
     # This is highly unlikely since we just ran Unit tests, but safety first.
     ANDROID_FILTER="RUN_ALL"
else
    ANDROID_FILTER=$(python3 .github/scripts/manage_test_state.py get_args --state-file .state/test-status.json --suite android)
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
if [ -d "app/build/outputs/androidTest-results/connected" ]; then
    python3 .github/scripts/manage_test_state.py update_state \
      --state-file .state/test-status.json \
      --xml-dir app/build/outputs/androidTest-results/connected \
      --suite android
else
    echo ">> ⚠️ No Android Test results found to parse."
fi

echo "----------------------------------------------------------------"
echo "TEST RUNNER COMPLETE"
echo "----------------------------------------------------------------"