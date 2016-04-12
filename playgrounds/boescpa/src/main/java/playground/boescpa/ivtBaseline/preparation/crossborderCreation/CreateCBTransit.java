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
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;
import playground.boescpa.ivtBaseline.preparation.secondaryFacilityCreation.CreationOfCrossBorderFacilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static playground.boescpa.ivtBaseline.preparation.IVTConfigCreator.HOME;

/**
 * Implementation of the trunk class CreateCBsubpop for the creation of transit single-trip cb-agents.
 *
 * @author boescpa
 */
public class CreateCBTransit extends CreateSingleTripPopulation {

	private Map<Character, List<Tuple<Double, ActivityFacility>>> destFacilities;

	public CreateCBTransit(CreateSingleTripPopulationConfigGroup configGroup) {
		super(configGroup);
	}

	/*public static void main(final String[] args) {
		final String pathToFacilities = args[0]; // all scenario facilities incl secondary facilities and bc facilities.
		final String pathToCumulativeDepartureProbabilities = args[1];
		final double samplePercentage = Double.parseDouble(args[2]);
		final long randomSeed = Long.parseLong(args[3]);
		final String pathToCB_transit = args[4];
		final String pathToOutput_CBPopulation = args[5];

		CreateCBTransit cbTransit = new CreateCBTransit(pathToFacilities, pathToCumulativeDepartureProbabilities, samplePercentage, randomSeed);
		cbTransit.readDestinations(pathToCB_transit);
		cbTransit.createCBPopulation(pathToCB_transit);
		cbTransit.writeOutput(pathToOutput_CBPopulation);
	}*/

	@Override
	void readDestinations() {
		this.destFacilities = new HashMap<>();
		BufferedReader reader = IOUtils.getBufferedReader(this.configGroup.getPathToOriginsFile());
		try {
			reader.readLine(); // read header
			String line = reader.readLine();
			while (line != null) {
				String[] lineElements = line.split(this.configGroup.getDelimiter());
				char country = lineElements[2].toCharArray()[0];
				List<Tuple<Double, ActivityFacility>> countryFacilities = this.destFacilities.get(country);
				if (countryFacilities == null) {
					countryFacilities = new ArrayList<>();
					this.destFacilities.put(country, countryFacilities);
				}
				ActivityFacility destFacility =
						getOrigFacilities().getFacilities().get(Id.create(CreationOfCrossBorderFacilities.BC_TAG + lineElements[0], ActivityFacility.class));
				double cumulativeProbability = Double.parseDouble(lineElements[4]);
				countryFacilities.add(new Tuple<>(cumulativeProbability, destFacility));
				line = reader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	final void createCBPopulation() {
		BufferedReader reader = IOUtils.getBufferedReader(this.configGroup.getPathToOriginsFile());
		try {
			log.info("CB-Pop creation...");
			Counter counter = new Counter(" CB-Facility # ");
			reader.readLine(); // read header
			String line = reader.readLine();
			while (line != null) {
				String[] lineElements = line.split(this.configGroup.getDelimiter());
				ActivityFacility origFacility =
						getOrigFacilities().getFacilities().get(Id.create(CreationOfCrossBorderFacilities.BC_TAG + lineElements[0], ActivityFacility.class));
				if (origFacility == null) {
					log.error("BC-Facility " + CreationOfCrossBorderFacilities.BC_TAG + lineElements[0] + " not found.");
				}
				counter.incCounter();
				createCBTransitPopulation(origFacility, Integer.parseInt(lineElements[5]), 'A');
				createCBTransitPopulation(origFacility, Integer.parseInt(lineElements[6]), 'D');
				createCBTransitPopulation(origFacility, Integer.parseInt(lineElements[7]), 'F');
				createCBTransitPopulation(origFacility, Integer.parseInt(lineElements[8]), 'I');
				line = reader.readLine();
			}
			counter.printCounter();
			log.info("CB-Pop creation... done.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createCBTransitPopulation(ActivityFacility origFacility, int numberOfAgents, char origCountry) {
		for (int i = 0; i < numberOfAgents; i++) {
			List<Tuple<Double, ActivityFacility>> destFacilitiesOfCountry = this.destFacilities.get(origCountry);
			ActivityFacility destFacility = null;
			double facilityChoice = random.nextDouble();
			int j = 0;
			while (j < destFacilitiesOfCountry.size() && destFacilitiesOfCountry.get(j).getFirst() <= facilityChoice) {
				destFacility = destFacilitiesOfCountry.get(j).getSecond();
				j++;
			}
			createSingleTripAgent(origFacility, destFacility, "transit");
		}
	}

	@Override
	final Plan createSingleTripPlan(ActivityFacility origFacility, ActivityFacility destFacility) {
		Plan plan = new PlanImpl();
		double departureTime = getDepartureTime();

		ActivityImpl actStart = new ActivityImpl(this.configGroup.getTag() + "Home", origFacility.getCoord(), origFacility.getLinkId());
		actStart.setFacilityId(origFacility.getId());
		actStart.setStartTime(0.0);
		actStart.setMaximumDuration(departureTime);
		actStart.setEndTime(departureTime);
		plan.addActivity(actStart);

		plan.addLeg(new LegImpl("car"));

		ActivityImpl actEnd = new ActivityImpl(this.configGroup.getTag() + "Home", destFacility.getCoord(), destFacility.getLinkId());
		actEnd.setFacilityId(destFacility.getId());
		actEnd.setStartTime(departureTime);
		//actEnd.setMaximumDuration(24.0 * 3600.0 - departureTime);
		plan.addActivity(actEnd);
		return plan;
	}
}
