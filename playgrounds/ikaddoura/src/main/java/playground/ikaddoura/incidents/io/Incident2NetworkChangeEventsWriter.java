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
import java.util.List;
import java.util.Map;

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
			
//			final List<NetworkChangeEvent> startChangeEvents = new ArrayList<>();
//			final List<NetworkChangeEvent> endChangeEvents = new ArrayList<>();
			
			final Map<String, NetworkIncident> dayNetworkIncidents = getDayNetworkIncidents(dateInSec);
			final Map<Id<Link>, List<NetworkIncident>> linkId2dayNetworkIncident = getLink2Incidents(dayNetworkIncidents);

			final List<NetworkChangeEvent> allChangeEvents = getNetworkChangeEvents(linkId2dayNetworkIncident);
			
			new NetworkChangeEventsWriter().write(outputDirectory + "networkChangeEvents_" + DateTime.secToDateTimeString(dateInSec) + ".xml.gz", allChangeEvents);
			dateInSec = dateInSec + 24 * 3600.;
		}
	}
	
	private List<NetworkChangeEvent> getNetworkChangeEvents(Map<Id<Link>, List<NetworkIncident>> linkId2incidents) {
		Map<Id<Link>, List<NetworkIncident>> linkId2networkChangeIncidents = new HashMap<>();

		for (Id<Link> linkId : linkId2incidents.keySet()) {
			for (NetworkIncident incident : linkId2incidents.get(linkId)) {
				
				if (linkId2networkChangeIncidents.containsKey(linkId)) {
					// not the first incident on this link on this day
					
					for (NetworkIncident otherIncident : linkId2incidents.get(linkId)) {
						
						if (incident.toString().equals(otherIncident.toString())) {
							// all relevant parameters (start time, end time, flow capacity, freespeed, number of lanes) are the same
							
						} else {
							
							if (incident.getStartTime() > otherIncident.getEndTime() || otherIncident.getStartTime() > incident.getEndTime()) {
							
								// one traffic item starts after the other one has ended, everything fine...
								linkId2networkChangeIncidents.get(linkId).add(incident);
								
							} else if ((int) incident.getStartTime() == (int) otherIncident.getStartTime()
									&& (int) incident.getEndTime() == (int) otherIncident.getEndTime()) {

								// exact the times --> adjust the existing item to the minimum values...
								log.warn("Same start and end time. Using the more restrictive incident...");
								log.warn("> New incident: " + incident.toString());
								log.warn("Other incident: " + otherIncident.toString());
								
								NetworkIncident moreRestrictiveIncident = getMoreRestrictiveIncident(incident, otherIncident);
								log.warn("More restrictive incident: " + moreRestrictiveIncident.toString());
								linkId2networkChangeIncidents.get(linkId).add(moreRestrictiveIncident);
								
							} else {
								
								log.warn("Overlapping incidents on link " +  linkId);
								log.warn(" > New incident: " + incident.toString());
								log.warn(" Other incident: " + otherIncident.toString());
								
								// the first part --> adjust the end time but keep the incident code
//								if (incident.getStartTime() < otherIncident.getStartTime()) {
//									incident.setEndTime(otherIncident.getStartTime());
//								} else if (otherIncident.getStartTime() < incident.getStartTime()) {
//									otherIncident.setEndTime(incident.getStartTime());
//								}
								
								// the middle part --> 
								
								// the last part
//								if (incident.getEndTime() > otherIncident.getEndTime()) {
//									otherIncident.setStartTime(incident);
//								}
								
								// TODO: Use in such cases the minimum values!
//								incidentToAdd = new NetworkIncident(incident.getStartTime(), incident.getEndTime(), getMoreRestrictiveTrafficItem(link, incident.getTrafficItem(), otherIncident.getTrafficItem()));
									
							}
						}	
					}
					
				} else {
					List<NetworkIncident> incidents = new ArrayList<>();
					incidents.add(incident);
					linkId2networkChangeIncidents.put(linkId, incidents);
				}
			}
		}
		
		// Now create the network change events
		List<NetworkChangeEvent> networkChangeEvents = new ArrayList<>();
		
		for (Id<Link> linkId : linkId2networkChangeIncidents.keySet()) {
		
			for (NetworkIncident incident : linkId2networkChangeIncidents.get(linkId)) {
				
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
		}
		return networkChangeEvents;
	}

	private Map<Id<Link>, List<NetworkIncident>> getLink2Incidents(Map<String, NetworkIncident> dayNetworkIncidents) {
		Map<Id<Link>, List<NetworkIncident>> linkId2incidents = new HashMap<>();
		
		for (String trafficItemId : dayNetworkIncidents.keySet()) {
			NetworkIncident incident = dayNetworkIncidents.get(trafficItemId);
			
			for (Link link : this.trafficItemId2path.get(trafficItemId).links) {
				incident.setLink(link);
				incident.setIncidentLink(tmc.getTrafficIncidentLink(link, this.trafficItems.get(trafficItemId)));
				
				if (linkId2incidents.containsKey(link.getId())) {
					
					if (incident.getIncidentLink() != null) {
						linkId2incidents.get(link.getId()).add(incident);
					}
					
				} else {
					
					if (incident.getIncidentLink() != null) {
						List<NetworkIncident> incidents = new ArrayList<>();
						incidents.add(incident);
						linkId2incidents.put(link.getId(), incidents);
					}
				}
			}
		}
		
		return linkId2incidents;	
	}
	
	private NetworkIncident getMoreRestrictiveIncident(NetworkIncident incident1, NetworkIncident incident2) {
		Link trafficIncidentLink1 = incident1.getIncidentLink();
		Link trafficIncidentLink2 = incident2.getIncidentLink();
		
		if (trafficIncidentLink1 == null && trafficIncidentLink2 == null) {
			return null;
		} else if (trafficIncidentLink1 == null && trafficIncidentLink2 != null) {
			return incident2;
		} else if (trafficIncidentLink1 != null && trafficIncidentLink2 == null) {
			return incident1;
		} else {
			Link moreRestrictiveIncidentLink = getMoreRestrictiveLink(trafficIncidentLink1, trafficIncidentLink2);
			
			NetworkIncident moreRestrictiveIncident = null;
			if (moreRestrictiveIncidentLink.equals(trafficIncidentLink1)) {
				moreRestrictiveIncident = incident1;
			} else if (moreRestrictiveIncidentLink.equals(trafficIncidentLink2)) {
				moreRestrictiveIncident = incident2;
			}
			return moreRestrictiveIncident;
		}
	}

	private Link getMoreRestrictiveLink(Link incidentLink1, Link incidentLink2) {
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

	private Map<String, NetworkIncident> getDayNetworkIncidents(double dateInSec) {
		Map<String, NetworkIncident> dayNetworkIncidents = new HashMap<>();
		
		for (TrafficItem item : this.trafficItems.values()) {
//			log.info("Creating network change event for traffic item " + item.getId() + " (" + this.trafficItemId2path.get(item.getId()).links.size() + " links)." );
		
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
				dayNetworkIncidents.put(item.getId(), new NetworkIncident(startTime, endTime));
			}
		}
		
		return dayNetworkIncidents;
	}

}

