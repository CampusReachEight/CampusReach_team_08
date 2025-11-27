import os
import json
import argparse
import xml.etree.ElementTree as ET
from glob import glob

def load_state(state_file):
    if os.path.exists(state_file):
        with open(state_file, 'r') as f:
            return json.load(f)
    return {"tests": {}}

def save_state(state_file, state):
    os.makedirs(os.path.dirname(state_file), exist_ok=True)
    with open(state_file, 'w') as f:
        json.dump(state, f, indent=2)

def update_state_from_xml(state, xml_dir):
    # Find all JUnit XML files
    xml_files = glob(os.path.join(xml_dir, "**/*.xml"), recursive=True)

    for xml_file in xml_files:
        try:
            tree = ET.parse(xml_file)
            root = tree.getroot()

            # Handle standard JUnit XML structure
            testcases = root.findall(".//testcase")
            for tc in testcases:
                classname = tc.get("classname")
                name = tc.get("name")

                if not classname or not name:
                    continue

                test_id = f"{classname}#{name}"

                # Check for failure or error tags
                failure = tc.find("failure")
                error = tc.find("error")

                status = "failed" if (failure is not None or error is not None) else "passed"

                # Update state: Overwrite previous status with new status
                state["tests"][test_id] = status
        except Exception as e:
            print(f"Warning: Could not parse {xml_file}: {e}")

    return state

def get_gradle_args(state):
    # If state is empty, it implies a fresh run (Run All)
    if not state["tests"]:
        return "RUN_ALL"

    failed_tests = [tid for tid, status in state["tests"].items() if status == "failed"]

    if not failed_tests:
        # State exists but no failures? All passed.
        return "NONE"

    # Format for Android Instrumentation runner:
    # -Pandroid.testInstrumentationRunnerArguments.class=com.pkg.Class#method,com.pkg.Class#method
    return ",".join(failed_tests)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=["get_args", "update_state"])
    parser.add_argument("--state-file", required=True, help="Path to JSON state file")
    parser.add_argument("--xml-dir", help="Path to directory containing JUnit XMLs (for update_state)")

    args = parser.parse_args()

    state = load_state(args.state_file)

    if args.mode == "get_args":
        result = get_gradle_args(state)
        print(result)

    elif args.mode == "update_state":
        if not args.xml_dir:
            raise ValueError("--xml-dir is required for update_state")
        updated_state = update_state_from_xml(state, args.xml_dir)
        save_state(args.state_file, updated_state)
        print(f"State updated. Total tracked tests: {len(updated_state['tests'])}")

if __name__ == "__main__":
    main()