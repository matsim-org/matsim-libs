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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.incidents.data.TrafficItem;

/** 
* Provides the MATSim interpretation of traffic incident codes and some log information.
* 
* @author ikaddoura
*/

public class TMCAlerts {
	
	private static final Logger log = Logger.getLogger(TMCAlerts.class);
	
	private final Set<String> unconsideredCodes = new HashSet<>();
	private final Set<String> loggedCodeAssumedAsCapacityHalving = new HashSet<>();
	private final Set<String> loggedCodeAssumedAsMinusOneLane = new HashSet<>();
	private int warnCnt = 0;
	
	private final boolean printLogStatements = true;
	
	public static final boolean trafficItemIsAnUpdate(TrafficItem trafficItem) {
		
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
	
	public final Link getTrafficIncidentLink(Link link, TrafficItem trafficItem) {
		Link incidentLink = null;
		
		if (trafficItem.getTMCAlert() != null && trafficItem.getTMCAlert().getPhraseCode() != null) {
			
			if (trafficItemIsAnUpdate(trafficItem)) {
				// skip update traffic items
				
			} else {
				
				Network network = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
				NetworkFactoryImpl nf = new NetworkFactoryImpl(network);
								
				// closed roads
				if (containsOrEndsWith(trafficItem, "C1")
						|| containsOrEndsWith(trafficItem, "C6")
						|| containsOrEndsWith(trafficItem, "C7")
						|| containsOrEndsWith(trafficItem, "C14")
						|| containsOrEndsWith(trafficItem, "C35")) {
					
					incidentLink = nf.createLink(link.getId(), link.getFromNode(), link.getToNode());
					incidentLink.setAllowedModes(link.getAllowedModes());
					incidentLink.setCapacity(0.1);
					incidentLink.setNumberOfLanes(0.1);
					incidentLink.setFreespeed(0.22227);
				
				} else if (containsOrEndsWith(trafficItem, "C31")) { // closed for cars
					
					Set<String> allowedModes = link.getAllowedModes();
					if (allowedModes.contains(TransportMode.car)) {
						allowedModes.remove(TransportMode.car);
					}
					
					incidentLink = nf.createLink(link.getId(), link.getFromNode(), link.getToNode());
					incidentLink.setAllowedModes(allowedModes);
					incidentLink.setCapacity(0.1);
					incidentLink.setNumberOfLanes(0.1);
					incidentLink.setFreespeed(0.22227);
				
				// one (or maybe more?) lane(s) closed
				} else if (containsOrEndsWith(trafficItem, "D1")
						|| containsOrEndsWith(trafficItem, "D2")
						|| containsOrEndsWith(trafficItem, "D3")
						|| containsOrEndsWith(trafficItem, "D4")
						|| containsOrEndsWith(trafficItem, "D5")
						|| containsOrEndsWith(trafficItem, "D8")
						|| containsOrEndsWith(trafficItem, "D10")
						|| containsOrEndsWith(trafficItem, "D24")) {
					
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
					
					if (printLogStatements) {
						if (loggedCodeAssumedAsMinusOneLane.contains(trafficItem.getTMCAlert().getDescription())) {
							// warning for this message already logged
						} else {
							log.warn("Assuming that there is only one lane closed. Check the message: " + trafficItem.getTMCAlert().getDescription());
							loggedCodeAssumedAsMinusOneLane.add(trafficItem.getTMCAlert().getDescription());
						}
					}
					
					incidentLink = nf.createLink(link.getId(), link.getFromNode(), link.getToNode());
					incidentLink.setAllowedModes(link.getAllowedModes());
					incidentLink.setCapacity(remainingNumberOfLanes * (link.getCapacity() / link.getNumberOfLanes()));
					incidentLink.setNumberOfLanes(remainingNumberOfLanes);
					incidentLink.setFreespeed(reduceSpeedToNextFreeSpeedLevel(link));
					
				// two lanes closed
				} else if (containsOrEndsWith(trafficItem, "D6")) {
					double remainingNumberOfLanes = 0.;
					if (link.getNumberOfLanes() <= 2.) {
						remainingNumberOfLanes = 1.;
						if (warnCnt <= 3) {
							log.warn("2 lanes closed even though the road segment contains less than three lanes. Setting the lane number to 1.0.");
							if (warnCnt == 2) {
								log.warn("Furhter messages of this type are not printed out.");
							}
							warnCnt++;
						}
					} else {
						remainingNumberOfLanes = link.getNumberOfLanes() - 2.;
					}
						
					incidentLink = nf.createLink(link.getId(), link.getFromNode(), link.getToNode());
					incidentLink.setAllowedModes(link.getAllowedModes());
					incidentLink.setCapacity(remainingNumberOfLanes * (link.getCapacity() / link.getNumberOfLanes()));
					incidentLink.setNumberOfLanes(remainingNumberOfLanes);
					incidentLink.setFreespeed(reduceSpeedToNextFreeSpeedLevel(link));
				
				// three lanes closed
				} else if (containsOrEndsWith(trafficItem, "D7")) {
					double remainingNumberOfLanes = 0.;
					if (link.getNumberOfLanes() <= 3.) {
						remainingNumberOfLanes = 1.;
						if (warnCnt <= 3) {
							log.warn("3 lanes closed even though the road segment contains less than four lanes. Setting the lane number to 1.0.");
							if (warnCnt == 2) {
								log.warn("Furhter messages of this type are not printed out.");
							}
							warnCnt++;
						}
					} else {
						remainingNumberOfLanes = link.getNumberOfLanes() - 3.;
					}
					
					incidentLink = nf.createLink(link.getId(), link.getFromNode(), link.getToNode());
					incidentLink.setAllowedModes(link.getAllowedModes());
					incidentLink.setCapacity(remainingNumberOfLanes * (link.getCapacity() / link.getNumberOfLanes()));
					incidentLink.setNumberOfLanes(remainingNumberOfLanes);
					incidentLink.setFreespeed(reduceSpeedToNextFreeSpeedLevel(link));
										
				// reduction to one lane
				} else if (containsOrEndsWith(trafficItem, "D15")) {
					
					incidentLink = nf.createLink(link.getId(), link.getFromNode(), link.getToNode());
					incidentLink.setAllowedModes(link.getAllowedModes());
					incidentLink.setCapacity((link.getCapacity() / link.getNumberOfLanes()));
					incidentLink.setNumberOfLanes(1.0);
					incidentLink.setFreespeed(reduceSpeedToNextFreeSpeedLevel(link));
				
				// reduction to two lanes
				} else if (containsOrEndsWith(trafficItem, "D16")) {
					
					incidentLink = nf.createLink(link.getId(), link.getFromNode(), link.getToNode());
					incidentLink.setAllowedModes(link.getAllowedModes());
					incidentLink.setCapacity(2.0 * (link.getCapacity() / link.getNumberOfLanes()));
					incidentLink.setNumberOfLanes(2.0);
					incidentLink.setFreespeed(reduceSpeedToNextFreeSpeedLevel(link));
					
				// one alternating lane closed per direction
				} else if (containsOrEndsWith(trafficItem, "E14")) {
					
					incidentLink = nf.createLink(link.getId(), link.getFromNode(), link.getToNode());
					incidentLink.setAllowedModes(link.getAllowedModes());
					incidentLink.setCapacity((link.getNumberOfLanes() - 0.5) * (link.getCapacity() / link.getNumberOfLanes()));
					incidentLink.setNumberOfLanes(link.getNumberOfLanes() - 0.5);
					incidentLink.setFreespeed(reduceSpeedToNextFreeSpeedLevel(link));
								
				// accidents, e.g. B1: accident, .. TODO: Adjust according to type
				} else if (trafficItem.getTMCAlert().getPhraseCode().contains("B")) {
					
					incidentLink = nf.createLink(link.getId(), link.getFromNode(), link.getToNode());
					incidentLink.setAllowedModes(link.getAllowedModes());
					incidentLink.setCapacity(link.getCapacity() / 2.0);
					incidentLink.setNumberOfLanes(1.0);
					incidentLink.setFreespeed(reduceSpeedToNextFreeSpeedLevel(link));
					
					logCodeConsideredAsCapacityAndSpeedReduction(trafficItem.getTMCAlert().getPhraseCode(), trafficItem.getTMCAlert().getDescription());		
	
				// E, U: construction work, water, gas, buried cables, buried services main work
				// F: obstruction hazards, e.g. F14 broken water pipe, F15 gas leak, F16 fire, F17 animals on road
				// G: snow, aquaplaning, hazardous driving
				// P: dangerous situations, e.g. P39: Personen auf der Fahrbahn
				// R: dangerous situations
				} else if (containsOrEndsWith(trafficItem, "E1")
						|| containsOrEndsWith(trafficItem, "E7")
						|| containsOrEndsWith(trafficItem, "E11")
						|| containsOrEndsWith(trafficItem, "E15")
						|| containsOrEndsWith(trafficItem, "E16")
						|| containsOrEndsWith(trafficItem, "E17")
						|| containsOrEndsWith(trafficItem, "E18")
						|| containsOrEndsWith(trafficItem, "F15")
						|| containsOrEndsWith(trafficItem, "U8")
						|| trafficItem.getTMCAlert().getPhraseCode().contains("E")
						|| trafficItem.getTMCAlert().getPhraseCode().contains("F")
						|| trafficItem.getTMCAlert().getPhraseCode().contains("G")
						|| trafficItem.getTMCAlert().getPhraseCode().contains("P")
						|| trafficItem.getTMCAlert().getPhraseCode().contains("R")
						) {
					
					incidentLink = nf.createLink(link.getId(), link.getFromNode(), link.getToNode());
					incidentLink.setAllowedModes(link.getAllowedModes());
					incidentLink.setCapacity(link.getCapacity() / 2.0);
					incidentLink.setNumberOfLanes(1.0);
					incidentLink.setFreespeed(reduceSpeedToNextFreeSpeedLevel(link));
					
					logCodeConsideredAsCapacityAndSpeedReduction(trafficItem.getTMCAlert().getPhraseCode(), trafficItem.getTMCAlert().getDescription());
					
				} else {
					if (printLogStatements) {
						if (unconsideredCodes.contains(trafficItem.getTMCAlert().getPhraseCode())) {
							// warning for code already logged
						} else {
							log.warn("+++ Code " +  trafficItem.getTMCAlert().getPhraseCode() + " / message: " + trafficItem.getTMCAlert().getDescription() + " is not defined! Check if code is important and needs to be added. +++");
							unconsideredCodes.add(trafficItem.getTMCAlert().getPhraseCode());
						}	
					}			
				}
			}
		}
				
		return incidentLink;
	}

	private void logCodeConsideredAsCapacityAndSpeedReduction(String alertCode, String description) {
		if (printLogStatements) {
			if (loggedCodeAssumedAsCapacityHalving.contains(alertCode)) {
				// warning for code already logged
			} else {
				log.warn("Code " +  alertCode + " / message: " + description + " is interpreted as an incident where the capacity is halved, the speed is reduced and the number of lanes is set to 1 lane.");
				loggedCodeAssumedAsCapacityHalving.add(alertCode);
			}
		}
	}

	private static boolean containsOrEndsWith(TrafficItem trafficItem, String code) {
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
	
	private static double reduceSpeedToNextFreeSpeedLevel(Link link) {
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

	public double getAdditionalTravelTime(TrafficItem trafficItem) {
		
		// Q: 
		if (trafficItem.getTMCAlert().getPhraseCode().contains("Q1(5)")) {
			return 5 * 60.;
		} else {
			return 0.;
		}
	}

}

