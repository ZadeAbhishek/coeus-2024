#!/usr/bin/env bash

# Variables
JAR_FILE="target/coeus-1.0-SNAPSHOT.jar"
TREC_EVAL_URL="https://trec.nist.gov/trec_eval/trec_eval-9.0.7.tar.gz"
TREC_EVAL_DIR="./trec_eval-9.0.7"
TOPICS_FILE="./topics.401-450"
RESULTS_FILE="./queryResults"
DOCUMENTS_DIR="./Documents"
ZIP_FILE="downloaded_file.zip"
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
        if ! command -v tar &> /dev/null; then
            echo "Installing tar..."
            sudo apt install tar -y || {
                echo "Failed to install tar. Exiting."
                exit 1
            }
        fi
        if ! command -v make &> /dev/null; then
            echo "Installing make..."
            sudo apt install build-essential -y || {
                echo "Failed to install make. Exiting."
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
        if ! command -v make &> /dev/null; then
            echo "Installing make via Homebrew..."
            brew install make || {
                echo "Failed to install make. Exiting."
                exit 1
            }
        fi
    else
        echo "Unsupported OS: $OSTYPE. Please ensure dependencies are installed manually."
        exit 1
    fi
}

# Step 1: Download and compile trec_eval
download_and_compile_trec_eval() {
    if [ -d "$TREC_EVAL_DIR" ]; then
        echo "trec_eval directory already exists. Skipping download and compilation."
    else
        echo "Downloading and compiling trec_eval..."
        curl -L "$TREC_EVAL_URL" -o trec_eval.tar.gz
        check_exit_status "Downloading trec_eval with curl"
        tar -xzf trec_eval.tar.gz
        check_exit_status "Extracting trec_eval tarball"
        cd "$TREC_EVAL_DIR" || exit
        make
        check_exit_status "Compiling trec_eval with make"
        cd ..
        rm -f trec_eval.tar.gz
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
        echo "Extracting ZIP file..."
        mkdir -p "$DOCUMENTS_DIR"
        unzip "$ZIP_FILE" -d "$DOCUMENTS_DIR"
        check_exit_status "Extracting ZIP file"
        rm -f "$ZIP_FILE"
    fi
}

# Step 4: Compile the Maven project
compile_maven_project() {
    echo "Compiling the Maven project..."
    mvn clean compile
    check_exit_status "Compilation"
}

# Step 5: Package the Maven project
package_maven_project() {
    echo "Packaging the Maven project..."
    mvn package
    check_exit_status "Packaging"
}

# Step 6: Run the JAR file with parameters
run_jar_file() {
    echo "Running the JAR file with 512MB heap size..."
    if [ -f "$JAR_FILE" ]; then
        if [ -z "$1" ] || [ -z "$2" ]; then
            echo "Error: Missing parameters for JAR execution."
            echo "Usage: ./run.sh <ranking_model> <analyzer> <qrels_path>"
            exit 1
        fi
        java -Xmx512M -jar "$JAR_FILE" "$1" "$2"
        check_exit_status "JAR execution"
    else
        echo "JAR file not found: $JAR_FILE. Exiting."
        exit 1
    fi
}

# Step 7: Run trec_eval
run_trec_eval() {
    echo "Running trec_eval..."
    local qrels_path="$1"
    if [ -z "$qrels_path" ]; then
        echo "Error: qrels file path not provided."
        echo "Usage: ./run.sh <ranking_model> <analyzer> <qrels_path>"
        exit 1
    fi
    if [ -d "$TREC_EVAL_DIR" ] && [ -x "$TREC_EVAL_DIR/trec_eval" ] && [ -f "$qrels_path" ] && [ -f "$RESULTS_FILE" ]; then
        "$TREC_EVAL_DIR/trec_eval" "$qrels_path" "$RESULTS_FILE"
        check_exit_status "trec_eval execution"
    else
        echo "Required files or directories for trec_eval are missing or not executable. Check:"
        echo " - Directory exists and contains trec_eval: $TREC_EVAL_DIR"
        echo " - Qrels file exists: $qrels_path"
        echo " - Query results file exists: $RESULTS_FILE"
        exit 1
    fi
}

# Main Script Execution
install_dependencies
download_and_compile_trec_eval
download_topics_file
download_zip_file
compile_maven_project
package_maven_project
run_jar_file "$1" "$2"
run_trec_eval "$3"

echo "Script completed successfully."