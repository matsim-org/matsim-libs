/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.andreas.P2.replanning.modules.deprecated;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.replanning.AbstractPStrategyModule;
import playground.andreas.P2.replanning.PPlan;

/**
 * 
 * Limits demand to stops with demand. Use {@link ReduceStopsToBeServed} or {@link ReduceStopsToBeServedR}. No Tests for this one.
 * 
 * @author aneumann
 *
 */
@Deprecated
public class StopReduceDemand extends AbstractPStrategyModule implements VehicleArrivesAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{
	
	private final static Logger log = Logger.getLogger(StopReduceDemand.class);
	
	public static final String STRATEGY_NAME = "StopReduceDemand";

	private LinkedHashMap<Id, LinkedHashMap<Id, int[]>> lineId2stopId2DemandBoardingAlighting = new LinkedHashMap<Id, LinkedHashMap<Id, int[]>>();
	private LinkedHashMap<Id, Id> vehId2stopId = new LinkedHashMap<Id, Id>();

	private String pIdentifier;
	
	public StopReduceDemand(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 0){
			log.error("Too many parameter. Will ignore: " + parameter);
		}
		log.info("enabled");
	}
	
	@Override
	public PPlan run(Cooperative cooperative) {
		
		ArrayList<TransitStopFacility> stopsToBeServed = new ArrayList<TransitStopFacility>();
		LinkedHashMap<Id, int[]> stop2DemandMap = this.lineId2stopId2DemandBoardingAlighting.get(cooperative.getId());
		
		for (TransitRoute route : cooperative.getBestPlan().getLine().getRoutes().values()) {
			TransitStopFacility firstStop = null;
			for (TransitRouteStop stop : route.getStops()) {
				if(firstStop == null){
					firstStop = stop.getStopFacility();
				}
				if(stop2DemandMap.get(stop.getStopFacility().getId()) == null){
					// no entry - no demand
					continue;
				}
				int demandForThatStop = stop2DemandMap.get(stop.getStopFacility().getId())[0] + stop2DemandMap.get(stop.getStopFacility().getId())[1];
				if (demandForThatStop > 0) {
					// keep it
					stopsToBeServed.add(stop.getStopFacility());
				}
			}
			if (stopsToBeServed.get(stopsToBeServed.size() - 1).getId() == firstStop.getId()) {
				stopsToBeServed.remove(stopsToBeServed.size() - 1);
			}
		}
				
		// profitable route, change startTime
		PPlan newPlan = new PPlan(cooperative.getNewRouteId(), this.getName());
		newPlan.setNVehicles(1);
		newPlan.setStopsToBeServed(stopsToBeServed);
		newPlan.setStartTime(cooperative.getBestPlan().getStartTime());
		newPlan.setEndTime(cooperative.getBestPlan().getEndTime());
		
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan));
		
		return newPlan;
	}

	@Override
	public String getName() {
		return StopReduceDemand.STRATEGY_NAME;
	}

	@Override
	public void reset(int iteration) {
		this.vehId2stopId = new LinkedHashMap<Id, Id>();
		this.lineId2stopId2DemandBoardingAlighting = new LinkedHashMap<Id, LinkedHashMap<Id,int[]>>();
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if(event.getVehicleId().toString().startsWith(this.pIdentifier)){
			this.vehId2stopId.put(event.getVehicleId(), event.getFacilityId());
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(event.getVehicleId().toString().startsWith(this.pIdentifier)){
			if(!event.getPersonId().toString().contains(this.pIdentifier)){
				Id lineId = new IdImpl(event.getVehicleId().toString().split("-")[0]);
				Id vehId = event.getVehicleId();

				if(this.lineId2stopId2DemandBoardingAlighting.get(lineId) == null){
					this.lineId2stopId2DemandBoardingAlighting.put(lineId, new LinkedHashMap<Id, int[]>());
				}
				
				if(this.lineId2stopId2DemandBoardingAlighting.get(lineId).get(this.vehId2stopId.get(vehId)) == null){
					this.lineId2stopId2DemandBoardingAlighting.get(lineId).put(this.vehId2stopId.get(vehId), new int[2]);
				}

				this.lineId2stopId2DemandBoardingAlighting.get(lineId).get(this.vehId2stopId.get(vehId))[0]++;
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(event.getVehicleId().toString().startsWith(this.pIdentifier)){
			if(!event.getPersonId().toString().contains(this.pIdentifier)){
				Id lineId = new IdImpl(event.getVehicleId().toString().split("-")[0]);
				Id vehId = event.getVehicleId();

				if(this.lineId2stopId2DemandBoardingAlighting.get(lineId) == null){
					this.lineId2stopId2DemandBoardingAlighting.put(lineId, new LinkedHashMap<Id, int[]>());
				}
				
				if(this.lineId2stopId2DemandBoardingAlighting.get(lineId).get(this.vehId2stopId.get(vehId)) == null){
					this.lineId2stopId2DemandBoardingAlighting.get(lineId).put(this.vehId2stopId.get(vehId), new int[2]);
				}

				this.lineId2stopId2DemandBoardingAlighting.get(lineId).get(this.vehId2stopId.get(vehId))[1]++;
			}
		}		
	}

	public void setPIdentifier(String pIdentifier) {
		this.pIdentifier = pIdentifier;		
	}
}