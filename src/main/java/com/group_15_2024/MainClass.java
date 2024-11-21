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
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.QueryParser;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.List;

public class MainClass {

    private static final Path CURRENT_PATH = Paths.get("").toAbsolutePath();
    private static final String INDEX_PATH = CURRENT_PATH + "/Index";
    private static final String FIN_TIMES_PATH = CURRENT_PATH + "/Documents/Assignment Two/ft";
    private static final String FED_REGISTER_PATH = CURRENT_PATH + "/Documents/Assignment Two/fr94";
    private static final String LA_TIMES_PATH = CURRENT_PATH + "/Documents/Assignment Two/latimes";
    private static final String FBIS_PATH = CURRENT_PATH + "/Documents/Assignment Two/fbis";
    private static final String RESULTS_PATH = CURRENT_PATH + "/queryResults";
    private static final int MAX_RESULTS = 100;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java MainClass <RankingModel> <Analyzer>");
            System.err.println("Example: java MainClass BM25 CUSTOM");
            return;
        }

        String rankingModel = args[0];
        String analyzerType = args[1];

        System.out.printf("Using Ranking Model: %s, Analyzer: %s%n", rankingModel, analyzerType);

        Similarity similarity = RankAndAnalyzers.callSetRankingModel(rankingModel);
        Analyzer analyzer = RankAndAnalyzers.callSetAnalyzer(analyzerType);

        try {
            logSystemUsage("Before Execution");

            // Delete existing index directory
            cleanIndexDirectory();

            // Create index and execute queries
            try (Directory directory = FSDirectory.open(Paths.get(INDEX_PATH))) {
                createIndex(directory, similarity, analyzer);
                logSystemUsage("After Indexing");

                executeQueries(directory, similarity, analyzer);
                logSystemUsage("After Query Execution");
            }

        } catch (IOException | ParseException e) {
            System.err.printf("Error: %s%n", e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createIndex(Directory directory, Similarity similarity, Analyzer analyzer) {
        try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)
                .setSimilarity(similarity)
                .setRAMBufferSizeMB(16))) {

            System.out.println("Indexing Financial Times documents...");
            FTParser.loadFinTimesDocs(FIN_TIMES_PATH, writer);

            System.out.println("Indexing Federal Register documents...");
            FRParser.loadFedRegisterDocs(FED_REGISTER_PATH, writer);

            System.out.println("Indexing LA Times documents...");
            LATParser.loadLaTimesDocs(LA_TIMES_PATH, writer);

            System.out.println("Indexing FBIS documents...");
            FBISParser.loadFBISDocs(FBIS_PATH, writer);

            System.out.println("Indexing completed successfully.");

        } catch (IOException e) {
            System.err.printf("Error during indexing: %s%n", e.getMessage());
        }
    }

    private static void executeQueries(Directory directory, Similarity similarity, Analyzer analyzer) throws IOException, ParseException {
        try (IndexReader reader = DirectoryReader.open(directory);
             PrintWriter writer = new PrintWriter(RESULTS_PATH, "UTF-8")) {

            System.out.printf("Total documents in index: %d%n", reader.numDocs());

            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(similarity);

            QueryParser parser = new MultiFieldQueryParser(new String[]{"headline", "text"}, analyzer);
            List<QueryObject> queries = loadQueriesFromFile();

            System.out.printf("Loaded %d queries.%n", queries.size());

            for (QueryObject query : queries) {
                processQuery(query, searcher, parser, writer);
                System.out.printf("Processed query: %s%n", query.getQueryNum());
            }

            System.out.println("Query execution completed successfully.");
        }
    }

    private static void processQuery(QueryObject query, IndexSearcher searcher, QueryParser parser, PrintWriter writer) throws ParseException, IOException {
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

        Query titleQuery = parser.parse(QueryParser.escape(query.getTitle()));
        booleanQuery.add(new BoostQuery(titleQuery, 4.0f), BooleanClause.Occur.SHOULD);

        Query descQuery = parser.parse(QueryParser.escape(query.getDescription()));
        booleanQuery.add(new BoostQuery(descQuery, 2.0f), BooleanClause.Occur.SHOULD);

        TopDocs results = searcher.search(booleanQuery.build(), MAX_RESULTS);

        int rank = 0;
        for (ScoreDoc scoreDoc : results.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            String docno = doc.get("docno");

            if (docno == null) {
                System.err.println("Skipping document without docno.");
                continue;
            }

            writer.printf("%s 0 %s %d %.6f 0%n", query.getQueryNum(), docno, rank, scoreDoc.score);
            rank++;
        }
    }

    private static void cleanIndexDirectory() throws IOException {
        Path indexPath = Paths.get(INDEX_PATH);

        if (Files.exists(indexPath)) {
            System.out.println("Cleaning existing index directory...");
            Files.walk(indexPath)
                 .map(Path::toFile)
                 .sorted((a, b) -> -a.compareTo(b)) // Delete directories after files
                 .forEach(File::delete);
            System.out.println("Index directory cleaned.");
        } else {
            System.out.println("No existing index directory found.");
        }
    }

    private static void logSystemUsage(String stage) {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long totalMemory = runtime.maxMemory();
        long diskSpaceUsed = new File(CURRENT_PATH.toString()).getTotalSpace() -
                             new File(CURRENT_PATH.toString()).getFreeSpace();

        DecimalFormat df = new DecimalFormat("#.##");

        System.out.printf("%n==== System Usage at %s ====%n", stage);
        System.out.printf("Memory Usage: %s MB (%.2f%%)%n",
                df.format(usedMemory / (1024.0 * 1024)),
                (double) usedMemory / totalMemory * 100);
        System.out.printf("Disk Usage: %s GB%n", df.format(diskSpaceUsed / (1024.0 * 1024 * 1024)));
        System.out.println("=============================\n");
    }
}