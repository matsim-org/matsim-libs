/* *********************************************************************** *
 * project: org.matsim.*
 * Covriance2DfromParameters.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 *
 */
package playground.yu.utils.io;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import utilities.math.Covariance;
import utilities.math.Matrix;
import utilities.math.Vector;

/**
 * calculates var{E{...}} (variance of expectation, the 2nd part of total
 * variance), see also: {@link http
 * ://en.wikipedia.org/wiki/Law_of_total_covariance}
 *
 * @author yu
 *
 */
public class Covariance2DfromParameters {
	public class ParameterFileHandler implements TabularFileHandler {
		private final List<Integer> iterations = new ArrayList<Integer>();
		private final List<Double> travelingPts = new ArrayList<Double>(),
				constantPts = new ArrayList<Double>();

		public List<Double> getConstantPts() {
			return constantPts;
		}

		public List<Integer> getIterations() {
			return iterations;
		}

		public List<Double> getTravelingPts() {
			return travelingPts;
		}

		@Override
		public void startRow(String[] row) {
			int size = row.length;
			if (size != 3) {
				throw new RuntimeException(
						"Each line in parameter file should contains 3 columes");
			}

			iterations.add(Integer.parseInt(row[0]));
			travelingPts.add(Double.parseDouble(row[1]));
			constantPts.add(Double.parseDouble(row[2]));
		}

	}

	// -----------------------------NORMAL------------------------------
	private static TabularFileParserConfig parserConfig = new TabularFileParserConfig();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String paramFilename, outputFilename;
		int width;
		if (args.length != 3) {
			// paramFilename = "test/input/bln2pct/baseSyn3PureParams.log";
			paramFilename = "test/input/bln2pct/CHmsd80pureParams.log";
			outputFilename = "test/output/bln2pct/CHmsd80varOfExp1kWindows.log";
			width = 1000;
		} else {
			paramFilename = args[0];
			outputFilename = args[1];
			width = Integer.parseInt(args[2]);
		}

		Covariance2DfromParameters cfp = new Covariance2DfromParameters(
				paramFilename, width, outputFilename);

		System.out.println("Done!");
	}

	private final List<Integer> iterations;

	private final List<Double> travelingPts, constantPts;

	private int windowWidth = 1000;

	private final SimpleWriter writer;

	public Covariance2DfromParameters(String filename, int windowWidth,
			String outputFilename) {
		this.windowWidth = windowWidth;
		// /////////////////////////////////////////////////////
		parserConfig.setStartTag("iter");
		parserConfig.setDelimiterTags(new String[] { "\t" });
		parserConfig.setFileName(filename);

		writer = new SimpleWriter(outputFilename);
		writer.writeln("Iteration\tvarianceOfExpection");
		writer.flush();

		ParameterFileHandler fileHandler = new ParameterFileHandler();

		new TabularFileParser().parse(parserConfig, fileHandler);

		iterations = fileHandler.getIterations();
		travelingPts = fileHandler.getTravelingPts();
		constantPts = fileHandler.getConstantPts();
		// ///////////////////////////////////////////////////////
		createCovrainces();
		writer.close();
	}

	private Matrix createCovariance(int endIndex) {
		Covariance cov = new Covariance(2, 2);
		for (int idx = endIndex; idx > endIndex - windowWidth; idx--) {
			Vector params = new Vector(travelingPts.get(idx),
					constantPts.get(idx));
			cov.add(params, params);
		}
		return cov.getCovariance();
	}

	private void createCovrainces() {
		for (int idx = iterations.size() - 1; idx >= windowWidth - 1; idx--) {
			int iteration = iterations.get(idx);
			Matrix covariance = createCovariance(idx);
			writer.writeln(iteration + "\t" + covariance.toSingleLineString());
			writer.flush();
		}
	}

}
