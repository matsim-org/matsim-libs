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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.*;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import playground.boescpa.ivtBaseline.preparation.secondaryFacilityCreation.CreationOfCrossBorderFacilities;
import playground.boescpa.lib.tools.FacilityUtils;
import playground.boescpa.lib.tools.PopulationUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Random;

import static playground.boescpa.ivtBaseline.preparation.IVTConfigCreator.*;

/**
 * Trunk class for the creation of cross-border (cb) sub-populations.
 *
 * @author boescpa
 */
public abstract class CreateSingleTripPopulation {
	final static Logger log = Logger.getLogger(CreateSingleTripPopulation.class);

	String actTag = null;

	private final Population newCBPopulation;
	protected final CreateSingleTripPopulationConfigGroup configGroup;
	private final ActivityFacilities origFacilities;
	private final ActivityFacilities bcFacilities = FacilitiesUtils.createActivityFacilities();
	private final double[] cummulativeDepartureProbability;
	private final double samplePercentage;
	protected final Random random;
	private int index = 0;

	protected final Population getPopulation() {
		return newCBPopulation;
	}

	protected final ActivityFacilities getOrigFacilities() {
		return origFacilities;
	}

	public CreateSingleTripPopulation(CreateSingleTripPopulationConfigGroup configGroup) {
		this.configGroup = configGroup;
		this.newCBPopulation = PopulationUtils.getEmptyPopulation();
		this.origFacilities = FacilityUtils.readFacilities(this.configGroup.getPathToFacilities());
		addHomeActivityIfNotInFacilityYet(this.origFacilities);
		this.cummulativeDepartureProbability = readDepartures(this.configGroup.getPathToCumulativeDepartureProbabilities());
		this.samplePercentage = this.configGroup.getSamplePercentage();
		this.random = new Random(this.configGroup.getRandomSeed());
	}


	// ******************************* Public Methods ***********************************

	public final void runPopulationCreation() {
		readDestinations();
		createCBPopulation();
	}

	public final void writeOutput() {
		String pathToOutput_CBTransitPopulation = this.configGroup.getPathToOutput();
		new PopulationWriter(getPopulation()).write(pathToOutput_CBTransitPopulation);
		new ObjectAttributesXmlWriter(getPopulation().getPersonAttributes())
				.writeFile(pathToOutput_CBTransitPopulation.substring(0, pathToOutput_CBTransitPopulation.indexOf(".xml")) + "_Attributes.xml.gz");
		new FacilitiesWriter(bcFacilities)
				.write(pathToOutput_CBTransitPopulation.substring(0, pathToOutput_CBTransitPopulation.indexOf(".xml")) + "_Facilities.xml.gz");
	}


	// ******************************* Abstract Methods ***********************************

	abstract void readDestinations();

	/**
	 * The method createSingleTripAgent (which calls the method createSingleTripPlan) is available to create the population.
	 */
	abstract void createCBPopulation();

	abstract Plan createSingleTripPlan(ActivityFacility origFacility, ActivityFacility destFacility);


	// ******************************* Children Service Methods ***********************************

	protected final void createSingleTripAgent(ActivityFacility origFacility, ActivityFacility destFacility, String subTag) {
		if (origFacility == null || destFacility == null) {
			throw new RuntimeException("Expected CB-Facility not found.");
		}
		if (random.nextDouble() > samplePercentage) return;
		// create and add new agent
		Person p = org.matsim.core.population.PopulationUtils.createPerson(Id.create(this.configGroup.getTag() + "_" + subTag + "_" + index++, Person.class));
		newCBPopulation.addPerson(p);
		newCBPopulation.getPersonAttributes().putAttribute(p.getId().toString(), "subpopulation", this.configGroup.getTag());
		// store facilities (if not already stored)
		origFacility = addCBFacility(origFacility);
		destFacility = addCBFacility(destFacility);
		// create and add new plan
		p.addPlan(createSingleTripPlan(origFacility, destFacility));
	}

	protected final double getDepartureTime() {
		double randDep = random.nextDouble();
		// identify selected hour of day
		int hour = 0;
		while (hour < 23 && cummulativeDepartureProbability[hour + 1] < randDep) {
			hour++;
		}
		double time = hour*60*60;
		// random assignment within that hour of the day
		time += random.nextInt(3600);
		return time;
	}


	// ******************************* Private Methods ***********************************

	private double[] readDepartures(String pathToCumulativeDepartureProbabilities) {
		BufferedReader reader = IOUtils.getBufferedReader(pathToCumulativeDepartureProbabilities);
		double[] cumulativeDepartureProbabilities = null;
		try {
			cumulativeDepartureProbabilities = new double[24];
			for (int i = 0; i < 24; i++) {
				String[] line = reader.readLine().split(this.configGroup.getDelimiter());
				cumulativeDepartureProbabilities[i] = Double.parseDouble(line[1]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cumulativeDepartureProbabilities;
	}

	private void addHomeActivityIfNotInFacilityYet(ActivityFacilities facilities) {
		for (ActivityFacility facility : facilities.getFacilities().values()) {
			if (facility.getId().toString().contains(CreationOfCrossBorderFacilities.BC_TAG)) {
				if (!facility.getActivityOptions().keySet().contains(this.configGroup.getTag() + "Home")) {
					((ActivityFacilityImpl)facility).createAndAddActivityOption(this.configGroup.getTag() + "Home");
					OpeningTime ot = new OpeningTimeImpl(0.0, 24.0 * 3600.0);
					facility.getActivityOptions().get(this.configGroup.getTag() + "Home").addOpeningTime(ot);
				}
			}
		}
	}

	private ActivityFacility addCBFacility(ActivityFacility facility) {
		ActivityFacility finalFacility;
		if (facility.getId().toString().contains(CreationOfCrossBorderFacilities.BC_TAG)) {
			if (!bcFacilities.getFacilities().containsKey(facility.getId())) {
				bcFacilities.addActivityFacility(facility);
			}
			finalFacility = facility;
		} else {
			Id<ActivityFacility> facilityId =
					Id.create(this.configGroup.getTag() + "_act_" + facility.getCoord().getX() + "_" + facility.getCoord().getY(), ActivityFacility.class);
			if (!bcFacilities.getFacilities().containsKey(facilityId)) {
				ActivityFacility newActFacility = bcFacilities.getFactory().createActivityFacility(facilityId, facility.getCoord());
				((ActivityFacilityImpl) newActFacility).createAndAddActivityOption(this.actTag);
				bcFacilities.addActivityFacility(newActFacility);
			}
			finalFacility = bcFacilities.getFacilities().get(facilityId);
			if (!finalFacility.getActivityOptions().containsKey(this.actTag)) {
				((ActivityFacilityImpl) finalFacility).createAndAddActivityOption(this.actTag);
			}
		}
		return finalFacility;
	}
}
