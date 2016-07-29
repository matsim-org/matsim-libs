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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;

/**
 * 
 * @author ikaddoura
 */

public class NetworkChangeEventsUtils {
	private static final Logger log = Logger.getLogger(NetworkChangeEventsUtils.class);

	public static Set<Id<Link>> getIncidentLinksFromNetworkChangeEventsFile(Scenario scenario) {
				
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

//	public static void reducePopulationToAgentsDrivingAlongSpecificLinks(final Scenario scenario, Set<Id<Link>> linkIDs) {
//
//		for ( Iterator<? extends Person> it = scenario.getPopulation().getPersons().values().iterator() ; it.hasNext() ; ) {
//			Person person = it.next() ;
//			
//			boolean agentDrivesAlongSpecificLink = false;
//			for ( Leg leg : TripStructureUtils.getLegs( person.getSelectedPlan() ) ) {
//				
//				if (leg.getMode().equals(TransportMode.car)) {
//					
//					if ( leg.getRoute() instanceof NetworkRoute ) {					
//						
//						NetworkRoute route = (NetworkRoute) leg.getRoute() ;
//						
//						for (Id<Link> linkId : linkIDs) {
//							if (route.getLinkIds().contains(linkId)) {
//								agentDrivesAlongSpecificLink = true;
//								break;	// no need to go through all other link IDs						
//							}
//						}
//						
//						break; // no need to go through all other legs
//						
//					} else {					
//						log.warn("This car trip has no network route: " + leg.toString() + " Aborting...");
//					}		
//				}
//			}
//			
//			if (!agentDrivesAlongSpecificLink) {
//				it.remove(); 
//			}
//		}		
//	}
	
	public static void filterPopulation(final Scenario scenario, Set<Id<Person>> personIdsToKeepInPopulation) {

		for ( Iterator<? extends Person> it = scenario.getPopulation().getPersons().values().iterator() ; it.hasNext() ; ) {
			Person person = it.next() ;
			
			if (!personIdsToKeepInPopulation.contains(person.getId())) {
				it.remove();
			}
		}		
	}
	
	public static Set<Id<Person>> getPersonIDsOfAgentsDrivingAlongSpecificLinks(final Scenario scenario, Set<Id<Link>> linkIDs) {

		Set<Id<Person>> personIDs = new HashSet<>();

		for ( Iterator<? extends Person> it = scenario.getPopulation().getPersons().values().iterator() ; it.hasNext() ; ) {
			Person person = it.next() ;
			
			boolean agentDrivingAlongSpecificLink = false;
			for ( Leg leg : TripStructureUtils.getLegs( person.getSelectedPlan() ) ) {
				
				if (leg.getMode().equals(TransportMode.car)) {
					
					if ( leg.getRoute() instanceof NetworkRoute ) {					
						
						NetworkRoute route = (NetworkRoute) leg.getRoute() ;
						
						for (Id<Link> linkId : linkIDs) {
							if (route.getLinkIds().contains(linkId)) {
								agentDrivingAlongSpecificLink = true;
								break;	// no need to go through all other incident link IDs						
							}
						}
						
						break; // no need to go through all other legs
						
					} else {					
						log.warn("This car trip has no network route: " + leg.toString() + " Aborting...");
					}		
				}
			}
			
			if (agentDrivingAlongSpecificLink) {
				personIDs.add(person.getId());
			}
		}
		
		log.info("Total number of agents: " + scenario.getPopulation().getPersons().size());
		log.info("Number of agents considered for within-day replanning: " + personIDs.size());
		
		return personIDs;
	}

}

