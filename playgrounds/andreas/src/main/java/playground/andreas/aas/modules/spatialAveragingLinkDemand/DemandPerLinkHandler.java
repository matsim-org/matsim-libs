/* *********************************************************************** *
 * project: org.matsim.*
 * CongestionPerLinkHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.andreas.aas.modules.spatialAveragingLinkDemand;


/**
 * @author benjamin after fhuelsmann
 *
 */

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

public class DemandPerLinkHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler, Wait2LinkEventHandler {
	
	private static final Logger logger = Logger.getLogger(DemandPerLinkHandler.class);

	Map<Id<Vehicle>, Id<Person>> vehicleId2DriverId = new HashMap<>();
	
	Map<Id<Vehicle>, Tuple<Id<Link>, Double>> linkEnterPerVehId = new HashMap<>();
	Map<Id<Person>, Tuple<Id<Link>, Double>> arrivalPerPersonId = new HashMap<>();
	Map<Id<Person>, Tuple<Id<Link>, Double>> departurePerPersonId = new HashMap<>();
	
	Map<Double, Map<Id<Link>, Integer>> time2LinkIdAndDemand = new HashMap<>();
	Map<Double, Map<Id<Link>, Double>> time2LinkIdAndCongestionTime = new HashMap<>();
	private final Network network;
	private final int noOfTimeBins;
	private final double timeBinSize;

	int linkLeaveCnt = 0;
	int linkLeaveWarnCnt = 0;
	final int maxLinkLeaveWarnCnt = 3;

	public DemandPerLinkHandler(Network network, double simulationEndTime, int noOfTimeBins) {
		this.network = network;
		this.noOfTimeBins = noOfTimeBins;
		this.timeBinSize = simulationEndTime / noOfTimeBins;
	}

	@Override
	public void reset(final int iteration) {
		this.time2LinkIdAndDemand.clear();
		logger.info("Resetting travel demand aggregation to " + this.time2LinkIdAndDemand);
		this.time2LinkIdAndCongestionTime.clear();
		logger.info("Resetting congestion time aggregation to " + this.time2LinkIdAndCongestionTime);
		
		linkEnterPerVehId.clear();
		arrivalPerPersonId.clear();
		departurePerPersonId.clear();
		
		linkLeaveCnt = 0;
		linkLeaveWarnCnt = 0;
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		vehicleId2DriverId.put(event.getVehicleId(), event.getPersonId());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Tuple<Id<Link>, Double> linkId2Time = new Tuple<>(event.getLinkId(), event.getTime());
		this.linkEnterPerVehId.put(event.getVehicleId(), linkId2Time);
	}
	
	public void handleEvent(final LinkLeaveEvent event) {
		Id<Link> linkId = event.getLinkId();
		double linkLeaveTime = event.getTime();
		double endOfTimeInterval;
		
		for(int i = 0; i < noOfTimeBins; i++){
			if(linkLeaveTime > i * timeBinSize && linkLeaveTime <= (i + 1) * timeBinSize){
				endOfTimeInterval = (i + 1) * timeBinSize;
				Map<Id<Link>, Integer> linkId2Counts = new HashMap<>();

				if(time2LinkIdAndDemand.get(endOfTimeInterval) == null){
					linkId2Counts.put(linkId, 1);
				} else {
					linkId2Counts = time2LinkIdAndDemand.get(endOfTimeInterval);
					if(linkId2Counts.get(linkId) == null){
						linkId2Counts.put(linkId, 1);
					} else {
						int countsSoFar = linkId2Counts.get(linkId);
						int newValue = countsSoFar + 1;
						linkId2Counts.put(linkId, newValue);
					}
				}
				time2LinkIdAndDemand.put(endOfTimeInterval, linkId2Counts);
			}
		}
		
		
		Id<Vehicle> vehId = event.getVehicleId();
		Id<Person> driverId = vehicleId2DriverId.get(vehId);
		if(!this.linkEnterPerVehId.containsKey(vehId)){
			if(linkLeaveWarnCnt < maxLinkLeaveWarnCnt){
				logger.warn("Vehicle " + vehId + " is leaving link " + linkId + " without having entered. " +
				"Thus, no emissions are calculated for this link leave event.");
				if (linkLeaveWarnCnt == maxLinkLeaveWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED);
			}
			linkLeaveWarnCnt++;
			return;
		}
		double enterTime = this.linkEnterPerVehId.get(vehId).getSecond();
		double travelTime;
		if(!this.arrivalPerPersonId.containsKey(driverId) || !this.departurePerPersonId.containsKey(driverId)){
			travelTime = linkLeaveTime - enterTime;
		}
		else if(!this.arrivalPerPersonId.get(driverId).getFirst().equals(event.getLinkId())
				|| !this.departurePerPersonId.get(driverId).getFirst().equals(event.getLinkId())){

			travelTime = linkLeaveTime - enterTime;
		} else {
		double arrivalTime = this.arrivalPerPersonId.get(driverId).getSecond();		
		double departureTime = this.departurePerPersonId.get(driverId).getSecond();	
		travelTime = linkLeaveTime - enterTime - departureTime + arrivalTime;	
		}
		
		double freeFlowSpeed = this.network.getLinks().get(linkId).getFreespeed();
		double linkLength = this.network.getLinks().get(linkId).getLength();
		double freeFlowTravelTime = linkLength / freeFlowSpeed;
		
		double timeSpentInCongestion;
		if(travelTime <= freeFlowTravelTime){
			timeSpentInCongestion = 0.0;
		} else {
			timeSpentInCongestion = travelTime - freeFlowTravelTime;
		}
		
		for(int i = 0; i < noOfTimeBins; i++){
			if(linkLeaveTime > i * timeBinSize && linkLeaveTime <= (i + 1) * timeBinSize){
				endOfTimeInterval = (i + 1) * timeBinSize;
				Map<Id<Link>, Double> linkId2CongestionTime = new HashMap<>();

				if(time2LinkIdAndCongestionTime.get(endOfTimeInterval) == null){
					linkId2CongestionTime.put(linkId, timeSpentInCongestion);
				} else {
					linkId2CongestionTime = time2LinkIdAndCongestionTime.get(endOfTimeInterval);
					if(linkId2CongestionTime.get(linkId) == null){
						linkId2CongestionTime.put(linkId, timeSpentInCongestion);
					} else {
						double delaySoFar = linkId2CongestionTime.get(linkId);
						double newValue = delaySoFar + timeSpentInCongestion;
						linkId2CongestionTime.put(linkId, newValue);
					}
				}
				time2LinkIdAndCongestionTime.put(endOfTimeInterval, linkId2CongestionTime);
			}
		}
		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if(!event.getLegMode().equals(TransportMode.car)){ // link travel time calculation not neccessary for other modes
			return;
		}
		Tuple<Id<Link>, Double> linkId2Time = new Tuple<>(event.getLinkId(), event.getTime());
		this.arrivalPerPersonId.put(event.getPersonId(), linkId2Time);
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(!event.getLegMode().equals(TransportMode.car)){ // link travel time calculation not neccessary for other modes
			return;
		}
		Tuple<Id<Link>, Double> linkId2Time = new Tuple<>(event.getLinkId(), event.getTime());
		this.departurePerPersonId.put(event.getPersonId(), linkId2Time);
	}
	
	public Map<Double, Map<Id<Link>, Integer>> getDemandPerLinkAndTimeInterval() {
		return this.time2LinkIdAndDemand;
	}

	public Map<Double, Map<Id<Link>, Double>> getCongestionTimePerLinkAndTimeInterval() {
		return this.time2LinkIdAndCongestionTime;
	}

}