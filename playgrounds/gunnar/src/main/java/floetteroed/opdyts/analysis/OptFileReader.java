package floetteroed.opdyts.analysis;

import java.io.IOException;

import floetteroed.opdyts.searchalgorithms.OuterIterationStatistics;
import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class OptFileReader extends AbstractTabularFileHandlerWithHeaderLine {

	// -------------------- MEMBERS --------------------

	private OptFileSummary summary = new OptFileSummary();

	// -------------------- CONSTRUCTION --------------------

	public OptFileReader(final String optFileName) {
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "\t" });
		parser.setOmitEmptyColumns(false);
		try {
			parser.parse(optFileName, this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// -------------------- RESULT ACCESS --------------------

	public OptFileSummary getOptFileSummary() {
		return this.summary;
	}

	// --------------- IMPLEMENTATION OF TabularFileHandler... ---------------

	@Override
	public String preprocess(final String line) {
		return line;
	}

	@Override
	public void startDocument() {
	}

	@Override
	public void startCurrentDataRow() {
		final Integer initialTransitionCount = this
				.getIntValue(OuterIterationStatistics.INITIAL_TRANSITION_NUMBER);
		final Double initialEquilibriumGapWeight = this
				.getDoubleValue(OuterIterationStatistics.INITIAL_EQUILIBRIUM_GAP_WEIGHT);
		final Double initialUniformityGapWeight = this
				.getDoubleValue(OuterIterationStatistics.INITIAL_UNIFORMITY_GAP_WEIGHT);
		final Double finalObjectiveFunctionValue = this
				.getDoubleValue(OuterIterationStatistics.FINAL_OBJECTIVE_FUNCTION_VALUE);
		final Integer addedTransitionCount = this
				.getIntValue(OuterIterationStatistics.ADDITIONAL_TRANSITION_NUMBER);
		this.summary.add(initialTransitionCount, initialEquilibriumGapWeight,
				initialUniformityGapWeight, finalObjectiveFunctionValue,
				addedTransitionCount);
	}

	@Override
	public void endDocument() {
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws IOException {

		System.out.println("STARTED ...");

		final OptFileReader lfa = new OptFileReader(
				"./output/roadpricing/logfiles/naive_2a.opt");
		final OptFileSummary summary = lfa.getOptFileSummary();

		for (int stage = 0; stage < summary.getStageCnt(); stage++) {
			System.out.print(stage + 1);
			System.out.print("\t"
					+ summary.getInitialTransitionCounts().get(stage));
			System.out.print("\t"
					+ summary.getInitialEquilbriumGapWeights().get(stage));
			System.out.print("\t"
					+ summary.getInitialUniformityGapWeights().get(stage));
			System.out.print("\t"
					+ summary.getFinalObjectiveFunctionValues().get(stage));
			System.out.print("\t"
					+ summary.getAddedTransitionCounts().get(stage));
			System.out.println();
		}

		System.out.println("... DONE");

	}
}
