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
package playground.ikaddoura.internalizationCar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.Time;

/**
 * @author ikaddoura
 *
 */
public class TollHandler implements MarginalCongestionEventHandler, LinkLeaveEventHandler {
	private static final Logger log = Logger.getLogger(TollHandler.class);
	private double timeBinSize = 900.;
	
	private Map<Id, Map<Double, Double>> linkId2timeBin2tollSum = new HashMap<Id, Map<Double, Double>>();
	private Map<Id, Map<Double, Integer>> linkId2timeBin2leavingAgents = new HashMap<Id, Map<Double, Integer>>();
	
	private List<MarginalCongestionEvent> congestionEvents = new ArrayList<MarginalCongestionEvent>();
	private List<LinkLeaveEvent> linkLeaveEvents = new ArrayList<LinkLeaveEvent>();

	private Map<Id, Map<Double, Double>> linkId2timeBin2avgToll = new HashMap<Id, Map<Double, Double>>();
	private double vtts_car;
	
	public TollHandler(Scenario scenario) {
		this.vtts_car = (scenario.getConfig().planCalcScore().getTraveling_utils_hr() - scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		log.info("VTTS_car: " + vtts_car);
	}

	@Override
	public void reset(int iteration) {
		linkId2timeBin2tollSum.clear();
		linkId2timeBin2leavingAgents.clear();
		this.congestionEvents.clear();
		this.linkId2timeBin2avgToll.clear();
		this.linkLeaveEvents.clear();
	}

	@Override
	public void handleEvent(MarginalCongestionEvent event) {
		this.congestionEvents.add(event);
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		this.linkLeaveEvents.add(event);
	}

	public void setLinkId2timeBin2avgToll() {
		
		if (!this.linkId2timeBin2tollSum.isEmpty()) {
			throw new RuntimeException("Map linkId2timeBin2tollSum should be empty!");
		} else {
			// calculate toll sum for each link and time bin
			setlinkId2timeBin2tollSum();
		}
		
		if (!this.linkId2timeBin2leavingAgents.isEmpty()) {
			throw new RuntimeException("Map linkId2timeBin2leavingAgents should be empty!");
		} else {
			// calculate leaving agents for each link and time bin
			setlinkId2timeBin2leavingAgents();
		}
		
		if (!this.linkId2timeBin2avgToll.isEmpty()) {
			throw new RuntimeException("Map linkId2timeBin2avgToll should be empty!");
		} else {
			// calculate average toll for each link and time bin
			
			for (Id linkId : this.linkId2timeBin2tollSum.keySet()) {
				log.info("Calculating average toll for link " + linkId);
				Map<Double, Double> timeBin2tollSum = this.linkId2timeBin2tollSum.get(linkId);
				Map<Double, Double> timeBin2avgToll = new HashMap<Double, Double>();

				for (Double timeBin : timeBin2tollSum.keySet()){
					double avgToll = 0.0;
					double tollSum = timeBin2tollSum.get(timeBin);
					if (tollSum == 0.) {
						// avg toll is zero for this time bin on that link
					} else {
						double leavingAgents = this.linkId2timeBin2leavingAgents.get(linkId).get(timeBin);
						avgToll = tollSum / leavingAgents;
						log.info("linkId: " + linkId + " // timeBin: " + Time.writeTime(timeBin, Time.TIMEFORMAT_HHMMSS) + " // toll sum: " + tollSum + " // leaving agents: " + leavingAgents + " // avg toll: " + avgToll);
					}
					timeBin2avgToll.put(timeBin, avgToll);
				}
				linkId2timeBin2avgToll.put(linkId , timeBin2avgToll);
			}
		}
	}

	private void setlinkId2timeBin2leavingAgents() {
		for (LinkLeaveEvent event : this.linkLeaveEvents){
			
			if (this.linkId2timeBin2tollSum.containsKey(event.getLinkId())){
				// Tolls paid on this link.
				
				Map<Double, Integer> timeBin2leavingAgents = new HashMap<Double, Integer>();

				if (this.linkId2timeBin2leavingAgents.containsKey(event.getLinkId())) {
					// link already in map
					timeBin2leavingAgents = this.linkId2timeBin2leavingAgents.get(event.getLinkId());
					
					// for this link: search for the right time bin
					for (double time = 0; time < (30 * 3600);) {
						time = time + this.timeBinSize;

						if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
							// link leave event in time bin
							// update leaving agents on this link and in this time bin
							
							if (timeBin2leavingAgents.get(time) != null) {
								// not the first agent leaving this link in this time bin
								int leavingAgentsSoFar = timeBin2leavingAgents.get(time);
								int leavingAgents = leavingAgentsSoFar + 1;
								timeBin2leavingAgents.put(time, leavingAgents);
							} else {
								// first leaving agent leaving this link in this time bin
								timeBin2leavingAgents.put(time, 1);
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
							timeBin2leavingAgents.put(time, 1);
						}
					}
				}
				
				this.linkId2timeBin2leavingAgents.put(event.getLinkId(), timeBin2leavingAgents);	
			
			} else {
				// No tolls paid on that link. Skip that link.
		
			}
		}
		
	}

	private void setlinkId2timeBin2tollSum() {

		for (MarginalCongestionEvent event : this.congestionEvents) {
			Map<Double, Double> timeBin2tollSum = new HashMap<Double, Double>();

			if (this.linkId2timeBin2tollSum.containsKey(event.getLinkId())) {
				// link already in map
				timeBin2tollSum = this.linkId2timeBin2tollSum.get(event.getLinkId());

				// for this link: search for the right time bin
				for (double time = 0; time < (30 * 3600);) {
					time = time + this.timeBinSize;
					
					if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
						// congestion event in time bin
						// update toll sum of this link and time bin
						
						if (timeBin2tollSum.get(time) != null) {
							// toll sum was calculated before for this time bin
							double sum = timeBin2tollSum.get(time);
							double amount = event.getDelay() / 3600.0 * this.vtts_car;
							double sumNew = sum + amount;
							timeBin2tollSum.put(time, sumNew);
						} else {
							// toll sum was not calculated before for this time bin
							double amount = event.getDelay() / 3600.0 * this.vtts_car;
							timeBin2tollSum.put(time, amount);
						}	
					}
				}

			} else {
				// link not yet in map
				
				// for this link: search for the right time bin
				for (double time = 0; time < (30 * 3600);) {
					time = time + this.timeBinSize;

					if (event.getTime() < time && event.getTime() >= (time - this.timeBinSize)) {
						// congestion event in time bin
						double amount = event.getDelay() / 3600.0 * this.vtts_car;
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
	public double getAvgToll(Link link, double time) {
		double avgToll = 0.;
		
		if (this.linkId2timeBin2avgToll.containsKey(link.getId())){
			Map<Double, Double> timeBin2avgToll = this.linkId2timeBin2avgToll.get(link.getId());
			for (Double timeBin : timeBin2avgToll.keySet()) {
				if (time < timeBin && time >= timeBin - this.timeBinSize){
					avgToll = timeBin2avgToll.get(timeBin);
				}
			}
		}
		
		return avgToll;
	}
}
