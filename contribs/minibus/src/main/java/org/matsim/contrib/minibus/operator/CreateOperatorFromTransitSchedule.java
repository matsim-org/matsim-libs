/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.operator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConstants;
import org.matsim.contrib.minibus.replanning.PStrategy;
import org.matsim.contrib.minibus.routeProvider.PRouteProvider;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.Map.Entry;

/**
 * 
 * Creates operators from a given transit schedule
 * 
 * @author aneumann
 *
 */
public final class CreateOperatorFromTransitSchedule implements PStrategy {
	
	private final static Logger log = Logger.getLogger(CreateOperatorFromTransitSchedule.class);
	private static final String STRATEGY_NAME = "CreateOperatorFromSchedule";

	private final OperatorFactory operatorFactory;
	private final PRouteProvider routeProvider;
	private final PConfigGroup pConfig;
	private final LinkedHashMap<String, TransitStopFacility> originalStops;
	
	private LinkedHashMap<Id<Operator>, PPlan> operatorId2PlanMap = new LinkedHashMap<>();

	public CreateOperatorFromTransitSchedule(OperatorFactory operatorFactory, PRouteProvider routeProvider, PConfigGroup pConfig, TransitSchedule originalSchedule) {
		this.operatorFactory = operatorFactory;
		this.routeProvider = routeProvider;
		this.pConfig = pConfig;
		
		this.originalStops = new LinkedHashMap<>();
		for (TransitStopFacility stop : originalSchedule.getFacilities().values()) {
			this.originalStops.put(stop.getId().toString(), stop);
		}
	}

	public LinkedList<Operator> run() {
		LinkedList<Operator> operatorsToReturn = new LinkedList<>();
		
		if (pConfig.getTransitScheduleToStartWith() != null) {
			TransitSchedule tS = readTransitSchedule(pConfig.getTransitScheduleToStartWith());

			for (Entry<Id<TransitLine>, TransitLine> lineEntry : tS.getTransitLines().entrySet()) {
				Operator operator = this.operatorFactory.createNewOperator(Id.create(this.pConfig.getPIdentifier() + lineEntry.getKey(), Operator.class));

				PPlan plan = createPlan(lineEntry.getValue());
				this.operatorId2PlanMap.put(operator.getId(), plan);			

				double initialBudget = this.pConfig.getInitialBudget() % pConfig.getPricePerVehicleBought();			
				operator.init(this.routeProvider, this, 0, initialBudget);
				operatorsToReturn.add(operator);
			}
		}
		
		this.operatorId2PlanMap = null;
		log.info("Returning " + operatorsToReturn.size() + " operators created from transit schedule file " + pConfig.getTransitScheduleToStartWith());
		return operatorsToReturn;
	}

	private PPlan createPlan(TransitLine line) {
		Id<PPlan> id = Id.create(0, PPlan.class);
		TransitRoute longestRouteH = null;
		TransitRoute longestRouteR = null;
		
		double startTime = Double.MAX_VALUE;
		double endTime = Double.MIN_VALUE;
		Set<Id<Vehicle>> vehicleIds = new TreeSet<>();
		
		for (TransitRoute route : line.getRoutes().values()) {
			if(route.getId().toString().endsWith(".H")){
				if (longestRouteH == null) {
					longestRouteH = route;
				} else if (longestRouteH.getStops().size() < route.getStops().size()) {
					longestRouteH = route;
				}				
			} else if (route.getId().toString().endsWith(".R")) {
				if (longestRouteR == null) {
					longestRouteR = route;
				} else if (longestRouteR.getStops().size() < route.getStops().size()) {
					longestRouteR = route;
				}
			}			
			
			for (Departure departure : route.getDepartures().values()) {
				if (startTime > departure.getDepartureTime()) {
					startTime = departure.getDepartureTime();
				}
				
				if (endTime < departure.getDepartureTime()) {
					endTime = departure.getDepartureTime();
				}
				
				vehicleIds.add(departure.getVehicleId());
			}
		}
		
		// limit parameters to reasonable ones		
		startTime = Math.max(startTime, 0.0);
		endTime = Math.min(endTime, 24 * 3600.0);
		
		// current planned headway in case number of vehicles is not changed
		double departureOffset = 0.0;
		if (longestRouteH != null) {
			departureOffset += longestRouteH.getStops().get(longestRouteH.getStops().size() - 1).getDepartureOffset();
		}
		
		if (longestRouteR != null) {
			departureOffset += longestRouteR.getStops().get(longestRouteR.getStops().size() - 1).getDepartureOffset();
		}
		
		double headway = departureOffset / vehicleIds.size(); 
		// set headway to a minimum of 3min
		headway = Math.max(3*60, headway);
		// resulting number of vehicles with the new headway
		int nVehicles = (int) (departureOffset / headway);
		
		ArrayList<TransitStopFacility> stopsToBeServed = new ArrayList<>();
		if (longestRouteH != null) {
			for (TransitRouteStop routeStop : longestRouteH.getStops()) {
				if (this.checkStopInServiceArea(routeStop.getStopFacility(), this.pConfig)) {
					TransitStopFacility originalStopFacility = this.originalStops.get(routeStop.getStopFacility().getId().toString());
					stopsToBeServed.add(originalStopFacility);
				}
			}
		}
		if (longestRouteR != null) {
			for (TransitRouteStop routeStop : longestRouteR.getStops()) {
				if (this.checkStopInServiceArea(routeStop.getStopFacility(), this.pConfig)) {
					TransitStopFacility originalStopFacility = this.originalStops.get(routeStop.getStopFacility().getId().toString());
					stopsToBeServed.add(originalStopFacility);
				}
			}
		}
		
		PPlan plan = new PPlan(id, this.getStrategyName(), PConstants.founderPlanId);
		plan.setStopsToBeServed(stopsToBeServed);
		plan.setStartTime(startTime);
		plan.setEndTime(endTime);
		plan.setNVehicles(nVehicles);
		return plan;
	}


	private TransitSchedule readTransitSchedule(String transitScheduleToStartWith) {
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().transit().setUseTransit(true);
		log.info("Reading " + transitScheduleToStartWith);
		new TransitScheduleReaderV1(sc).readFile(transitScheduleToStartWith);
		return sc.getTransitSchedule();
	}

	private boolean checkStopInServiceArea(TransitStopFacility stop, PConfigGroup pC){
		boolean isInServiceArea = true;
		
		if(stop.getCoord().getX() < pC.getMinX()){isInServiceArea = false;}
		if(stop.getCoord().getX() > pC.getMaxX()){isInServiceArea = false;}
		
		if(stop.getCoord().getY() < pC.getMinY()){isInServiceArea = false;}
		if(stop.getCoord().getY() > pC.getMaxY()){isInServiceArea = false;}
		
		return isInServiceArea;
	}


	@Override
	public PPlan run(Operator operator) {
		PPlan newPlan = this.operatorId2PlanMap.get(operator.getId());		
		newPlan.setLine(operator.getRouteProvider().createTransitLineFromOperatorPlan(operator.getId(), newPlan));
		return newPlan;
	}

	@Override
	public String getStrategyName() {
		return CreateOperatorFromTransitSchedule.STRATEGY_NAME;
	}
}