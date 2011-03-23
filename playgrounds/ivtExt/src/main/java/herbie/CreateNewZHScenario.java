/* *********************************************************************** *
 * project: org.matsim.CreateNewZHScenario
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

package herbie;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class CreateNewZHScenario {

	private final static Logger log = Logger.getLogger(CreateNewZHScenario.class);
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	public static String outputFolder="src/main/java/playground/anhorni/output/NEWZH/";
	private static String path = "src/main/java/playground/anhorni/";
	private String networkfilePath = path + "input/NEWZH/network.xml";
	private String facilitiesfilePath = path + "input/NEWZH/facilities.xml";
	private String plansV2filePath = path + "input/NEWZH/plans.xml";
	
	// cross-border
	private String crossBorderFacilitiesFilePath = path + "input/NEWZH/facilities.xml";
	private String crossBorderPlansFilePath = path + "input/NEWZH/plans.xml";
	
	// ====================================================================================
	public static void main(final String[] args) {
		CreateNewZHScenario creator = new CreateNewZHScenario();
		creator.init();
		creator.run();
		log.info("Creation finished -----------------------------------------");
	}
	// ====================================================================================
	// read in network, facilities and plans into scenario
	private void init() {
		new MatsimNetworkReader(scenario).readFile(networkfilePath);
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
		populationReader.readFile(plansV2filePath);
	}
	// ====================================================================================
	private void run() {
		this.init();
		this.addCrossBorderTraffic();
		this.addFreightTraffic();
		this.cutZhRegion();
		this.samplePlans(100.0);
		this.write();
	}
	
	// read cross-border plans and add them to the scenario
	// the cross border facilities are already integrated in the facilities
	private void addCrossBorderTraffic() {
		ScenarioImpl sTmp = (ScenarioImpl) ScenarioUtils.createScenario(
				ConfigUtils.createConfig());
		
		new MatsimNetworkReader(sTmp).readFile(networkfilePath);
		new FacilitiesReaderMatsimV1(sTmp).readFile(crossBorderFacilitiesFilePath);
		MatsimPopulationReader populationReader = new MatsimPopulationReader(sTmp);
		populationReader.readFile(crossBorderPlansFilePath);
		
		for (Person p : sTmp.getPopulation().getPersons().values()){
			this.scenario.getPopulation().addPerson(p);
		}
	} 
	
	// read and add freight traffic
	private void addFreightTraffic() {
		this.redistributeFreightFacilities();
	}
	
	private void samplePlans(double percent) {	
	}
	
	private void cutZhRegion() {	
	}
	
	// find all links in zone
	// randomly assign facilities
	private void redistributeFreightFacilities() {	
	}
	
	private void write() {
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(path + "plans.xml");
	}
}
