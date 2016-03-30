package floetteroed.opdyts.example.pathological;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import floetteroed.opdyts.analysis.LogFileReader;
import floetteroed.opdyts.analysis.LogFileSummary;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class ArticleFigureCreator {

	private final List<LogFileSummary> summaries = new LinkedList<>();

	ArticleFigureCreator() {
	}

	void add(final LogFileSummary summary) {
		this.summaries.add(summary);
	}

	private enum WhatToWrite {
		ObjFctVals, EquilGaps, UnifGaps
	};

	String asPSTricksSnippet(final double xFact, final double yFact,
			final String text, final String style, final String marker,
			final WhatToWrite whatToWrite) {

		final StringBuffer result = new StringBuffer();

		boolean dataAvail = true;
		int analyzedStageIndex = 1;

		do {
			final DescriptiveStatistics transitions = new DescriptiveStatistics();
			final DescriptiveStatistics values = new DescriptiveStatistics();
			for (LogFileSummary summary : this.summaries) {
				if (summary.getStageCnt() > analyzedStageIndex) {
					transitions.addValue(summary.getTotalTransitionCounts()
							.get(analyzedStageIndex));
					if (WhatToWrite.ObjFctVals.equals(whatToWrite)) {
						values.addValue(summary
								.getBestObjectiveFunctionValues().get(
										analyzedStageIndex));
					} else if (WhatToWrite.EquilGaps.equals(whatToWrite)) {
						values.addValue(summary.getEquilbriumGapWeights().get(
								analyzedStageIndex));
					} else if (WhatToWrite.UnifGaps.equals(whatToWrite)) {
						values.addValue(summary.getUniformityGapWeights().get(
								analyzedStageIndex));
					} else {
						throw new RuntimeException("Don't know what to write: "
								+ whatToWrite);
					}
				}
			}
			if (transitions.getN() > 0) {

				result.append("\\rput(" + xFact * transitions.getPercentile(50)
						+ "," + values.getPercentile(50) + "){" + marker
						+ "}\n");

				final double x1 = xFact * transitions.getPercentile(25);
				final double x2 = xFact * transitions.getPercentile(75);
				final double y1 = yFact * values.getPercentile(25);
				final double y2 = yFact * values.getPercentile(75);

				result.append("\\rput[tl](" + x1 + "," + y2 + "){" + text
						+ "}\n");

				result.append("\\psline" + style);
				result.append("(" + x1 + "," + y1 + ")");
				result.append("(" + x2 + "," + y1 + ")");
				result.append("(" + x2 + "," + y2 + ")");
				result.append("(" + x1 + "," + y2 + ")");
				result.append("(" + x1 + "," + y1 + ")");
				result.append("\n");

				analyzedStageIndex++;
			} else {
				dataAvail = false;
			}
		} while (dataAvail);

		return result.toString();
	}

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		for (int populationSize : new int[] { 2}) { // , 4, 8, 16, 32, 64, 128, 256 }) {
			final ArticleFigureCreator afcNaive = new ArticleFigureCreator();
			final ArticleFigureCreator afcProposed = new ArticleFigureCreator();
			for (int seed : new int[] { 1000, 2000, 3000, 4000, 5000, 6000,
					7000, 8000, 9000, 10000 }) {
				// read naive
				LogFileReader reader = new LogFileReader(
						PathologicalExample.logFileNameFromParams(
								"./output/pathological/naive/", populationSize,
								seed));
				afcNaive.add(reader.getLogFileSummary());
				// read proposed
				reader = new LogFileReader(
						PathologicalExample.logFileNameFromParams(
								"./output/pathological/proposed/",
								populationSize, seed));
				afcProposed.add(reader.getLogFileSummary());
			}
			// write objective function values
			try {
				final PrintWriter writer;
				writer = new PrintWriter(
						"./output/pathological/latex/ObjFctVals_popSize"
								+ populationSize + ".tex");
				writer.println(afcNaive.asPSTricksSnippet(1.0, 1.0, "\\tiny{"
						+ populationSize + "}", "[linestyle=dashed]",
						"$\\circ$", WhatToWrite.ObjFctVals));
				writer.println(afcProposed.asPSTricksSnippet(1.0, 1.0,
						"\\tiny{" + populationSize + "}", "[linestyle=solid]",
						"$\\bullet$", WhatToWrite.ObjFctVals));
				writer.flush();
				writer.close();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
			// write equilibrium gaps
			try {
				final PrintWriter writer;
				writer = new PrintWriter(
						"./output/pathological/latex/EquilGaps_popSize"
								+ populationSize + ".tex");
				writer.println(afcProposed.asPSTricksSnippet(1.0, 1.0,
						"\\tiny{" + populationSize + "}", "[linestyle=solid]",
						"", WhatToWrite.EquilGaps));
				writer.flush();
				writer.close();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
			// write objective function values
			try {
				final PrintWriter writer;
				writer = new PrintWriter(
						"./output/pathological/latex/UnifGaps_popSize"
								+ populationSize + ".tex");
				writer.println(afcProposed.asPSTricksSnippet(1.0, 1.0,
						"\\tiny{" + populationSize + "}", "[linestyle=solid]",
						"", WhatToWrite.UnifGaps));
				writer.flush();
				writer.close();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		System.out.println("... DONE");
	}
}
