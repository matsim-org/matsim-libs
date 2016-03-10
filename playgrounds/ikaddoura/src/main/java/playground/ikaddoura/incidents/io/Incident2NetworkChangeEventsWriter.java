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
			
			List<NetworkIncident> dayNetworkIncidents = new ArrayList<>();
			dayNetworkIncidents = getDayNetworkIncidents(dateInSec);
			
			log.info("Network incidents: " + dayNetworkIncidents.size());
			
			List<NetworkChangeEvent> allChangeEvents = new ArrayList<>();
			allChangeEvents = getNetworkChangeEvents(dayNetworkIncidents);
			
			log.info("Network change events: " + allChangeEvents.size());
			
			new NetworkChangeEventsWriter().write(outputDirectory + "networkChangeEvents_" + DateTime.secToDateTimeString(dateInSec) + ".xml.gz", allChangeEvents);
			dateInSec = dateInSec + 24 * 3600.;
		}
		
		log.info("Writing network change events completed.");

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

	private List<NetworkIncident> getDayNetworkIncidents(double dateInSec) {
		List<NetworkIncident> dayNetworkIncidents = new ArrayList<>();
		Set<Id<Link>> dayLinkIds = new HashSet<>();
		int counter = 0;
		
		for (TrafficItem item : this.trafficItems.values()) {
//			log.info("Creating network change event for traffic item " + item.getId() + " (" + this.trafficItemId2path.get(item.getId()).links.size() + " links)." );
			
			counter++;
			if (counter % 1000 == 0) {
				log.info("Traffic item: " + counter + " (" + (int) (( counter / (double) this.trafficItems.size()) * 100) + "%)"); 
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
				throw new RuntimeException("Oups. Aborting..." + item.toString());
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
						
						if (dayLinkIds.contains(link.getId())) {
														
							// check the existing incidents
							ListIterator<NetworkIncident> it = dayNetworkIncidents.listIterator();
							while (it.hasNext()) {
								NetworkIncident existingIncident = it.next();
									
								if (incident.getLink().getId().toString().equals(existingIncident.getLink().getId().toString())) {
									// same link
									
									if (incident.parametersToString().equals(existingIncident.parametersToString())) {
										// all relevant parameters (start time, end time, flow capacity, freespeed, number of lanes) are the same --> no need to add the incident
										
									} else {
										// incident1 and incident2 have different relevant parameters
											
										if (incident.getStartTime() > existingIncident.getEndTime() || existingIncident.getStartTime() > incident.getEndTime()) {
											// one traffic item starts after the other one has ended --> keep both
											it.add(incident);
												
										} else if ((int) incident.getStartTime() == (int) existingIncident.getStartTime()
												&& (int) incident.getEndTime() == (int) existingIncident.getEndTime()
												) {
											// exact the same times --> merge incident 1 and 2 to the more restrictive incident
										
//											log.warn("Same start and end time. Using the more restrictive incident...");
//											log.warn("Incident: " + incident);
//											log.warn("Existing incident: " + existingIncident);
											
											NetworkIncident mergedIncident = new NetworkIncident(incident.getId() + "+" + existingIncident.getId(), incident.getStartTime(), incident.getEndTime());
											mergedIncident.setLink(incident.getLink());
											mergedIncident.setIncidentLink(getMoreRestrictiveIncidentLink(incident, existingIncident));

											it.remove();
											it.add(mergedIncident);
											
//											log.warn("Merged incident: " + mergedIncident);
																			
										} else if (incident.getIncidentLink().getCapacity() == existingIncident.getIncidentLink().getCapacity()
												&& incident.getIncidentLink().getFreespeed() == existingIncident.getIncidentLink().getFreespeed()
												&& incident.getIncidentLink().getNumberOfLanes() == existingIncident.getIncidentLink().getNumberOfLanes()
												) {
											
											// same capacity, freespeed and number of lanes --> merge incident 1 and 2 to the minimum start time and the maximum end time
											
//											log.warn("Same incident parameters but different overlapping times. Using the min start and max end time...");
//											log.warn("Incident: " + incident);
//											log.warn("Existing incident: " + existingIncident);
											
											NetworkIncident mergedIncident = new NetworkIncident(incident.getId() + "+" + existingIncident.getId(), getMin(incident.getStartTime(), existingIncident.getStartTime()),  getMax(incident.getEndTime(), existingIncident.getEndTime()));
											mergedIncident.setLink(incident.getLink());
											mergedIncident.setIncidentLink(incident.getIncidentLink());
											
											it.remove();
											it.add(mergedIncident);
											
//											log.warn("Merged incident: " + mergedIncident);

										} else {
																			
//											log.warn("Different incident parameters and overlapping times.");
//											log.warn("Incident: " + incident);
//											log.warn("Existing incident: " + existingIncident);

											NetworkIncident firstPartIncident = null;
											NetworkIncident middlePartIncident = null;
											NetworkIncident lastPartIncident = null;

											// first part
											if (incident.getStartTime() < existingIncident.getStartTime()) {
												firstPartIncident = new NetworkIncident(incident.getId() + "+(f)+" + existingIncident.getId(), incident.getStartTime(), existingIncident.getStartTime());
												firstPartIncident.setIncidentLink(incident.getIncidentLink());
											} else if (existingIncident.getStartTime() < incident.getStartTime()) {
												firstPartIncident = new NetworkIncident(incident.getId() + "+(f)+" + existingIncident.getId(), existingIncident.getStartTime(), incident.getStartTime());
												firstPartIncident.setIncidentLink(existingIncident.getIncidentLink());
											} else {
												// same start time
												firstPartIncident = new NetworkIncident(incident.getId() + "+(f)+" + existingIncident.getId(), incident.getStartTime(), existingIncident.getStartTime());
												firstPartIncident.setIncidentLink(incident.getIncidentLink());
											}
											firstPartIncident.setLink(incident.getLink());
																					
											// last part
											if (incident.getEndTime() < existingIncident.getEndTime()) {
												lastPartIncident = new NetworkIncident(incident.getId() + "+(l)+" + existingIncident.getId(), incident.getEndTime(), existingIncident.getEndTime());
												lastPartIncident.setIncidentLink(existingIncident.getIncidentLink());
											} else if (existingIncident.getEndTime() < incident.getEndTime()) {
												lastPartIncident = new NetworkIncident(incident.getId() + "+(l)+" + existingIncident.getId(), existingIncident.getEndTime(), incident.getEndTime());
												lastPartIncident.setIncidentLink(incident.getIncidentLink());
											} else {
												// same end time
												lastPartIncident = new NetworkIncident(incident.getId() + "+(l)+" + existingIncident.getId(), incident.getEndTime(), existingIncident.getEndTime());
												lastPartIncident.setIncidentLink(existingIncident.getIncidentLink());
											}
											lastPartIncident.setLink(incident.getLink());
											
											// middle part
											middlePartIncident = new NetworkIncident(incident.getId() + "+(m)+" + existingIncident.getId(), firstPartIncident.getEndTime(), lastPartIncident.getStartTime());
											middlePartIncident.setIncidentLink(getMoreRestrictiveIncidentLink(incident, existingIncident));
											middlePartIncident.setLink(incident.getLink());
																						
											it.remove();
											
											if (firstPartIncident.getEndTime() > firstPartIncident.getStartTime()) {
												it.add(firstPartIncident);
//												log.warn("First part incident: " + firstPartIncident);
											}

											if (middlePartIncident.getEndTime() > middlePartIncident.getStartTime()) {
												it.add(middlePartIncident);
//												log.warn("Middle part incident: " + middlePartIncident);
											}
											
											if (lastPartIncident.getEndTime() > lastPartIncident.getStartTime()) {
												it.add(lastPartIncident);
//												log.warn("Last part incident: " + lastPartIncident);
											}
										}
									}
								} 
							}
							
						} else {
							
							// first incident on link
							dayLinkIds.add(link.getId());
							dayNetworkIncidents.add(incident);
						}
					}
				}				
			}
		}
		
		return dayNetworkIncidents;
	}
	
	private double getMax(double v1, double v2) {
		if (v1 < v2) {
			return v2;
		} else if (v1 > v2) {
			return v1;
		} else {
			return v1;
		}
	}

	private double getMin(double v1, double v2) {
		if (v1 < v2) {
			return v1;
		} else if (v1 > v2) {
			return v2;
		} else {
			return v1;
		}
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

