#!/usr/bin/env bash

# Variables
JAR_FILE="target/coeus-1.0-SNAPSHOT.jar"
TREC_EVAL_DIR="./trec_eval-9.0.7"
QRELS_FILE="./qrels"
TOPICS_FILE="./topics.401-450"
RESULTS_FILE="./queryResults"
DOCUMENTS_DIR="./Documents"
ZIP_FILE="downloaded_file.zip"
QRELS_DOWNLOAD_URL="https://learn-eu-central-1-prod-fleet01-xythos.content.blackboardcdn.com/62b985ffa0afc/7043706?X-Blackboard-S3-Bucket=learn-eu-central-1-prod-fleet01-xythos&X-Blackboard-Expiration=1732212000000&X-Blackboard-Signature=SiXxf%2BXT4LbfgH5rqpHGmQAQ5QQU1tYNVZHgn7SV12E%3D&X-Blackboard-Client-Id=300200&X-Blackboard-S3-Region=eu-central-1&response-cache-control=private%2C%20max-age%3D21600&response-content-disposition=inline%3B%20filename%2A%3DUTF-8%27%27qrels.assignment2.part1&response-content-type=application%2Foctet-stream&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEAsaDGV1LWNlbnRyYWwtMSJGMEQCIH1HmxI1jVbChgI9QHrcGiMSWjQk%2FaVVpH5UOD4jlf7CAiA%2FYsRh5NcT41UW296eyUZKH4j8MZme9OKrUvL7PjG0SirHBQil%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F8BEAQaDDYzNTU2NzkyNDE4MyIMHwJsR8TvM4ATAIXOKpsFMBbcxtX12%2FLRdK%2Bpu9To3qH2%2BDTUMTsu%2FLFj50x8%2FRqvNq9R7vrWzUrPiefpjOAloTK6s6wLl5aUaEVJiKfoM7kSfXX49VHNTNKwP6WpVvBqTHbxc9POg44A9TwAu5uh5fnGNQKm%2Fa6nic9tqO1J3FNbDukGWKU%2FqiwDH19mXR6IPg71HTOncVZrgBn8uR0H1JqxD9ohidq94KVjYshKoR5xsiE0MfUIguZmS3P84xKsxXg0Uwo7o%2F3khXnkXJldQj0s4pTJJcAxj5BsYnXsGDfj31XynpC9VHUG%2F5X%2BAgwZjNQPZNl%2BdOoqNkmatNH7YV5pZ7Vrmrtp2Qnrgusq3rIFUMny8ycAmIaZD01K6%2BkIILLepr5uVNmk3cCNiJEvglV%2F7ph%2BNBxe3jkAsIswjuCYuJAD7oxt5vm0%2FgfVQ%2FogP9zF5z9%2FpTEcLiqe3qqLgyZc9REPlu3kIkFU%2BS%2FilqGvXDDfPrN7bit9cbpG4H3He9%2Bhu5ho%2FEZ%2BI9ogPXItLfxuKwwfFLtySB2OX0z1VZ1EglmpjVqY1ht3HMYNDcnuyR%2FH0hOdC20ZQ5XnV%2B96KLY8D7%2FVEjHbpKF8Pjk62wGDt4fd0ieOYpMSSTAbFb5gkidTHh1mWizNG8jDdcs7LfJdaT965J1vkytUqgz32h2Y1cXZT%2FT%2F%2Bi9lFMQ2s7%2FOvQ8r5iIOAWV5dc4w7pUbQMbazUwQmZakeKF2v%2BMl3fLm7zSLR%2BEx%2Bv%2FrszGy13UDljvJetwfR8EwKm4SXGGWOuUIURCnAbjco%2BmwJjkokX57JFjVR3r1DMeeAISxms%2B1PFa7S80mLReuScFmT2WoFp7Dr50YhgnssyQXbBQ0MRrPTbcigxs8JPTaKmH0kvKji4ySbd5fQnHqPzDUsPy5BjqyAW%2F9xf1RvitjH%2FmTWVSPmVHk%2FgrTtnVtL0zps2j5SOts5f4k9DwrBTaqKUnyHusdAwTlHAxBf6ISwYcW545qt2LFgX56oPeXjO1htm4fVRqK8Y7Z1QqA3Z%2FwTrjMoYb6XMKGpK2dzzzkWBKZ7%2FCCYYbqgUgmNasnSxK0zMQmUXNtiiRNTgkfePLkP9UWFGHUWwlgoa3i8hdP0yDdfPGP%2BAUh4jP3OpkZI0GvrJ5i5sorqqk%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20241121T120000Z&X-Amz-SignedHeaders=host&X-Amz-Expires=21600&X-Amz-Credential=ASIAZH6WM4PLTGWUCKWC%2F20241121%2Feu-central-1%2Fs3%2Faws4_request&X-Amz-Signature=425de5109acb0aa0131bd2e69ab38065e42f4263d6df7490ef59ebf6a8c9f2b7"
ZIP_DOWNLOAD_URL="https://drive.usercontent.google.com/download?id=17KpMCaE34eLvdiTINqj1lmxSBSu8BtDP&export=download&authuser=0&confirm=t&uuid=d917ae3e-4708-4095-addd-36f30015871e&at=AENtkXZ5eH3PwXBOVrHnKsTEkYdH%3A1732190524221"
TOPICS_DOWNLOAD_URL="https://drive.usercontent.google.com/u/0/uc?id=1CaCtA2RHhW4DP--5HyHnKm9jjqyWvc99&export=download"

# Function to check exit status and handle errors
check_exit_status() {
    if [ $? -ne 0 ]; then
        echo "$1 failed. Exiting."
        exit 1
    fi
}

# OS Detection and Dependency Installation
install_dependencies() {
    echo "Detecting OS and checking dependencies..."
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        echo "Linux detected."
        if ! command -v curl &> /dev/null; then
            echo "Installing curl..."
            sudo apt update && sudo apt install curl -y || {
                echo "Failed to install curl. Exiting."
                exit 1
            }
        fi
        if ! command -v unzip &> /dev/null; then
            echo "Installing unzip..."
            sudo apt install unzip -y || {
                echo "Failed to install unzip. Exiting."
                exit 1
            }
        fi
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        echo "macOS detected."
        if ! command -v curl &> /dev/null; then
            echo "Installing curl via Homebrew..."
            brew install curl || {
                echo "Failed to install curl. Exiting."
                exit 1
            }
        fi
        if ! command -v unzip &> /dev/null; then
            echo "Installing unzip via Homebrew..."
            brew install unzip || {
                echo "Failed to install unzip. Exiting."
                exit 1
            }
        fi
    elif [[ "$OSTYPE" == "cygwin" ]] || [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "win32" ]]; then
        echo "Windows detected. Please ensure curl and unzip are installed manually."
        exit 1
    else
        echo "Unsupported OS: $OSTYPE. Please ensure dependencies are installed manually."
        exit 1
    fi
}

# Step 1: Download qrels file
download_qrels() {
    if [ -f "$QRELS_FILE" ]; then
        echo "qrels file already exists. Skipping download."
    else
        echo "Downloading qrels file..."
        curl -L "$QRELS_DOWNLOAD_URL" -o "$QRELS_FILE"
        check_exit_status "Downloading qrels file with curl"
    fi
}

# Step 2: Download topics file
download_topics_file() {
    if [ -f "$TOPICS_FILE" ]; then
        echo "topics.401-450 file already exists. Skipping download."
    else
        echo "Downloading topics file..."
        curl -L "$TOPICS_DOWNLOAD_URL" -o "$TOPICS_FILE"
        check_exit_status "Downloading topics file with curl"
    fi
}

# Step 3: Download ZIP file
download_zip_file() {
    if [ -d "$DOCUMENTS_DIR" ]; then
        echo "Documents directory already exists. Skipping ZIP file download and extraction."
    else
        echo "Downloading ZIP file..."
        curl -L "$ZIP_DOWNLOAD_URL" -o "$ZIP_FILE"
        check_exit_status "Downloading ZIP file with curl"

        # Validate file type
        if file "$ZIP_FILE" | grep -q "Zip archive data"; then
            echo "Valid ZIP file downloaded."
            extract_zip
        else
            echo "Downloaded file is not a valid ZIP file. Inspecting content..."
            head -n 10 "$ZIP_FILE"
            echo "Exiting due to invalid ZIP file."
            exit 1
        fi
    fi
}

# Step 4: Extract ZIP file
extract_zip() {
    echo "Extracting ZIP file..."
    if [ -f "$ZIP_FILE" ]; then
        mkdir -p "$DOCUMENTS_DIR"
        unzip "$ZIP_FILE" -d "$DOCUMENTS_DIR"
        check_exit_status "Extracting ZIP file"
        echo "Cleaning up ZIP file..."
        rm -f "$ZIP_FILE"
    else
        echo "ZIP file not found: $ZIP_FILE. Exiting."
        exit 1
    fi
}

# Step 5: Compile the Maven project
compile_maven_project() {
    echo "Compiling the Maven project..."
    mvn clean compile
    check_exit_status "Compilation"
}

# Step 6: Package the Maven project
package_maven_project() {
    echo "Packaging the Maven project..."
    mvn package
    check_exit_status "Packaging"
}

# Step 7: Run the JAR file with parameters
run_jar_file() {
    echo "Running the JAR file with 512MB heap size..."
    if [ -f "$JAR_FILE" ]; then
        if [ -z "$1" ] || [ -z "$2" ]; then
            echo "Error: Missing parameters for JAR execution."
            echo "Usage: ./run.sh <ranking_model> <analyzer>"
            exit 1
        fi
        java -Xmx512M -jar "$JAR_FILE" "$1" "$2"
        check_exit_status "JAR execution"
    else
        echo "JAR file not found: $JAR_FILE. Exiting."
        exit 1
    fi
}

# Step 8: Run trec_eval
run_trec_eval() {
    echo "Running trec_eval..."
    if [ -d "$TREC_EVAL_DIR" ] && [ -x "$TREC_EVAL_DIR/trec_eval" ] && [ -f "$QRELS_FILE" ] && [ -f "$RESULTS_FILE" ]; then
        "$TREC_EVAL_DIR/trec_eval" "$QRELS_FILE" "$RESULTS_FILE"
        check_exit_status "trec_eval execution"
    else
        echo "Required files or directories for trec_eval are missing or not executable. Check:"
        echo " - Directory exists and contains trec_eval: $TREC_EVAL_DIR"
        echo " - Qrels file exists: $QRELS_FILE"
        echo " - Query results file exists: $RESULTS_FILE"
        exit 1
    fi
}

# Main Script Execution
install_dependencies
download_qrels
download_topics_file
download_zip_file
compile_maven_project
package_maven_project
run_jar_file "$1" "$2"
run_trec_eval

echo "Script completed successfully."