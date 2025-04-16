#!/bin/bash

# Enhance script robustness
set -euo pipefail

# Define the list of databases
Test_Database=("mysql" "mariadb" "oceanbase" "postgres" "opengauss" "tdsql")

# Define the function to stop a screen session
stop_screen_session() {
    local session_name="$1"

    # Check if the session exists
    if screen -list | grep -q "\.${session_name}[[:space:]]"; then
        echo "Terminating screen session '$session_name'..."
        
        # Terminate the screen session
        screen -S "$session_name" -X quit
        
        # Wait briefly to ensure the session has terminated
        sleep 1

        # Verify termination
        if screen -list | grep -q "\.${session_name}[[:space:]]"; then
            echo "Error: Failed to terminate screen session '$session_name'."
            return 1
        else
            echo "Success: Screen session '$session_name' terminated."
            return 0
        fi
    else
        echo "Info: Screen session '$session_name' does not exist. Skipping."
        return 0
    fi
}

# Iterate over each database type
for database in "${Test_Database[@]}"; do
    echo "Processing database type: '$database'"

    # Set isolation levels based on the database type
    if [[ "$database" == "mysql" || "$database" == "mariadb" || "$database" == "postgres" || "$database" == "tdsql" ]]; then
        Test_Isolation=("serializable" "repeatable_read" "read_committed")
    elif [[ "$database" == "oceanbase" || "$database" == "opengauss" ]]; then
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
        
        # Log the termination action
        echo "Terminating session: '$session_name'"

        # Stop the screen session
        stop_screen_session "$session_name"
    done
done

echo "All applicable screen sessions have been terminated."
