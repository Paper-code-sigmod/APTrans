#!/bin/bash

# Enhance script robustness
set -euo pipefail

# Define the list of databases
Test_Database=("mysql" "mariadb" "oceanbase")
# Test_Database=("tdsql")

# Define the function to start a screen session
start_screen_session() {
    local session_name="$1"
    local command="$2"

    # Check if both parameters are provided
    if [ -z "$session_name" ] || [ -z "$command" ]; then
        echo "Error: start_screen_session function requires two parameters: session_name and command."
        return 1
    fi

    # Check if a screen session with the same name already exists
    if screen -list | grep -q "\.${session_name}[[:space:]]"; then
        echo "Warning: A screen session named '$session_name' already exists. Terminating it..."
        
        # Terminate the existing screen session
        screen -S "$session_name" -X quit
        
        # Wait briefly to ensure the session has terminated
        sleep 1

        # Verify termination
        if screen -list | grep -q "\.${session_name}[[:space:]]"; then
            echo "Error: Failed to terminate existing screen session '$session_name'."
            return 1
        else
            echo "Info: Existing screen session '$session_name' terminated successfully."
        fi
    fi

    # Start the screen session and execute the command
    screen -S "$session_name" -dm bash -c "$command; exec bash"

    # Verify if the screen session was successfully started
    if screen -list | grep -q "\.${session_name}[[:space:]]"; then
        echo "Success: Started screen session '$session_name' and executed the command."
        return 0
    else
        echo "Error: Failed to start screen session '$session_name'."
        return 1
    fi
}

# Iterate over each database type
for database in "${Test_Database[@]}"; do
    echo "Processing database type: '$database'"

    # Set isolation levels based on the database type
    if [[ "$database" == "mysql" || "$database" == "mariadb" || "$database" == "postgres" || "$database" == "tdsql" || "$database" == "oceanbase" ]]; then
        Test_Isolation=("serializable" "repeatable_read" "read_committed")
    elif [[ "$database" == "opengauss" ]]; then
        Test_Isolation=("repeatable_read" "read_committed")
    else
        echo "Warning: Unknown database type '$database', skipping."
        continue
    fi

    # Set sample_type based on the database type
    if [[ "$database" == "mysql" || "$database" == "mariadb" || "$database" == "oceanbase" ]]; then
        sample_type="mysql"
    elif [[ "$database" == "postgres" || "$database" == "opengauss" || "$database" == "tdsql" ]]; then
        sample_type="pg"
    else
        echo "Warning: Unknown database type '$database', skipping."
        continue
    fi

    # Iterate over each isolation level
    for isolation in "${Test_Isolation[@]}"; do
        # Correct variable assignment (no spaces around =)
        session_name="${database}_${isolation}"
        execute_script="execute_${sample_type}.sh"

        # Check if the execute script exists and is executable
        if [ ! -x "${execute_script}" ]; then
            echo "Error: Script '${execute_script}' does not exist or is not executable."
            exit 1
        fi

        # Define the command to execute with logging (capture both stdout and stderr)
        command="bash ${execute_script} -i ${isolation} -d ${database} > log/${session_name}.log"

        # Inform about the current session and command
        echo "Starting session: '$session_name', executing command: '$command'"

        # Start the screen session
        start_screen_session "$session_name" "$command"
    done
done

echo "All tasks have been initiated."
