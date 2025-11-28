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

def update_state_from_xml(state, xml_dir, suite_name):
    # Recursively find all xml files
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
                # Unique ID
                test_id = f"{classname}#{name}"

                # Determine Status
                failure = tc.find("failure")
                error = tc.find("error")
                status = "failed" if (failure is not None or error is not None) else "passed"

                # Update state with Suite info
                # Note: We persist the status. If it was previously recorded, we overwrite it.
                state["tests"][test_id] = {
                    "status": status,
                    "suite": suite_name
                }
        except Exception as e:
            print(f"Warning: Could not parse {xml_file}: {e}")

    print(f">> Parsed {found_count} test results for suite '{suite_name}'.")
    return state

def get_gradle_args(state, target_suite):
    # Filter memory for tests belonging to this suite
    suite_tests = {k: v for k, v in state["tests"].items() if v.get("suite") == target_suite}

    # If we have NO record of tests for this suite, it means we've never run them.
    # So we must RUN_ALL.
    if not suite_tests:
        return "RUN_ALL"

    # Find failed tests for this specific suite
    failed_tests = [tid for tid, data in suite_tests.items() if data.get("status") == "failed"]

    # If we have history, but no failures, we can skip.
    if not failed_tests:
        return "NONE"

    if target_suite == "android":
        # Android Format: com.package.Class#method
        return ",".join(failed_tests)
    else:
        # Unit Test Format: --tests "com.package.Class.method"
        # Convert '#' to '.' because testDebugUnitTest uses dots for methods
        return " ".join([f'--tests "{t.replace("#", ".")}"' for t in failed_tests])

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=["get_args", "update_state"])
    parser.add_argument("--state-file", required=True)
    parser.add_argument("--xml-dir")
    parser.add_argument("--suite", choices=["unit", "android"], required=True)

    args = parser.parse_args()

    state = load_state(args.state_file)

    if args.mode == "get_args":
        print(get_gradle_args(state, args.suite))

    elif args.mode == "update_state":
        if not args.xml_dir:
            raise ValueError("--xml-dir required for update_state")
        updated = update_state_from_xml(state, args.xml_dir, args.suite)
        save_state(args.state_file, updated)
        print(f">> State updated for suite '{args.suite}'. Database now holds {len(updated['tests'])} total tests (Unit + Android).")

if __name__ == "__main__":
    main()