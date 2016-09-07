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

package playground.ikaddoura.incidents;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.StringUtils;

/**
* @author ikaddoura
* 
*/
public class NetworkChangeEventsPersonTripAnalysis {

// ############################################################################################################################################

	private static final String baseCaseOutputDirectory = "../../../runs-svn/incidents/output/baseCase/";
	private static final String networkChangeEventsDirectory = "../../../shared-svn/studies/ihab/incidents/analysis/berlin_2016-02-11_2016-05-18/networkChangeEventFiles_workingDays/";
	private static final String analysisOutputDirectory = networkChangeEventsDirectory + "nce_personTripAnalysis/";

// ############################################################################################################################################
	
	private static final Logger log = Logger.getLogger(NetworkChangeEventsPersonTripAnalysis.class);
		
	private final SortedMap<String, Integer> day2affectedAgents = new TreeMap<>();
	private final SortedMap<String, Integer> day2notAffectedAgents = new TreeMap<>();
	private final SortedMap<String, Integer> day2agentsWithCarTrip = new TreeMap<>();
	private final SortedMap<String, Integer> day2agentsWithoutCarTrip = new TreeMap<>();
	
	private final SortedMap<String, Integer> day2affectedCarTrips = new TreeMap<>();
	private final SortedMap<String, Integer> day2notAffectedCarTrips = new TreeMap<>();
	private final SortedMap<String, Integer> day2carTrips = new TreeMap<>();
	private final SortedMap<String, Integer> day2nonCarTrips = new TreeMap<>();

	public static void main(String[] args) throws IOException {
		NetworkChangeEventsPersonTripAnalysis analysis = new NetworkChangeEventsPersonTripAnalysis();
		analysis.run();
	}

	private void run() throws IOException {

		OutputDirectoryLogging.catchLogEntries();
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(analysisOutputDirectory);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		File[] fileList = new File(networkChangeEventsDirectory).listFiles();
		
		log.info("Loading scenario...");

		final Config config0 = ConfigUtils.createConfig();
		config0.network().setInputFile(baseCaseOutputDirectory + "output_network.xml.gz");
		config0.plans().setInputFile(baseCaseOutputDirectory + "output_plans.xml.gz");
		config0.plans().setRemovingUnneccessaryPlanAttributes(true);
		final Scenario scenario0 = ScenarioUtils.loadScenario(config0);
		
		log.info("Loading scenario... Done.");

		for (File f : fileList) {

			if (f.getName().endsWith(".xml.gz") && f.getName().startsWith("networkChangeEvents_")) {
				
				String delimiter1 = "_";
				String delimiter2 = ".";
				String dateString = StringUtils.explode(StringUtils.explode(f.getName(), delimiter1.charAt(0))[1], delimiter2.charAt(0))[0];
				log.info("DAY: " + dateString);
				
				log.info("Loading scenario with network change events...");
				
				final Config config1 = ConfigUtils.createConfig();
				config1.network().setTimeVariantNetwork(true);
				config1.network().setChangeEventsInputFile(f.toString());
				config1.network().setInputFile(baseCaseOutputDirectory + "output_network.xml.gz");
				config1.plans().setRemovingUnneccessaryPlanAttributes(true);
				final Scenario scenario1 = ScenarioUtils.loadScenario(config1);
								
				log.info("Loading scenario with network change events... Done.");
				
				Set<Id<Link>> incidentLinkIds = getIncidentLinksFromNetworkChangeEventsFile(scenario1);
				analyzePopulation(dateString, scenario0.getPopulation(), incidentLinkIds);
								
				writeStatistics();

			}
		}		
	}

	private void writeStatistics() throws IOException {
		
		String outputFile = analysisOutputDirectory + "nce_personTripAnalysis.csv";
		try ( BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile)) ) {
			
			bw.write("Day ;"
					+ " Total number of agents ;"
					+ " Number of agents with at least on car trip ;"
					+ " Number of agents that are directly affected by an incident ;"
					+ " Total number of trips ;"
					+ " Total number of car trips ;"
					+ " Number of trips where an agent was affected by an incident");
			bw.newLine();

			for (String  day : this.day2carTrips.keySet()) {
				bw.write(day + ";"
						+ (this.day2affectedAgents.get(day) + this.day2notAffectedAgents.get(day)) + " ; "
						+ this.day2agentsWithCarTrip.get(day) + " ; "
						+ this.day2affectedAgents.get(day) + " ; "
						+ (this.day2carTrips.get(day) + this.day2nonCarTrips.get(day)) + " ; "
						+ this.day2carTrips.get(day) + " ; "
						+ this.day2affectedCarTrips.get(day));
				bw.newLine();
			}
			log.info("Traffic items written to " + outputFile);
			bw.close();
		}

	}

	private static Set<Id<Link>> getIncidentLinksFromNetworkChangeEventsFile(Scenario scenario) {
		
		Set<Id<Link>> incidentLinkIds = new HashSet<>(); 
		
		Network network = (Network) scenario.getNetwork();
		
		for (NetworkChangeEvent nce : NetworkUtils.getNetworkChangeEvents(network)) {
			for (Link link : nce.getLinks()) {
				if (!incidentLinkIds.contains(link)) {
					incidentLinkIds.add(link.getId());
				}
			}
		}
		
		log.info("Number of incident links: " + incidentLinkIds.size() );
		log.info("Total number of links: " + network.getLinks().size());
		
		return incidentLinkIds;
	}

	private void analyzePopulation(String day, final Population population, Set<Id<Link>> incidentLinkIds) {

		int affectedAgents = 0;
		int notAffectedAgents = 0;
		int agentsWithCarTrip = 0;
		int agentsWithoutCarTrip = 0;
		
		int affectedCarTrips = 0;
		int notAffectedCarTrips = 0;
		int carTrips = 0;
		int nonCarTrips = 0;
		
		for (Person person : population.getPersons().values()) {
			
			boolean agentWithCarTrip = false;
			boolean agentAffectedByIncident = false;
			for ( Leg leg : TripStructureUtils.getLegs( person.getSelectedPlan() ) ) {
				
				boolean legAffectedByIncident = false;
				if (leg.getMode().equals(TransportMode.car)) {
					carTrips++;
					agentWithCarTrip = true;
					
					if ( leg.getRoute() instanceof NetworkRoute ) {					
						
						NetworkRoute route = (NetworkRoute) leg.getRoute() ;
						
						for (Id<Link> linkId : incidentLinkIds) {
							if (route.getLinkIds().contains(linkId)) {
								agentAffectedByIncident = true;
								legAffectedByIncident = true;
								break;	// no need to go through all other incident link IDs						
							}
						}						
						
					} else {					
						log.warn("This car trip has no network route: " + leg.toString() + " Aborting...");
					}
					
					
				} else {
					nonCarTrips++;
				}
				
				if (legAffectedByIncident) {
					affectedCarTrips++;
				} else {
					notAffectedCarTrips++;
				}
				
			}
			
			// after iterating through all legs:
			
			if (agentWithCarTrip) {
				agentsWithCarTrip++;
			} else {
				agentsWithoutCarTrip++;
			}
			
			if (agentAffectedByIncident) {
				affectedAgents++;
			} else {
				notAffectedAgents++;
			}
		}
		
		this.day2affectedAgents.put(day, affectedAgents);
		this.day2notAffectedAgents.put(day, notAffectedAgents);
		this.day2agentsWithCarTrip.put(day, agentsWithCarTrip);
		this.day2agentsWithoutCarTrip.put(day, agentsWithoutCarTrip);
		
		this.day2affectedCarTrips.put(day, affectedCarTrips);
		this.day2notAffectedCarTrips.put(day, notAffectedCarTrips);
		this.day2carTrips.put(day, carTrips);
		this.day2nonCarTrips.put(day, nonCarTrips);
	}
	
}

