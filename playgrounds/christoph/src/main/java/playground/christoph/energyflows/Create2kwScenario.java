/* *********************************************************************** *
 * project: org.matsim.*
 * Create2kwScenario.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.energyflows;

import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.christoph.energyflows.facilities.FacilitiesToRemoveFromZH;
import playground.christoph.energyflows.facilities.RemoveFacilitiesFromZH;

public class Create2kwScenario {

	final private static Logger log = Logger.getLogger(Create2kwScenario.class);

	private String networkFile = "../../matsim/mysimulations/2kw/network/network.xml.gz";
	
	private String populationInFile = "../../matsim/mysimulations/2kw/population/plans.xml.gz";
	private String populationOutFile = "../../matsim/mysimulations/2kw/population/plans_adapted.xml.gz";
	
	private String facilitiesInFile = "../../matsim/mysimulations/2kw/facilities/facilities.xml.gz";
	private String facilitiesOutFile = "../../matsim/mysimulations/2kw/facilities/facilities_adapted.xml.gz";
	
	public static void main(String[] args) throws Exception {
		new Create2kwScenario();
	}
	
	public Create2kwScenario() throws Exception {
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		log.info("read network...");
		new MatsimNetworkReader(scenario).parse(networkFile);
		log.info("...done.");
		Gbl.printMemoryUsage();
		
		log.info("read facilities...");
		new MatsimFacilitiesReader(scenario).parse(facilitiesInFile);
		log.info("read " + scenario.getActivityFacilities().getFacilities().size() + " facilities");
		log.info("...done.");
		Gbl.printMemoryUsage();
		
		log.info("read population...");
		new MatsimPopulationReader(scenario).parse(populationInFile);
		log.info("...done.");
		Gbl.printMemoryUsage();
		
		log.info("read facilities to remove...");
		Set<Id> facilitiesToRemove = new FacilitiesToRemoveFromZH().getFacilitiesToRemove();
		log.info("...done.");
		
		log.info("remove facilities...");
		new RemoveFacilitiesFromZH(scenario.getActivityFacilities(), facilitiesToRemove);
		log.info("...done.");
		
		log.info("write facilities...");
		new FacilitiesWriter(scenario.getActivityFacilities()).write(facilitiesOutFile);
		log.info("wrote " + scenario.getActivityFacilities().getFacilities().size() + " facilities");
		log.info("...done.");
	}
}
