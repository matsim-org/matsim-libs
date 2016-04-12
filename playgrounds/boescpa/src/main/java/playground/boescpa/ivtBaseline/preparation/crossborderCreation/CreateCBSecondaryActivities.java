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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;
import playground.boescpa.ivtBaseline.preparation.PrefsCreator;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static playground.boescpa.ivtBaseline.preparation.IVTConfigCreator.*;
import static playground.boescpa.ivtBaseline.preparation.secondaryFacilityCreation.CreationOfCrossBorderFacilities.*;

/**
 * Implementation of the trunk class CreateCBsubpop for the creation of secondary activity single-trip cb-agents.
 *
 * @author boescpa
 */
public class CreateCBSecondaryActivities extends CreateSingleTripPopulation {

	private static final double MAX_BEELINE_DISTANCE = 20000;

	public CreateCBSecondaryActivities(CreateSingleTripPopulationConfigGroup configGroup) {
		super(configGroup);
	}

	/*public static void main(final String[] args) {
		final String pathToFacilities = args[0]; // all scenario facilities incl secondary facilities and bc facilities.
		final String pathToCumulativeDepartureProbabilities = args[1];
		final double samplePercentage = Double.parseDouble(args[2]);
		final long randomSeed = Long.parseLong(args[3]);
		final String pathToCB_SA = args[4];
		final String pathToOutput_CBPopulation = args[5];

		CreateCBSecondaryActivities cbSecondaryActivities =
				new CreateCBSecondaryActivities(pathToFacilities, pathToCumulativeDepartureProbabilities, samplePercentage, randomSeed);

		cbSecondaryActivities.createCBPopulation(pathToCB_SA);
		cbSecondaryActivities.writeOutput(pathToOutput_CBPopulation);
	}*/

	@Override
	void readDestinations() {
		// Here the destinations are given by the provided facilities. Ergo they don't need to be explicitly read.
	}

	@Override
	final void createCBPopulation() {
		BufferedReader reader = IOUtils.getBufferedReader(this.configGroup.getPathToOriginsFile());
		try {
			reader.readLine(); // read header
			log.info("CB-Pop creation...");
			Counter counter = new Counter(" CB-SA-Pop - Zollst # ");
			String line = reader.readLine();
			while (line != null) {
				counter.incCounter();
				String[] lineElements = line.split(this.configGroup.getDelimiter());
				ActivityFacility homeFacility =
						getOrigFacilities().getFacilities().get(Id.create(BC_TAG + lineElements[0], ActivityFacility.class));
				if (homeFacility == null) {
					log.error("Expected BC-Facility " + BC_TAG + lineElements[0] + " not found. Will continue but population contain this demand.");
					continue;
				}
				this.actTag = this.configGroup.getTag() + "Shop";
				List<Tuple<Integer, ActivityFacility>> shopCandidates = getFacilityCandidates(homeFacility, SHOP);
				for (int i = 0; i < Integer.parseInt(lineElements[1]); i++) {
					createSingleTripAgent(homeFacility, getSAFacility(shopCandidates, SHOP), SHOP);
				}
				this.actTag = this.configGroup.getTag() + "Leisure";
				List<Tuple<Integer, ActivityFacility>> leisureCandidates = getFacilityCandidates(homeFacility, LEISURE);
				for (int i = 0; i < Integer.parseInt(lineElements[2]); i++) {
					createSingleTripAgent(homeFacility, getSAFacility(leisureCandidates, LEISURE), LEISURE);
				}
				line = reader.readLine();
			}
			counter.printCounter();
			log.info("CB-Pop creation... done.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private List<Tuple<Integer, ActivityFacility>> getFacilityCandidates(ActivityFacility facility, String seondaryActivityType) {
		List<Tuple<Integer, ActivityFacility>> shopFacilitiesForBC = new ArrayList<>();
		for (ActivityFacility facilityCandidate : getOrigFacilities().getFacilitiesForActivityType(seondaryActivityType).values()) {
			if (!facilityCandidate.getId().toString().contains(BC_TAG) && // we don't want the BC-facilities themselves to be candidates here.
					CoordUtils.calcEuclideanDistance(facility.getCoord(), facilityCandidate.getCoord()) < MAX_BEELINE_DISTANCE) {
				if (shopFacilitiesForBC.isEmpty()) {
					shopFacilitiesForBC.add(new Tuple<>(0, facilityCandidate));
				} else {
					int cummulativeProbFacility = shopFacilitiesForBC.get(shopFacilitiesForBC.size()-1).getFirst()
							+ (int) Math.round(shopFacilitiesForBC.get(shopFacilitiesForBC.size() - 1).getSecond()
							.getActivityOptions().get(seondaryActivityType).getCapacity());
					shopFacilitiesForBC.add(new Tuple<>(cummulativeProbFacility, facilityCandidate));
				}
			}
		}
		return shopFacilitiesForBC;
	}

	private ActivityFacility getSAFacility(List<Tuple<Integer, ActivityFacility>> candidates, String saType) {
		int maxCapSum = candidates.get(candidates.size()-1).getFirst()
				+ (int) Math.round(candidates.get(candidates.size()-1).getSecond().getActivityOptions().get(saType).getCapacity());
		int randFacility = random.nextInt(maxCapSum);
		// identify selected facility
		int currentCandidate = 0;
		while (currentCandidate < (candidates.size() - 1) && candidates.get(currentCandidate + 1).getFirst() < randFacility) {
			currentCandidate++;
		}
		return candidates.get(currentCandidate).getSecond();
	}

	@Override
	final Plan createSingleTripPlan(ActivityFacility origFacility, ActivityFacility destFacility) {
		Plan plan = new PlanImpl();

		double departureTime = getDepartureTime();
		double actDuration = (4 + random.nextInt(24) + random.nextDouble())
				* PrefsCreator.actCharacteristics.valueOf(this.actTag.substring(2).toUpperCase()).getMinDur();
		if (this.actTag.substring(2).equals(SHOP)) {
			actDuration *= 1.5; // shop has a very small min-time (representing kiosk-shopping)
		}
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

		ActivityImpl actSA = new ActivityImpl(this.actTag, destFacility.getCoord(), destFacility.getLinkId());
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
