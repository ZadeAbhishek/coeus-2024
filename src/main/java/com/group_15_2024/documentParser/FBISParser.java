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

    /**
     * Parses and indexes FBIS documents from the specified directory.
     *
     * @param fbisDirectory The path to the FBIS directory.
     * @param writer        The IndexWriter to write parsed documents to.
     * @throws IOException If there is an issue reading files or directories.
     */
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

    /**
     * Determines if a file should be ignored based on its name.
     *
     * @param fileName The name of the file.
     * @return True if the file should be ignored, false otherwise.
     */
    private static boolean shouldIgnoreFile(String fileName) {
        for (String ignoreFile : IGNORE_FILES) {
            if (fileName.equalsIgnoreCase(ignoreFile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Processes a single document and writes it to the IndexWriter.
     *
     * @param br     The BufferedReader to read the document.
     * @param writer The IndexWriter to write the parsed document to.
     * @throws IOException If there is an issue reading or writing the document.
     */
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

        writer.commit(); // Final commit
        if (!skippedDocs.isEmpty()) {
            System.err.println("Skipped documents:");
            skippedDocs.forEach(System.err::println);
        }
    }

    /**
     * Extracts and trims data from an element based on the given tag.
     *
     * @param doc The element to extract data from.
     * @param tag The FBISTags to look for.
     * @return The trimmed data, or null if the tag is not found.
     */
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

    /**
     * Removes nested tags from the specified elements.
     *
     * @param elements The elements to process.
     * @param currTag  The current FBISTags being processed.
     */
    private static void removeNestedTags(Elements elements, FBISTags currTag) {
        for (FBISTags tag : FBISTags.values()) {
            if (!tag.equals(currTag)) {
                elements.select(tag.getTagName()).remove();
            }
        }
    }

    /**
     * Creates a Lucene Document from the extracted FBIS data.
     *
     * @param fbisData The FBIS data to use.
     * @return A Lucene Document containing the parsed fields.
     */
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

    /**
     * Reads the entire content of a file using a BufferedReader.
     *
     * @param br The BufferedReader to read from.
     * @return The content of the file as a String.
     * @throws IOException If there is an issue reading the file.
     */
    private static String readFile(BufferedReader br) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    /**
     * Class representing FBIS data.
     */
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

    /**
     * Enum representing FBIS tags.
     */
    enum FBISTags {
        HEADER, TEXT, ABS, AU, DATE1, DOC, DOCNO, H1, H2, H3, H4, H5, H6, H7, H8, HT, TR, TXT5, TI;

        public String getTagName() {
            return name().toLowerCase();
        }
    }
}