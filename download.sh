#!/usr/bin/env bash

# Variables
ZIP_FILE="downloaded_file.zip"
ZIP_DOWNLOAD_URL="https://drive.usercontent.google.com/download?id=17KpMCaE34eLvdiTINqj1lmxSBSu8BtDP&export=download&authuser=0&confirm=t&uuid=d917ae3e-4708-4095-addd-36f30015871e&at=AENtkXZ5eH3PwXBOVrHnKsTEkYdH%3A1732190524221"
DOCUMENTS_DIR="./Documents"

TREC_EVAL_URL="https://trec.nist.gov/trec_eval/trec_eval-9.0.7.tar.gz"
TREC_EVAL_DIR="./trec_eval-9.0.7"

# Function to check exit status
check_exit_status() {
    if [ $? -ne 0 ]; then
        echo "$1 failed. Exiting."
        exit 1
    fi
}

# Install required dependencies
install_dependencies() {
    echo "Checking dependencies..."
    if ! command -v curl &> /dev/null; then
        echo "Installing curl..."
        sudo apt update && sudo apt install -y curl || {
            echo "Failed to install curl. Exiting."
            exit 1
        }
    fi
    if ! command -v unzip &> /dev/null; then
        echo "Installing unzip..."
        sudo apt install -y unzip || {
            echo "Failed to install unzip. Exiting."
            exit 1
        }
    fi
    if ! command -v tar &> /dev/null; then
        echo "Installing tar..."
        sudo apt install -y tar || {
            echo "Failed to install tar. Exiting."
            exit 1
        }
    fi
    if ! command -v make &> /dev/null; then
        echo "Installing build-essential..."
        sudo apt install -y build-essential || {
            echo "Failed to install build-essential. Exiting."
            exit 1
        }
    fi
}

# Download and extract the large file
download_and_extract_zip() {
    if [ -d "$DOCUMENTS_DIR" ]; then
        echo "Documents directory already exists. Skipping download and extraction."
    else
        echo "Downloading large ZIP file..."
        curl -L "$ZIP_DOWNLOAD_URL" -o "$ZIP_FILE"
        check_exit_status "Downloading ZIP file"
        
        echo "Extracting ZIP file to $DOCUMENTS_DIR..."
        mkdir -p "$DOCUMENTS_DIR"
        unzip "$ZIP_FILE" -d "$DOCUMENTS_DIR"
        check_exit_status "Extracting ZIP file"
        
        echo "Cleaning up ZIP file..."
        rm -f "$ZIP_FILE"
    fi
}

# Download and compile trec_eval
download_and_compile_trec_eval() {
    if [ -d "$TREC_EVAL_DIR" ]; then
        echo "trec_eval already exists. Skipping download and compilation."
    else
        echo "Downloading and compiling trec_eval..."
        curl -L "$TREC_EVAL_URL" -o trec_eval.tar.gz
        check_exit_status "Downloading trec_eval"
        tar -xzf trec_eval.tar.gz
        check_exit_status "Extracting trec_eval"
        cd "$TREC_EVAL_DIR" || exit
        make
        check_exit_status "Compiling trec_eval"
        cd ..
        rm -f trec_eval.tar.gz
    fi
}

# Main script execution
install_dependencies
download_and_extract_zip
download_and_compile_trec_eval

echo "Large file downloaded and extracted successfully."
echo "trec_eval downloaded and compiled successfully."