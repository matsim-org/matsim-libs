/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.ivtBaseline.preparation.crossborderCreation;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.facilities.ActivityFacility;

/**
 * Implementation of the trunk class CreateCBsubpop for the creation of work single-trip cb-agents.
 *
 * @author boescpa
 */
public class CreateCBWork extends CreateCBsubpop {

	private CreateCBWork(String pathToFacilities, String pathToCumulativeDepartureProbabilities, double samplePercentage, long randomSeed) {
		super(pathToFacilities, pathToCumulativeDepartureProbabilities, samplePercentage, randomSeed);
	}

	public static void main(final String[] args) {
		final String pathToFacilities = args[0];
		final String pathToCumulativeDepartureProbabilities = args[1];
		final double samplePercentage = Double.parseDouble(args[2]);
		final long randomSeed = Long.parseLong(args[3]);
		final String pathToCB_transit = args[4];
		final String pathToOutput_CBPopulation = args[5];

		CreateCBWork cbSecondaryActivities =
				new CreateCBWork(pathToFacilities, pathToCumulativeDepartureProbabilities, samplePercentage, randomSeed);

		cbSecondaryActivities.createCBPopulation(pathToCB_transit);
		cbSecondaryActivities.writeOutput(pathToOutput_CBPopulation);
	}

	@Override
	void createCBPopulation(String path2CBFile) {

	}

	@Override
	Plan createSingleTripPlan(ActivityFacility origFacility, ActivityFacility destFacility) {
		return null;
	}
}
