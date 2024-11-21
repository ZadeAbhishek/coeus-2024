package com.group_15_2024.documentParser;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FTParser {

    private static boolean headlineFlag = false, textFlag = false;
    private static final int BATCH_COMMIT_SIZE = 100; // Number of documents to process before committing

    public static void loadFinTimesDocs(String baseDirPath, IndexWriter writer) throws IOException {
        File baseDir = new File(baseDirPath);
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            throw new IOException("Invalid directory: " + baseDirPath);
        }

        List<File> allFiles = getAllFilesRecursively(baseDir);
        System.out.println("Found " + allFiles.size() + " files to process in " + baseDirPath);

        int documentCount = 0;

        for (File file : allFiles) {
            try {
                int docsInFile = processFile(file, writer);
                documentCount += docsInFile;

                // Commit in batches to optimize performance
                if (documentCount % BATCH_COMMIT_SIZE == 0) {
                    writer.commit();
                    System.out.println("Committed " + documentCount + " documents so far.");
                }
            } catch (Exception e) {
                System.err.println("Error processing file: " + file.getAbsolutePath() + " - " + e.getMessage());
            }
        }

        // Final commit for any remaining documents
        writer.commit();
        System.out.println("Successfully indexed " + documentCount + " Financial Times documents.");
    }

    private static int processFile(File file, IndexWriter writer) throws IOException {
        if (!file.canRead()) {
            System.err.println("Cannot read file: " + file.getAbsolutePath());
            return 0;
        }

        int documentCount = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String currLine;
            FinTimesObject finTimesObject = new FinTimesObject();

            while ((currLine = br.readLine()) != null) {
                currLine = currLine.trim();

                if (currLine.isEmpty()) continue;

                if (currLine.contains(FinTimesTags.DOC_START.getTag())) {
                    finTimesObject.clear(); // Start a new document
                } else if (currLine.contains(FinTimesTags.DOC_END.getTag())) {
                    writer.addDocument(createNewFinTimesDoc(finTimesObject));
                    documentCount++;
                    finTimesObject.clear(); // Reset for the next document
                } else {
                    setFinTimesObjData(currLine, finTimesObject);
                }
            }
        }

        return documentCount;
    }

    private static void setFinTimesObjData(String currLine, FinTimesObject finTimesObject) {
        if (currLine.contains(FinTimesTags.DOC_NO_START.getTag())) {
            finTimesObject.setDocNo(parseFinTimesDoc(currLine, FinTimesTags.DOC_NO_START, FinTimesTags.DOC_NO_END));
        } else if (currLine.contains(FinTimesTags.HEADLINE_START.getTag())) {
            headlineFlag = true;
        } else if (currLine.contains(FinTimesTags.HEADLINE_END.getTag())) {
            headlineFlag = false;
        } else if (currLine.contains(FinTimesTags.TEXT_START.getTag())) {
            textFlag = true;
        } else if (currLine.contains(FinTimesTags.TEXT_END.getTag())) {
            textFlag = false;
        } else {
            if (headlineFlag) {
                finTimesObject.appendHeadline(currLine);
            } else if (textFlag) {
                finTimesObject.appendText(currLine);
            }
        }
    }

    private static String parseFinTimesDoc(String currLine, FinTimesTags startTag, FinTimesTags endTag) {
        return currLine.replace(startTag.getTag(), "").replace(endTag.getTag(), "").trim();
    }

    private static Document createNewFinTimesDoc(FinTimesObject finTimesObject) {
        Document document = new Document();

        if (finTimesObject.getDocNo() != null && !finTimesObject.getDocNo().isEmpty()) {
            document.add(new StringField("docno", finTimesObject.getDocNo(), Field.Store.YES));
        }

        if (finTimesObject.getHeadline() != null && !finTimesObject.getHeadline().isEmpty()) {
            document.add(new TextField("headline", finTimesObject.getHeadline(), Field.Store.YES));
        }

        if (finTimesObject.getText() != null && !finTimesObject.getText().isEmpty()) {
            document.add(new TextField("text", finTimesObject.getText(), Field.Store.YES));
        }

        return document;
    }

    private static List<File> getAllFilesRecursively(File baseDir) {
        List<File> fileList = new ArrayList<>();
        File[] files = baseDir.listFiles();
        if (files == null) return fileList;

        for (File file : files) {
            if (file.isDirectory()) {
                fileList.addAll(getAllFilesRecursively(file));
            } else if (file.isFile() && file.canRead()) {
                fileList.add(file);
            }
        }

        return fileList;
    }

    private static class FinTimesObject {
        private String docNo = "";
        private StringBuilder headline = new StringBuilder();
        private StringBuilder text = new StringBuilder();

        public String getDocNo() {
            return docNo;
        }

        public void setDocNo(String docNo) {
            this.docNo = docNo;
        }

        public String getHeadline() {
            return headline.toString().trim();
        }

        public void appendHeadline(String headlinePart) {
            this.headline.append(" ").append(headlinePart);
        }

        public String getText() {
            return text.toString().trim();
        }

        public void appendText(String textPart) {
            this.text.append(" ").append(textPart);
        }

        public void clear() {
            docNo = "";
            headline.setLength(0);
            text.setLength(0);
        }
    }

    private enum FinTimesTags {
        TEXT_START("<TEXT>"), TEXT_END("</TEXT>"),
        HEADLINE_START("<HEADLINE>"), HEADLINE_END("</HEADLINE>"),
        DOC_NO_START("<DOCNO>"), DOC_NO_END("</DOCNO>"),
        DOC_START("<DOC>"), DOC_END("</DOC>");

        private final String tag;

        FinTimesTags(final String tag) {
            this.tag = tag;
        }

        public String getTag() {
            return this.tag;
        }
    }
}