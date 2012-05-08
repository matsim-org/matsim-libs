/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatFitnessFunctionFactory.java
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
package playground.thibautd.planomat.api;

import org.jgap.Configuration;
import org.matsim.api.core.v01.population.Plan;

/**
 * Creates a fitness function to use in the Genetic Algorithm.
 * This class is responsible for creating fully-initialised
 * scoring functions, including travel time estimators, 
 * PlanomatChromosomeFactory, etc.
 *
 * @author thibautd
 */
public interface PlanomatFitnessFunctionFactory {
	/**
	 * Creates a new fitness function.
	 *
	 * @param jgapConfig the Configuration object. Be aware
	 * that this method is called <b>in the constructor</b>
	 * of the configuration object, and thus that it should
	 * only be used as a reference to store in the created
	 * scoring funtion.
	 * @param plan the plan for which this fitness function is created
	 * @param whiteList an {@link ActivityWhiteList} giving information
	 * on the activity type that planomat is allowed to optimise.
	 *
	 * @return An initialised {@link PlanomatFitnessFunction}
	 */
	public PlanomatFitnessFunction createFitnessFunction(
			Configuration jgapConfig,
			Plan plan,
			ActivityWhiteList whiteList);
}

