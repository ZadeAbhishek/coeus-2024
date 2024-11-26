package com.group_15_2024;

import com.group_15_2024.documentParser.FBISParser;
import com.group_15_2024.documentParser.FRParser;
import com.group_15_2024.documentParser.FTParser;
import com.group_15_2024.documentParser.LATParser;
import com.group_15_2024.QueryLogic.QueryObject;

import static com.group_15_2024.QueryLogic.QueryLib.loadQueriesFromFile;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.index.Term;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.BreakIterator;
import java.text.DecimalFormat;

public class MainClass {

    private static final Path CURRENT_PATH = Paths.get("").toAbsolutePath();
    private static final String INDEX_PATH = CURRENT_PATH + "/Index";
    private static final String FIN_TIMES_PATH = CURRENT_PATH + "/Documents/Assignment Two/ft";
    private static final String FED_REGISTER_PATH = CURRENT_PATH + "/Documents/Assignment Two/fr94";
    private static final String LA_TIMES_PATH = CURRENT_PATH + "/Documents/Assignment Two/latimes";
    private static final String FBIS_PATH = CURRENT_PATH + "/Documents/Assignment Two/fbis";
    private static final String RESULTS_PATH = CURRENT_PATH + "/queryResults";
    private static final int MAX_RESULTS = 1000;

    private static Similarity similarityModel;
    private static Analyzer analyzer;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java MainClass <RankingModel> <Analyzer>");
            System.err.println("Example: java MainClass BM25 CUSTOM");
            return;
        }

        String rankingModel = args[0];
        String analyzerType = args[1];

        System.out.printf("Using Ranking Model: %s, Analyzer: %s%n", rankingModel, analyzerType);

        similarityModel = RankAndAnalyzers.callSetRankingModel(rankingModel);
        analyzer = RankAndAnalyzers.callSetAnalyzer(analyzerType);

        try {
            logSystemUsage("Before Execution");

            cleanIndexDirectory();

            try (Directory directory = FSDirectory.open(Paths.get(INDEX_PATH))) {
                createIndex(directory);
                logSystemUsage("After Indexing");

                executeQueries(directory);
                logSystemUsage("After Query Execution");
            }

        } catch (IOException | ParseException e) {
            System.err.printf("Error: %s%n", e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createIndex(Directory directory) throws IOException {
        try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer)
                .setSimilarity(similarityModel)
                .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND)
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

    private static void executeQueries(Directory directory) throws IOException, ParseException {
        try (IndexReader reader = DirectoryReader.open(directory);
             PrintWriter writer = new PrintWriter(RESULTS_PATH, "UTF-8")) {

            System.out.printf("Total documents in index: %d%n", reader.numDocs());

            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(similarityModel);

            Map<String, Float> boost = createBoostMap();
            QueryParser parser = new MultiFieldQueryParser(new String[]{"headline", "text"}, analyzer, boost);
            List<QueryObject> queries = loadQueriesFromFile();

            System.out.printf("Loaded %d queries.%n", queries.size());

            for (QueryObject queryData : queries) {
                processQuery(queryData, searcher, parser, writer);
                System.out.printf("Processed query: %s%n", queryData.getQueryNum());
            }

            System.out.println("Query execution completed successfully.");
        }
    }

    private static void processQuery(QueryObject queryData, IndexSearcher searcher, QueryParser parser, PrintWriter writer) throws ParseException, IOException {
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

        List<String> splitNarrative = splitNarrIntoRelNotRel(queryData.getNarrative());
        String relevantNarr = splitNarrative.get(0).trim();

        System.out.println("Processing Query ID: " + queryData.getQueryNum());

        if (!queryData.getTitle().trim().isEmpty()) {
            addQueriesToBooleanQuery(booleanQuery, parser, "headline", queryData.getTitle(), 3.5f, 4f);
        } else {
            System.out.println("Title is empty for Query ID: " + queryData.getQueryNum());
        }

        if (!queryData.getDescription().trim().isEmpty()) {
            addQueriesToBooleanQuery(booleanQuery, parser, "text", queryData.getDescription(), 2.5f, 3.0f);
        } else {
            System.out.println("Description is empty for Query ID: " + queryData.getQueryNum());
        }

        if (!relevantNarr.isEmpty()) {
            addQueriesToBooleanQuery(booleanQuery, parser, "text", relevantNarr, 2.0f, 2.5f);
        } else {
            System.out.println("Relevant Narrative is empty for Query ID: " + queryData.getQueryNum());
        }

        Query finalQuery = booleanQuery.build();

        TopDocs results = searcher.search(finalQuery, MAX_RESULTS);

        int rank = 0;
        for (ScoreDoc scoreDoc : results.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);
            String docno = doc.get("docno");

            if (docno == null) {
                System.err.println("Skipping document without docno.");
                continue;
            }

            writer.printf("%s 0 %s %d %.6f 0%n", queryData.getQueryNum(), docno, rank, scoreDoc.score);
            rank++;
        }
    }

    private static void addQueriesToBooleanQuery(BooleanQuery.Builder booleanQuery, QueryParser queryParser,
                                                 String fieldName, String text, float queryBoost, float phraseBoost) throws ParseException {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        Query query = queryParser.parse(QueryParser.escape(text));
        booleanQuery.add(new BoostQuery(query, queryBoost), BooleanClause.Occur.SHOULD);

        PhraseQuery phraseQuery = createPhraseQuery(fieldName, text);
        booleanQuery.add(new BoostQuery(phraseQuery, phraseBoost), BooleanClause.Occur.SHOULD);
    }

    private static PhraseQuery createPhraseQuery(String fieldName, String text) {
        PhraseQuery.Builder phraseQueryBuilder = new PhraseQuery.Builder();
        String[] terms = text.split("\\s+");
        int position = 0;
        for (String term : terms) {
            phraseQueryBuilder.add(new Term(fieldName, term.toLowerCase()), position++);
        }
        return phraseQueryBuilder.build();
    }

    private static List<String> splitNarrIntoRelNotRel(String narrative) {
        StringBuilder relevantNarr = new StringBuilder();
        StringBuilder irrelevantNarr = new StringBuilder();

        BreakIterator bi = BreakIterator.getSentenceInstance();
        bi.setText(narrative);
        int index = 0;
        while (bi.next() != BreakIterator.DONE) {
            String sentence = narrative.substring(index, bi.current());

            if (!sentence.contains("not relevant") && !sentence.contains("irrelevant")) {
                relevantNarr.append(sentence.replaceAll(
                        "a relevant document identifies|a relevant document could|a relevant document may|a relevant document must|a relevant document will|a document will|to be relevant|relevant documents|a document must|relevant|will contain|will discuss|will provide|must cite",
                        ""));
            } else {
                irrelevantNarr.append(sentence.replaceAll("are also not relevant|are not relevant|are irrelevant|is not relevant|not|NOT", ""));
            }
            index = bi.current();
        }
        List<String> splitNarrative = new ArrayList<>(2);
        splitNarrative.add(relevantNarr.toString());
        splitNarrative.add(irrelevantNarr.toString());
        return splitNarrative;
    }

    private static Map<String, Float> createBoostMap() {
        Map<String, Float> boost = new HashMap<>();
        boost.put("headline", 0.1f);
        boost.put("text", 0.9f);
        return boost;
    }

    private static void cleanIndexDirectory() throws IOException {
        Path indexPath = Paths.get(INDEX_PATH);

        if (Files.exists(indexPath)) {
            System.out.println("Cleaning existing index directory...");
            Files.walk(indexPath)
                    .map(Path::toFile)
                    .sorted((a, b) -> -a.compareTo(b))
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
