package cba.trianglenet;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import floetteroed.utilities.tabularfileparser.TabularFileHandler;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ScoreAnalyzer {

	private final String expectationPrefix;

	private final String experiencePrefix;

	public ScoreAnalyzer(final String expectationPrefix, final String experiencePrefix) {
		this.expectationPrefix = expectationPrefix;
		this.experiencePrefix = experiencePrefix;
	}

	private Map<String, Double> loadId2Score(final String fileName) {
		final Map<String, Double> result = new LinkedHashMap<>();
		final TabularFileHandler handler = new TabularFileHandler() {
			@Override
			public void startRow(String[] row) {
				result.put(row[0], Double.parseDouble(row[1]));
			}

			@Override
			public void startDocument() {
			}

			@Override
			public String preprocess(String line) {
				return line;
			}

			@Override
			public void endDocument() {
			}
		};
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterRegex("\\s");
		try {
			parser.parse(fileName, handler);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	public String comparisonAsString(final int outerIt) {

		final Map<String, Double> id2expected = this.loadId2Score(this.expectationPrefix + outerIt + ".txt");
		final Map<String, Double> id2experienced = this.loadId2Score(this.experiencePrefix + outerIt + ".txt");

		final StringBuffer result = new StringBuffer();
		for (Map.Entry<String, Double> expectedEntry : id2expected.entrySet()) {
			result.append(expectedEntry.getKey());
			result.append("\t");
			result.append(expectedEntry.getValue());
			result.append("\t");
			result.append(id2experienced.get(expectedEntry.getKey()));
			result.append("\n");
		}
		
		return result.toString();
	}

	public static void main(String[] args) {
		final ScoreAnalyzer analyzer = new ScoreAnalyzer(Main.expectationFilePrefix, 
				Main.experienceFilePrefix);

		System.out.println(analyzer.comparisonAsString(10));
	}
	
}
