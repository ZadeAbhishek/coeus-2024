package com.group_15_2024.documentParser;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LATParser {

    /**
     * Loads and parses LA Times documents from the specified directory and writes them to the IndexWriter.
     *
     * @param pathToLATimesRegister The path to the directory containing LA Times files.
     * @param writer                The IndexWriter to which documents are added.
     * @throws IOException If there is an issue reading files from the directory.
     */
    public static void loadLaTimesDocs(String pathToLATimesRegister, IndexWriter writer) throws IOException {
        File folder = new File(pathToLATimesRegister);
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IOException("Invalid directory: " + pathToLATimesRegister);
        }

        File[] listOfFiles = folder.listFiles(File::isFile);
        if (listOfFiles == null || listOfFiles.length == 0) {
            System.out.println("No files found in directory: " + pathToLATimesRegister);
            return;
        }

        int totalDocuments = 0;

        for (File file : listOfFiles) {
            try {
                int docsParsed = parseLATimesFile(file, writer);
                totalDocuments += docsParsed;
            } catch (Exception e) {
                System.err.println("Error processing file: " + file.getName() + " - " + e.getMessage());
            }
        }

        System.out.println("Successfully indexed " + totalDocuments + " documents from LA Times files.");
    }

    /**
     * Parses a single LA Times file and adds parsed documents to the IndexWriter.
     *
     * @param file   The file to parse.
     * @param writer The IndexWriter to which parsed documents are added.
     * @return The number of documents successfully parsed and indexed.
     * @throws IOException If there is an issue reading the file.
     */
    private static int parseLATimesFile(File file, IndexWriter writer) throws IOException {
        org.jsoup.nodes.Document laTimesContent = Jsoup.parse(file, StandardCharsets.UTF_8.name());

        // Extract DOC elements from the parsed file
        Elements docs = laTimesContent.select("DOC");
        if (docs.isEmpty()) {
            System.out.println("No <DOC> elements found in file: " + file.getName());
            return 0;
        }

        int count = 0;

        // For each DOC element, extract the relevant fields and add to IndexWriter
        for (Element doc : docs) {
            String docNo = extractElementText(doc, "DOCNO");
            String headline = extractElementText(doc.selectFirst("HEADLINE"), "P");
            String text = extractElementText(doc.selectFirst("TEXT"), "P");

            if (docNo.isEmpty() && headline.isEmpty() && text.isEmpty()) {
                System.out.println("Skipping empty document in file: " + file.getName());
                continue;
            }

            writer.addDocument(createDocument(docNo, headline, text));
            count++;
        }

        writer.commit(); // Commit after processing each file
        return count;
    }

    /**
     * Safely extracts text from an element.
     *
     * @param element The element to extract text from.
     * @param tag     The tag to look for within the element.
     * @return The extracted text, or an empty string if the element or tag is missing.
     */
    private static String extractElementText(Element element, String tag) {
        if (element == null) {
            return "";
        }
        Elements tagElements = element.select(tag);
        return tagElements != null ? tagElements.text().trim() : "";
    }

    /**
     * Creates a Lucene Document from the extracted fields.
     *
     * @param docNo    The document number.
     * @param headline The headline of the document.
     * @param text     The main text content of the document.
     * @return A Lucene Document containing the parsed fields.
     */
    private static Document createDocument(String docNo, String headline, String text) {
        Document document = new Document();
        document.add(new StringField("docno", docNo, Field.Store.YES));  // Field for exact matches
        document.add(new TextField("headline", headline, Field.Store.YES));  // Field for full-text search
        document.add(new TextField("text", text, Field.Store.YES));  // Field for full-text search
        return document;
    }
}