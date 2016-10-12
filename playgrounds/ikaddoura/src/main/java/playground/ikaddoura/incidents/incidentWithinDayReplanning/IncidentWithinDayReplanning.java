/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.incidents.incidentWithinDayReplanning;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;

import playground.ikaddoura.incidents.NetworkChangeEventsUtils;

/**
* @author ikaddoura
* 
*/
public class IncidentWithinDayReplanning {

// ############################################################################################################################################

	private static String day = "2016-03-15";

	private static String populationFile = "../../../runs-svn/incidents/input/bvg.run189.10pct.100.plans.selected.genericPt_baseCase100it_output_plans.xml.gz";
	private static String networkFile = "../../../runs-svn/incidents/input/network.xml";
	private static String configFile = "../../../runs-svn/incidents/input/baseCase_output_config_updated.xml";
	private static String networkChangeEventsFile = "../../../runs-svn/incidents/input/networkChangeEvents_" + day + ".xml.gz";
	private static String runOutputBaseDirectory = "../../../runs-svn/incidents/output/";
		
	private static final boolean reducePopulationToAffectedAgents = true;
	private static final String reducedPopulationFile = "../../../runs-svn/incidents/input/" + day + "_reduced-population.xml.gz";
	
	private static final boolean applyNetworkChangeEventsAndRunWithinDayReplanning = false;
		
// ############################################################################################################################################
	
	private static final Logger log = Logger.getLogger(IncidentWithinDayReplanning.class);
	
	public static void main(String[] args) {
		
		if (args.length > 0) {
			
			// TODO
			
		}
		
		IncidentWithinDayReplanning incidentWithinDayReplanning = new IncidentWithinDayReplanning();
		incidentWithinDayReplanning.run();
	}

	private void run() {
		OutputDirectoryLogging.catchLogEntries();
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(runOutputBaseDirectory);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		final Config config = ConfigUtils.loadConfig(configFile);
		config.network().setInputFile(networkFile);
		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(0);
				
		if (applyNetworkChangeEventsAndRunWithinDayReplanning) {
			
			config.controler().setOutputDirectory(runOutputBaseDirectory + day + "_within-day-replanning" + "/");
			
			// consider the network change events
			config.network().setChangeEventsInputFile(networkChangeEventsFile);
			config.network().setTimeVariantNetwork(true);
			
			config.plans().setInputFile(populationFile);
			
		} else {
			config.controler().setOutputDirectory(runOutputBaseDirectory + day + "_baseCase" + "/");

			if (reducePopulationToAffectedAgents) {
				// use the reduced population
				config.plans().setInputFile(reducedPopulationFile);
			} else {
				config.plans().setInputFile(populationFile);
			}
		}
				
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler(scenario);
		controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		
		if (applyNetworkChangeEventsAndRunWithinDayReplanning) {
			
			if (reducePopulationToAffectedAgents) {
				log.info("Reducing the population size from " + scenario.getPopulation().getPersons().size() + "...");

				Set<Id<Link>> incidentLinkIds = NetworkChangeEventsUtils.getIncidentLinksFromNetworkChangeEventsFile(scenario);
				Set<Id<Person>> personIdsToKeepInPopulation = NetworkChangeEventsUtils.getPersonIDsOfAgentsDrivingAlongSpecificLinks(scenario, incidentLinkIds);
				NetworkChangeEventsUtils.filterPopulation(scenario, personIdsToKeepInPopulation);

				log.info("... to " + scenario.getPopulation().getPersons().size() + " agents (= those agents driving along incident links).");
				PopulationWriter writer = new PopulationWriter(scenario.getPopulation());
				writer.write(reducedPopulationFile);
			}
			
			// start within day replanning

			Set<String> analyzedModes = new HashSet<>();
			analyzedModes.add(TransportMode.car);
			final TravelTimeCollector travelTime = new TravelTimeCollector(controler.getScenario(), analyzedModes);
							
			controler.addOverridingModule( new AbstractModule() {
				@Override public void install() {
					
					this.bind(IncidentBestRouteMobsimListener.class).asEagerSingleton();
					this.addMobsimListenerBinding().to(IncidentBestRouteMobsimListener.class);
					this.addControlerListenerBinding().to(IncidentBestRouteMobsimListener.class);

					this.bind(TravelTime.class).toInstance(travelTime);
					this.addEventHandlerBinding().toInstance(travelTime);
					this.addMobsimListenerBinding().toInstance(travelTime);
				}
			}) ;
		}
				
		controler.run();		
	}
	
}

