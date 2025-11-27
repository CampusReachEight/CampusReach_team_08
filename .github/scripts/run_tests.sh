#!/bin/bash
set -e

# Print the filter for debugging
echo ">> Executing Tests with Filter: $TEST_FILTER"

if [ "$TEST_FILTER" == "RUN_ALL" ]; then
  echo ">> Mode: FULL SUITE"
  ./gradlew connectedCheck --parallel --build-cache
else
  echo ">> Mode: PARTIAL RERUN"
  echo ">> Target Classes: $TEST_FILTER"
  ./gradlew connectedCheck -Pandroid.testInstrumentationRunnerArguments.class=$TEST_FILTER --parallel --build-cache
fi
