/* *********************************************************************** *
 * project: org.matsim.*
 * CrossOverRateCalculator.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules;

import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * Updates rate of the cross-overs dynamically, based on the fitnesses of the
 * children in the previous iteration.
 *
 * @author thibautd
 */
public class CrossOverRateCalculator implements RateCalculator {

	private static int N_OPERATORS = 3;
	private final double[] rates;
	private final double[] contributions;
	private final int[] operationCounts;

	private final double totalRate;

	/*
	 * =========================================================================
	 * Constructors
	 * =========================================================================
	 */
	public CrossOverRateCalculator(
			final JointReplanningConfigGroup configGroup) {
		this(configGroup.getWholeCrossOverProbability(),
				configGroup.getSimpleCrossOverProbability(),
				configGroup.getSingleCrossOverProbability());
	}

	public CrossOverRateCalculator(
			final double wholeCOInitialRate,
			final double simpleCOInitialRate,
			final double singleCOInitialRate) {
		this.rates = new double[N_OPERATORS];
		this.rates[0] = wholeCOInitialRate;
		this.rates[1] = simpleCOInitialRate;
		this.rates[2] = singleCOInitialRate;

		this.contributions = new double[N_OPERATORS];
		this.operationCounts = new int[N_OPERATORS];
		totalRate = wholeCOInitialRate + simpleCOInitialRate + 
			singleCOInitialRate;
	}

	/**
	 * @see RateCalculator#getRates()
	 */
	public double[] getRates() {
		return this.rates;
	}

	/**
	 * @see RateCalculator#addResult(int,double,double)
	 */
	public void addResult(
			final int operatorIndex,
			final double fitnessParent,
			final double fitnessChild) {
		double contribution = fitnessChild - fitnessParent;
		this.contributions[operatorIndex] += Math.max(0d, contribution);
		this.operationCounts[operatorIndex]++;
	}

	/**
	 * @see RateCalculator#iterationIsOver()
	 */
	public void iterationIsOver() {
		double coef = 0d;

		for (int i=0; i < N_OPERATORS; i++) {
			coef += this.contributions[i];
		}

		coef = (1d - (N_OPERATORS * this.totalRate))
			/ coef;

		for (int i=0; i < N_OPERATORS; i++) {
			this.rates[i] = coef * this.contributions[i] + this.totalRate;
		}
	}
}

