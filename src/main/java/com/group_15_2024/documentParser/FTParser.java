package com.group_15_2024.documentParser;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FTParser {

    private static boolean headlineFlag = false, textFlag = false;

    public static void loadFinTimesDocs(String baseDirPath, IndexWriter writer) throws IOException {
        File baseDir = new File(baseDirPath);
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            throw new IOException("Invalid directory: " + baseDirPath);
        }

        List<File> allFiles = getAllFilesRecursively(baseDir);
        System.out.println("Found " + allFiles.size() + " files to process in " + baseDirPath);

        for (File file : allFiles) {
            try {
                processFile(file, writer);
            } catch (Exception e) {
                System.err.println("Error processing file: " + file.getAbsolutePath() + " - " + e.getMessage());
            }
        }

        System.out.println("Successfully indexed Financial Times documents.");
    }

    private static void processFile(File file, IndexWriter writer) throws IOException {
        if (!file.canRead()) {
            System.err.println("Cannot read file: " + file.getAbsolutePath());
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String currLine;
            FinTimesObject finTimesObject = null;

            while ((currLine = br.readLine()) != null) {
                currLine = currLine.trim();

                if (currLine.contains(FinTimesTags.DOC_START.getTag())) {
                    finTimesObject = new FinTimesObject(); // Start a new document
                } else if (currLine.contains(FinTimesTags.DOC_END.getTag())) {
                    if (finTimesObject != null) {
                        writer.addDocument(createNewFinTimesDoc(finTimesObject)); // Add document to IndexWriter
                        finTimesObject = null; // Clear the object to free memory
                    }
                } else if (finTimesObject != null) {
                    setFinTimesObjData(currLine, finTimesObject);
                }
            }

            writer.commit(); // Commit changes after processing the file
        }
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
            if (headlineFlag && currLine.length() > 0) {
                finTimesObject.appendHeadline(currLine);
            } else if (textFlag && currLine.length() > 0) {
                finTimesObject.appendText(currLine);
            }
        }
    }

    private static String parseFinTimesDoc(String currLine, FinTimesTags startTag, FinTimesTags endTag) {
        return currLine.replace(startTag.getTag(), "")
                       .replace(endTag.getTag(), "")
                       .trim();
    }

    private static Document createNewFinTimesDoc(FinTimesObject finTimesObject) {
        Document document = new Document();
        document.add(new StringField("docno", finTimesObject.getDocNo() != null ? finTimesObject.getDocNo() : "", Field.Store.YES));
        document.add(new TextField("headline", finTimesObject.getHeadline() != null ? finTimesObject.getHeadline() : "", Field.Store.YES));
        document.add(new TextField("text", finTimesObject.getText() != null ? finTimesObject.getText() : "", Field.Store.YES));
        return document;
    }

    private static List<File> getAllFilesRecursively(File baseDir) {
        List<File> fileList = new ArrayList<>();
        if (baseDir.isDirectory()) {
            for (File file : baseDir.listFiles()) {
                if (file.isDirectory()) {
                    fileList.addAll(getAllFilesRecursively(file));
                } else {
                    fileList.add(file);
                }
            }
        } else if (baseDir.isFile()) {
            fileList.add(baseDir);
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