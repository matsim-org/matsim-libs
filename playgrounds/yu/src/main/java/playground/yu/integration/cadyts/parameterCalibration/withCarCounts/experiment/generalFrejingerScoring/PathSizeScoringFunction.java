/* *********************************************************************** *
 * project: org.matsim.*
 * PathSizeScoringFunction.java
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

package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.experiment.generalFrejingerScoring;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.scoring.interfaces.BasicScoring;

import playground.yu.choiceSetGeneration.PathSizeFromPlanChoiceSet;

public class PathSizeScoringFunction implements BasicScoring {

	private Plan plan;
	private Network network;
	private ScoringParameters scoringParams;

	private double lnPathSizeAttr;

	public PathSizeScoringFunction(Plan plan, Network network, Config config) {
		this.plan = plan;
		this.network = network;
		scoringParams = new ScoringParameters(config);

		double PS = calculatePathSize();
		if (PS < 0d) {
			throw new RuntimeException(
					"Path-size should be bigger than 0.0, and smaller than or equal to 1.0!!!");
		}
		lnPathSizeAttr = Math.log(PS);
	}

	public double getLnPathSizeAttr() {
		return lnPathSizeAttr;
	}

	/**
	 * @return path-size of plan
	 */
	protected double calculatePathSize() {
		PathSizeFromPlanChoiceSet psfpcs = new PathSizeFromPlanChoiceSet(
				network, plan.getPerson().getPlans());
		return psfpcs.getPlanPathSize(plan);
	}

	@Override
	public void finish() {

	}

	/***/
	@Override
	public double getScore() {
		return scoringParams.betaLnPathSize * lnPathSizeAttr;
	}

	@Override
	public void reset() {

	}

}
