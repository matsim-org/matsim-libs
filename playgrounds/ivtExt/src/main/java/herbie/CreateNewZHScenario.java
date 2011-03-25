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
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class CreateNewZHScenario {
	private String currentDir = "//pingelap/matsim/";
	private final static Logger log = Logger.getLogger(CreateNewZHScenario.class);
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private String outputFolder;
	private String networkfilePath;
	private String facilitiesfilePath;
	private String plansV2filePath;
	
	// cross-border
	private String crossBorderPlansFilePath;
	
	//freight zh
	private String freightPlansFilePath0;
	private String freightPlansFilePath1;
	private String freightFacilitiesFilePath0;
	private String freightFacilitiesFilePath1;
	
	// ====================================================================================
	public static void main(final String[] args) {
		if (args.length != 1) {
			log.error("Please specify a running location! Either 'l' (locally) or 'r' (remotely)");
			return;
		}				
		CreateNewZHScenario creator = new CreateNewZHScenario();
		creator.init();
		creator.run(args[0]);
		log.info("Creation finished -----------------------------------------");
	}
	
	// ====================================================================================
	private void run(String runningLocation) {
		if (runningLocation.equals("l")) {
			this.currentDir = "//pingelap/matsim/";
		}
		else {
			this.currentDir = "/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/projekt/matsim/";
		}
		this.init();
		this.addCrossBorderTraffic();
		this.addFreightTraffic();
		this.cutZhRegion();
		this.samplePlans(100.0);
		this.assignPlans2Networks();
		this.write();
	}
	
	// ====================================================================================
	// read in network, facilities and plans into scenario
	private void init() {
		this.readPathsFile(currentDir, "herbie/configs/paths-config.xml");
		new MatsimNetworkReader(scenario).readFile(networkfilePath);
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
		populationReader.readFile(plansV2filePath);
	}
	
	private void readPathsFile(String currentDir, String pathsfile) {
		Config config = new Config();
    	MatsimConfigReader matsimConfigReader = new MatsimConfigReader(config);
    	matsimConfigReader.readFile(currentDir + pathsfile);   	
		
		this.outputFolder = currentDir + config.getParam("pathsettings", "outputFolder");
		this.networkfilePath = currentDir + config.getParam("pathsettings", "networkfilePath");
		this.facilitiesfilePath = currentDir + config.getParam("pathsettings", "facilitiesfilePath");
		this.plansV2filePath = currentDir + config.getParam("pathsettings", "plansV2filePath");
		this.crossBorderPlansFilePath = currentDir + config.getParam("pathsettings", "crossBorderPlansFilePath");
		
		this.freightPlansFilePath0 = currentDir + config.getParam("pathsettings", "freightPlansFilePath0");
		this.freightPlansFilePath1 = currentDir + config.getParam("pathsettings", "freightPlansFilePath1");
		this.freightFacilitiesFilePath0 = currentDir + config.getParam("pathsettings", "freightFacilitiesFilePath0");
		this.freightFacilitiesFilePath1 = currentDir + config.getParam("pathsettings", "freightFacilitiesFilePath1");
    }
	
	// ====================================================================================
	// read cross-border plans and add them to the scenario
	// the cross border facilities are already integrated in the facilities
	private void addCrossBorderTraffic() {
		ScenarioImpl sTmp = (ScenarioImpl) ScenarioUtils.createScenario(
				ConfigUtils.createConfig());
		
		new MatsimNetworkReader(sTmp).readFile(networkfilePath);
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
	
	private void assignPlans2Networks() {	
	}
	
	private void samplePlans(double percent) {	
	}
	
	private void cutZhRegion() {	
	}
	
	// find all facilities in the respective (OD-)zone
	// assign facilities according to capacities and facility type (e.g., shops versus home)
	private void redistributeFreightFacilities() {	
	}
	
	private void write() {
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(this.outputFolder + "plans.xml");
	}	
}
