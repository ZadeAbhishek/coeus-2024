package com.group_15_2024;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.miscellaneous.TrimFilter;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.util.CharsRef;

public class CustomAnalyzer extends Analyzer {

	private final Path currentRelativePath = Paths.get("").toAbsolutePath();

	public CustomAnalyzer() {
		super();
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		// Tokenizer
		StandardTokenizer tokenizer = new StandardTokenizer();
		TokenStream tokenStream = tokenizer;

		// Apply custom filters
		tokenStream = new EnglishPossessiveFilter(tokenStream);
		tokenStream = new ASCIIFoldingFilter(tokenStream);
		tokenStream = new LowerCaseFilter(tokenStream);
		tokenStream = new TrimFilter(tokenStream);
		tokenStream = new LengthFilter(tokenStream, 2, 20);  // Filter for words length between 2 and 20
		tokenStream = new PorterStemFilter(tokenStream);  // Apply Porter Stemming

		tokenStream = new StopFilter(tokenStream, createStopWordList());
		SynonymMap synonymMap = createSynonymMap();
		if (synonymMap != null) {
			tokenStream = new SynonymGraphFilter(tokenStream, synonymMap, true);
		}

		return new TokenStreamComponents(tokenizer, tokenStream);
	}

	private SynonymMap createSynonymMap() {
		try {
			BufferedReader countries = new BufferedReader(new FileReader(currentRelativePath + "/synonyms.txt"));
			SynonymMap.Builder builder = new SynonymMap.Builder(true);
			String country;

			while ((country = countries.readLine()) != null) {
				builder.add(new CharsRef("country"), new CharsRef(country), true);
				builder.add(new CharsRef("countries"), new CharsRef(country), true);
			}

			countries.close();
			return builder.build();
		} catch (Exception e) {
			System.out.println("ERROR: " + e.getLocalizedMessage() + " occurred when trying to create synonym map");
			return null;
		}
	}

	private CharArraySet createStopWordList() {
		CharArraySet stopWords = new CharArraySet(100, true);
		try (BufferedReader reader = new BufferedReader(new FileReader(currentRelativePath + "/stopwords.txt"))) {
			String line;
			while ((line = reader.readLine()) != null) {
				stopWords.add(line.trim());
			}
		} catch (IOException e) {
			System.out.println("ERROR: " + e.getLocalizedMessage() + " occurred when trying to create stop word list");
		}
		return stopWords;
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