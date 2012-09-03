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

package playground.andreas.P2.routeProvider;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.TransitDriverStartsEvent;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;

/**
 * @author aneumann
 */
public class TimeAwareComplexCircleScheduleProviderHandler implements TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler{

	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger(TimeAwareComplexCircleScheduleProviderHandler.class);
	
	private String pIdentifier;
	private HashMap<Id, TransitDriverStartsEvent> vehId2StartsEvent = new HashMap<Id, TransitDriverStartsEvent>();
	private HashMap<Id, ArrayList<Double>> vehId2Offset = new HashMap<Id, ArrayList<Double>>();
	private HashMap<Id, ArrayList<TinyStatsContainer>> routeId2StatsContrainerMap = new HashMap<Id, ArrayList<TinyStatsContainer>>();

	
	public TimeAwareComplexCircleScheduleProviderHandler(String pIdentifier) {
		this.pIdentifier = pIdentifier;
	}

	@Override
	public void reset(int iteration) {
		this.vehId2StartsEvent = new HashMap<Id, TransitDriverStartsEvent>();
		this.vehId2Offset = new HashMap<Id, ArrayList<Double>>();
		this.routeId2StatsContrainerMap = new HashMap<Id, ArrayList<TinyStatsContainer>>();
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if(event.getVehicleId().toString().contains(this.pIdentifier)){
			if (this.vehId2Offset.get(event.getVehicleId()) == null) {
				this.vehId2Offset.put(event.getVehicleId(), new ArrayList<Double>());				
			}
			this.vehId2Offset.get(event.getVehicleId()).add(new Double(event.getTime()));
		}
		
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if(event.getVehicleId().toString().contains(this.pIdentifier)){
			// first complete old entry
			addEntry2Stats(this.vehId2StartsEvent.get(event.getVehicleId()), this.vehId2Offset.get(event.getVehicleId()));
			// add new event
			this.vehId2StartsEvent.put(event.getVehicleId(), event);
			this.vehId2Offset.put(event.getVehicleId(), new ArrayList<Double>());
			
		}
		
	}
	
	private void addEntry2Stats(TransitDriverStartsEvent event, ArrayList<Double> offsetList){
		if (event == null || offsetList == null) {
			return;
		}
		
		if (this.routeId2StatsContrainerMap.get(event.getTransitRouteId()) == null) {
			// first entry - create new one
			ArrayList<TinyStatsContainer> statsList = new ArrayList<TinyStatsContainer>();
			for (Double offset : offsetList) {
				TinyStatsContainer statsContainer = new TinyStatsContainer();
				statsContainer.handleEntry(offset - event.getTime());
				statsList.add(statsContainer);
			}
			this.routeId2StatsContrainerMap.put(event.getTransitRouteId(), statsList);
		} else {
			// update existing one
			for (int i = 0; i < offsetList.size(); i++) {
				this.routeId2StatsContrainerMap.get(event.getTransitRouteId()).get(i).handleEntry(offsetList.get(i) - event.getTime());
			}
		}
	}
	
	private class TinyStatsContainer{
		private int numberOfEntries = 0;
		private double sumOfEntries = 0.0;
		
		void handleEntry(double entry){
			this.sumOfEntries += entry;
			this.numberOfEntries++;
		}
		
		double getMean(){
			return this.sumOfEntries / this.numberOfEntries;
		}
	}

	public double getOffsetForRouteAndStopNumber(Id routeID, int stopIndex) {
		if (this.routeId2StatsContrainerMap.get(routeID) == null) {
			return -Double.MAX_VALUE;
		}
		
		return this.routeId2StatsContrainerMap.get(routeID).get(stopIndex).getMean();
	}
}