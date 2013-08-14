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
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;

import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.replanning.AbstractPStrategyModule;
import playground.andreas.P2.replanning.PPlan;
import playground.andreas.P2.replanning.modules.ReduceTimeServedRFare;

/**
 * 
 * Limits demand to periods with demand.
 * 
 * @author aneumann
 *
 * @deprecated Use {@linkplain ReduceTimeServedRFare} instead
 *
 */
public class TimeReduceDemand extends AbstractPStrategyModule implements TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{
	
	private final static Logger log = Logger.getLogger(TimeReduceDemand.class);
	
	public static final String STRATEGY_NAME = "TimeReduceDemand";
	
	private final int timeBinSize;
	private LinkedHashMap<String, int[]> lineId2BoardingDemand = new LinkedHashMap<String, int[]>();
	private LinkedHashMap<String, int[]> lineId2AlightingDemand = new LinkedHashMap<String, int[]>();
	private LinkedHashMap<Id, Double> vehId2DepartureTime = new LinkedHashMap<Id, Double>();

	private String pIdentifier;
	
	public TimeReduceDemand(ArrayList<String> parameter) {
		super(parameter);
		if(parameter.size() != 1){
			log.error("Too many parameter. Will ignore: " + parameter);
		}
		this.timeBinSize = Integer.parseInt(parameter.get(0));
		log.info("enabled");
	}
	
	@Override
	public PPlan run(Cooperative cooperative) {
		// get start Time
		
		int[] boardingDemand = this.lineId2BoardingDemand.get(cooperative.getId().toString());
		int[] alightingDemand = this.lineId2AlightingDemand.get(cooperative.getId().toString());
		double startTime = 0.0;
		double endTime = 24 * 3600.0;
		
		if(boardingDemand == null){
			// there is no demand
			return null;
		}
		
		for (int i = 0; i < boardingDemand.length; i++) {
			if(boardingDemand[i] == 0){
				startTime = this.timeBinSize * (i + 1);
			} else {
				break;
			}			
		}
		
		for (int i = alightingDemand.length - 1; i > 0; i--) {
			if(alightingDemand[i] == 0){
				endTime = this.timeBinSize * i;
			} else {
				break;
			}			
		}
				
		// create new plan
		PPlan newPlan = new PPlan(cooperative.getNewRouteId(), this.getName());
		newPlan.setNVehicles(1);
		newPlan.setStopsToBeServed(cooperative.getBestPlan().getStopsToBeServed());
		newPlan.setStartTime(startTime);
		newPlan.setEndTime(endTime);
		
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan));
		
		return newPlan;
	}

	@Override
	public String getName() {
		return TimeReduceDemand.STRATEGY_NAME;
	}

	@Override
	public void reset(int iteration) {
		this.lineId2BoardingDemand = new LinkedHashMap<String, int[]>();
		this.lineId2AlightingDemand = new LinkedHashMap<String, int[]>();
		this.vehId2DepartureTime = new LinkedHashMap<Id, Double>();
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(event.getVehicleId().toString().startsWith(this.pIdentifier)){
			if(!event.getPersonId().toString().contains(this.pIdentifier)){
				String lineId = event.getVehicleId().toString().split("-")[0];

				if(this.lineId2BoardingDemand.get(lineId) == null){
					this.lineId2BoardingDemand.put(lineId, new int[24*3600/this.timeBinSize+1]);
				}

				int slot = ((int) this.vehId2DepartureTime.get(event.getVehicleId()).doubleValue() / this.timeBinSize);
				if(slot < this.lineId2BoardingDemand.get(lineId).length - 1){
					this.lineId2BoardingDemand.get(lineId)[slot]++;
				} else {
					this.lineId2BoardingDemand.get(lineId)[this.lineId2BoardingDemand.get(lineId).length - 1]++;
				}
			}
		}
	}

	public void setPIdentifier(String pIdentifier) {
		this.pIdentifier = pIdentifier;		
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(event.getVehicleId().toString().startsWith(this.pIdentifier)){
			if(!event.getPersonId().toString().contains(this.pIdentifier)){
				String lineId = event.getVehicleId().toString().split("-")[0];

				if(this.lineId2AlightingDemand.get(lineId) == null){
					this.lineId2AlightingDemand.put(lineId, new int[24*3600/this.timeBinSize+1]);
				}

//				int slot = ((int) event.getTime() / this.timeBinSize);
				int slot = ((int) this.vehId2DepartureTime.get(event.getVehicleId()).doubleValue() / this.timeBinSize);
				if(slot < this.lineId2AlightingDemand.get(lineId).length - 1){
					this.lineId2AlightingDemand.get(lineId)[slot]++;
				} else {
					this.lineId2AlightingDemand.get(lineId)[this.lineId2AlightingDemand.get(lineId).length - 1]++;
				}
			}
		}
	}	

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.vehId2DepartureTime.put(event.getVehicleId(), new Double(event.getTime()));		
	}

	

}
