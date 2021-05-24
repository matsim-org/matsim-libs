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
package playground.vsp.congestion.handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;

import playground.vsp.congestion.events.CongestionEvent;

/**
 * @author ikaddoura
 *
 */
public class TollHandler implements CongestionEventHandler, LinkEnterEventHandler, PersonDepartureEventHandler {
	private static final Logger log = Logger.getLogger(TollHandler.class);
	private double timeBinSize = 900.;
	
	private Map<Id<Link>, Map<Double, Double>> linkId2timeBin2tollSum = new HashMap<Id<Link>, Map<Double, Double>>();
	private Map<Id<Link>, Map<Double, Integer>> linkId2timeBin2enteringAndDepartingAgents = new HashMap<Id<Link>, Map<Double, Integer>>();
	
	private List<CongestionEvent> congestionEvents = new ArrayList<CongestionEvent>();
	private List<LinkEnterEvent> linkEnterEvents = new ArrayList<LinkEnterEvent>();
	private List<PersonDepartureEvent> personDepartureEvents = new ArrayList<PersonDepartureEvent>();

	private Map<Id<Link>, Map<Double, Double>> linkId2timeBin2avgToll = new HashMap<Id<Link>, Map<Double,Double>>();
	private Map<Id<Link>, Map<Double, Double>> linkId2timeBin2avgTollOldValue = new HashMap<Id<Link>, Map<Double, Double>>();
	
	private boolean setMethodsExecuted = false;
	
	private double vtts_car;
	private double congestionTollFactor;
		
	public TollHandler(Scenario scenario, double congestionTollFactor) {
		this.vtts_car = (scenario.getConfig().planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() - scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		this.congestionTollFactor = congestionTollFactor;
		log.info("VTTS_car: " + vtts_car);
		log.info("Congestion toll factor: " + congestionTollFactor);
	}
	
	public TollHandler(Scenario scenario) {
		this(scenario, 1.0);
	}

	@Override
	public void reset(int iteration) {
		this.linkId2timeBin2tollSum.clear();
		this.linkId2timeBin2enteringAndDepartingAgents.clear();
		this.congestionEvents.clear();
		this.linkEnterEvents.clear();
		this.personDepartureEvents.clear();

		this.linkId2timeBin2avgTollOldValue.clear();
		this.linkId2timeBin2avgTollOldValue.putAll(linkId2timeBin2avgToll);
		this.linkId2timeBin2avgToll.clear();
		
		this.setMethodsExecuted = false;		
	}

	@Override
	public void handleEvent(CongestionEvent event) {
		this.congestionEvents.add(event);
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.linkEnterEvents.add(event);	
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.car.toString())) {
			this.personDepartureEvents.add(event);
		} else {
			// other simulated modes are not accounted for
		}
	}

	public void setLinkId2timeBin2avgToll() {
		
		log.info("Total number of congestion events: " + this.congestionEvents.size());
		
		if (!this.linkId2timeBin2tollSum.isEmpty()) {
			throw new RuntimeException("Map linkId2timeBin2tollSum should be empty!");
		} else {
			// calculate toll sum for each link and time bin
			setlinkId2timeBin2tollSum();
		}
		
		if (!this.linkId2timeBin2enteringAndDepartingAgents.isEmpty()) {
			throw new RuntimeException("Map linkId2timeBin2enteringAndDepartingAgents should be empty!");
		} else {
			// calculate leaving agents for each link and time bin
			setlinkId2timeBin2enteringAndDepartingAgents();
		}
		
		this.setMethodsExecuted = true;
		
		if (!this.linkId2timeBin2avgToll.isEmpty()) {
			throw new RuntimeException("Map linkId2timeBin2avgToll should be empty!");
		} else {
			// calculate average toll for each link and time bin
			
			for (Id<Link> linkId : this.linkId2timeBin2tollSum.keySet()) {
//				log.info("Calculating average toll for link " + linkId);
				Map<Double, Double> timeBin2tollSum = this.linkId2timeBin2tollSum.get(linkId);
				Map<Double, Double> timeBin2avgToll = new HashMap<Double, Double>();

				for (Double timeBin : timeBin2tollSum.keySet()){
					double avgToll = 0.0;
					double tollSum = timeBin2tollSum.get(timeBin);
					if (tollSum == 0.) {
						// avg toll is zero for this time bin on that link
					} else {
						double enteringAndDepartingAgents = this.linkId2timeBin2enteringAndDepartingAgents.get(linkId).get(timeBin);
						if (enteringAndDepartingAgents == 0.) {
							throw new RuntimeException("Toll sum on link " + linkId + " in time bin " + timeBin + " is " + tollSum + ", but there is no agent departing / entering that link in that time bin. Aborting...");
						} else {
							avgToll = tollSum / enteringAndDepartingAgents;
//							log.info("linkId: " + linkId + " // timeBin: " + Time.writeTime(timeBin, Time.TIMEFORMAT_HHMMSS) + " // toll sum: " + tollSum + " // leaving agents: " + enteringAndDepartingAgents + " // avg toll: " + avgToll);
						}
					}
					timeBin2avgToll.put(timeBin, avgToll);
				}
				linkId2timeBin2avgToll.put(linkId , timeBin2avgToll);
			}
		}
	}

	private void setlinkId2timeBin2enteringAndDepartingAgents() {
		
		// first go through all link enter events
		for (LinkEnterEvent event : this.linkEnterEvents){
			
//			if (this.linkId2timeBin2tollSum.containsKey(event.getLinkId())){
//				// Tolls paid on this link.
				
				Map<Double, Integer> timeBin2enteringAgents = new HashMap<Double, Integer>();

				if (this.linkId2timeBin2enteringAndDepartingAgents.containsKey(event.getLinkId())) {
					// link already in map
					timeBin2enteringAgents = this.linkId2timeBin2enteringAndDepartingAgents.get(event.getLinkId());
					
					// for this link: search for the right time bin
					for (double time = 0; time < (30 * 3600);) {
						time = time + this.timeBinSize;

						if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
							// event in time bin
							// update entering agents on this link and in this time bin
							
							if (timeBin2enteringAgents.get(time) != null) {
								// not the first agent entering this link in this time bin
								int enteringAgentsSoFar = timeBin2enteringAgents.get(time);
								int enteringAgents = enteringAgentsSoFar + 1;
								timeBin2enteringAgents.put(time, enteringAgents);
							} else {
								// first agent entering this link in this time bin
								timeBin2enteringAgents.put(time, 1);
							}
						}
					}

				} else {
					// link not yet in map

					// for this link: search for the right time bin
					for (double time = 0; time < (30 * 3600);) {
						time = time + this.timeBinSize;

						if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
							// link leave event in time bin
							timeBin2enteringAgents.put(time, 1);
						}
					}
				}
				
				this.linkId2timeBin2enteringAndDepartingAgents.put(event.getLinkId(), timeBin2enteringAgents);	
			
//			} else {
//				// No tolls paid on that link. Skip that link.
//		
//			}
		}
		
		
		// then go through all person departure events
		// a person departure event means an agent also 'enters' the link
		for (PersonDepartureEvent event : this.personDepartureEvents) {

//			if (this.linkId2timeBin2tollSum.containsKey(event.getLinkId())) {
//				// Tolls paid on this link.

				Map<Double, Integer> timeBin2departingAgents = new HashMap<Double, Integer>();

				if (this.linkId2timeBin2enteringAndDepartingAgents.containsKey(event.getLinkId())) {
					// link already in map
					timeBin2departingAgents = this.linkId2timeBin2enteringAndDepartingAgents.get(event.getLinkId());

					// for this link: search for the right time bin
					for (double time = 0; time < (30 * 3600);) {
						time = time + this.timeBinSize;

						if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
							// event in time bin
							// update agents on this link and in this time bin

							if (timeBin2departingAgents.get(time) != null) {
								// not the first agent departing on this link in this time bin
								int departingAgentsSoFar = timeBin2departingAgents.get(time);
								int departingAgents = departingAgentsSoFar + 1;
								timeBin2departingAgents.put(time, departingAgents);
							} else {
								// first agent departing on this link in this time bin
								timeBin2departingAgents.put(time, 1);
							}
						}
					}

				} else {
					// link not yet in map

					// for this link: search for the right time bin
					for (double time = 0; time < (30 * 3600);) {
						time = time + this.timeBinSize;

						if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
							// event in time bin
							timeBin2departingAgents.put(time, 1);
						}
					}
				}

				this.linkId2timeBin2enteringAndDepartingAgents.put(event.getLinkId(), timeBin2departingAgents);

//			} else {
//				// No tolls paid on that link. Skip that link.
//
//			}
		}

	}

	private void setlinkId2timeBin2tollSum() {

		for (CongestionEvent event : this.congestionEvents) {
			Map<Double, Double> timeBin2tollSum = new HashMap<Double, Double>();

			if (this.linkId2timeBin2tollSum.containsKey(event.getLinkId())) {
				// link already in map
				timeBin2tollSum = this.linkId2timeBin2tollSum.get(event.getLinkId());

				// for this link: search for the right time bin
				for (double time = 0; time < (30 * 3600);) {
					time = time + this.timeBinSize;
					
					if (event.getEmergenceTime() < time && event.getEmergenceTime() >= (time - this.timeBinSize)) {
						// congestion event in time bin
						// update toll sum of this link and time bin
						
						if (timeBin2tollSum.get(time) != null) {
							// toll sum was calculated before for this time bin
							double sum = timeBin2tollSum.get(time);
							double amount = this.congestionTollFactor * (event.getDelay() / 3600.0 * this.vtts_car);
							double sumNew = sum + amount;
							timeBin2tollSum.put(time, sumNew);
						} else {
							// toll sum was not calculated before for this time bin
							double amount = this.congestionTollFactor * (event.getDelay() / 3600.0 * this.vtts_car);
							timeBin2tollSum.put(time, amount);
						}	
					}
				}

			} else {
				// link not yet in map
				
				// for this link: search for the right time bin
				for (double time = 0; time < (30 * 3600);) {
					time = time + this.timeBinSize;

					if (event.getEmergenceTime() < time && event.getEmergenceTime() >= (time - this.timeBinSize)) {
						// congestion event in time bin
						double amount = this.congestionTollFactor * (event.getDelay() / 3600.0 * this.vtts_car);
						timeBin2tollSum.put(time, amount);
					}
				}
			}
			
			this.linkId2timeBin2tollSum.put(event.getLinkId(), timeBin2tollSum);
		}
	}

	/**
	 * Returns the avg toll (negative monetary amount) paid on that link during that time bin.
	 */
	public double getAvgToll(Id<Link> linkId, double time) {
		double avgToll = 0.;
		
		if (this.linkId2timeBin2avgToll.containsKey(linkId)){
			Map<Double, Double> timeBin2avgToll = this.linkId2timeBin2avgToll.get(linkId);
			for (Double timeBin : timeBin2avgToll.keySet()) {
				if (time < timeBin && time >= timeBin - this.timeBinSize){
					avgToll = timeBin2avgToll.get(timeBin);
				}
			}
		}
		
		return avgToll;
	}
	
	/**
	 * Returns the avg toll (old value) (negative monetary amount) paid on that link during that time bin.
	 */
	public double getAvgTollOldValue(Id<Link> linkId, double time) {
		double avgTollOldValue = 0.;
		
		if (this.linkId2timeBin2avgTollOldValue.containsKey(linkId)){
			Map<Double, Double> timeBin2avgTollOldValue = this.linkId2timeBin2avgTollOldValue.get(linkId);
			for (Double timeBin : timeBin2avgTollOldValue.keySet()) {
				if (time < timeBin && time >= timeBin - this.timeBinSize){
					avgTollOldValue = timeBin2avgTollOldValue.get(timeBin);
				}
			}
		}
		
		return avgTollOldValue;
	}

	public void writeTollStats(String fileName) {
		
		if (this.setMethodsExecuted == false) {
			log.info("Average tolls per link Id and time bin have to be set. Running required method...");
			this.setLinkId2timeBin2avgToll();
		}
		
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("link;total toll (per day);entering (and departing) agents (per day)");
			bw.newLine();
			
			for (Id<Link> linkId : this.linkId2timeBin2enteringAndDepartingAgents.keySet()){
				double totalToll = 0.;
				int enteringAgents = 0;
								
				if (this.linkId2timeBin2tollSum.get(linkId) == null) {
					// There is no toll payment in any time bin.
					
				} else {
					for (Double tollSum_timeBin : this.linkId2timeBin2tollSum.get(linkId).values()){
						totalToll = totalToll + tollSum_timeBin;
					}
				}
				
				for (Integer enteringDepartingAgents_timeBin : this.linkId2timeBin2enteringAndDepartingAgents.get(linkId).values()){
					enteringAgents = enteringAgents + enteringDepartingAgents_timeBin;
				}
				
				bw.write(linkId + ";" + totalToll + ";" + enteringAgents);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public List<LinkEnterEvent> getLinkEnterEvents() {
		return linkEnterEvents;
	}
}
