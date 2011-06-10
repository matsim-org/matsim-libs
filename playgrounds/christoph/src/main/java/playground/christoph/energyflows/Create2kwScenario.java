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

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.facilities.algorithms.WorldConnectLocations;

import playground.christoph.energyflows.facilities.AnalyzeRemovedFacilities;
import playground.christoph.energyflows.facilities.EnterpriseFacilitiesCreator;
import playground.christoph.energyflows.facilities.FacilitiesToRemoveFromZH;
import playground.christoph.energyflows.facilities.RemoveFacilitiesFromZH;
import playground.christoph.energyflows.facilities.BuildingsZHCreator;
import playground.christoph.energyflows.population.GetPersonsToAdapt;
import playground.christoph.energyflows.population.RelocateActivities;

public class Create2kwScenario {

	final private static Logger log = Logger.getLogger(Create2kwScenario.class);

	/*
	 * Scenario
	 */
	private String networkFile = "../../matsim/mysimulations/2kw/network/network.xml.gz";	
	private String populationInFile = "../../matsim/mysimulations/2kw/population/plans_100pct_dilZh30km_with_crossboarder.xml.gz";
	private String populationOutFile = "../../matsim/mysimulations/2kw/population/plans_adapted.xml.gz";
	private String facilitiesF2lFile = "../../matsim/mysimulations/2kw/facilities/f2l.txt";
	private String facilitiesInFile = "../../matsim/mysimulations/2kw/facilities/facilities.xml.gz";
	private String facilitiesOutFile = "../../matsim/mysimulations/2kw/facilities/facilities_adapted.xml.gz";
	
	/*
	 * FacilitiesToRemoveFromZH
	 */
	private String facilitiesToRemoveTextFile = "../../matsim/mysimulations/2kw/facilities/Facilities2CutFromMATSim.txt";
	
	/*
	 * BuildingsZHCreator
	 */
	private String buildingsTextFile = "../../matsim/mysimulations/2kw/facilities/Wilke_Gebäude_100709.csv";
	private String apartmentsTextFile = "../../matsim/mysimulations/2kw/facilities/Wilke_Wohnungen_100709.csv";
	private String apartmentBuildingsTextFile =  "../../matsim/mysimulations/2kw/gis/apartmentBuildings.csv";
//	private String facilitiesZHFile = "../../matsim/mysimulations/2kw/facilities/facilitiesZH.xml.gz";
	
	/*
	 * EnterpriseFacilitiesCreator
	 */
	private String enterpriseToBuildingTextFile = "../../matsim/mysimulations/2kw/gis/EnterpriseToBuilding.txt";
	private String enterpriseCensusVAETextFile =  "../../matsim/mysimulations/2kw/gis/AST_08_Capacities.csv";
	private String enterpriseCensusTextFile = "../../matsim/mysimulations/2kw/facilities/AST_08.csv";
	private String outEmptyFacilitiesZHFile = "../../matsim/mysimulations/2kw/facilities/output_emptyFacilitiesZH.txt";
	
	public static void main(String[] args) throws Exception {
		new Create2kwScenario();
	}
	
	public Create2kwScenario() throws Exception {
		
		Config config = ConfigUtils.createConfig();
		config.setParam("f2l", "inputF2LFile", facilitiesF2lFile);
		config.network().setInputFile(networkFile);
		config.facilities().setInputFile(facilitiesInFile);
		config.plans().setInputFile(populationInFile);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		Gbl.printMemoryUsage();
		
		log.info("connecting facilities to network...");
		new WorldConnectLocations(config).connectFacilitiesWithLinks(scenario.getActivityFacilities(), scenario.getNetwork());
		log.info("...done.");
			
		log.info("read facilities to remove...");
		Set<Id> facilitiesToRemove = new FacilitiesToRemoveFromZH(facilitiesToRemoveTextFile).getFacilitiesToRemove();
		log.info("...done.");
		
		log.info("create new Zurich facilites...");
		BuildingsZHCreator residentialCreator = new BuildingsZHCreator();
		residentialCreator.parseBuildingFile(buildingsTextFile);
		residentialCreator.parseApartmentFile(apartmentsTextFile);
		residentialCreator.writeApartmentBuildingsFile(apartmentBuildingsTextFile);
		residentialCreator.createFacilities();
		ActivityFacilities zurichFacilities = residentialCreator.getResidentialFacilities();
//		residentialCreator.calcCapacities(analyzeRemovedFacilities.getRemovedCapacities());
		log.info("...done.");
		
		log.info("add enterprise information to Zurich facilities...");
		EnterpriseFacilitiesCreator creator = new EnterpriseFacilitiesCreator(zurichFacilities, scenario.getActivityFacilities());
		creator.createShopQuadTrees();
		creator.parseEnterpriseToBuildingTextFile(enterpriseToBuildingTextFile);
		creator.parseEnterpriseCensusVAETextFile(enterpriseCensusVAETextFile);
		creator.parseEnterpriseTextFile(enterpriseCensusTextFile);
		creator.setOpeningTimes();
		creator.analyseFacilites();
		creator.writeEmptyFacilitiesFile(outEmptyFacilitiesZHFile);
//		creator.writeFacilitiesFile(outFacilitiesZHFile);
		log.info("...done.");

		log.info("connecting Zurich facilities to network...");
		config.getModule("f2l").getParams().remove("inputF2LFile"); // there is no f2l file for the new Zurich facilities
		new WorldConnectLocations(config).connectFacilitiesWithLinks(zurichFacilities, scenario.getNetwork());
		log.info("...done.");

		/*
		 * We need the original facilities to create the new ones. Therefore we cannot remove
		 * the facilities within Zurich City before we have created their replacements.
		 */
		log.info("remove facilities...");
		List<ActivityFacility> removedFacilities = new RemoveFacilitiesFromZH().removeFacilities(scenario.getActivityFacilities(), facilitiesToRemove);
		log.info("...done.");
		
		log.info("analyze removed facilities...");
		AnalyzeRemovedFacilities analyzeRemovedFacilities = new AnalyzeRemovedFacilities(removedFacilities);
		log.info("...done.");
		
		log.info("add new facilities...");
		scenario.getActivityFacilities().getFacilities().putAll(zurichFacilities.getFacilities());
		log.info("done.");

		log.info("write facilities...");
		new FacilitiesWriter(scenario.getActivityFacilities()).write(facilitiesOutFile);
		log.info("...done.");
		
		log.info("identify people who have to adapt their plans...");
		Set<Id> persons = new GetPersonsToAdapt().getPersons(scenario.getPopulation(), facilitiesToRemove);
		log.info("found " + persons.size() + " agents that have to adapt their plan");
		log.info("...done.");	
		
		log.info("relocate activities from removed facilites...");
		RelocateActivities relocateActivities = new RelocateActivities(zurichFacilities);
		relocateActivities.relocateActivities(scenario.getPopulation(), persons, facilitiesToRemove);
		relocateActivities.checkCapacityUsage();
		log.info("...done.");
		
		log.info("write population...");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork(), ((ScenarioImpl)scenario).getKnowledges()).write(populationOutFile);
		log.info("...done.");
	}
}
