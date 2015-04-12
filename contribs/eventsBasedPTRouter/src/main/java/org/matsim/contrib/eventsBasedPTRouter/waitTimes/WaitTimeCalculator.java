/* *********************************************************************** *
 * project: org.matsim.*
 * WaitTimeCalculator.java
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

package org.matsim.contrib.eventsBasedPTRouter.waitTimes;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Save waiting times of agents while mobsim is running
 * 
 * @author sergioo
 */

public class WaitTimeCalculator implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler, TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler {

	//Attributes
	private final double timeSlot;
	private final Map<Tuple<Id, Id>, Map<Id, WaitTimeData>> waitTimes = new HashMap<Tuple<Id, Id>, Map<Id, WaitTimeData>>(1000);
	private final Map<Tuple<Id, Id>, Map<Id, double[]>> scheduledWaitTimes = new HashMap<Tuple<Id, Id>, Map<Id, double[]>>(1000);
	private final Map<Id, Double> agentsWaitingData = new HashMap<Id, Double>();
	private Map<Id, Tuple<Id, Id>> linesRoutesOfVehicle = new HashMap<Id, Tuple<Id, Id>>();
	private Map<Id, Id> stopOfVehicle = new HashMap<Id, Id>();
	
	//Constructors
	public WaitTimeCalculator(final TransitSchedule transitSchedule, final Config config) {
		this(transitSchedule, config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config.qsim().getEndTime()-config.qsim().getStartTime()));
	}
	public WaitTimeCalculator(final TransitSchedule transitSchedule, final int timeSlot, final int totalTime) {
		this.timeSlot = timeSlot;
		for(TransitLine line:transitSchedule.getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values()) {
				double[] sortedDepartures = new double[route.getDepartures().size()];
				int d=0;
				for(Departure departure:route.getDepartures().values())
					sortedDepartures[d++] = departure.getDepartureTime();
				Arrays.sort(sortedDepartures);
				Map<Id, WaitTimeData> stopsMap = new HashMap<Id, WaitTimeData>(100);
				Map<Id, double[]> stopsScheduledMap = new HashMap<Id, double[]>(100);
				for(TransitRouteStop stop:route.getStops()) {
					stopsMap.put(stop.getStopFacility().getId(), new WaitTimeDataArray((int) (totalTime/timeSlot)+1));
					double[] cacheWaitTimes = new double[(int) (totalTime/timeSlot)+1];
					for(int i=0; i<cacheWaitTimes.length; i++) {
						double endTime = timeSlot*(i+1);
						if(endTime>24*3600)
							endTime-=24*3600;
						cacheWaitTimes[i] = Time.UNDEFINED_TIME;
						SORTED_DEPARTURES:
						for(double departure:sortedDepartures) {
							double arrivalTime = departure+(stop.getArrivalOffset()!=Time.UNDEFINED_TIME?stop.getArrivalOffset():stop.getDepartureOffset()); 
							if(arrivalTime>=endTime) {
								cacheWaitTimes[i] = arrivalTime-endTime;
								break SORTED_DEPARTURES;
							}
						}
						if(cacheWaitTimes[i]==Time.UNDEFINED_TIME)
							cacheWaitTimes[i] = sortedDepartures[0]+24*3600+(stop.getArrivalOffset()!=Time.UNDEFINED_TIME?stop.getArrivalOffset():stop.getDepartureOffset())-endTime;
					}
					stopsScheduledMap.put(stop.getStopFacility().getId(), cacheWaitTimes);
				}
				Tuple<Id, Id> key = new Tuple<Id, Id>(line.getId(), route.getId());
				waitTimes.put(key, stopsMap);
				scheduledWaitTimes.put(key, stopsScheduledMap);
			}
	}

	//Methods
	public WaitTime getWaitTimes() {
		return new WaitTime() {
			@Override
			public double getRouteStopWaitTime(Id lineId, Id routeId, Id stopId, double time) {
				return WaitTimeCalculator.this.getRouteStopWaitTime(lineId, routeId, stopId, time);
			}
		};
	}
	private double getRouteStopWaitTime(Id lineId, Id routeId, Id stopId, double time) {
		Tuple<Id, Id> key = new Tuple<Id, Id>(lineId, routeId);
		WaitTimeData waitTimeData = waitTimes.get(key).get(stopId);
		if(waitTimeData.getNumData((int) (time/timeSlot))==0) {
			double[] waitTimes = scheduledWaitTimes.get(key).get(stopId);
			return waitTimes[(int) (time/timeSlot)<waitTimes.length?(int) (time/timeSlot):(waitTimes.length-1)];
		}
		else
			return waitTimeData.getWaitTime((int) (time/timeSlot));
	}
	@Override
	public void reset(int iteration) {
		for(Map<Id, WaitTimeData> routeData:waitTimes.values())
			for(WaitTimeData waitTimeData:routeData.values())
				waitTimeData.resetWaitTimes();
		agentsWaitingData.clear();
		linesRoutesOfVehicle.clear();
		stopOfVehicle.clear();
	}
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(event.getLegMode().equals("pt") && agentsWaitingData.get(event.getPersonId())==null)
			agentsWaitingData.put(event.getPersonId(), event.getTime());
		else if(agentsWaitingData.get(event.getPersonId())!=null)
			new RuntimeException("Departing with old data");
	}
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Double startWaitingTime = agentsWaitingData.get(event.getPersonId());
		if(startWaitingTime!=null) {
			Tuple<Id, Id> lineRoute = linesRoutesOfVehicle.get(event.getVehicleId());
			WaitTimeData data = waitTimes.get(lineRoute).get(stopOfVehicle.get(event.getVehicleId()));
			data.addWaitTime((int) (startWaitingTime/timeSlot), event.getTime()-startWaitingTime);
			agentsWaitingData.remove(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if(linesRoutesOfVehicle.get(event.getVehicleId())!=null)
			stopOfVehicle.put(event.getVehicleId(), event.getFacilityId());
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		linesRoutesOfVehicle.put(event.getVehicleId(), new Tuple<Id, Id>(event.getTransitLineId(), event.getTransitRouteId()));
	}

}
