package floetteroed.opdyts.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import floetteroed.opdyts.logging.ConvergedObjectiveFunctionValue;
import floetteroed.opdyts.logging.SurrogateObjectiveFunctionValue;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LogFileAnalyzer extends AbstractTabularFileHandlerWithHeaderLine {

	// -------------------- MEMBERS --------------------

	private int currentRandomSearchIteration = 0;

	private int lastTotalTransitionCnt = 0;

	private Double lastConvergedObjectiveFunctionValue = null;

	private Double lastSurrogateObjectiveFunctionValue = null;
	
	private List<Integer> totalTransitionCnts = new ArrayList<Integer>();
	
	private List<Double> convergedObjectiveFunctionValues = new ArrayList<>();

	private List<Double> surrogateObjectiveFunctionValues = new ArrayList<>();

	// -------------------- CONSTRUCTION --------------------

	public LogFileAnalyzer(final String logFileName) throws IOException {
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "\t" });
		// parser.setDelimiterRegex("\\s");
		parser.setOmitEmptyColumns(false);
		parser.parse(logFileName, this);
	}

	// -------------------- RESULT ACCESS --------------------

	public List<Integer> getTotalTransitions() {
		return this.totalTransitionCnts;
	}
	
	public List<Double> getConvergedObjectiveFunctionValues() {
		return this.convergedObjectiveFunctionValues;
	}

	public List<Double> getSurrogateObjectiveFunctionValues() {
		return this.surrogateObjectiveFunctionValues;
	}

	public List<Double> getObjectiveFunctionValueGaps() {
		final List<Double> gaps = new ArrayList<>(
				this.convergedObjectiveFunctionValues.size());
		for (int i = 0; i < this.convergedObjectiveFunctionValues.size(); i++) {
			gaps.add(this.surrogateObjectiveFunctionValues.get(i)
					- this.convergedObjectiveFunctionValues.get(i));
		}
		return gaps;
	}

	// --------------- IMPLEMENTATION OF TabularFileHandler ---------------

	@Override
	public String preprocess(final String line) {
		return line;
	}

	@Override
	public void startDocument() {
	}

	@Override
	public void startCurrentDataRow() {
		final int rndSearchIt = this
				.getIntValue(RandomSearch.RANDOM_SEARCH_ITERATION);
		if (rndSearchIt > this.currentRandomSearchIteration) {
			this.totalTransitionCnts.add(this.lastTotalTransitionCnt);
			this.convergedObjectiveFunctionValues
					.add(this.lastConvergedObjectiveFunctionValue);
			this.surrogateObjectiveFunctionValues
					.add(this.lastSurrogateObjectiveFunctionValue);
		}
		this.lastTotalTransitionCnt++;
		this.currentRandomSearchIteration = rndSearchIt;
		this.lastSurrogateObjectiveFunctionValue = this
				.getDoubleValue(SurrogateObjectiveFunctionValue.LABEL);
		this.lastConvergedObjectiveFunctionValue = this
				.getDoubleValue(ConvergedObjectiveFunctionValue.LABEL);
	}

	@Override
	public void endDocument() {
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws IOException {

		System.out.println("STARTED ...");

		final LogFileAnalyzer lfa = new LogFileAnalyzer(
				"./small-system_pop-size-14_repl-1.log");
		
		System.out.println("transitions\t" + lfa.totalTransitionCnts);
		System.out
				.println("surrogate\t" + lfa.surrogateObjectiveFunctionValues);
		System.out
				.println("converged\t" + lfa.convergedObjectiveFunctionValues);
		System.out
				.println("gaps\t" + lfa.getObjectiveFunctionValueGaps());

		System.out.println("... DONE");

	}
}
