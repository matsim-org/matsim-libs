/* *********************************************************************** *
 * project: org.matsim.*
 * AssignFacs.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mfeil.analysis;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;


/**
 * Simple class to assign facilities to a MZ plans file.
 *
 * @author mfeil
 */
public class AssignFacs {


	protected final String outputDir;
	protected static final Logger log = Logger.getLogger(AssignFacs.class);



	public AssignFacs(ActivityFacilitiesImpl facs, Population population, final String outputDir) {
		this.outputDir = outputDir;
		PersonSetNearestFacCoord f = new PersonSetNearestFacCoord(facs);

		for (Person person : population.getPersons().values()) {
			f.run(person);
		}
	}


	public static void main(final String [] args) {
		final String facilitiesFilename = "/home/baug/mfeil/data/Zurich10/facilities.xml";
		final String networkFilename = "/home/baug/mfeil/data/Zurich10/network.xml";
		final String populationFilename = "/home/baug/mfeil/data/mz/plans.xml";
//		final String populationFilename = "./plans/output_plans.xml.gz";
//		final String networkFilename = "./plans/network.xml";
//		final String facilitiesFilename = "./plans/facilities.xml.gz";

		final String outputDir = "/home/baug/mfeil/data/mz/plans_fac.xml";

		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		new MatsimFacilitiesReader(scenario).readFile(facilitiesFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		new AssignFacs(scenario.getActivityFacilities(), scenario.getPopulation(), outputDir);

//		new PopulationWriter(scenario.getPopulation(), outputDir);
		log.info("Analysis of plan finished.");
	}

}

