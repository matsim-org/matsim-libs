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

package playground.ikaddoura.incidents.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkChangeEventFactory;
import org.matsim.core.network.NetworkChangeEventFactoryImpl;
import org.matsim.core.network.NetworkChangeEventsWriter;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import playground.ikaddoura.incidents.DateTime;
import playground.ikaddoura.incidents.TMCAlerts;
import playground.ikaddoura.incidents.data.NetworkIncident;
import playground.ikaddoura.incidents.data.TrafficItem;

/**
 * 
 * Writes traffic incidents to network change event files.
 * 
 * Link attributes are modified for the duration of the incident. Whenever an incident is over, the network attributes are set back to the original Link attributes.
 * In case of spatially and temporally overlapping traffic incidents the more restrictive information will be used (smaller flow capacity, smaller freespeed, smaller number of lanes).
 * 
* @author ikaddoura
*/

public class Incident2NetworkChangeEventsWriter {

	private static final Logger log = Logger.getLogger(Incident2NetworkChangeEventsWriter.class);

	private Map<String, TrafficItem> trafficItems = null;
	private Map<String, Path> trafficItemId2path = null;
	private TMCAlerts tmc = null;
	
	private final NetworkChangeEventFactory nceFactory = new NetworkChangeEventFactoryImpl();
	
	private Map<Id<Link>, List<NetworkIncident>> linkId2incidents = new HashMap<>();
	
	public Incident2NetworkChangeEventsWriter(TMCAlerts tmc, Map<String, TrafficItem> trafficItems, Map<String, Path> trafficItemId2path) {
		this.tmc = tmc;
		this.trafficItems = trafficItems;
		this.trafficItemId2path = trafficItemId2path;		
	}

	public void writeIncidentLinksToNetworkChangeEventFile(String startDateTime, String endDateTime, String outputDirectory) {
				
		final double startDate = DateTime.parseDateTimeToDateTimeSeconds(startDateTime);
		final double endDate = DateTime.parseDateTimeToDateTimeSeconds(endDateTime);
		
		double dateInSec = startDate;
		while (dateInSec <= endDate) {
			
			log.info("Writing network change events for day " + DateTime.secToDateTimeString(dateInSec));
			linkId2incidents.clear();
			
			collectDayLinkIncidents(dateInSec);
			
			List<NetworkIncident> processedNetworkIncidents = processLinkIncidents();
			List<NetworkChangeEvent> allChangeEvents = getNetworkChangeEvents(processedNetworkIncidents);

			new NetworkChangeEventsWriter().write(outputDirectory + "networkChangeEvents_" + DateTime.secToDateTimeString(dateInSec) + ".xml.gz", allChangeEvents);
			dateInSec = dateInSec + 24 * 3600.;
		}
		
		log.info("Writing network change events completed.");

	}
	
	private List<NetworkIncident> processLinkIncidents() {
		
		log.info("Processing the network incidents...");
		
		Map<Id<Link>, List<NetworkIncident>> linkId2linkIncidentsProcessed = new HashMap<>();
		
		int counter = 0;
		for (Id<Link> linkId : this.linkId2incidents.keySet()) {
			
			counter++;
			if (counter % 1000 == 0) {
				log.info("Link: #" + counter + " (" + (int) (( counter / (double) this.linkId2incidents.size()) * 100) + "%)"); 
			}
			
			log.warn("#############################################");
			log.warn("Processing " + this.linkId2incidents.get(linkId).size() + " incidents on link " + linkId + "...");

			for (NetworkIncident incidentNotInMap : this.linkId2incidents.get(linkId)) {
				log.warn("Incident which is not in the map: " + incidentNotInMap.toString());

				if (linkId2linkIncidentsProcessed.containsKey(linkId)) {
					// compare the incident which is not in the map with all other incidents (on the same link)

					log.warn("---");
					
					log.warn("Before processing the incidents:");
					for (NetworkIncident ni : linkId2linkIncidentsProcessed.get(linkId)) {
						log.warn(ni.toString());
					}
					
					List<NetworkIncident> nonOverlappingIncidents = new ArrayList<>();
					nonOverlappingIncidents = addIncidentAndSplitOverlappingIncidents(incidentNotInMap, linkId2linkIncidentsProcessed.get(linkId));
					
					linkId2linkIncidentsProcessed.put(linkId, nonOverlappingIncidents);
					
					log.warn("After processing the incidents:");
					for (NetworkIncident ni : linkId2linkIncidentsProcessed.get(linkId)) {
						log.warn(ni.toString());
					} 
					
				} else {
					// first processed incident on link
					log.warn("First incident on link. Adding incident to map: " + incidentNotInMap.toString());

					List<NetworkIncident> incidents = new ArrayList<>();
					incidents.add(incidentNotInMap);
					linkId2linkIncidentsProcessed.put(linkId, incidents);
				}
			}
		}
		
		log.info("Processing the network incidents... Done.");
		log.info("Adding all network incidents to one list...");

		List<NetworkIncident> allNetworkIncidentsFromLinks = new ArrayList<>();
		for (Id<Link> linkId : linkId2linkIncidentsProcessed.keySet()) {
			allNetworkIncidentsFromLinks.addAll(linkId2linkIncidentsProcessed.get(linkId));
		}
		log.info("Adding all network incidents to one list... Done.");

		return allNetworkIncidentsFromLinks;
	}
	
	private List<NetworkIncident> addIncidentAndSplitOverlappingIncidents(NetworkIncident incidentNotInMap, List<NetworkIncident> incidents) {
		log.warn("		(a) Incident not in map: " + incidentNotInMap);

		boolean addNonOverlappingIncident = true;
		
		List<NetworkIncident> incidentsProcessed = new ArrayList<>();
		incidentsProcessed.addAll(incidents);
				
		ListIterator<NetworkIncident> it = incidentsProcessed.listIterator();
		while (it.hasNext()) {
			NetworkIncident existingIncident = it.next();
			
			log.warn("		(b) Incident in map: " + existingIncident);

			if (incidentNotInMap.parametersToString().equals(existingIncident.parametersToString())) {
				addNonOverlappingIncident = false;
				log.warn("			--> Equal incidents. Do not add the (a) to the map.");

			} else if ((int) incidentNotInMap.getStartTime() == (int) existingIncident.getStartTime() || (int) existingIncident.getEndTime() == (int) incidentNotInMap.getEndTime()) {
				addNonOverlappingIncident = false;

				NetworkIncident moreRestrictiveIncident = new NetworkIncident(incidentNotInMap.getId() + "+(m)+" + existingIncident.getId(), existingIncident.getStartTime(), existingIncident.getEndTime());
				moreRestrictiveIncident.setIncidentLink(getMoreRestrictiveIncidentLink(incidentNotInMap, existingIncident));
				moreRestrictiveIncident.setLink(incidentNotInMap.getLink());
				it.remove();
				it.add(moreRestrictiveIncident);
				
				log.warn("			--> Same times. Adding the more restrictive incident and removing the existing incident.");

			} else if ((int) incidentNotInMap.getStartTime() >= (int) existingIncident.getEndTime() || (int) existingIncident.getStartTime() >= (int) incidentNotInMap.getEndTime()) {
				// no overlap --> keep existing
				log.warn("			--> No overlap between (a) and (b). Check also the other existing incidents. If there is no overlap with any other incident, add (a) to map.");
				
			} else {
				// overlap --> split the incident in three parts, add the three parts and remove the old one
				addNonOverlappingIncident = false;

				log.warn("			--> Overlap between (a) and (b). Splitting the overlapping incidents in three parts and removing the existing incident.");

				NetworkIncident firstPartIncident = null;
				NetworkIncident middlePartIncident = null;
				NetworkIncident lastPartIncident = null;

				// first part
				if (incidentNotInMap.getStartTime() < existingIncident.getStartTime()) {
					firstPartIncident = new NetworkIncident(incidentNotInMap.getId() + "+(f)+" + existingIncident.getId(), incidentNotInMap.getStartTime(), existingIncident.getStartTime());
					firstPartIncident.setIncidentLink(incidentNotInMap.getIncidentLink());
				} else if (existingIncident.getStartTime() < incidentNotInMap.getStartTime()) {
					firstPartIncident = new NetworkIncident(incidentNotInMap.getId() + "+(f)+" + existingIncident.getId(), existingIncident.getStartTime(), incidentNotInMap.getStartTime());
					firstPartIncident.setIncidentLink(existingIncident.getIncidentLink());
				} else {
					// same start time
					firstPartIncident = new NetworkIncident(incidentNotInMap.getId() + "+(f)+" + existingIncident.getId(), incidentNotInMap.getStartTime(), existingIncident.getStartTime());
					firstPartIncident.setIncidentLink(incidentNotInMap.getIncidentLink());
				}
				firstPartIncident.setLink(incidentNotInMap.getLink());
														
				// last part
				if (incidentNotInMap.getEndTime() < existingIncident.getEndTime()) {
					lastPartIncident = new NetworkIncident(incidentNotInMap.getId() + "+(l)+" + existingIncident.getId(), incidentNotInMap.getEndTime(), existingIncident.getEndTime());
					lastPartIncident.setIncidentLink(existingIncident.getIncidentLink());
				} else if (existingIncident.getEndTime() < incidentNotInMap.getEndTime()) {
					lastPartIncident = new NetworkIncident(incidentNotInMap.getId() + "+(l)+" + existingIncident.getId(), existingIncident.getEndTime(), incidentNotInMap.getEndTime());
					lastPartIncident.setIncidentLink(incidentNotInMap.getIncidentLink());
				} else {
					// same end time
					lastPartIncident = new NetworkIncident(incidentNotInMap.getId() + "+(l)+" + existingIncident.getId(), incidentNotInMap.getEndTime(), existingIncident.getEndTime());
					lastPartIncident.setIncidentLink(existingIncident.getIncidentLink());
				}
				lastPartIncident.setLink(incidentNotInMap.getLink());
				
				// middle part
				middlePartIncident = new NetworkIncident(incidentNotInMap.getId() + "+(m)+" + existingIncident.getId(), firstPartIncident.getEndTime(), lastPartIncident.getStartTime());
				middlePartIncident.setIncidentLink(getMoreRestrictiveIncidentLink(incidentNotInMap, existingIncident));
				middlePartIncident.setLink(incidentNotInMap.getLink());
								
				it.remove();
				
				if ((int) firstPartIncident.getEndTime() > (int) firstPartIncident.getStartTime()) {
					log.warn("		--> First part incident: " + firstPartIncident);
					incidentsProcessed = addIncidentAndSplitOverlappingIncidents(firstPartIncident, incidentsProcessed);
				}

				if ((int) middlePartIncident.getEndTime() > (int) middlePartIncident.getStartTime()) {
					log.warn("		--> Middle part incident: " + middlePartIncident);
					incidentsProcessed = addIncidentAndSplitOverlappingIncidents(middlePartIncident, incidentsProcessed);
				}
				
				if ((int) lastPartIncident.getEndTime() > (int) lastPartIncident.getStartTime()) {
					log.warn("		--> Last part incident: " + lastPartIncident);
					incidentsProcessed = addIncidentAndSplitOverlappingIncidents(lastPartIncident, incidentsProcessed);
				}
			}
			
		}
		
		if (addNonOverlappingIncident) {

			incidentsProcessed.add(incidentNotInMap);

			log.warn("No overlap with any other incident. Adding (a) to map.");
						
			log.warn("Before adding:");
			for (NetworkIncident ni : incidentsProcessed) {
				log.warn(ni.toString());
			}
			
			log.warn(" --> Adding " + incidentNotInMap.toString());
			
			log.warn("After adding:");
			for (NetworkIncident ni : incidentsProcessed) {
				log.warn(ni.toString());
			} 	
		}
		
		// check if there is any overlap...
		for (NetworkIncident incident1 : incidentsProcessed) {
			for (NetworkIncident incident2 : incidentsProcessed) {
				if (incident1 != incident2) {
					if ((int) incident1.getStartTime() >= (int) incident2.getEndTime() || (int) incident2.getStartTime() >= (int) incident1.getEndTime()) {
						// no overlap
					} else {
						log.warn("Overlap1: " + incident1.toString());
						log.warn("Overlap2: " + incident2.toString());
						throw new RuntimeException("there should not be any overlap...");
					}
				}
			}
		}
		
		
		return incidentsProcessed;
	}

	private List<NetworkChangeEvent> getNetworkChangeEvents(List<NetworkIncident> incidents) {
		
		List<NetworkChangeEvent> networkChangeEvents = new ArrayList<>();
		
		for (NetworkIncident incident : incidents) {
			
			// incident start: change values
			NetworkChangeEvent nceStart = nceFactory.createNetworkChangeEvent(incident.getStartTime());
			nceStart.addLink(incident.getLink());
			
			nceStart.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE, incident.getIncidentLink().getCapacity()));
			nceStart.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, incident.getIncidentLink().getFreespeed()));
			nceStart.setLanesChange(new ChangeValue(ChangeType.ABSOLUTE, incident.getIncidentLink().getNumberOfLanes()));
			
			networkChangeEvents.add(nceStart);
			
			// incident end: set back to original values
			NetworkChangeEvent nceEnd = nceFactory.createNetworkChangeEvent(incident.getEndTime());
			nceEnd.addLink(incident.getLink());
			
			nceEnd.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE, incident.getLink().getCapacity()));
			nceEnd.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, incident.getLink().getFreespeed()));
			nceEnd.setLanesChange(new ChangeValue(ChangeType.ABSOLUTE, incident.getLink().getNumberOfLanes()));
			
			networkChangeEvents.add(nceEnd);
		}
		return networkChangeEvents;
	}
	
	private void collectDayLinkIncidents(double dateInSec) {
				
		log.info("Collecting all incidents that are relevant for this day...");
		int counter = 0;
		
		for (TrafficItem item : this.trafficItems.values()) {
//			log.info("Creating network change event for traffic item " + item.getId() + " (" + this.trafficItemId2path.get(item.getId()).links.size() + " links)." );
			
			counter++;
			if (counter % 1000 == 0) {
				log.info("Traffic item: #" + counter + " (" + (int) (( counter / (double) this.trafficItems.size()) * 100) + "%)"); 
			}
			
			double startTime = Double.NEGATIVE_INFINITY;
			double endTime = Double.NEGATIVE_INFINITY;
			
			if (DateTime.parseDateTimeToDateTimeSeconds(item.getEndDateTime()) < dateInSec 
					|| DateTime.parseDateTimeToDateTimeSeconds(item.getStartDateTime()) > dateInSec + (24 * 3600.)) {
				// traffic item ends on a previous day or starts on a later day --> the traffic item is not relevant for this day
			
			} else if (DateTime.parseDateTimeToDateTimeSeconds(item.getStartDateTime()) < dateInSec
					&& DateTime.parseDateTimeToDateTimeSeconds(item.getEndDateTime()) > dateInSec + (24 * 3600.)) {
				// traffic item starts on a previous day and ends on a later day
				
				startTime = 0.;
				endTime = 24. * 3600.;
				
			} else if (DateTime.parseDateTimeToDateTimeSeconds(item.getStartDateTime()) < dateInSec
					&& DateTime.parseDateTimeToDateTimeSeconds(item.getEndDateTime()) > dateInSec
					&& DateTime.parseDateTimeToDateTimeSeconds(item.getEndDateTime()) < dateInSec + (24 * 3600.)) {
				// traffic item starts on a previous day and ends on this day
				
				startTime = 0.;
				endTime = DateTime.parseDateTimeToTimeSeconds(item.getEndDateTime());
									
			} else if (DateTime.parseDateTimeToDateTimeSeconds(item.getStartDateTime()) > dateInSec
					&& DateTime.parseDateTimeToDateTimeSeconds(item.getStartDateTime()) < dateInSec + (24 * 3600.)
					&& DateTime.parseDateTimeToDateTimeSeconds(item.getEndDateTime()) > dateInSec + (24 * 3600.)) {
				// traffic item starts on this day and ends on a later day

				startTime = DateTime.parseDateTimeToTimeSeconds(item.getStartDateTime());
				endTime = 24. * 3600.;
								
			} else if (DateTime.parseDateTimeToDateTimeSeconds(item.getStartDateTime()) > dateInSec
					&& DateTime.parseDateTimeToDateTimeSeconds(item.getStartDateTime()) < dateInSec + (24 * 3600.)
					&& DateTime.parseDateTimeToDateTimeSeconds(item.getEndDateTime()) > dateInSec
					&& DateTime.parseDateTimeToDateTimeSeconds(item.getEndDateTime()) < dateInSec + (24 * 3600.)) {
				// traffic item starts and ends on this day

				startTime = DateTime.parseDateTimeToTimeSeconds(item.getStartDateTime());
				endTime = DateTime.parseDateTimeToTimeSeconds(item.getEndDateTime());
									
			} else {
				throw new RuntimeException("Aborting..." + item.toString());
			}
		
			// store the traffic item info which is relevant for this day.
			if (startTime >= 0. && endTime >= 0.) {
				
				if (endTime <= startTime) {
					throw new RuntimeException("Start time: " + startTime + " - End time: " + endTime + " - Traffic item: " + item.toString());
				}				
								
				for (Link link : this.trafficItemId2path.get(item.getId()).links) {
					NetworkIncident incident = new NetworkIncident(item.getId(), startTime, endTime);
					incident.setLink(link);
					incident.setIncidentLink(tmc.getTrafficIncidentLink(link, this.trafficItems.get(item.getId())));
					
					if (incident.getIncidentLink() != null) {
												
						if (linkId2incidents.containsKey(link.getId())) {
							linkId2incidents.get(link.getId()).add(incident);
							
						} else {
														
							List<NetworkIncident> dayNetworkIncidents = new ArrayList<>();
							dayNetworkIncidents.add(incident);
							linkId2incidents.put(link.getId(), dayNetworkIncidents);
						}
					}
				}
			}
		}
		log.info("Collecting all incidents that are relevant for this day... Done.");
	}
	
	private Link getMoreRestrictiveIncidentLink(NetworkIncident incident1, NetworkIncident incident2) {
		Link trafficIncidentLink1 = incident1.getIncidentLink();
		Link trafficIncidentLink2 = incident2.getIncidentLink();
		
		if (trafficIncidentLink1 == null && trafficIncidentLink2 == null) {
			return null;
		} else if (trafficIncidentLink1 == null && trafficIncidentLink2 != null) {
			return incident2.getIncidentLink();
		} else if (trafficIncidentLink1 != null && trafficIncidentLink2 == null) {
			return incident1.getIncidentLink();
		} else {
			Link moreRestrictiveIncidentLink = getMoreRestrictiveIncidentLink(trafficIncidentLink1, trafficIncidentLink2);
			return moreRestrictiveIncidentLink;
		}
	}

	private Link getMoreRestrictiveIncidentLink(Link incidentLink1, Link incidentLink2) {
		if (incidentLink1.getCapacity() < incidentLink2.getCapacity()) {
			return incidentLink1;
		} else if (incidentLink1.getCapacity() > incidentLink2.getCapacity()) {
			return incidentLink2;
		} else {
			if (incidentLink1.getFreespeed() < incidentLink2.getFreespeed()) {
				return incidentLink1;
			} else if (incidentLink1.getFreespeed() > incidentLink2.getFreespeed()) {
				return incidentLink2;
			} else {
				if (incidentLink1.getNumberOfLanes() < incidentLink2.getNumberOfLanes()) {
					return incidentLink1;
				} else if (incidentLink1.getNumberOfLanes() > incidentLink2.getNumberOfLanes()) {
					return incidentLink2;
				} else {
					return incidentLink1;
				}
			}
		}
	}


}

