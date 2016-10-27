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

package contrib.baseline.preparation.crossborderCreation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;
import contrib.baseline.preparation.secondaryFacilityCreation.CreationOfCrossBorderFacilities;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Implementation of the trunk class CreateCBsubpop for the creation of public transport transit single-trip cb-agents.
 *
 * @author boescpa
 */
public class CreateCBTransitPT extends CreateSingleTripPopulation {

	public CreateCBTransitPT(CreateSingleTripPopulationConfigGroup configGroup) {
		super(configGroup);
	}

	@Override
	void readDestinations() {
		// Not required here.
	}

	@Override
	void createCBPopulation() {
		ActivityFacility origFacility, destFacility;
		BufferedReader reader = IOUtils.getBufferedReader(configGroup.getPathToOriginsFile());
		try {
			reader.readLine(); // header
			String line = reader.readLine();
			while (line != null) {
				String[] lineElements = line.split(configGroup.getDelimiter());
				origFacility = getOrigFacilities().getFacilities().get(
						Id.create(CreationOfCrossBorderFacilities.BC_TAG + lineElements[0], ActivityFacility.class));
				for (int destNr = 1; destNr < 10; destNr++) {
					for (int i = 0; i < Integer.parseInt(lineElements[destNr+1]); i++) {
						destFacility = getOrigFacilities().getFacilities().get(
								Id.create(CreationOfCrossBorderFacilities.BC_TAG + (2000 + destNr), ActivityFacility.class));
						createSingleTripAgent(origFacility, destFacility, "transit");
					}
				}
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	Plan createSingleTripPlan(ActivityFacility origFacility, ActivityFacility destFacility) {
		Plan plan = PopulationUtils.createPlan();
		double departureTime = getDepartureTime();

		Activity actStart = PopulationUtils.createActivityFromCoordAndLinkId(this.configGroup.getTag() + "Home", origFacility.getCoord(), origFacility.getLinkId());
		actStart.setFacilityId(origFacility.getId());
		actStart.setStartTime(0.0);
		actStart.setMaximumDuration(departureTime);
		actStart.setEndTime(departureTime);
		plan.addActivity(actStart);

		plan.addLeg(PopulationUtils.createLeg(mode));

		Activity actEnd = PopulationUtils.createActivityFromCoordAndLinkId(this.configGroup.getTag() + "Home", destFacility.getCoord(), destFacility.getLinkId());
		actEnd.setFacilityId(destFacility.getId());
		actEnd.setStartTime(departureTime);
		//actEnd.setMaximumDuration(24.0 * 3600.0 - departureTime);
		plan.addActivity(actEnd);
		return plan;
	}
}
