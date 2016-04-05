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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacility;
import playground.boescpa.ivtBaseline.preparation.PrefsCreator;
import playground.boescpa.lib.obj.CSVReader;

import java.util.ArrayList;
import java.util.List;

import static playground.boescpa.ivtBaseline.preparation.IVTConfigCreator.*;

/**
 * Implementation of the trunk class CreateCBsubpop for the creation of single-trip work cb-agents.
 *
 * @author boescpa
 */
public class CreateCBWork extends CreateCBsubpop {

	private final List<Tuple<Double, Coord>> cumProbWorkFromA = new ArrayList<>();
	private final List<Tuple<Double, Coord>> cumProbWorkFromD = new ArrayList<>();
	private final List<Tuple<Double, Coord>> cumProbWorkFromF = new ArrayList<>();
	private final List<Tuple<Double, Coord>> cumProbWorkFromI = new ArrayList<>();

	private CreateCBWork(String pathToFacilities, String pathToCumulativeDepartureProbabilities, double samplePercentage, long randomSeed) {
		super(pathToFacilities, pathToCumulativeDepartureProbabilities, samplePercentage, randomSeed);
	}

	public static void main(final String[] args) {
		final String pathToFacilities = args[0];
		final String pathToCumulativeDepartureProbabilities = args[1];
		final double samplePercentage = Double.parseDouble(args[2]);
		final long randomSeed = Long.parseLong(args[3]);
		final String pathToCB_workDestination = args[4];
		final String pathToCB_workOrigin = args[5];
		final String pathToOutput_CBPopulation = args[6];

		CreateCBWork cbSecondaryActivities =
				new CreateCBWork(pathToFacilities, pathToCumulativeDepartureProbabilities, samplePercentage, randomSeed);

		cbSecondaryActivities.createDestinations(pathToCB_workDestination);
		cbSecondaryActivities.createCBPopulation(pathToCB_workOrigin);
		cbSecondaryActivities.writeOutput(pathToOutput_CBPopulation);
	}

	private void createDestinations(String pathToCB_workDestination) {
		CSVReader reader = new CSVReader(pathToCB_workDestination, DELIMITER);
		reader.skipLine(); // header
		String[] lineElements = reader.readLine();
		while (lineElements != null) {
			Coord coord = new Coord(Double.parseDouble(lineElements[1]), Double.parseDouble(lineElements[2]));
			cumProbWorkFromA.add(new Tuple<>(Double.parseDouble(lineElements[3]), coord));
			cumProbWorkFromD.add(new Tuple<>(Double.parseDouble(lineElements[4]), coord));
			cumProbWorkFromF.add(new Tuple<>(Double.parseDouble(lineElements[5]), coord));
			cumProbWorkFromI.add(new Tuple<>(Double.parseDouble(lineElements[6]), coord));
			lineElements = reader.readLine();
		}
	}

	@Override
	final void createCBPopulation(String pathToCB_workOrigin) {
		CSVReader reader = new CSVReader(pathToCB_workOrigin, DELIMITER);
		reader.skipLine(); // header
		String[] lineElements = reader.readLine();
		while (lineElements != null) {
			ActivityFacility homeFacility = getOrigFacilities().getFacilities().get(
					Id.create(CB_TAG + "_" + lineElements[0], ActivityFacility.class));
			switch (lineElements[1].charAt(0)) {
				case 'A': createWorkPopulation(homeFacility, Integer.parseInt(lineElements[2]), cumProbWorkFromA); break;
				case 'D': createWorkPopulation(homeFacility, Integer.parseInt(lineElements[2]), cumProbWorkFromD); break;
				case 'F': createWorkPopulation(homeFacility, Integer.parseInt(lineElements[2]), cumProbWorkFromF); break;
				case 'I': createWorkPopulation(homeFacility, Integer.parseInt(lineElements[2]), cumProbWorkFromI); break;
				default: log.error("Expected country " + lineElements[1].charAt(0) + " not in destinations.");
			}
			lineElements = reader.readLine();
		}
	}

	private void createWorkPopulation(ActivityFacility homeFacility, int numberOfAgents, List<Tuple<Double, Coord>> cumProbWorkFromX) {
		for (int i = 0; i < numberOfAgents; i++) {
			ActivityFacility workFacility = getWorkFacility(cumProbWorkFromX);
			createSingleTripAgent(homeFacility, workFacility, "work");
		}
	}

	private ActivityFacility getWorkFacility(List<Tuple<Double, Coord>> cumProbWorkFromX) {
		return null;
	}

	@Override
	final Plan createSingleTripPlan(ActivityFacility origFacility, ActivityFacility destFacility) {
		Plan plan = new PlanImpl();

		double departureTime = getDepartureTime();
		double actDuration = (8 + random.nextInt(12) + random.nextDouble())
			* PrefsCreator.actCharacteristics.valueOf(WORK.toUpperCase()).getMinDur();
		actDuration *= 60; // the above comes in minutes, we need seconds.
		double returnTime = departureTime + actDuration;
		if (returnTime > 24.0 * 3600.0) {
			returnTime = 24.0 * 3600.0;
			departureTime = returnTime - actDuration;
		}

		ActivityImpl actStart = new ActivityImpl(HOME, origFacility.getCoord(), origFacility.getLinkId());
		actStart.setFacilityId(origFacility.getId());
		actStart.setStartTime(0.0);
		actStart.setMaximumDuration(departureTime);
		actStart.setEndTime(departureTime);
		plan.addActivity(actStart);

		plan.addLeg(new LegImpl("car"));

		ActivityImpl actSA = new ActivityImpl(WORK, destFacility.getCoord(), destFacility.getLinkId());
		actSA.setFacilityId(destFacility.getId());
		actSA.setStartTime(departureTime);
		actSA.setMaximumDuration(returnTime - departureTime);
		actSA.setEndTime(returnTime);
		plan.addActivity(actSA);

		plan.addLeg(new LegImpl("car"));

		ActivityImpl actEnd = new ActivityImpl(HOME, origFacility.getCoord(), origFacility.getLinkId());
		actEnd.setFacilityId(origFacility.getId());
		actEnd.setStartTime(returnTime);
		actEnd.setMaximumDuration(24.0 * 3600.0 - returnTime);
		plan.addActivity(actEnd);
		return plan;
	}
}
