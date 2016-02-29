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
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;

import playground.ikaddoura.incidents.data.TrafficItem;

/** 
* @author ikaddoura
*/

public class TMCAlerts {
	private static final Logger log = Logger.getLogger(TMCAlerts.class);
	private Set<String> unconsideredCodes = new HashSet<>();
	private Set<String> loggedCodeAssumedAsCapacityHalving = new HashSet<>();
	private Set<String> loggedCodeAssumedAsMinusOneLane = new HashSet<>();
	private int warnCnt = 0;
	
	private Object[] createIncidentObject(Object[] incidentObject, Link link, TrafficItem trafficItem,
			Set<String> changedAllowedModes,
			double changedCapacity,
			double remainingNumberOfLanes,
			double changedFreeSpeed) {

		if (incidentObject == null) {
			incidentObject = new Object[] {link.getId(), trafficItem.getId(), (trafficItem.getOrigin().getDescription() + " --> " + trafficItem.getTo().getDescription()), trafficItem.getTMCAlert().getPhraseCode(), trafficItem.getTMCAlert().getDescription(), link.getLength(),
					
					// the parameters under normal conditions
					link.getAllowedModes(), link.getCapacity(), link.getNumberOfLanes(), link.getFreespeed(),
						
					// incident specific values
					changedAllowedModes, changedCapacity, remainingNumberOfLanes, changedFreeSpeed,
						
					// start and end time
					trafficItem.getStartTime(), trafficItem.getEndTime()
			};			
		} else {
			throw new RuntimeException("This should not happen. Aborting...");
		}
		
		return incidentObject;
	}
	
	protected boolean trafficItemIsAnUpdate(TrafficItem trafficItem) {
		
		if (trafficItem.getTMCAlert().getPhraseCode().endsWith("86") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("87") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("88") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("89") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("90") ||
			
				// Z91 is not a cancellation / clearance
				trafficItem.getTMCAlert().getPhraseCode().endsWith("C91") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("P91") ||
				
				trafficItem.getTMCAlert().getPhraseCode().endsWith("92") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("93") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("94") ||
				
				// Z95 is not a cancellation / clearance 
				trafficItem.getTMCAlert().getPhraseCode().endsWith("Y95") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("X95") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("U95") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("T95") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("Q95") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("P95") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("L95") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("H95") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("G95") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("F95") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("E95") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("D95") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("C95") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("B95") ||
							
				trafficItem.getTMCAlert().getPhraseCode().endsWith("96") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("97") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("98") ||
				
				// T99 is not a cancellation / clearance
				trafficItem.getTMCAlert().getPhraseCode().endsWith("F99") ||
				trafficItem.getTMCAlert().getPhraseCode().endsWith("H99")
				) {
			
			return true;
						
		} else {
			return false;
		}
	}
	
	public Object[] getIncidentObject(Link link, TrafficItem trafficItem) {
		Object[] incidentObject = null;
		
//		log.info(trafficItem.getTMCAlert().getPhraseCode());
		
		if (trafficItem.getTMCAlert() != null && trafficItem.getTMCAlert().getPhraseCode() != null) {
			
			if (trafficItemIsAnUpdate(trafficItem)) {
				// incident / warning cleared or no longer valid
				
			} else {
				
				// ####### specific codes ########
				
				if (containsOrEndsWith(trafficItem, "C1")) { // closed
					incidentObject = createIncidentObject(incidentObject, link, trafficItem, 
							link.getAllowedModes(),
							1.0,
							1.0,
							0.22227
					);
				
				} else if (containsOrEndsWith(trafficItem, "C31")) { // closed for cars
					incidentObject = createIncidentObject(incidentObject, link, trafficItem, 
							link.getAllowedModes(),
							1.0,
							1.0,
							0.22227
					);
				
				} else if (containsOrEndsWith(trafficItem, "D1") || containsOrEndsWith(trafficItem, "D2") || containsOrEndsWith(trafficItem, "D3") || containsOrEndsWith(trafficItem, "D4") || containsOrEndsWith(trafficItem, "D5")) { // eine bestimmte Zahl an Fahrstreifen gesperrt
					double remainingNumberOfLanes = 0.;
					if (link.getNumberOfLanes() == 1.) {
						remainingNumberOfLanes = 1.;
						if (warnCnt <= 2) {
							log.warn("Lane closed even though the road segment contains only one lane. Setting the lane number to 1.0.");
							if (warnCnt == 2) {
								log.warn("Furhter messages of this type are not printed out.");
							}
							warnCnt++;
						}
					} else {
						remainingNumberOfLanes = link.getNumberOfLanes() - 1.;
					}
					
					if (loggedCodeAssumedAsMinusOneLane.contains(trafficItem.getTMCAlert().getDescription())) {
						// warning for this message already logged
					} else {
						log.warn("Assuming that there is only one lane closed. Check the message: " + trafficItem.getTMCAlert().getDescription());
						loggedCodeAssumedAsMinusOneLane.add(trafficItem.getTMCAlert().getDescription());
					}
					
					incidentObject = createIncidentObject(incidentObject, link, trafficItem, 
							link.getAllowedModes(),
							remainingNumberOfLanes * (link.getCapacity() / link.getNumberOfLanes()),
							remainingNumberOfLanes,
							reduceSpeedToNextFreeSpeedLevel(link)
					);
					
				} else if (containsOrEndsWith(trafficItem, "D15")) { // reduction to one lane
					incidentObject = createIncidentObject(incidentObject, link, trafficItem, 
							link.getAllowedModes(),
							(link.getCapacity() / link.getNumberOfLanes()),
							1.0,
							reduceSpeedToNextFreeSpeedLevel(link)
					);
				
				} else if (containsOrEndsWith(trafficItem, "D16")) { // reduction to two lanes
					incidentObject = createIncidentObject(incidentObject, link, trafficItem, 
							link.getAllowedModes(),
							2.0 * (link.getCapacity() / link.getNumberOfLanes()),
							2.0,
							reduceSpeedToNextFreeSpeedLevel(link)
					);
				
				} else if (containsOrEndsWith(trafficItem, "E1") || containsOrEndsWith(trafficItem, "E7") || containsOrEndsWith(trafficItem, "E11") ) { // construction work
					incidentObject = createIncidentObject(incidentObject, link, trafficItem, 
							link.getAllowedModes(),
							link.getCapacity() / 2.0,
							1.0,
							reduceSpeedToNextFreeSpeedLevel(link)
					);
					
					logCodeConsideredAsCapacityAndSpeedReduction(trafficItem.getTMCAlert().getPhraseCode(), trafficItem.getTMCAlert().getDescription());
					
				} else if (containsOrEndsWith(trafficItem, "E14")) { // abwechselnd in beide Richtungen nur ein Fahrstreifen frei
					incidentObject = createIncidentObject(incidentObject, link, trafficItem, 
							link.getAllowedModes(),
							1.5 * (link.getCapacity() / link.getNumberOfLanes()),
							1.0,
							reduceSpeedToNextFreeSpeedLevel(link)
					);
				
				} else if (containsOrEndsWith(trafficItem, "E15") || containsOrEndsWith(trafficItem, "E16") || containsOrEndsWith(trafficItem, "E17") || containsOrEndsWith(trafficItem, "E18")) { // water, gas, buried cables, buried services main work
					incidentObject = createIncidentObject(incidentObject, link, trafficItem, 
							link.getAllowedModes(),
							link.getCapacity() / 2.0,
							1.0,
							reduceSpeedToNextFreeSpeedLevel(link)
					);
				
				} else if (containsOrEndsWith(trafficItem, "F15")) { // water, gas, buried cables, buried services main work
					incidentObject = createIncidentObject(incidentObject, link, trafficItem, 
							link.getAllowedModes(),
							link.getCapacity() / 2.0,
							1.0,
							reduceSpeedToNextFreeSpeedLevel(link)
					);
				
				// #######  other codes are decoded using more general code categories ######## 
				
				} else if (trafficItem.getTMCAlert().getPhraseCode().contains("B")) { // accidents, e.g. B1: accident, .. TODO: Adjust according to type
					incidentObject = createIncidentObject(incidentObject, link, trafficItem, 
							link.getAllowedModes(),
							link.getCapacity() / 2.0,
							1.0,
							reduceSpeedToNextFreeSpeedLevel(link)
					);
					
					logCodeConsideredAsCapacityAndSpeedReduction(trafficItem.getTMCAlert().getPhraseCode(), trafficItem.getTMCAlert().getDescription());
					
				} else if (trafficItem.getTMCAlert().getPhraseCode().contains("E")) { // (other) construction work types
					incidentObject = createIncidentObject(incidentObject, link, trafficItem, 
							link.getAllowedModes(),
							link.getCapacity() / 2.0,
							1.0,
							reduceSpeedToNextFreeSpeedLevel(link)
					);
					
					logCodeConsideredAsCapacityAndSpeedReduction(trafficItem.getTMCAlert().getPhraseCode(), trafficItem.getTMCAlert().getDescription());
					
				} else if (trafficItem.getTMCAlert().getPhraseCode().contains("F")) { // obstruction hazards, e.g. F14 broken water pipe, F15 gas leak, F16 fire, F17 animals on road
										
					incidentObject = createIncidentObject(incidentObject, link, trafficItem, 
							link.getAllowedModes(),
							link.getCapacity() / 2.0,
							1.0,
							reduceSpeedToNextFreeSpeedLevel(link)
					);
					
					logCodeConsideredAsCapacityAndSpeedReduction(trafficItem.getTMCAlert().getPhraseCode(), trafficItem.getTMCAlert().getDescription());
					
				} else if (trafficItem.getTMCAlert().getPhraseCode().contains("P")) { // dangerous situations, e.g. P39: Personen auf der Fahrbahn
					incidentObject = createIncidentObject(incidentObject, link, trafficItem, 
							link.getAllowedModes(),
							link.getCapacity() / 2.0,
							1.0,
							reduceSpeedToNextFreeSpeedLevel(link)
					);

					logCodeConsideredAsCapacityAndSpeedReduction(trafficItem.getTMCAlert().getPhraseCode(), trafficItem.getTMCAlert().getDescription());

					
				} else {
					if (unconsideredCodes.contains(trafficItem.getTMCAlert().getPhraseCode())) {
						// warning for code already logged
					} else {
						log.warn("+++ Code " +  trafficItem.getTMCAlert().getPhraseCode() + " / message: " + trafficItem.getTMCAlert().getDescription() + " is not defined! Check if code is important and needs to be added. +++");
						unconsideredCodes.add(trafficItem.getTMCAlert().getPhraseCode());
					}				
				}
			}
		}
		
		return incidentObject;
	}

	private void logCodeConsideredAsCapacityAndSpeedReduction(String alertCode, String description) {
		if (loggedCodeAssumedAsCapacityHalving.contains(alertCode)) {
			// warning for code already logged
		} else {
			log.warn("Code " +  alertCode + " / message: " + description + " is interpreted as an incident where the capacity is halved, the speed is reduced and the number of lanes is set to 1 lane.");
			loggedCodeAssumedAsCapacityHalving.add(alertCode);
		}
	}

	private boolean containsOrEndsWith(TrafficItem trafficItem, String code) {
		if (trafficItem.getTMCAlert().getPhraseCode() == null) {
			log.warn("Alert with no code: " + trafficItem.toString());
			return false;
		} else {
			if (trafficItem.getTMCAlert().getPhraseCode().contains(code + ".") || trafficItem.getTMCAlert().getPhraseCode().endsWith(code)) {
				return true;
			} else {
				return false;
			}
		}
	}
	
	private double reduceSpeedToNextFreeSpeedLevel(Link link) {
		double changedFreeSpeed = 0.;

			if (link.getFreespeed() >= 16.66667) {
				changedFreeSpeed = 16.66667;
				
			} else if (link.getFreespeed() < 16.66667 && link.getFreespeed() > 8. ) {
				changedFreeSpeed = 8.33334;
			}
			
			else if (link.getFreespeed() <= 8. ) {
				changedFreeSpeed = 2.77778;
			}
			
		return changedFreeSpeed;
	}

}

