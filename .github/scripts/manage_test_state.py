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
    # Recursively find all xml files
    xml_files = glob(os.path.join(xml_dir, "**/*.xml"), recursive=True)

    if not xml_files:
        print(f"No XML files found in {xml_dir}")
        return state

    for xml_file in xml_files:
        try:
            tree = ET.parse(xml_file)
            root = tree.getroot()

            # Standard JUnit XML format
            testcases = root.findall(".//testcase")
            for tc in testcases:
                classname = tc.get("classname")
                name = tc.get("name")

                if not classname or not name:
                    continue

                # Construct unique ID: com.example.MyTest#testMethod
                test_id = f"{classname}#{name}"

                # Check for failure or error child tags
                failure = tc.find("failure")
                error = tc.find("error")

                status = "failed" if (failure is not None or error is not None) else "passed"

                # Update map: "passed" overwrites "failed", "failed" overwrites "passed"
                # This ensures the state reflects the MOST RECENT run of that specific test
                state["tests"][test_id] = status
        except Exception as e:
            print(f"Warning: Could not parse {xml_file}: {e}")

    return state

def get_gradle_args(state):
    # If state is empty, we have no history -> First Run -> Run All
    if not state.get("tests"):
        return "RUN_ALL"

    # Filter for tests that are NOT passed (failed)
    failed_tests = [tid for tid, status in state["tests"].items() if status == "failed"]

    if not failed_tests:
        # State exists, but no failures found -> All passed?
        # Return NONE so we can skip execution
        return "NONE"

    # Join with commas for Gradle: -Pandroid...class=com.A#m1,com.B#m2
    return ",".join(failed_tests)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=["get_args", "update_state"])
    parser.add_argument("--state-file", required=True, help="Path to JSON state file")
    parser.add_argument("--xml-dir", help="Path to directory containing JUnit XMLs (required for update_state)")

    args = parser.parse_args()

    state = load_state(args.state_file)

    if args.mode == "get_args":
        result = get_gradle_args(state)
        print(result)

    elif args.mode == "update_state":
        if not args.xml_dir:
            raise ValueError("--xml-dir is required for update_state mode")
        updated_state = update_state_from_xml(state, args.xml_dir)
        save_state(args.state_file, updated_state)
        print(f"State updated. Tracked {len(updated_state['tests'])} tests.")

if __name__ == "__main__":
    main()