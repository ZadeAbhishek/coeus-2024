# Project: Coeus

## Description
This project is designed to perform indexing and querying using Apache Lucene and evaluate results using `trec_eval`.

## Grant Permission
To grant executable permission to the script:
```bash
chmod +x run.sh
```

## Run Everything at Once
To run the entire process (indexing, querying, and evaluation):
```bash
./run.sh BM25 CUSTOM ./qrels
```

## Steps to Perform Manually

### Step 1: Download Data and Load
Grant executable permission and run the download script:
```bash
chmod +x download.sh
./download.sh
```

### Step 2: Run a Java File
To run the Java application with the specified parameters:
```bash
java -Xmx512M -jar target/coeus-1.0-SNAPSHOT.jar BM25 CUSTOM
```

### Step 3: Run Evaluation
To evaluate the results using `trec_eval`:
```bash
./trec_eval/trec_eval <qrels_file_path> <results_file_path>
```
