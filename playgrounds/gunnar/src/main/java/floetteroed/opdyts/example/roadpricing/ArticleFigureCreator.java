package floetteroed.opdyts.example.roadpricing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import floetteroed.opdyts.analysis.OptFileReader;
import floetteroed.opdyts.analysis.OptFileSummary;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class ArticleFigureCreator {

	private final List<OptFileSummary> summaries = new LinkedList<>();

	ArticleFigureCreator() {
	}

	void add(final OptFileSummary summary) {
		this.summaries.add(summary);
	}

	private enum WhatToWrite {
		ObjFctVals, EquilGaps, UnifGaps
	};

	String asPSTricksSnippet(final double xFact, final double yFact,
			final String text, final String style, final WhatToWrite whatToWrite) {

		final DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setDecimalSeparator('.');
		final DecimalFormat df = new DecimalFormat("#");
		df.setMaximumFractionDigits(4);
		df.setDecimalFormatSymbols(dfs);

		final StringBuffer result = new StringBuffer();

		boolean dataAvail = true;
		int outerIt = 0;

		do {
			final DescriptiveStatistics transitions = new DescriptiveStatistics();
			final DescriptiveStatistics values = new DescriptiveStatistics();
			for (OptFileSummary summary : this.summaries) {
				if (outerIt < summary.getStageCnt()) {
					if (WhatToWrite.ObjFctVals.equals(whatToWrite)) {
						transitions.addValue(summary
								.getInitialTransitionCounts().get(outerIt)
								+ summary.getAddedTransitionCounts().get(
										outerIt));
						values.addValue(summary
								.getFinalObjectiveFunctionValues().get(outerIt));
					} else if (WhatToWrite.EquilGaps.equals(whatToWrite)) {
						transitions.addValue(summary
								.getInitialTransitionCounts().get(outerIt));
						values.addValue(summary
								.getInitialEquilbriumGapWeights().get(outerIt));
					} else if (WhatToWrite.UnifGaps.equals(whatToWrite)) {
						transitions.addValue(summary
								.getInitialTransitionCounts().get(outerIt));
						values.addValue(summary
								.getInitialUniformityGapWeights().get(outerIt));
					} else {
						throw new RuntimeException("Don't know what to write: "
								+ whatToWrite);
					}
				}
			}
			if (transitions.getN() > 0) {

				// result.append("\\rput("
				// + df.format(xFact * transitions.getPercentile(50))
				// + "," + df.format(values.getPercentile(50)) + "){"
				// + marker + "}\n");

				result.append("\\psdot[dotstyle=x]("
						+ df.format(xFact * transitions.getPercentile(50))
						+ "," + df.format(values.getPercentile(50)) + ")\n");

				final double x1 = xFact * transitions.getPercentile(25);
				final double x2 = xFact * transitions.getPercentile(75);
				final double y1 = yFact * values.getPercentile(25);
				final double y2 = yFact * values.getPercentile(75);

				result.append("\\rput[l](" + df.format(x1) + ","
						+ df.format((y1 + y2) / 2.0) + "){ " + text + "}\n");

				result.append("\\psline" + style);
				result.append("(" + df.format(x1) + "," + df.format(y1) + ")");
				result.append("(" + df.format(x2) + "," + df.format(y1) + ")");
				result.append("(" + df.format(x2) + "," + df.format(y2) + ")");
				result.append("(" + df.format(x1) + "," + df.format(y2) + ")");
				result.append("(" + df.format(x1) + "," + df.format(y1) + ")");
				result.append("\n");

				outerIt++;
			} else {
				dataAvail = false;
			}
		} while (dataAvail);

		return result.toString();
	}

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		final int[] populationSizes = new int[] { 2, 4, 8, 16, 32, 64, 128, 256 };
		final String[] runIdentifiers = new String[] { "a", "b", "c", "d", "e",
				"f", "g", "h", "i", "j" };

		for (int populationSize : populationSizes) {
			final ArticleFigureCreator afcNaive = new ArticleFigureCreator();
			final ArticleFigureCreator afcProposed = new ArticleFigureCreator();
			for (String run : runIdentifiers) {
				// read naive
				final String naiveFileName = "./output/roadpricing/logfiles/naive_"
						+ populationSize + run + ".opt";
				if (new File(naiveFileName).exists()) {
					final OptFileReader reader = new OptFileReader(
							naiveFileName);
					afcNaive.add(reader.getOptFileSummary());
				}
				// read proposed
				final String proposedFileName = "./output/roadpricing/logfiles/search_"
						+ populationSize + run + ".opt";
				if (new File(proposedFileName).exists()) {
					final OptFileReader reader = new OptFileReader(
							proposedFileName);
					afcProposed.add(reader.getOptFileSummary());
				}
			}

			final double xFact = 1.0 / 1000.0; // simulation equivalent

			// write objective function values
			try {
				final PrintWriter writer = new PrintWriter(
						"./output/roadpricing/latex/ObjFctVals_popSize"
								+ populationSize + ".tex");
				writer.println(afcNaive.asPSTricksSnippet(xFact, 1.0, "\\tiny{"
						+ populationSize + "}", "[linestyle=solid]",
						WhatToWrite.ObjFctVals));
				writer.println(afcProposed.asPSTricksSnippet(xFact, 1.0,
						"\\tiny{" + populationSize + "}", "[linestyle=solid]",
						WhatToWrite.ObjFctVals));
				writer.flush();
				writer.close();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
			// write equilibrium gaps
			try { 
				final PrintWriter writer = new PrintWriter(
						"./output/roadpricing/latex/EquilGaps_popSize"
								+ populationSize + ".tex");
				writer.println(afcProposed.asPSTricksSnippet(xFact, 1.0,
						"\\tiny{" + populationSize + "}", "[linestyle=solid]",
						WhatToWrite.EquilGaps));
				writer.flush();
				writer.close();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
			// write uniformity gaps
			try {
				final PrintWriter writer = new PrintWriter(
						"./output/roadpricing/latex/UnifGaps_popSize"
								+ populationSize + ".tex");
				writer.println(afcProposed.asPSTricksSnippet(xFact, 1.0,
						"\\tiny{" + populationSize + "}", "[linestyle=solid]",
						WhatToWrite.UnifGaps));
				writer.flush();
				writer.close();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		System.out.println("... DONE");
	}
}
