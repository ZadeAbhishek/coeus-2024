package com.group_15_2024;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.*;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.miscellaneous.*;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CustomAnalyzer extends Analyzer {

    private static final Path currentRelativePath = Paths.get("").toAbsolutePath();

    private static final CharArraySet STOP_WORDS = loadStopWords();
    private static final SynonymMap SYNONYM_MAP = loadSynonymMap();

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        // Tokenizer
        StandardTokenizer tokenizer = new StandardTokenizer();
        TokenStream tokenStream = tokenizer;

        tokenStream = new EnglishPossessiveFilter(tokenStream);
        tokenStream = new ASCIIFoldingFilter(tokenStream);
        tokenStream = new LowerCaseFilter(tokenStream);
        tokenStream = new TrimFilter(tokenStream);
        tokenStream = new LengthFilter(tokenStream, 2, 20);
        tokenStream = new StopFilter(tokenStream, STOP_WORDS);
        tokenStream = new PorterStemFilter(tokenStream); // Apply Porter Stemming

        if (SYNONYM_MAP != null) {
            tokenStream = new SynonymGraphFilter(tokenStream, SYNONYM_MAP, true);
        }

        return new TokenStreamComponents(tokenizer, tokenStream);
    }

    private static CharArraySet loadStopWords() {
        CharArraySet stopWords = new CharArraySet(100, true);
        File stopWordsFile = new File(currentRelativePath + "/stopwords.txt");

        if (!stopWordsFile.exists()) {
            System.err.println("WARNING: Stop words file not found: " + stopWordsFile.getAbsolutePath());
            return CharArraySet.EMPTY_SET;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(stopWordsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stopWords.add(line.trim());
            }
        } catch (IOException e) {
            System.err.println("ERROR: Unable to load stop words - " + e.getMessage());
        }
        return stopWords;
    }

    private static SynonymMap loadSynonymMap() {
        SynonymMap.Builder builder = new SynonymMap.Builder(true);
        File synonymsFile = new File(currentRelativePath + "/synonyms.txt");

        if (!synonymsFile.exists()) {
            System.err.println("WARNING: Synonyms file not found: " + synonymsFile.getAbsolutePath());
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(synonymsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.add(new CharsRef("country"), new CharsRef(line.trim()), true);
                builder.add(new CharsRef("countries"), new CharsRef(line.trim()), true);
            }
            return builder.build();
        } catch (IOException e) {
            System.err.println("ERROR: Unable to load synonyms - " + e.getMessage());
        }
        return null;
    }

    public List<String> analyze(String text) throws IOException {
        List<String> result = new ArrayList<>();
        try (TokenStream tokenStream = tokenStream("content", text)) {
            CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                result.add(attr.toString());
            }
            tokenStream.end();
        }
        return result;
    }
}