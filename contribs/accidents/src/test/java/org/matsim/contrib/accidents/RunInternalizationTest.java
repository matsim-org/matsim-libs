/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.accidents;

import static org.junit.Assert.assertEquals;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.contrib.accidents.data.berlin.PlanfreeLinkIDs;
import org.matsim.contrib.accidents.data.berlin.TunnelLinkIDs;

/**
 * @author ikaddoura, mmayobre
 */

public class RunInternalizationTest {
	private static final Logger log = Logger.getLogger(RunInternalizationTest.class);
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void test1() {		
		
		log.info(utils.getPackageInputDirectory());

log.info(utils.getPackageInputDirectory());
		
		String configFile = utils.getPackageInputDirectory() + "/internalization_test/internalization_config.xml";
		String outputDirectory1 = utils.getOutputDirectory() + "/run_1/";
		String outputDirectory2 = utils.getOutputDirectory() + "/run_2/";
		String runId1 = "run1";
		String runId2 = "run2";

		String landUseFile = null;
		String popDensityFile = null;
			
		Controler controler1;
		{
			log.info("Loading scenario 1...");
			
			Config config = ConfigUtils.loadConfig(configFile);	
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory(outputDirectory1);
			config.controler().setRunId(runId1);
			
			AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
			accidentsSettings.setEnableAccidentsModule(true);
			accidentsSettings.setSampleSize(10.);
			accidentsSettings.setTunnelLinksArray(TunnelLinkIDs.getTunnelLinkIDs());
			accidentsSettings.setPlanFreeLinksArray(PlanfreeLinkIDs.getPlanfreeLinkIDs());
			accidentsSettings.setLanduseOSMInputShapeFile(landUseFile);
			accidentsSettings.setPlacesOSMInputFile(popDensityFile);
			accidentsSettings.setInternalizeAccidentCosts(true);
			accidentsSettings.setTollFactor(10.);
			
			Scenario scenario = ScenarioUtils.loadScenario(config);
			controler1 = new Controler(scenario);
			
			controler1.addOverridingModule(new AccidentsModule(scenario));
					
			log.info("Loading scenario 1... Done.");

			controler1.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
			controler1.run();
			
			log.info("Simulation run 1 completed.");
		}
		
		Controler controler2;
		{
			log.info("Loading scenario 2...");
			
			Config config = ConfigUtils.loadConfig(configFile);	
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setOutputDirectory(outputDirectory2);
			config.controler().setRunId(runId2);
			
			AccidentsConfigGroup accidentsSettings = ConfigUtils.addOrGetModule(config, AccidentsConfigGroup.class);
			accidentsSettings.setSampleSize(10.);
			accidentsSettings.setEnableAccidentsModule(true);
			accidentsSettings.setTunnelLinksArray(TunnelLinkIDs.getTunnelLinkIDs());
			accidentsSettings.setPlanFreeLinksArray(PlanfreeLinkIDs.getPlanfreeLinkIDs());
			accidentsSettings.setLanduseOSMInputShapeFile(landUseFile);
			accidentsSettings.setPlacesOSMInputFile(popDensityFile);
			accidentsSettings.setInternalizeAccidentCosts(false);
			accidentsSettings.setTollFactor(10.);
			
			Scenario scenario = ScenarioUtils.loadScenario(config);
			controler2 = new Controler(scenario);
			
			controler2.addOverridingModule(new AccidentsModule(scenario));
					
			log.info("Loading scenario 2... Done.");

			controler2.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
			controler2.run();
			
			log.info("Simulation 2 run completed.");
			
		}
		
		int numberOfroute45usersRun1 = 0;
		int numberOfroute23usersRun1 = 0;
		int numberOfroute45usersRun2 = 0;
		int numberOfroute23usersRun2 = 0;
		
		log.info("+++++++++ Run 1 (internalization):");
		for (Person person : controler1.getScenario().getPopulation().getPersons().values()) {
			
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
				if (pE instanceof Leg) {
					Leg leg = (Leg) pE;
					log.info(" --> " + leg.getRoute().toString());
					if (leg.getRoute().getRouteDescription().toString().equals("1 2 3 6")){
						numberOfroute23usersRun1 += 1;
					} else numberOfroute45usersRun1 +=1;
				}
			}
		}
		log.info("RUN 1, Route 2-3: " + numberOfroute23usersRun1);
		log.info("RUN 1, Route 4-5: " + numberOfroute45usersRun1);
		
		log.info("+++++++++ Run 2 (base case):");
		for (Person person : controler2.getScenario().getPopulation().getPersons().values()) {
			
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
				if (pE instanceof Leg) {
					Leg leg = (Leg) pE;
					log.info(" --> " + leg.getRoute().toString());
					if (leg.getRoute().getRouteDescription().toString().equals("1 2 3 6")){
						numberOfroute23usersRun2 += 1;
					} else numberOfroute45usersRun2 +=1;
				}
			}
		}
		log.info("RUN 2, Route 2-3: " + numberOfroute23usersRun2);
		log.info("RUN 2, Route 4-5: " + numberOfroute45usersRun2);
		
		boolean rerouting;
		if (numberOfroute23usersRun1 > numberOfroute23usersRun2){
			rerouting = true;
		} else rerouting = false;
		
		if (rerouting == true){
			log.info("Internalization works!!  --> Agents move from FASTER but EXPENSIVE route to SLOWER but CHEAPER route");
		} else log.info("Internalization doesn´t work");
		
		assertEquals(true, rerouting);
	
		// TODO: auch die input files verschieben
		
		
				// TODO: optional:
//				BufferedReader br = IOUtils.getBufferedReader(outputDirectory1 + "ITERS/it.10/run1.10.accidentCosts_BVWP.csv");
//				
//				String line = null;
//				try {
//					line = br.readLine();
//				} catch (IOException e) {
//					e.printStackTrace();
//				} // headers
		//
//				try {
//					int lineCounter = 0;
//					while ((line = br.readLine()) != null) {
//						
//						String[] columns = line.split(";");
//						for (int column = 0; column < columns.length; column++) {
//							
//							if (lineCounter == 0 && column == 49) {
//								double accidentCosts = Double.valueOf(columns[column]);
//								Assert.assertEquals("wrong accident costs", 18.5355, accidentCosts , 0.01);		
//							}
//							
//							if (lineCounter == 1 && column == 49) {
//								double accidentCosts = Double.valueOf(columns[column]);
//								Assert.assertEquals("wrong accident costs", 6.1785, accidentCosts , 0.01);		
//							}
//												
//						}
//						
//						lineCounter++;
//					}
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
	}
}
