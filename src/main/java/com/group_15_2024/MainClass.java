package com.group_15_2024;

import com.group_15_2024.documentParser.FTParser;
import com.group_15_2024.documentParser.FRParser;
import com.group_15_2024.documentParser.LATParser;
import com.group_15_2024.documentParser.FBISParser;
import com.group_15_2024.QueryLogic.QueryObject;

import static com.group_15_2024.QueryLogic.QueryLib.loadQueriesFromFile;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.document.Document;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.List;

public class MainClass {

    private static final Path currentRelativePath = Paths.get("").toAbsolutePath();
    private static final String INDEX_PATH = currentRelativePath + "/Index";
    private static final String FIN_TIMES_PATH = currentRelativePath + "/Documents/Assignment Two/ft";
    private static final String FED_REGISTER_PATH = currentRelativePath + "/Documents/Assignment Two/fr94";
    private static final String LA_TIMES_PATH = currentRelativePath + "/Documents/Assignment Two/latimes";
    private static final String FBIS_PATH = currentRelativePath + "/Documents/Assignment Two/fbis";
    private static final String RESULTS_PATH = currentRelativePath + "/queryResults";

    private static final int MAX_RETURN_RESULTS = 100; // Limit query results to save memory

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java MainClass <RankingModel> <Analyzer>");
            System.err.println("Example: java MainClass BM25 CUSTOM");
            return;
        }

        String rankingModel = args[0];
        String analyzerType = args[1];

        System.out.println("Using Ranking Model: " + rankingModel);
        System.out.println("Using Analyzer: " + analyzerType);

        Similarity similarityModel = RankAndAnalyzers.callSetRankingModel(rankingModel);
        Analyzer analyzer = RankAndAnalyzers.callSetAnalyzer(analyzerType);

        try {
            logSystemUsage("Before Execution");

            System.out.println("Checking if index directory exists...");
            // Delete existing index directory if it exists
            if (Files.exists(Paths.get(INDEX_PATH))) {
                System.out.println("Index directory found. Deleting existing directory...");
                deleteDirectory(new File(INDEX_PATH));
                System.out.println("Existing index directory deleted.");
            } else {
                System.out.println("No existing index directory found.");
            }

            // Create index and execute queries
            try (Directory directory = FSDirectory.open(Paths.get(INDEX_PATH))) {
                System.out.println("Starting indexing process...");
                createIndex(directory, similarityModel, analyzer);
                logSystemUsage("After Indexing");
                System.out.println("Indexing process completed.");

                System.out.println("Starting query execution...");
                executeQueries(directory, similarityModel, analyzer);
                logSystemUsage("After Query Execution");
                System.out.println("Query execution completed.");
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private static void createIndex(Directory directory, Similarity similarityModel, Analyzer analyzer) {
        try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)
                .setSimilarity(similarityModel)
                .setRAMBufferSizeMB(16))) { // Limit RAM buffer size to 16 MB

            System.out.println("Indexing Financial Times documents...");
            FTParser.loadFinTimesDocs(FIN_TIMES_PATH, writer);

            System.out.println("Indexing Federal Register documents...");
            FRParser.loadFedRegisterDocs(FED_REGISTER_PATH, writer);

            System.out.println("Indexing LA Times documents...");
            LATParser.loadLaTimesDocs(LA_TIMES_PATH, writer);

            System.out.println("Indexing FBIS documents...");
            FBISParser.loadFBISDocs(FBIS_PATH, writer);

            System.out.println("All documents loaded and indexed successfully.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void executeQueries(Directory directory, Similarity similarityModel, Analyzer analyzer) throws ParseException {
        try (IndexReader indexReader = DirectoryReader.open(directory);
             PrintWriter writer = new PrintWriter(RESULTS_PATH, "UTF-8")) {

            System.out.println("Total documents in index: " + indexReader.numDocs());

            IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            indexSearcher.setSimilarity(similarityModel);

            QueryParser queryParser = new MultiFieldQueryParser(new String[]{"headline", "text"}, analyzer);
            List<QueryObject> loadedQueries = loadQueriesFromFile();

            System.out.println("Number of queries loaded: " + loadedQueries.size());

            for (QueryObject queryData : loadedQueries) {
                processQuery(queryData, indexSearcher, queryParser, writer);
                System.out.println("Processed query: " + queryData.getQueryNum());
            }

            System.out.println("Queries executed successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processQuery(QueryObject queryData, IndexSearcher indexSearcher, QueryParser queryParser, PrintWriter writer) throws ParseException, IOException {
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

        // Construct query
        Query titleQuery = queryParser.parse(QueryParser.escape(queryData.getTitle()));
        booleanQuery.add(new BoostQuery(titleQuery, 4.0f), BooleanClause.Occur.SHOULD);

        Query descQuery = queryParser.parse(QueryParser.escape(queryData.getDescription()));
        booleanQuery.add(new BoostQuery(descQuery, 2.0f), BooleanClause.Occur.SHOULD);

        Query finalQuery = booleanQuery.build();

        // Search results
        TopDocs topDocs = indexSearcher.search(finalQuery, MAX_RETURN_RESULTS);
        int rank = 0; // Start ranking from 0

        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = indexSearcher.doc(scoreDoc.doc);

            // Get the document number (docno)
            String docno = doc.get("docno");
            if (docno == null) {
                System.err.println("Document without docno found, skipping.");
                continue;
            }

            // Write results in the required format
            writer.printf("%s 0 %s %d %.6f 0%n",
                    queryData.getQueryNum(),  // <query_id>
                    docno,                   // <docno>
                    rank,                    // <rank>
                    scoreDoc.score           // <score>
            );

            rank++; // Increment rank
        }
    }

    private static void deleteDirectory(File directory) throws IOException {
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                deleteDirectory(file);
            }
        }
        Files.delete(directory.toPath());
    }

    private static void logSystemUsage(String stage) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        double usedMemoryPercentage = ((double) usedMemory / runtime.maxMemory()) * 100;

        File disk = new File(currentRelativePath.toString());
        long totalDiskSpace = disk.getTotalSpace();
        long freeDiskSpace = disk.getFreeSpace();
        long usedDiskSpace = totalDiskSpace - freeDiskSpace;

        double usedDiskPercentage = ((double) usedDiskSpace / totalDiskSpace) * 100;

        DecimalFormat df = new DecimalFormat("#.##");

        System.out.println("\n==== System Usage at " + stage + " ====");
        System.out.println("RAM Usage: " + df.format((double) usedMemory / (1024 * 1024)) + " MB (" +
                df.format(usedMemoryPercentage) + "% of max)");
        System.out.println("Disk Usage: " + df.format((double) usedDiskSpace / (1024 * 1024 * 1024)) + " GB (" +
                df.format(usedDiskPercentage) + "% of total)");
        System.out.println("=====================================\n");
    }
}