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

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
	private final double dayEndTime = (24 * 3600.) - 1.;
	
	private boolean debug = false;
	
	private Map<String, TrafficItem> trafficItems = null;
	private Map<String, Path> trafficItemId2path = null;
	private TMCAlerts tmc = null;
	private String crs = null;
	
	private final NetworkChangeEventFactory nceFactory = new NetworkChangeEventFactoryImpl();
		
	public Incident2NetworkChangeEventsWriter(TMCAlerts tmc, Map<String, TrafficItem> trafficItems, Map<String, Path> trafficItemId2path, String crs) {
		this.tmc = tmc;
		this.trafficItems = trafficItems;
		this.trafficItemId2path = trafficItemId2path;
		this.crs = crs;
	}

	public void writeIncidentLinksToNetworkChangeEventFile(String startDateTime, String endDateTime, String outputDirectory) throws IOException, ParseException {
				
		final double startDate = DateTime.parseDateTimeToDateTimeSeconds(startDateTime);
		final double endDate = DateTime.parseDateTimeToDateTimeSeconds(endDateTime);
		
		double dateInSec = startDate;
		while (dateInSec <= endDate) {
			
			log.info("Writing network change events for day " + DateTime.secToDateString(dateInSec) + " (date in seconds: " + dateInSec + ")");
			
			Map<Id<Link>, List<NetworkIncident>> linkId2rawIncidentsCurrentDay = collectDayLinkIncidents(dateInSec);
			Map<Id<Link>, List<NetworkIncident>> linkId2processedIncidentsCurrentDay = processLinkIncidents(linkId2rawIncidentsCurrentDay);
			List<NetworkChangeEvent> allChangeEvents = getNetworkChangeEvents(linkId2processedIncidentsCurrentDay);

			new NetworkChangeEventsWriter().write(outputDirectory + "networkChangeEvents_" + DateTime.secToDateString(dateInSec) + ".xml.gz", allChangeEvents);
			Incident2CSVWriter.writeProcessedNetworkIncidents(linkId2processedIncidentsCurrentDay, outputDirectory + "processedNetworkIncidents_" + DateTime.secToDateString(dateInSec) + ".csv");
			Incident2SHPWriter.writeDailyIncidentLinksToShapeFile(linkId2processedIncidentsCurrentDay, outputDirectory, dateInSec, crs);
			dateInSec = dateInSec + 24 * 3600.;
		}
		
		log.info("Writing network change events completed.");
	}
	
	private Map<Id<Link>, List<NetworkIncident>> processLinkIncidents(Map<Id<Link>, List<NetworkIncident>> linkId2rawIncidentsCurrentDay) {
		
		log.info("Processing the network incidents...");
		
		Map<Id<Link>, List<NetworkIncident>> linkId2linkIncidentsProcessed = new HashMap<>();
		
		int counter = 0;
		for (Id<Link> linkId : linkId2rawIncidentsCurrentDay.keySet()) {
			
			counter++;
			if (counter % 100 == 0) {
				log.info("Link: #" + counter + " (" + (int) (( counter / (double) linkId2rawIncidentsCurrentDay.size()) * 100) + "%)"); 
			}
			
			if (debug) log.warn("#############################################");
			if (debug) log.warn("Processing " + linkId2rawIncidentsCurrentDay.get(linkId).size() + " incidents on link " + linkId + "...");

			if (debug) log.warn("##### Raw incidents:");
			for (NetworkIncident ni : linkId2rawIncidentsCurrentDay.get(linkId)) {
				if (debug) log.warn("	" + ni.toString());
			}
					
			List<NetworkIncident> processedIncidents = processIncidents(linkId2rawIncidentsCurrentDay.get(linkId));
			linkId2linkIncidentsProcessed.put(linkId, processedIncidents);	
					
			if (debug) log.warn("##### Processed incidents:");
			for (NetworkIncident ni : linkId2linkIncidentsProcessed.get(linkId)) {
				if (debug) log.warn("	" + ni.toString());
			} 
		}
		
		log.info("Processing the network incidents... Done.");
		
		return linkId2linkIncidentsProcessed;
	}
	
	private List<NetworkIncident> processIncidents(List<NetworkIncident> incidents) {
		List<NetworkIncident> incidentsProcessed = new ArrayList<>();
		incidentsProcessed.addAll(incidents);

		// empty list
		if (incidents.isEmpty()) {
			log.warn("	> Empty list.");
			return incidentsProcessed;
		}
		
		// easier for debugging
		if (incidentsProcessed.size() > 1) {
			Collections.sort(incidentsProcessed, new IntervalComparatorStartTimeEndTime());	
		}
				
		// remove equal incidents
		if (incidentsProcessed.size() > 1) {
			incidentsProcessed = removeEqualIncidents(incidentsProcessed);
			if (debug) log.warn("	> After removing duplicate incidents:");
			for (NetworkIncident ni : incidentsProcessed) {
				if (debug) log.warn("		>> " + ni.toString());
			}
		}
				
		// split overlaps
		if (incidentsProcessed.size() > 1) {
			incidentsProcessed = breakOverlappingIntervals(incidentsProcessed);
			if (debug) log.warn("	> After breaking overlapping incidents:");
			for (NetworkIncident ni : incidentsProcessed) {
				if (debug) log.warn("		>> " + ni.toString());
			}
		}
		
		// test
		NetworkIncident current = incidentsProcessed.get(0);
		for (int i = 1; i < incidentsProcessed.size(); i++) {
			NetworkIncident next = incidentsProcessed.get(i);
			if ( (int) next.getStartTime() < (int) current.getEndTime() ) {
				throw new RuntimeException("Overlap. Aborting...");
			}
		}
				
        return incidentsProcessed;
	}
	
	private static List<NetworkIncident> removeEqualIncidents(List<NetworkIncident> incidents) {
	
		List<NetworkIncident> incidentsProcessed = new ArrayList<>();
		
		for (NetworkIncident incidentToCheck : incidents) {
			boolean equalToAnExistingIncident = false;

			for (NetworkIncident existingIncident : incidentsProcessed) {
				if (incidentToCheck.parametersToString().equals(existingIncident.parametersToString())) {
					equalToAnExistingIncident = true;
				}
			}
			
			if (equalToAnExistingIncident == false) {
				incidentsProcessed.add(incidentToCheck);
			}
		}
		return incidentsProcessed;
	}

	private static List<NetworkIncident> breakOverlappingIntervals( List<NetworkIncident> incidents ) {

	    TreeMap<Integer,Integer> endPoints = new TreeMap<>();

	    for ( NetworkIncident incident : incidents ) {
	    	
	        Integer startTime = (int) incident.getStartTime();
	        
	        if ( endPoints.containsKey(startTime)) {
	            endPoints.put(startTime, endPoints.get(startTime) + 1);
	        } else {
	            endPoints.put(startTime, 1);
	        }
	        
	        Integer endTime = (int) incident.getEndTime();
	        if ( endPoints.containsKey(endTime)) {
	            endPoints.put(endTime, endPoints.get(startTime) - 1);
	        } else {
	            endPoints.put(endTime, -1);
	        }
	        
	    }

	    int curr = 0;
	    Integer currStart = null;

	    List<NetworkIncident> incidentsProcessedTmp = new ArrayList<>();
	    for ( Map.Entry<Integer,Integer> e : endPoints.entrySet() ) {
	        if ( curr > 0 ) {
	        	NetworkIncident incidentPart = new NetworkIncident("xxx", currStart, e.getKey());
	        	incidentsProcessedTmp.add(incidentPart);
	        }
	        curr += e.getValue();
	        currStart = e.getKey();
	    }
	    
	    // for each split interval go through all incidents and get the most restrictive incident from the original incident.
	    List<NetworkIncident> incidentsProcessed = new ArrayList<>();
	    for (NetworkIncident processedIncidentTmp : incidentsProcessedTmp) {
	    	Link incidentLink = null;
	    	String id = "";

	    	for (NetworkIncident incident : incidents) {
	    		
	    		if (incident.getStartTime() <= processedIncidentTmp.getStartTime() && incident.getEndTime() >= processedIncidentTmp.getEndTime()) {
	    			// in relevant interval
	    			
	    			if (incidentLink == null) {
	    				incidentLink = incident.getIncidentLink();
	    				id = incident.getId();
	    			} else {
	    				incidentLink = getMoreRestrictiveIncidentLink(incidentLink, incident.getIncidentLink());
	    				id = id + "+" + incident.getId();
	    			}
	    			
	    		} else {
	    			// not in the relevant interval
	    		}
	    	}
	    	
	    	if (incidentLink != null) {
	    		NetworkIncident incidentPart = new NetworkIncident(id, processedIncidentTmp.getStartTime(), processedIncidentTmp.getEndTime());
	        	incidentPart.setLink(incidents.get(0).getLink());
	        	incidentPart.setIncidentLink(incidentLink);
	        	incidentsProcessed.add(incidentPart);
	    	} else {
	    		// This interval is a gap between two other incident intervals.
	    	}
	    }
	    
	    return incidentsProcessed;
	}

	class IntervalComparatorStartTimeEndTime implements Comparator<Object> {
        public int compare(Object o1, Object o2){
        	NetworkIncident i1 = (NetworkIncident) o1;
        	NetworkIncident i2 = (NetworkIncident) o2;
        	
        	if ((int) i1.getStartTime() < (int) i2.getStartTime()) {
        		return -1;
        	} else if ((int) i1.getStartTime() > (int) i2.getStartTime()) {
        		return +1;
        	} else {
        		if ((int) i1.getEndTime() < (int) i2.getEndTime()) {
        			return -1;
        		} else if ((int) i1.getEndTime() > (int) i2.getEndTime()) {
        			return +1;
        		} else {
        			return 0;
        		}
        	}
        }
	}
	
	private List<NetworkChangeEvent> getNetworkChangeEvents(Map<Id<Link>, List<NetworkIncident>> linkId2processedIncidentsCurrentDay) {
		
		List<NetworkChangeEvent> networkChangeEvents = new ArrayList<>();
		
		for (Id<Link> linkId : linkId2processedIncidentsCurrentDay.keySet()) {

			if (debug) log.warn("***" + linkId);

			List<NetworkIncident> incidents = linkId2processedIncidentsCurrentDay.get(linkId);
			incidents.sort(new IntervalComparatorStartTimeEndTime());
			
			for (int i = 0; i < incidents.size(); i++) {
				
				NetworkIncident incident = incidents.get(i);
				
				if (debug) log.warn(incident.parametersToString());
				
				// incident start: change values
				NetworkChangeEvent nceStart = nceFactory.createNetworkChangeEvent(incident.getStartTime());
				nceStart.addLink(incident.getLink());
				
				if (incident.getIncidentLink() == null) {
					log.warn("Incident link " +  incident.getId() + " for link " + linkId + " is null.");
					log.warn("Incident link: " + incident.toString());
				}
				
				nceStart.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE, Math.round((incident.getIncidentLink().getCapacity() / 3600.) * 1000) / 1000.0));
				nceStart.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, incident.getIncidentLink().getFreespeed()));
				nceStart.setLanesChange(new ChangeValue(ChangeType.ABSOLUTE, incident.getIncidentLink().getNumberOfLanes()));
				
				networkChangeEvents.add(nceStart);
				
				boolean setBackToOriginalValues = false;
				if (i == incidents.size() - 1) {
					// last incident during the day
					setBackToOriginalValues = true;
				} else {
					if ((int) incidents.get(i+1).getStartTime() == (int) incident.getEndTime()) {
						// the current incident will immediately be followed by another incident
					} else {
						setBackToOriginalValues = true;
					}
				}
				
				if (setBackToOriginalValues) {
					// incident end: set back to original values
					NetworkChangeEvent nceEnd = nceFactory.createNetworkChangeEvent(incident.getEndTime());
					nceEnd.addLink(incident.getLink());
					
					nceEnd.setFlowCapacityChange(new ChangeValue(ChangeType.ABSOLUTE, Math.round((incident.getLink().getCapacity() / 3600.) * 1000) / 1000.0));
					nceEnd.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, incident.getLink().getFreespeed()));
					nceEnd.setLanesChange(new ChangeValue(ChangeType.ABSOLUTE, incident.getLink().getNumberOfLanes()));
					
					networkChangeEvents.add(nceEnd);
				}
			}
		}
		return networkChangeEvents;
	}
	
	private Map<Id<Link>, List<NetworkIncident>> collectDayLinkIncidents(double dateInSec) throws ParseException {
				
		Map<Id<Link>, List<NetworkIncident>> linkId2rawIncidents = new HashMap<>();
		
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
			
			String info = null;
			
			if (DateTime.parseDateTimeToDateTimeSeconds(item.getEndDateTime()) <= dateInSec 
					|| DateTime.parseDateTimeToDateTimeSeconds(item.getStartDateTime()) > dateInSec + (24 * 3600.)) {
				// traffic item ends on a previous day or starts on a later day --> the traffic item is not relevant for this day
				info = "not relevant for this day";
				
			} else if (DateTime.parseDateTimeToDateTimeSeconds(item.getStartDateTime()) <= dateInSec
					&& DateTime.parseDateTimeToDateTimeSeconds(item.getEndDateTime()) >= dateInSec + (24 * 3600.)) {
				// traffic item starts on a previous day and ends on a later day
				
				startTime = 0.;
				endTime = dayEndTime;
				info = "A";
				
			} else if (DateTime.parseDateTimeToDateTimeSeconds(item.getStartDateTime()) <= dateInSec
					&& DateTime.parseDateTimeToDateTimeSeconds(item.getEndDateTime()) > dateInSec
					&& DateTime.parseDateTimeToDateTimeSeconds(item.getEndDateTime()) < dateInSec + (24 * 3600.)) {
				// traffic item starts on a previous day and ends on this day
				
				startTime = 0.;
				endTime = DateTime.parseDateTimeToTimeSeconds(item.getEndDateTime());
				info = "B";

			} else if (DateTime.parseDateTimeToDateTimeSeconds(item.getStartDateTime()) >= dateInSec
					&& DateTime.parseDateTimeToDateTimeSeconds(item.getStartDateTime()) <= dateInSec + (24 * 3600.)
					&& DateTime.parseDateTimeToDateTimeSeconds(item.getEndDateTime()) >= dateInSec + (24 * 3600.)) {
				// traffic item starts on this day and ends on a later day

				startTime = DateTime.parseDateTimeToTimeSeconds(item.getStartDateTime());
				endTime = dayEndTime;
				info = "C";
								
			} else if (DateTime.parseDateTimeToDateTimeSeconds(item.getStartDateTime()) > dateInSec
					&& DateTime.parseDateTimeToDateTimeSeconds(item.getStartDateTime()) < dateInSec + (24 * 3600.)
					&& DateTime.parseDateTimeToDateTimeSeconds(item.getEndDateTime()) > dateInSec
					&& DateTime.parseDateTimeToDateTimeSeconds(item.getEndDateTime()) < dateInSec + (24 * 3600.)) {
				// traffic item starts and ends on this day

				startTime = DateTime.parseDateTimeToTimeSeconds(item.getStartDateTime());
				endTime = DateTime.parseDateTimeToTimeSeconds(item.getEndDateTime());
				info = "D";
				
			} else {
				throw new RuntimeException("Aborting..." + item.toString());
			}
		
			// store the traffic item info which is relevant for this day.
			if (startTime >= 0. && endTime >= 0.) {
				
				if (endTime == startTime) {
					log.warn("Info: " + info);
					log.warn("Start time: " + startTime + " - End time: " + endTime + " - Traffic item: " + item.toString());
				
				} else if (endTime < startTime) {
					log.warn("Info: " + info);
					throw new RuntimeException("Start time: " + startTime + " - End time: " + endTime + " - Traffic item: " + item.toString());
				
				} else {
					for (Link link : this.trafficItemId2path.get(item.getId()).links) {
						NetworkIncident incident = new NetworkIncident(item.getId(), startTime, endTime);
						incident.setLink(link);
						incident.setIncidentLink(tmc.getTrafficIncidentLink(link, this.trafficItems.get(item.getId())));
						
						if (incident.getIncidentLink() != null) {
													
							if (linkId2rawIncidents.containsKey(link.getId())) {
								linkId2rawIncidents.get(link.getId()).add(incident);
								
							} else {
															
								List<NetworkIncident> dayNetworkIncidents = new ArrayList<>();
								dayNetworkIncidents.add(incident);
								linkId2rawIncidents.put(link.getId(), dayNetworkIncidents);
							}
						}
					}
				}
			}
		}
		log.info("Collecting all incidents that are relevant for this day... Done.");
		return linkId2rawIncidents;
	}

	private static Link getMoreRestrictiveIncidentLink(Link incidentLink1, Link incidentLink2) {
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
					if (incidentLink1.getAllowedModes().size() < incidentLink2.getAllowedModes().size()) {
						return incidentLink1;
					} else if (incidentLink1.getAllowedModes().size() > incidentLink2.getAllowedModes().size()) {
						return incidentLink2;
					} else {
						return incidentLink1;
					}
				}
			}
		}
	}
}

