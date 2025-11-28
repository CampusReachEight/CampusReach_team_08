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
    # Recursively find all xml files in the Android results directory
    xml_files = glob(os.path.join(xml_dir, "**/*.xml"), recursive=True)

    found_count = 0
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

                found_count += 1
                test_id = f"{classname}#{name}"

                failure = tc.find("failure")
                error = tc.find("error")
                status = "failed" if (failure is not None or error is not None) else "passed"

                # We only track Android tests individually
                state["tests"][test_id] = {
                    "status": status,
                    "suite": "android"
                }
        except Exception as e:
            print(f"Warning: Could not parse {xml_file}: {e}")

    print(f">> Parsed {found_count} Android test results.")
    return state

def get_android_args(state):
    # Filter for Android tests only
    android_tests = {k: v for k, v in state["tests"].items() if v.get("suite") == "android"}

    # If no Android tests recorded, it's a fresh run -> RUN_ALL
    if not android_tests:
        return "RUN_ALL"

    # Find failed tests
    failed_tests = [tid for tid, data in android_tests.items() if data.get("status") == "failed"]

    # If history exists but no failures, we can skip Android tests (NONE)
    if not failed_tests:
        return "NONE"

    # Return comma-separated list of failed tests
    return ",".join(failed_tests)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=["get_args", "update_state"])
    parser.add_argument("--state-file", required=True)
    parser.add_argument("--xml-dir")
    # Removed --suite argument as we simplified the logic

    args = parser.parse_args()

    state = load_state(args.state_file)

    if args.mode == "get_args":
        print(get_android_args(state))

    elif args.mode == "update_state":
        if not args.xml_dir:
            raise ValueError("--xml-dir required for update_state")
        updated = update_state_from_xml(state, args.xml_dir)
        save_state(args.state_file, updated)
        print(f">> State updated. Database now holds {len(updated['tests'])} Android tests.")

if __name__ == "__main__":
    main()