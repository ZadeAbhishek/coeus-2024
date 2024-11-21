# Project: Coeus

## Description
This project is designed to perform indexing and querying using Apache Lucene and evaluate results using `trec_eval`.

### Compile Trec Eval
```bash
cd trec_eval-9.0.7
```
```bash
make
```
```bash
make quicktest
```

### Grant Permission
```bash
 chmod +x run.sh
```
### Run 
```bash
  ./run.sh BM25 CUSTOM 
```