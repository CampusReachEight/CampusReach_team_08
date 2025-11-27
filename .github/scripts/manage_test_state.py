import os
import json
import argparse
import xml.etree.ElementTree as ET
from glob import glob

def load_state(state_file):
    if os.path.exists(state_file):
        with open(state_file, 'r') as f:
            try:
                return json.load(f)
            except json.JSONDecodeError:
                return {"tests": {}}
    return {"tests": {}}

def save_state(state_file, state):
    directory = os.path.dirname(state_file)
    if directory:
        os.makedirs(directory, exist_ok=True)
    with open(state_file, 'w') as f:
        json.dump(state, f, indent=2)

def update_state_from_xml(state, xml_dir):
    xml_files = glob(os.path.join(xml_dir, "**/*.xml"), recursive=True)
    if not xml_files:
        print(f"No XML files found in {xml_dir}")
        return state

    for xml_file in xml_files:
        try:
            tree = ET.parse(xml_file)
            root = tree.getroot()
            testcases = root.findall(".//testcase")
            for tc in testcases:
                classname = tc.get("classname")
                name = tc.get("name")
                if not classname or not name:
                    continue

                test_id = f"{classname}#{name}"
                # Check for failure/error
                failure = tc.find("failure")
                error = tc.find("error")
                status = "failed" if (failure is not None or error is not None) else "passed"

                # Update state
                state["tests"][test_id] = status
        except Exception as e:
            print(f"Warning: Could not parse {xml_file}: {e}")
    return state

def get_gradle_args(state):
    # No state = First Run = Run All
    if not state.get("tests"):
        return "RUN_ALL"

    failed_tests = [tid for tid, status in state["tests"].items() if status == "failed"]

    # State exists but no failures = All passed
    if not failed_tests:
        return "NONE"

    # Gradle format: -P...class=com.A#m1,com.B#m2
    return ",".join(failed_tests)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=["get_args", "update_state"])
    parser.add_argument("--state-file", required=True)
    parser.add_argument("--xml-dir")

    args = parser.parse_args()
    state = load_state(args.state_file)

    if args.mode == "get_args":
        print(get_gradle_args(state))
    elif args.mode == "update_state":
        if not args.xml_dir:
            raise ValueError("--xml-dir is required")
        updated_state = update_state_from_xml(state, args.xml_dir)
        save_state(args.state_file, updated_state)
        print(f"State updated. Tracked {len(updated_state['tests'])} tests.")

if __name__ == "__main__":
    main()