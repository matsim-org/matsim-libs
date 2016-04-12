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
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.*;
import playground.boescpa.ivtBaseline.preparation.PrefsCreator;
import playground.boescpa.lib.obj.CSVReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static playground.boescpa.ivtBaseline.preparation.IVTConfigCreator.*;

/**
 * Implementation of the trunk class CreateCBsubpop for the creation of single-trip work cb-agents.
 *
 * @author boescpa
 */
public class CreateCBWork extends CreateSingleTripPopulation {

	private static final double VICINITY_RADIUS = 10000; // radius [m] around zone centroid which is considered vicinity
	private final List<Tuple<Double, Coord>> cumProbWorkFromA = new ArrayList<>();
	private final List<Tuple<Double, Coord>> cumProbWorkFromD = new ArrayList<>();
	private final List<Tuple<Double, Coord>> cumProbWorkFromF = new ArrayList<>();
	private final List<Tuple<Double, Coord>> cumProbWorkFromI = new ArrayList<>();
	private final Map<String, List<ActivityFacility>> communities = new HashMap<>();

	public CreateCBWork(CreateSingleTripPopulationConfigGroup configGroup) {
		super(configGroup);
	}


	/*public static void main(final String[] args) {
		final String pathToFacilities = args[0]; // all scenario facilities incl secondary facilities and bc facilities.
		final String pathToCumulativeDepartureProbabilities = args[1];
		final double samplePercentage = Double.parseDouble(args[2]);
		final long randomSeed = Long.parseLong(args[3]);
		final String pathToCB_workDestination = args[4];
		final String pathToCB_workOrigin = args[5];
		final String pathToOutput_CBPopulation = args[6];

		CreateCBWork cbSecondaryActivities =
				new CreateCBWork(pathToFacilities, pathToCumulativeDepartureProbabilities, samplePercentage, randomSeed);

		cbSecondaryActivities.readDestinations(pathToCB_workDestination);
		cbSecondaryActivities.createCBPopulation(pathToCB_workOrigin);
		cbSecondaryActivities.writeOutput(pathToOutput_CBPopulation);
	}*/

	@Override
	void readDestinations() {
		CSVReader reader = new CSVReader(this.configGroup.getPathToDestinationsFile(), this.configGroup.getDelimiter());
		reader.skipLine(); // header
		log.info("CB-Dest creation...");
		Counter counter = new Counter(" CB-Destination # ");
		String[] lineElements = reader.readLine();
		while (lineElements != null) {
			counter.incCounter();
			Coord coord = new Coord(Double.parseDouble(lineElements[1]), Double.parseDouble(lineElements[2]));
			cumProbWorkFromA.add(new Tuple<>(Double.parseDouble(lineElements[3]), coord));
			cumProbWorkFromD.add(new Tuple<>(Double.parseDouble(lineElements[4]), coord));
			cumProbWorkFromF.add(new Tuple<>(Double.parseDouble(lineElements[5]), coord));
			cumProbWorkFromI.add(new Tuple<>(Double.parseDouble(lineElements[6]), coord));
			addFacilitiesToCommunity(coord);
			lineElements = reader.readLine();
		}
		counter.printCounter();
		log.info("CB-Dest creation... done.");
	}

	private void addFacilitiesToCommunity(Coord coord) {
		List<ActivityFacility> communalList = new ArrayList<>();
		for (ActivityFacility facility : getOrigFacilities().getFacilitiesForActivityType(WORK).values()) {
			if (CoordUtils.calcEuclideanDistance(facility.getCoord(), coord) <= VICINITY_RADIUS) {
				communalList.add(facility);
			}
		}
		if (communalList.isEmpty()) {
			Id<ActivityFacility> facilityId = Id.create("temp_" + coord.toString(), ActivityFacility.class);
			ActivityFacility tempFacility = getOrigFacilities().getFactory().createActivityFacility(facilityId, coord);
			getOrigFacilities().addActivityFacility(tempFacility);
			ActivityOption activityOption = new ActivityOptionImpl(WORK);
			activityOption.addOpeningTime(new OpeningTimeImpl(0.0, 24.0*3600.0));
			tempFacility.addActivityOption(activityOption);
			communalList.add(tempFacility);
		}
		communities.put(coord.toString(), communalList);
	}

	@Override
	final void createCBPopulation() {//(String pathToCB_workOrigin) {
		CSVReader reader = new CSVReader(this.configGroup.getPathToOriginsFile(), this.configGroup.getDelimiter());
		reader.skipLine(); // header
		log.info("CB-Pop creation...");
		Counter counter = new Counter(" CB-Work-Pop - Zollst # ");
		String[] lineElements = reader.readLine();
		while (lineElements != null) {
			counter.incCounter();
			ActivityFacility homeFacility = getOrigFacilities().getFacilities().get(
					Id.create("BC_" + lineElements[0], ActivityFacility.class));
			if (homeFacility == null) {
				log.error("BC-Facility BC_" + lineElements[0] + " not found.");
			}
			switch (lineElements[1].charAt(0)) {
				case 'A': createWorkPopulation(homeFacility, Integer.parseInt(lineElements[2]), cumProbWorkFromA); break;
				case 'D': createWorkPopulation(homeFacility, Integer.parseInt(lineElements[2]), cumProbWorkFromD); break;
				case 'F': createWorkPopulation(homeFacility, Integer.parseInt(lineElements[2]), cumProbWorkFromF); break;
				case 'I': createWorkPopulation(homeFacility, Integer.parseInt(lineElements[2]), cumProbWorkFromI); break;
				default: log.error("Expected country " + lineElements[1].charAt(0) + " not in destinations.");
			}
			lineElements = reader.readLine();
		}
		counter.printCounter();
		log.info("CB-Pop creation... done.");
	}

	private void createWorkPopulation(ActivityFacility homeFacility, int numberOfAgents, List<Tuple<Double, Coord>> cumProbWorkFromX) {
		this.actTag = this.configGroup.getTag() + "Work";
		for (int i = 0; i < numberOfAgents; i++) {
			ActivityFacility workFacility = getWorkFacility(cumProbWorkFromX);
			createSingleTripAgent(homeFacility, workFacility, WORK);
		}
	}

	private ActivityFacility getWorkFacility(List<Tuple<Double, Coord>> cumProbWorkFromX) {
		double randCommunity = random.nextDouble();
		// identify selected community
		Coord coord = null;
		int i = 0;
		while (i < cumProbWorkFromX.size() && cumProbWorkFromX.get(i).getFirst() < randCommunity) {
			coord = cumProbWorkFromX.get(i).getSecond();
			i++;
		}
		// get a work facility in the perimeter of the center of this community.
		int randFacility = random.nextInt(communities.get(coord.toString()).size());
		return communities.get(coord.toString()).get(randFacility);
	}

	@Override
	final Plan createSingleTripPlan(ActivityFacility origFacility, ActivityFacility destFacility) {
		Plan plan = new PlanImpl();

		double departureTime = getDepartureTime();
		double actDuration = (8 + random.nextInt(12) + random.nextDouble())
			* PrefsCreator.actCharacteristics.valueOf(WORK.toUpperCase()).getMinDur();
		double returnTime = departureTime + actDuration;
		if (returnTime > 24.0 * 3600.0) {
			returnTime = 24.0 * 3600.0;
			departureTime = returnTime - actDuration;
		}

		ActivityImpl actStart = new ActivityImpl(this.configGroup.getTag() + "Home", origFacility.getCoord(), origFacility.getLinkId());
		actStart.setFacilityId(origFacility.getId());
		actStart.setStartTime(0.0);
		actStart.setMaximumDuration(departureTime);
		actStart.setEndTime(departureTime);
		plan.addActivity(actStart);

		plan.addLeg(new LegImpl("car"));

		ActivityImpl actSA = new ActivityImpl(this.actTag, destFacility.getCoord());
		//destFacility.getActivityOptions().get(this.actTag).setCapacity(
		//		destFacility.getActivityOptions().get(this.actTag).getCapacity() + 1);
		actSA.setFacilityId(destFacility.getId());
		actSA.setStartTime(departureTime);
		actSA.setMaximumDuration(returnTime - departureTime);
		actSA.setEndTime(returnTime);
		plan.addActivity(actSA);

		plan.addLeg(new LegImpl("car"));

		ActivityImpl actEnd = new ActivityImpl(this.configGroup.getTag() + "Home", origFacility.getCoord(), origFacility.getLinkId());
		actEnd.setFacilityId(origFacility.getId());
		actEnd.setStartTime(returnTime);
		//actEnd.setMaximumDuration(24.0 * 3600.0 - returnTime);
		plan.addActivity(actEnd);
		return plan;
	}
}
