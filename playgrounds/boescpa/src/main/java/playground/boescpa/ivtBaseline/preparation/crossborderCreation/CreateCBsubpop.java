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
abstract class CreateCBsubpop {
	final static Logger log = Logger.getLogger(CreateCBsubpop.class);

	final static String DELIMITER = ";";
	final static String CB_TAG = "cb";

	private final Population newCBPopulation;
	private final ActivityFacilities origFacilities;
	private final double[] cummulativeDepartureProbability;
	final double samplePercentage;
	final Random random;
	private int index = 0;

	final Population getPopulation() {
		return newCBPopulation;
	}

	final ActivityFacilities getOrigFacilities() {
		return origFacilities;
	}

	CreateCBsubpop(String pathToFacilities, String pathToCumulativeDepartureProbabilities, double samplePercentage, long randomSeed) {
		this.newCBPopulation = PopulationUtils.getEmptyPopulation();
		this.origFacilities = FacilityUtils.readFacilities(pathToFacilities);
		addHomeActivityIfNotInFacilityYet(this.origFacilities);
		this.cummulativeDepartureProbability = readDepartures(pathToCumulativeDepartureProbabilities);
		this.samplePercentage = samplePercentage;
		this.random = new Random(randomSeed);
	}

	private double[] readDepartures(String pathToCumulativeDepartureProbabilities) {
		BufferedReader reader = IOUtils.getBufferedReader(pathToCumulativeDepartureProbabilities);
		double[] cumulativeDepartureProbabilities = null;
		try {
			cumulativeDepartureProbabilities = new double[24];
			for (int i = 0; i < 24; i++) {
				String[] line = reader.readLine().split(DELIMITER);
				cumulativeDepartureProbabilities[i] = Double.parseDouble(line[1]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cumulativeDepartureProbabilities;
	}

	private void addHomeActivityIfNotInFacilityYet(ActivityFacilities facilities) {
		for (ActivityFacility facility : facilities.getFacilities().values()) {
			if (facility.getId().toString().contains("BC_")) {
				if (!facility.getActivityOptions().keySet().contains(HOME)) {
					((ActivityFacilityImpl)facility).createAndAddActivityOption(HOME);
					OpeningTime ot = new OpeningTimeImpl(0.0, 24.0 * 3600.0);
					facility.getActivityOptions().get(HOME).addOpeningTime(ot);
				}
			}
		}
	}

	/**
	 * The method createSingleTripAgent (which calls the method createSingleTripPlan) is available to create the population.
	 */
	abstract void createCBPopulation(String path2CBFile);

	final void createSingleTripAgent(ActivityFacility origFacility, ActivityFacility destFacility, String subTag) {
		if (origFacility == null || destFacility == null) {
			throw new RuntimeException("Expected CB-Facility not found.");
		}
		if (random.nextDouble() > samplePercentage) return;
		// create and add new agent
		Person p = org.matsim.core.population.PopulationUtils.createPerson(Id.create(CB_TAG + "_" + subTag + "_" + index++, Person.class));
		newCBPopulation.addPerson(p);
		newCBPopulation.getPersonAttributes().putAttribute(p.getId().toString(), "subpopulation", CB_TAG);
		// create and add new plan
		p.addPlan(createSingleTripPlan(origFacility, destFacility));
	}

	abstract Plan createSingleTripPlan(ActivityFacility origFacility, ActivityFacility destFacility);

	final double getDepartureTime() {
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

	final void writeOutput(String pathToOutput_CBTransitPopulation) {
		new PopulationWriter(getPopulation()).write(pathToOutput_CBTransitPopulation);
		new ObjectAttributesXmlWriter(getPopulation().getPersonAttributes())
				.writeFile(pathToOutput_CBTransitPopulation.substring(0, pathToOutput_CBTransitPopulation.indexOf(".xml")) + "_Attributes.xml.gz");
		new FacilitiesWriter(getOrigFacilities())
				.write(pathToOutput_CBTransitPopulation.substring(0, pathToOutput_CBTransitPopulation.indexOf(".xml")) + "_Facilities.xml.gz");
	}
}
