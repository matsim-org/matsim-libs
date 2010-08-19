/* *********************************************************************** *
 * project: org.matsim.*
 * HTDistribution.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.statistics;

import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.snowball2.sim.deprecated.PopulationEstimator;

/**
 * @author illenberger
 *
 */
public class EstimatedDistribution extends Distribution {

	private final PopulationEstimator estimator;
	
	public EstimatedDistribution(PopulationEstimator estimator) {
		this.estimator = estimator;
	}
	
	@Override
	public double mean() {
		if(estimator == null)
			return super.mean();
		else
			return estimator.mean(getValues(), getWeights());
	}

}
