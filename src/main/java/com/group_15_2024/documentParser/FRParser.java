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

public class FRParser {

    private static final int BATCH_COMMIT_SIZE = 100; // Number of documents to process before committing

    /**
     * Parses and indexes Federal Register documents from the specified path.
     *
     * @param pathToFedRegister The path to the directory containing Federal Register files.
     * @param writer            The IndexWriter to write parsed documents to.
     * @throws IOException If there is an issue reading files or directories.
     */
    public static void loadFedRegisterDocs(String pathToFedRegister, IndexWriter writer) throws IOException {
        File[] directories = new File(pathToFedRegister).listFiles(File::isDirectory);
        if (directories == null || directories.length == 0) {
            System.out.println("No directories found in path: " + pathToFedRegister);
            return;
        }

        int totalDocuments = 0;

        for (File directory : directories) {
            File[] files = directory.listFiles();
            if (files == null || files.length == 0) {
                System.out.println("No files found in directory: " + directory.getAbsolutePath());
                continue;
            }

            for (File file : files) {
                try {
                    int docsParsed = parseFile(file, writer);
                    totalDocuments += docsParsed;
                } catch (Exception e) {
                    System.err.println("Error processing file: " + file.getName() + " - " + e.getMessage());
                }
            }
        }

        // Final commit after processing all directories
        writer.commit();
        System.out.println("Successfully indexed " + totalDocuments + " Federal Register documents.");
    }

    /**
     * Parses a single file and writes its documents to the IndexWriter.
     *
     * @param file   The file to parse.
     * @param writer The IndexWriter to write parsed documents to.
     * @return The number of documents successfully parsed and indexed.
     * @throws IOException If there is an issue reading the file.
     */
    private static int parseFile(File file, IndexWriter writer) throws IOException {
        System.out.println("Processing file: " + file.getName());
        org.jsoup.nodes.Document doc = Jsoup.parse(file, StandardCharsets.UTF_8.name());
        Elements documents = doc.select("DOC");

        if (documents.isEmpty()) {
            System.out.println("No <DOC> elements found in file: " + file.getName());
            return 0;
        }

        int count = 0;

        for (Element document : documents) {
            try {
                String docno = document.select("DOCNO").text();
                String title = document.select("DOCTITLE").text();

                // Clean document by removing unwanted tags
                document.select("DOCTITLE, ADDRESS, SIGNER, SIGNJOB, BILLING, FRFILING, DATE, CRFNO, RINDOCK").remove();
                String text = document.select("TEXT").text();

                writer.addDocument(createDocument(docno, text, title));
                count++;

                // Commit in batches for performance
                if (count % BATCH_COMMIT_SIZE == 0) {
                    writer.commit();
                    System.out.println("Committed batch of " + BATCH_COMMIT_SIZE + " documents.");
                }
            } catch (Exception e) {
                System.err.println("Error processing document in file: " + file.getName() + " - " + e.getMessage());
            }
        }

        return count;
    }

    /**
     * Creates a Lucene Document from the extracted fields.
     *
     * @param docno The document number.
     * @param text  The main text content of the document.
     * @param title The title or headline of the document.
     * @return A Lucene Document containing the parsed fields.
     */
    private static Document createDocument(String docno, String text, String title) {
        Document doc = new Document();

        // Add fields to the Lucene Document
        if (docno != null && !docno.isEmpty()) {
            doc.add(new StringField("docno", docno, Field.Store.YES)); // Exact match field
        } else {
            System.err.println("Document missing DOCNO, skipping.");
            return null; // Skip documents without a DOCNO
        }

        if (title != null && !title.isEmpty()) {
            doc.add(new TextField("headline", title, Field.Store.YES)); // Full-text search field
        }

        if (text != null && !text.isEmpty()) {
            doc.add(new TextField("text", text, Field.Store.YES)); // Full-text search field
        }

        System.out.println("Added document with DOCNO: " + docno);
        return doc;
    }
}