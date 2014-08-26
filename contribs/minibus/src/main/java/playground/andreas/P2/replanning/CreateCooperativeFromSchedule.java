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

package playground.andreas.P2.replanning;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.operator.Cooperative;
import playground.andreas.P2.operator.CooperativeFactory;
import playground.andreas.P2.routeProvider.PRouteProvider;

/**
 * 
 * Creates cooperatives from a given transit schedule
 * 
 * @author aneumann
 *
 */
public class CreateCooperativeFromSchedule implements PStrategy{
	
	private final static Logger log = Logger.getLogger(CreateCooperativeFromSchedule.class);
	public static final String STRATEGY_NAME = "CreateCooperativeFromSchedule";

	private CooperativeFactory cooperativeFactory;
	private PRouteProvider routeProvider;
	private PConfigGroup pConfig;
	private LinkedHashMap<String, TransitStopFacility> originalStops;
	
	private LinkedHashMap<Id, PPlan> lineId2PlanMap = new LinkedHashMap<Id, PPlan>();

	public CreateCooperativeFromSchedule(CooperativeFactory cooperativeFactory, PRouteProvider routeProvider, PConfigGroup pConfig, TransitSchedule originalSchedule) {
		this.cooperativeFactory = cooperativeFactory;
		this.routeProvider = routeProvider;
		this.pConfig = pConfig;
		
		this.originalStops = new LinkedHashMap<String, TransitStopFacility>();
		for (TransitStopFacility stop : originalSchedule.getFacilities().values()) {
			this.originalStops.put(stop.getId().toString(), stop);
		}
	}

	public LinkedList<Cooperative> run() {
		LinkedList<Cooperative> coopsToReturn = new LinkedList<Cooperative>();
		
		if (pConfig.getTransitScheduleToStartWith() != null) {
			TransitSchedule tS = readTransitSchedule(pConfig.getTransitScheduleToStartWith());

			for (Entry<Id, TransitLine> lineEntry : tS.getTransitLines().entrySet()) {
				Cooperative coop = this.cooperativeFactory.createNewCooperative(new IdImpl(this.pConfig.getPIdentifier() + lineEntry.getKey()));

				PPlan plan = createPlan(lineEntry.getValue());
				this.lineId2PlanMap.put(coop.getId(), plan);			

				double initialBudget = this.pConfig.getInitialBudget() % pConfig.getPricePerVehicleBought();			
				coop.init(this.routeProvider, this, 0, initialBudget);
				coopsToReturn.add(coop);
			}
		}
		
		this.lineId2PlanMap = null;
		log.info("Returning " + coopsToReturn.size() + " cooperative created from transit schedule file " + pConfig.getTransitScheduleToStartWith());
		return coopsToReturn;
	}

	private PPlan createPlan(TransitLine line) {
		Id id = new IdImpl(0);
		TransitRoute longestRouteH = null;
		TransitRoute longestRouteR = null;
		
		double startTime = Double.MAX_VALUE;
		double endTime = Double.MIN_VALUE;
		Set<Id> vehicleIds = new TreeSet<Id>();
		
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
		
		ArrayList<TransitStopFacility> stopsToBeServed = new ArrayList<TransitStopFacility>();
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
		
		PPlan plan = new PPlan(id, this.getName());
		plan.setStopsToBeServed(stopsToBeServed);
		plan.setStartTime(startTime);
		plan.setEndTime(endTime);
		plan.setNVehicles(nVehicles);
		return plan;
	}


	private TransitSchedule readTransitSchedule(String transitScheduleToStartWith) {
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sc.getConfig().scenario().setUseTransit(true);
		sc.getConfig().scenario().setUseVehicles(true);
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
	public PPlan run(Cooperative cooperative) {
		PPlan newPlan = this.lineId2PlanMap.get(cooperative.getId());		
		newPlan.setLine(cooperative.getRouteProvider().createTransitLine(cooperative.getId(), newPlan));
		return newPlan;
	}

	@Override
	public String getName() {
		return CreateCooperativeFromSchedule.STRATEGY_NAME;
	}
}