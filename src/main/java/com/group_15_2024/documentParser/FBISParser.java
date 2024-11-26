package com.group_15_2024.documentParser;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FBISParser {

    private static final String[] IGNORE_FILES = {"readchg.txt", "readmefb.txt"};
    public static void loadFBISDocs(String fbisDirectory, IndexWriter writer) throws IOException {
        Path dir = Paths.get(fbisDirectory);
        if (!Files.isDirectory(dir)) {
            throw new IOException("FBIS directory not found: " + fbisDirectory);
        }

        List<String> skippedFiles = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(Files::isRegularFile)
                 .filter(file -> !shouldIgnoreFile(file.getFileName().toString()))
                 .forEach(file -> {
                     try (BufferedReader br = Files.newBufferedReader(file)) {
                         processDocument(br, writer);
                     } catch (IOException e) {
                         skippedFiles.add(file.getFileName() + ": " + e.getMessage());
                     }
                 });
        }

        if (!skippedFiles.isEmpty()) {
            System.err.println("Skipped files:");
            skippedFiles.forEach(System.err::println);
        }
    }
    private static boolean shouldIgnoreFile(String fileName) {
        for (String ignoreFile : IGNORE_FILES) {
            if (fileName.equalsIgnoreCase(ignoreFile)) {
                return true;
            }
        }
        return false;
    }

    private static void processDocument(BufferedReader br, IndexWriter writer) throws IOException {
        String fileContent = readFile(br);
        org.jsoup.nodes.Document document = Jsoup.parse(fileContent);
        Elements docElements = document.getElementsByTag("DOC");
        List<String> skippedDocs = new ArrayList<>();

        int batchCounter = 0;

        for (Element doc : docElements) {
            try {
                FBISData fbisData = new FBISData();
                fbisData.setDocNum(trimData(doc, FBISTags.DOCNO));
                fbisData.setTi(trimData(doc, FBISTags.TI));
                fbisData.setText(trimData(doc, FBISTags.TEXT));

                if (fbisData.getDocNum() == null) {
                    skippedDocs.add("Document without DOCNO");
                    continue;
                }

                writer.addDocument(createFBISDocument(fbisData));
                batchCounter++;

                if (batchCounter % 100 == 0) { // Commit every 100 documents
                    writer.commit();
                }
            } catch (Exception e) {
                skippedDocs.add("Error processing document: " + e.getMessage());
            }
        }

        writer.commit();
        if (!skippedDocs.isEmpty()) {
            System.err.println("Skipped documents:");
            skippedDocs.forEach(System.err::println);
        }
    }

    private static String trimData(Element doc, FBISTags tag) {
        Elements elements = doc.getElementsByTag(tag.getTagName());
        if (elements.isEmpty()) {
            return null;
        }
        Elements tmpElements = elements.clone();
        removeNestedTags(tmpElements, tag);
        String data = tmpElements.text();
        return data != null ? data.trim() : null;
    }

    private static void removeNestedTags(Elements elements, FBISTags currTag) {
        for (FBISTags tag : FBISTags.values()) {
            if (!tag.equals(currTag)) {
                elements.select(tag.getTagName()).remove();
            }
        }
    }

    private static Document createFBISDocument(FBISData fbisData) {
        Document document = new Document();
        document.add(new StringField("docno", fbisData.getDocNum(), Field.Store.YES));
        if (fbisData.getTi() != null) {
            document.add(new TextField("headline", fbisData.getTi(), Field.Store.YES));
        }
        if (fbisData.getText() != null) {
            document.add(new TextField("text", fbisData.getText(), Field.Store.YES));
        }
        return document;
    }

    private static String readFile(BufferedReader br) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    static class FBISData {
        private String docNum, ti, text;

        public String getDocNum() {
            return docNum;
        }

        public void setDocNum(String docNum) {
            this.docNum = docNum;
        }

        public String getTi() {
            return ti;
        }

        public void setTi(String ti) {
            this.ti = ti;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    enum FBISTags {
        HEADER, TEXT, ABS, AU, DATE1, DOC, DOCNO, H1, H2, H3, H4, H5, H6, H7, H8, HT, TR, TXT5, TI;

        public String getTagName() {
            return name().toLowerCase();
        }
    }
}