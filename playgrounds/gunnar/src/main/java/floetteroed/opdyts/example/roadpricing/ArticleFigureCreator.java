package floetteroed.opdyts.example.roadpricing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import floetteroed.opdyts.analysis.OptFileReader;
import floetteroed.opdyts.analysis.OptFileSummary;
import floetteroed.utilities.Tuple;
import floetteroed.utilities.math.Covariance;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class ArticleFigureCreator {

	private enum WhatToWrite {
		ObjFctVals, EquilGaps, UnifGaps
	};

	private final int requiredTransitions;

	private final List<OptFileSummary> summaries = new LinkedList<>();

	private final DecimalFormat df;

	ArticleFigureCreator(final int requiredTransitions) {
		this.requiredTransitions = requiredTransitions;
		final DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setDecimalSeparator('.');
		this.df = new DecimalFormat("#");
		this.df.setMaximumFractionDigits(8);
		this.df.setDecimalFormatSymbols(dfs);
	}

	void add(final OptFileSummary summary) {
		this.summaries.add(summary);
	}

	String asPSTricksSnippet(final double xFact, final double yFact,
			final String style, final WhatToWrite whatToWrite) {

		final StringBuffer result = new StringBuffer();

		int outerIt = 0;
		boolean dataAvail = true;
		do {
			final List<Tuple<Double, Double>> points = new ArrayList<>();
			final DescriptiveStatistics transitions = new DescriptiveStatistics();
			final DescriptiveStatistics values = new DescriptiveStatistics();
			for (OptFileSummary summary : this.summaries) {

				if (outerIt < summary.getStageCnt()) {

					System.out.print(".");

					if (WhatToWrite.ObjFctVals.equals(whatToWrite)) {
						final double it = summary.getInitialTransitionCounts()
								.get(outerIt)
								+ summary.getAddedTransitionCounts().get(
										outerIt);
						final double val = summary
								.getFinalObjectiveFunctionValues().get(outerIt);
						transitions.addValue(it);
						values.addValue(val);
						points.add(new Tuple<Double, Double>(it, val));
					} else if (WhatToWrite.EquilGaps.equals(whatToWrite)) {
						final double it = summary.getInitialTransitionCounts()
								.get(outerIt);
						final double val = summary
								.getInitialEquilbriumGapWeights().get(outerIt);
						transitions.addValue(it);
						values.addValue(val);
						points.add(new Tuple<Double, Double>(it, val));
					} else if (WhatToWrite.UnifGaps.equals(whatToWrite)) {
						final double it = summary.getInitialTransitionCounts()
								.get(outerIt);
						final double val = summary
								.getInitialUniformityGapWeights().get(outerIt);
						transitions.addValue(it);
						values.addValue(val);
						points.add(new Tuple<Double, Double>(it, val));
					} else {
						throw new RuntimeException("Don't know what to write: "
								+ whatToWrite);
					}
				}
			}
			System.out.println();

			if (transitions.getN() > 0) {
				for (Tuple<Double, Double> point : points) {
					result.append("\\psdot("
							+ this.df.format(xFact * point.getA()) + ","
							+ this.df.format(yFact * point.getB()) + ")\n");
				}
				if (transitions.getN() >= this.requiredTransitions) {
					result.append("\\psdot[dotstyle=+]("
							+ this.df.format(xFact
									* transitions.getPercentile(50)) + ","
							+ this.df.format(yFact * values.getPercentile(50))
							+ ")\n");
					final double x1 = xFact * transitions.getPercentile(25);
					final double x2 = xFact * transitions.getPercentile(75);
					final double y1 = yFact * values.getPercentile(25);
					final double y2 = yFact * values.getPercentile(75);
					result.append("\\psframe" + style);
					// result.append("\\psline" + style);
					result.append("(" + this.df.format(x1) + ","
							+ this.df.format(y1) + ")");
					// result.append("\\psline" + style);
					// result.append("(" + this.df.format(x1) + ","
					// + this.df.format(y1) + ")");
					// result.append("(" + this.df.format(x2) + ","
					// + this.df.format(y1) + ")");
					result.append("(" + this.df.format(x2) + ","
							+ this.df.format(y2) + ")");
					// result.append("(" + this.df.format(x1) + ","
					// + this.df.format(y2) + ")");
					// result.append("(" + this.df.format(x1) + ","
					// + this.df.format(y1) + ")");
					result.append("\n");
				}
				outerIt++;
			} else {
				dataAvail = false;
			}

		} while (dataAvail);

		return result.toString();
	}

	static void createLatex() {

		System.out.println("STARTED ...");

		final int[] populationSizes = new int[] { 2, 4, 8, 16, 32, 64, 128 };
		final String[] runIdentifiers = new String[] { "a", "b", "c", "d", "e",
				"f", "g", "h", "i", "j" };

		for (int populationSize : populationSizes) {
			final ArticleFigureCreator afcNaive = new ArticleFigureCreator(10);
			final ArticleFigureCreator afcProposed = new ArticleFigureCreator(
					10);
			for (String run : runIdentifiers) {
				{ // read naive
					final String naiveFileName = "./output/roadpricing/logfiles/naive_"
							+ populationSize + run + ".opt";
					if (new File(naiveFileName).exists()) {
						final OptFileReader reader = new OptFileReader(
								naiveFileName);
						afcNaive.add(reader.getOptFileSummary());
					}
				}
				{ // read proposed
					final String proposedFileName = "./output/roadpricing/logfiles/search_"
							+ populationSize + run + ".opt";
					if (new File(proposedFileName).exists()) {
						final OptFileReader reader = new OptFileReader(
								proposedFileName);
						afcProposed.add(reader.getOptFileSummary());
					}
				}
			}

			final double xFact = 1.0 / 1000.0;
			final double yFact = 1.0;

			// write objective function values
			try {
				{
					final PrintWriter writer = new PrintWriter(
							"./output/roadpricing/latex/ObjFctVals_popSize"
									+ populationSize + "_naive.tex");
					writer.println(afcNaive.asPSTricksSnippet(xFact, yFact,
							"[linestyle=solid]", WhatToWrite.ObjFctVals));
					writer.flush();
					writer.close();
				}
				{
					final PrintWriter writer = new PrintWriter(
							"./output/roadpricing/latex/ObjFctVals_popSize"
									+ populationSize + "_search.tex");
					writer.println(afcProposed.asPSTricksSnippet(xFact, yFact,
							"[linestyle=solid]", WhatToWrite.ObjFctVals));
					writer.flush();
					writer.close();
				}
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
			// write equilibrium gaps
			try {
				final PrintWriter writer = new PrintWriter(
						"./output/roadpricing/latex/EquilGaps_popSize"
								+ populationSize + ".tex");
				writer.println(afcProposed.asPSTricksSnippet(xFact, yFact,
						"[linestyle=solid]", WhatToWrite.EquilGaps));
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
				writer.println(afcProposed.asPSTricksSnippet(xFact, yFact,
						"[linestyle=solid]", WhatToWrite.UnifGaps));
				writer.flush();
				writer.close();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		System.out.println("... DONE");
	}

	static void createWeights() {

		final Covariance cov = new Covariance(2, 2);

		// final int[] populationSizes = new int[] { 2, 4, 8, 16, 32, 64, 128,
		// 256 };
		final int[] populationSizes = new int[] { 8, 16, 32, 64, 128 };
		final String[] runIdentifiers = new String[] { "a", "b", "c", "d", "e",
				"f", "g", "h", "i", "j" };
		for (int populationSize : populationSizes) {
			for (String run : runIdentifiers) {
				// read proposed
				final String proposedFileName = "./output/roadpricing/logfiles/search_"
						+ populationSize + run + ".opt";
				if (new File(proposedFileName).exists()) {
					final OptFileReader reader = new OptFileReader(
							proposedFileName);
					final OptFileSummary summary = reader.getOptFileSummary();
					// for (int index = 0; index < summary.getStageCnt();
					// index++) {
					for (int index = summary.getStageCnt() - 1; index < summary
							.getStageCnt(); index++) {
						final double equilGapWeight = summary
								.getInitialEquilbriumGapWeights().get(index);
						final double unifGapWeight = summary
								.getInitialUniformityGapWeights().get(index);
						System.out.println("\t\\psdot("
								+ (1000.0 * equilGapWeight) + ","
								+ (10.0 * unifGapWeight) + ")\n");
						cov.add(new Vector(equilGapWeight, unifGapWeight),
								new Vector(equilGapWeight, unifGapWeight));
					}
					// System.out.println();
				}
			}
		}

		System.out.println();
		System.out.println("corr matrix:");
		System.out.println(cov.getCorrelation());

	}

	static void createDetourFlows() {

		/*
		 * WITHOUT TOLL, MORNING PEAK
		 */
//		final String[] results = new String[] {
//				"0	0	0	0	0	5.2	13.4	588.2	744.2	686.2	298.2	9.2	0.4	0	0	0	0	0	0	0	0	0	0	0",
//				"0	0	0	0	0	7	15.8	583.8	769	640.6	294.4	7.6	0.6	0	0	0	0	0	0	0	0	0	0	0",
//				"0	0	0	0	0	4.8	16.6	580.6	793.4	628.6	297.2	6.8	0.8	0	0	0	0	0	0	0	0	0	0	0",
//				"0	0	0	0	0	7.4	11.6	605	750.6	689.6	280.6	6.4	0.4	0	0	0	0	0	0	0	0	0	0	0",
//				"0	0	0	0	0	5	16	610.8	766.4	688	283.4	6.6	0.2	0	0	0	0	0	0	0	0	0	0	0",
//				"0	0	0	0	0	5.2	17.4	612.4	773.8	651.8	278.8	8.8	1.4	0	0	0	0	0	0	0	0	0	0	0",
//				"0	0	0	0	0	2.4	16.6	615	763.6	666.8	309.2	7.2	0.4	0	0	0	0	0	0	0	0	0	0	0",
//				"0	0	0	0	0	6.2	16.2	591	760.2	638.8	292	5	1	0	0	0	0	0	0	0	0	0	0	0",
//				"0	0	0	0	0	4	15.2	595.8	764.8	642.4	276.6	6.4	1	0	0	0	0	0	0	0	0	0	0	0",
//				"0	0	0	0	0	6	15.6	604	788.2	642.6	289.4	7	0.6	0	0	0	0	0	0	0	0	0	0	0" };

		/*
		 * WITHOUT TOLL, EVENING PEAK
		 */
//		 final String[] results = new String[] {
//		 "0	0	0	0	0	0	0	0	0	0	0	0	0.6	8.4	179.4	627.2	659.2	740.2	131.4	4.6	0	0	0	0",
//		 "0	0	0	0	0	0	0	0	0	0	0	0	0	7.4	165.2	611	627.4	674	98.8	2.2	0	0	0	0",
//		 "0	0	0	0	0	0	0	0	0	0	0	0	0	6.4	170.8	637.6	607.8	709.4	133.8	3.4	0	0	0	0",
//		 "0	0	0	0	0	0	0	0	0	0	0	0	0.2	7.4	185.2	623.8	646.4	629.8	107.8	3.4	0	0	0	0",
//		 "0	0	0	0	0	0	0	0	0	0	0	0	0.4	10.2	199	605	658.6	659.4	114.6	4.2	0	0	0	0",
//		 "0	0	0	0	0	0	0	0	0	0	0	0	0.6	7.4	189.2	653.2	626.6	738.8	140	3	0	0	0	0",
//		 "0	0	0	0	0	0	0	0	0	0	0	0	0.4	7.8	176.8	614.2	629.4	616	103.2	4.6	0	0	0	0",
//		 "0	0	0	0	0	0	0	0	0	0	0	0	0	5.8	185.6	614	659.4	681.6	157	3.2	0	0	0	0",
//		 "0	0	0	0	0	0	0	0	0	0	0	0	0.4	4.6	178.4	619	655.8	699.6	139.6	4.4	0	0	0	0",
//		 "0	0	0	0	0	0	0	0	0	0	0	0	0.2	5.6	185.6	608.6	665	648.2	164.6	3.4	0	0	0	0"
//		 };

		/*
		 * WITH TOLL, MORNING PEAK
		 */
//		 final String[] results = new String[] {
//		 "0	0	0	0	0	10.4	25.2	1056.2	1151.8	1132.8	970.2	221.4	11.2	0.8	0	0	0	0	0	0	0	0	0	0",
//		 "0	0	0	0	0	6.8	20.4	993.4	1143.8	1129.8	994	380.2	22	1.6	0	0	0	0	0	0	0	0	0	0",
//		 "0	0	0	0	0	7.4	23	996.6	1147	1123	995.6	398.2	20.4	1.4	0	0	0	0	0	0	0	0	0	0",
//		 "0	0	0	0	0	8.2	24.8	1078	1144.8	1128.8	916.4	251.2	41.4	4.6	0.4	0	0	0	0	0	0	0	0	0",
//		 "0	0	0	0	0	10.4	22.8	1006.6	1152	1131	1002.6	373	20.4	2.2	0	0	0	0	0	0	0	0	0	0",
//		 "0	0	0	0	0	5.8	23.6	977	1147.2	1144.6	640.4	17.6	2.4	0	0	0	0	0	0	0	0	0	0	0",
//		 "0	0	0	0	0	8.6	20.2	1054.4	1148.6	1119.2	1020.2	377.6	18.8	1.8	0	0	0	0	0	0	0	0	0	0",
//		 "0	0	0	0	0	8.6	22	999.6	1143	1140.2	1004.8	355.6	24.2	1.4	0	0	0	0	0	0	0	0	0	0",
//		 "0	0	0	0	0	9.4	22.8	1012	1140	1126.4	1022.6	357.6	24.2	1.2	0	0	0	0	0	0	0	0	0	0",
//		 "0	0	0	0	0	10	22.6	1057.6	1145.4	1137.6	987.4	383.8	25.6	2.8	0	0	0	0	0	0	0	0	0	0"
//		 };
		/*
		 * WITH TOLL, EVENING PEAK
		 */
		 final String[] results = new String[] {
		 "0	0	0	0	0	0	0	0	0	0	0	0.8	15.2	192.2	780	1113.4	1147.2	1146.6	109.2	8	0	0	0	0",
		 "0	0	0	0	0	0	0	0	0	0	0	1.4	16.2	307.8	929.4	1135.2	1148.2	1145.2	86.6	6.8	0	0	0	0",
		 "0	0	0	0	0	0	0	0	0	0	0	0.4	15.2	316.6	933.2	1129.6	1149	1152	86.8	5.4	0	0	0	0",
		 "0	0	0	0	0	0	0	0	0	0	0	3.6	17.8	241.6	752.4	1135.6	1142.6	1150	109.8	4	0	0	0	0",
		 "0	0	0	0	0	0	0	0	0	0	0	1.2	21.4	325.2	905	1123	1150	1152	98	5.6	0	0	0	0",
		 "0	0	0	0	0	0	0	0	0	0	0	0	0.2	11.8	277.6	1138.2	1152	1151.8	121.8	6	0	0	0	0",
		 "0	0	0	0	0	0	0	0	0	0	0	0.4	11.6	307.8	927.8	1127	1147.4	1146.6	98	4.4	0.2	0	0	0",
		 "0	0	0	0	0	0	0	0	0	0	0	0.8	18	301	942.6	1130.2	1139	1149.4	86.4	6.4	0	0	0	0",
		 "0	0	0	0	0	0	0	0	0	0	0	0.6	14.2	296.2	939.6	1127.6	1140	1147.2	94.2	4.8	0	0	0	0",
		 "0	0	0	0	0	0	0	0	0	0	0	1	19	307.6	911.8	1132	1142	1148.6	104.4	8.6	0	0	0	0"
		 };

		final List<List<Double>> allDataHW = new ArrayList<>();
		for (final String result : results) {
			final List<Double> data = new ArrayList<>();
			allDataHW.add(data);
			for (String element : result.split("\\s")) {
				element = element.trim();
				if (element.length() > 0) {
					final double val = Double.parseDouble(element);
					data.add(val);
				}
			}
			System.out.println();
			System.out.println();
		}

		List<Double> modes = new ArrayList<>();

		for (int k = 0; k < allDataHW.get(0).size(); k++) {

			final DescriptiveStatistics stat = new DescriptiveStatistics();
			for (List<Double> data : allDataHW) {
				stat.addValue(data.get(k));
			}

			final double x = 0.5 + k;
			final double dx = 0.4;

			final double fact = 1.0;
			final double min = fact * stat.getMin();
			final double max = fact * stat.getMax();
			final double p25 = fact * stat.getPercentile(25);
			final double p50 = fact * stat.getPercentile(50);
			final double p75 = fact * stat.getPercentile(75);

			modes.add(p50);

			// the box
			System.out.println("\\psframe*[linecolor=white](" + (x - dx) + ","
					+ p25 + ")(" + (x + dx) + "," + p75 + ")\n");
			System.out.println("\\psframe(" + (x - dx) + "," + p25 + ")("
					+ (x + dx) + "," + p75 + ")\n");
			System.out.println("\\psline(" + (x - dx) + "," + p50 + ")("
					+ (x + dx) + "," + p50 + ")\n");

			// upper vertical line
			System.out.println("\\psline(" + x + "," + p75 + ")(" + x + ","
					+ max + ")\n");
			// upper horizontal line
			System.out.println("\\psline(" + (x - 0.5 * dx) + "," + max + ")("
					+ (x + 0.5 * dx) + "," + max + ")\n");

			// lower vertical line
			System.out.println("\\psline(" + x + "," + min + ")(" + x + ","
					+ p25 + ")\n");
			// upper horizontal line
			System.out.println("\\psline(" + (x - 0.5 * dx) + "," + min + ")("
					+ (x + 0.5 * dx) + "," + min + ")\n");

			System.out.println();

		}

		double x = 0.5;
		for (Double mode : modes) {
			System.out.print("(" + x + "," + mode + ")");
			x += 1.0;
		}
	}

	public static void main(String[] args) {
		// createDetourFlows();
		// createLatex();
		createWeights();
	}
}
