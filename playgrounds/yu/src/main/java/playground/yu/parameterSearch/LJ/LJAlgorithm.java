/* *********************************************************************** *
 * project: org.matsim.*
 * LJAlgorithm.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.yu.parameterSearch.LJ;

import java.util.Random;

import org.matsim.core.gbl.MatsimRandom;

import playground.yu.parameterSearch.PatternSearchAlgoI;
import playground.yu.utils.io.SimpleWriter;

/**
 * Implementation of the algorithm under
 * http://en.wikipedia.org/wiki/Luus%E2%80%93Jaakola
 *
 * @author yu
 *
 */
public class LJAlgorithm implements PatternSearchAlgoI {
	private final double[] lowerBoundaries, upperBoundaries;
	private final int dimension, maxIter;
	private double[] parameters;
	private final double[] trial;
	private final double[] samplingRange;
	private double objective = Double.MAX_VALUE,
			samplingRangeDecreaseRatio = 0.95;
	private final Random random = MatsimRandom.getRandom();
	private int n = 0;
	private final double criterion;
	private SimpleWriter writer = null;

	public LJAlgorithm(final double[] lowerBoundaries,
			final double[] upperBoundaries) {
		this(lowerBoundaries, upperBoundaries, 1, 300);
	}

	/**
	 * @param lowerBoundaries
	 * @param upperBoundaries
	 * @param criterion
	 *            a positive value
	 */
	public LJAlgorithm(final double[] lowerBoundaries,
			final double[] upperBoundaries, final double criterion,
			final int maxIter) {
		this.lowerBoundaries = lowerBoundaries;
		this.upperBoundaries = upperBoundaries;
		dimension = lowerBoundaries.length;
		trial = new double[dimension];
		samplingRange = new double[dimension];
		if (dimension != upperBoundaries.length) {
			throw new RuntimeException(
					"FATAL: The lower- und upperBoundaries should have the same dimension!!");
		}
		for (int i = 0; i < dimension; i++) {
			samplingRange[i] = this.upperBoundaries[i]
					- this.lowerBoundaries[i];
		}
		this.criterion = criterion;
		this.maxIter = maxIter;
	}

	public void setOutputFilename(String filename) {
		writer = new SimpleWriter(filename);
	}

	public void initializeParameters() {
		parameters = new double[dimension];

		for (int i = 0; i < dimension; i++) {
			double randomD = random.nextDouble();
			parameters[i] = upperBoundaries[i] * randomD + lowerBoundaries[i]
					* (1d - randomD);
		}
	}

	public void initializeParameters(double[] parameters) {
		if (parameters.length != dimension) {
			throw new RuntimeException("The parameters has dimension = "
					+ parameters.length + ", which should be " + dimension
					+ ".");
		}
		this.parameters = parameters;
	}

	public double[] getParameters() {
		return parameters;
	}

	/**
	 * should be called "after" {@code ScoringListener} and "before"
	 * {@code ReplanningListener}, which means it is possible only by
	 * {@code IterationStartsListener} or {@code IterationEndsListener}
	 *
	 * @return
	 */
	@Override
	public double[] getTrial() {
		if (n == 0) {
			return parameters;
		}
		return trial;
	}

	public void setSamplingRangeDecreaseRatio(double samplingRangeDecreaseRatio) {
		this.samplingRangeDecreaseRatio = samplingRangeDecreaseRatio;
	}

	/**
	 * should be called by or "after" {@code AfterMobsimListener}
	 *
	 * @param objective
	 */
	@Override
	public void setObjective(double objective) {
		double difference = objective - this.objective;
		writer.writeln(">>>>>Difference\t(tried objective - objective):\t"
				+ difference + "\t(" + objective + "\t- " + this.objective
				+ ")");
		if (n < maxIter && (difference < -criterion || difference > criterion)) {
			if (difference < 0) {/* move to the new position */
				for (int i = 0; i < dimension; i++) {
					parameters[i] = trial[i];
				}
				this.objective = objective;
			} else/* >criterion */{/* decrease the sampling-range */
				for (int i = 0; i < dimension; i++) {
					samplingRange[i] *= samplingRangeDecreaseRatio;
				}
			}

			if (writer != null) {
				writer.write(">>>>>N =\t" + n + "\tsearchRange =");
				for (int i = 0; i < dimension; i++) {
					writer.write("\t" + samplingRange[i]);
				}
				writer.write(", parameters =");
				for (int i = 0; i < dimension; i++) {
					writer.write("\t" + parameters[i]);
				}
				writer.writeln("\tobjective =\t" + this.objective);
				writer.flush();
			}
			createTrial();

			n++;
		} else if (difference >= -criterion && difference < criterion) {
			if (writer != null) {
				writer.writeln("Bingo! We reached the best soluations:\t");
				writer.write(">>>>>\tN =\t" + n + ", parameters ="
				// + parameters
				);
				for (int i = 0; i < dimension; i++) {
					writer.write("\t" + parameters[i]);
				}
				writer.writeln();
				writer.flush();
				writer.close();
				System.exit(0);
			}
		} else if (n >= maxIter) {
			if (writer != null) {
				writer.writeln("Bummer! We haven't reach the best soluation.");
				writer.write(">>>>>\tN =\t" + n + ", parameters ="
				// + parameters
				);
				for (int i = 0; i < dimension; i++) {
					writer.write("\t" + parameters[i]);
				}
				writer.writeln();
				writer.flush();
				writer.close();
				System.exit(0);
			}
		}
	}

	@Override
	public void createTrial() {
		for (int i = 0; i < dimension; i++) {
			double randomD = random.nextDouble();
			trial[i] = Math.max(lowerBoundaries[i], Math.min(
					upperBoundaries[i], parameters[i] + (randomD * 2d - 1d)
							* samplingRange[i]/* increment */));
		}
		writer.write(">>>>>in creating:\ttried parameters:\tN =\t" + n
				+ ", trial =");
		for (int i = 0; i < dimension; i++) {
			writer.write("\t" + trial[i]);
		}
		writer.writeln();
		writer.flush();
	}
}
