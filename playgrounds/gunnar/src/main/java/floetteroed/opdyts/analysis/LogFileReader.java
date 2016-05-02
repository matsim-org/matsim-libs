package floetteroed.opdyts.analysis;

import java.io.IOException;

import floetteroed.opdyts.logging.EquilibriumGapWeight;
import floetteroed.opdyts.logging.UniformityGapWeight;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LogFileReader extends AbstractTabularFileHandlerWithHeaderLine {

	// -------------------- MEMBERS --------------------

	private LogFileSummary summary = new LogFileSummary();

	private Integer currentRandomSearchIteration = null;

	private int totalTransitionCnt = 0;

	//
	// private Double lastBestObjectiveFunctionValue = null;
	//
	// private Double lastEquilibriumGapWeight = null;
	//
	// private Double lastUniformityGapWeight = null;

	// private Double lastConvergedObjectiveFunctionValue = null;
	//
	// private Double lastSurrogateObjectiveFunctionValue = null;
	//
	// private List<Integer> totalTransitionCnts = new ArrayList<Integer>();
	//
	// private List<Double> convergedObjectiveFunctionValues = new
	// ArrayList<>();
	//
	// private List<Double> surrogateObjectiveFunctionValues = new
	// ArrayList<>();

	// -------------------- CONSTRUCTION --------------------

	public LogFileReader(final String logFileName) {
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "\t" });
		parser.setOmitEmptyColumns(false);
		try {
			parser.parse(logFileName, this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// -------------------- RESULT ACCESS --------------------

	public LogFileSummary getLogFileSummary() {
		return this.summary;
	}

	// public List<Integer> getTotalTransitions() {
	// return this.totalTransitionCnts;
	// }
	//
	// public List<Double> getConvergedObjectiveFunctionValues() {
	// return this.convergedObjectiveFunctionValues;
	// }
	//
	// public List<Double> getSurrogateObjectiveFunctionValues() {
	// return this.surrogateObjectiveFunctionValues;
	// }

	// public List<Double> getObjectiveFunctionValueGaps() {
	// final List<Double> gaps = new ArrayList<>(
	// this.convergedObjectiveFunctionValues.size());
	// for (int i = 0; i < this.convergedObjectiveFunctionValues.size(); i++) {
	// gaps.add(this.surrogateObjectiveFunctionValues.get(i)
	// - this.convergedObjectiveFunctionValues.get(i));
	// }
	// return gaps;
	// }

	// --------------- IMPLEMENTATION OF TabularFileHandler ---------------

	@Override
	public String preprocess(final String line) {
		return line;
	}

	@Override
	public void startDocument() {
		this.currentRandomSearchIteration = 0;
		this.totalTransitionCnt = 0;
	}

	@Override
	public void startCurrentDataRow() {
		final int randomSearchIteration = this
				.getIntValue(RandomSearch.RANDOM_SEARCH_ITERATION);
		if (this.currentRandomSearchIteration == null) {
			this.currentRandomSearchIteration = randomSearchIteration;
		}
		if (randomSearchIteration > this.currentRandomSearchIteration) {
			final double bestObjectiveFunctionValue = this
					.getDoubleValue(RandomSearch.BEST_OVERALL_SOLUTION);
			final double equilibriumGapWeight = this
					.getDoubleValue(EquilibriumGapWeight.LABEL);
			final double uniformityGapWeight = this
					.getDoubleValue(UniformityGapWeight.LABEL);
			this.summary.add(this.totalTransitionCnt,
					bestObjectiveFunctionValue, equilibriumGapWeight,
					uniformityGapWeight);
		}
		this.currentRandomSearchIteration = randomSearchIteration;
		this.totalTransitionCnt++; // just counting lines
	}

	@Override
	public void endDocument() {
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws IOException {

		System.out.println("STARTED ...");

		final LogFileReader lfa = new LogFileReader(
				"./output/pathological/proposed/popSize2_seed1000.log");
		final LogFileSummary summary = lfa.getLogFileSummary();

		for (int stage = 0; stage < summary.getStageCnt(); stage++) {
			System.out.print(stage + 1);
			System.out.print("\t"
					+ summary.getTotalTransitionCounts().get(stage));
			System.out.print("\t"
					+ summary.getBestObjectiveFunctionValues().get(stage));
			System.out.print("\t"
					+ summary.getEquilbriumGapWeights().get(stage));
			System.out.print("\t"
					+ summary.getUniformityGapWeights().get(stage));
			System.out.println();
		}

		System.out.println("... DONE");

	}
}
