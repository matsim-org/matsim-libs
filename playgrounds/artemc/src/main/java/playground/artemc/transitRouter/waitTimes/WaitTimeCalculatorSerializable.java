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

package playground.artemc.transitRouter.waitTimes;

import org.apache.log4j.Logger;
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

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Save waiting times of agents while mobsim is running
 * 
 * @author sergioo
 */

public class WaitTimeCalculatorSerializable implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler, TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler, Serializable {

	private static int scheduleCalls;
	private static int waitTimeCalls;
	//Attributes
	private final double timeSlot;
	private final Map<Tuple<String, String>, Map<String, WaitTimeData>> waitTimes = new HashMap<Tuple<String, String>, Map<String, WaitTimeData>>(1000);
	private final Map<Tuple<String, String>, Map<String, double[]>> scheduledWaitTimes = new HashMap<Tuple<String, String>, Map<String, double[]>>(1000);
	private final Map<String, Double> agentsWaitingData = new HashMap<String, Double>();
	private Map<String, Tuple<String, String>> linesRoutesOfVehicle = new HashMap<String, Tuple<String, String>>();
	private Map<String, String> stopOfVehicle = new HashMap<String, String>();
	public static void printCallStatisticsAndReset(){
		Logger logger = Logger.getLogger(WaitTimeCalculatorSerializable.class);
		logger.warn("scheduled wait time calls vs unscheduled: " + scheduleCalls + " : " + waitTimeCalls);
		scheduleCalls=0;
		waitTimeCalls=0;
	}
	//Constructors
	public WaitTimeCalculatorSerializable(final TransitSchedule transitSchedule, final Config config) {
		this(transitSchedule, config.travelTimeCalculator().getTraveltimeBinSize(), (int) (config.qsim().getEndTime()-config.qsim().getStartTime()));
	}
	public WaitTimeCalculatorSerializable(final TransitSchedule transitSchedule, final int timeSlot, final int totalTime) {
		this.timeSlot = timeSlot;
		for(TransitLine line:transitSchedule.getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values()) {
				double[] sortedDepartures = new double[route.getDepartures().size()];
				int d=0;
				for(Departure departure:route.getDepartures().values())
					sortedDepartures[d++] = departure.getDepartureTime();
				Arrays.sort(sortedDepartures);
				Map<String, WaitTimeData> stopsMap = new HashMap<String, WaitTimeData>(100);
				Map<String, double[]> stopsScheduledMap = new HashMap<String, double[]>(100);
				for(TransitRouteStop stop:route.getStops()) {
					stopsMap.put(stop.getStopFacility().getId().toString(), new WaitTimeDataArray((int) (totalTime/timeSlot)+1));
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
					stopsScheduledMap.put(stop.getStopFacility().getId().toString(), cacheWaitTimes);
				}
				Tuple<String, String> key = new Tuple<String, String>(line.getId().toString(), route.getId().toString());
				waitTimes.put(key, stopsMap);
				scheduledWaitTimes.put(key, stopsScheduledMap);
			}
	}

	//Methods
	public WaitTime getWaitTimes() {
		return new WaitTime() {
			@Override
			public double getRouteStopWaitTime(Id lineId, Id routeId, Id stopId, double time) {
				return WaitTimeCalculatorSerializable.this.getRouteStopWaitTime(lineId, routeId, stopId, time);
			}
		};
	}

	private double getRouteStopWaitTime(Id lineId, Id routeId, Id stopId, double time) {
		Tuple<String, String> key = new Tuple<String, String>(lineId.toString(), routeId.toString());
		waitTimeCalls++;
		WaitTimeData waitTimeData = waitTimes.get(key).get(stopId.toString());
		if(waitTimeData.getNumData((int) (time/timeSlot))==0) {
			scheduleCalls++;
			double[] waitTimes = scheduledWaitTimes.get(key).get(stopId.toString());
			return waitTimes[(int) (time/timeSlot)<waitTimes.length?(int) (time/timeSlot):(waitTimes.length-1)];
		}
		else{
			return waitTimeData.getWaitTime((int) (time/timeSlot));
		}
	}
	@Override
	public void reset(int iteration) {
		for(Map<String, WaitTimeData> routeData:waitTimes.values())
			for(WaitTimeData waitTimeData:routeData.values())
				waitTimeData.resetWaitTimes();
		agentsWaitingData.clear();
		linesRoutesOfVehicle.clear();
		stopOfVehicle.clear();
	}
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(event.getLegMode().equals("pt") && agentsWaitingData.get(event.getPersonId().toString())==null)
			agentsWaitingData.put(event.getPersonId().toString(), event.getTime());
		else if(agentsWaitingData.get(event.getPersonId().toString())!=null)
			new RuntimeException("Departing with old data");
	}
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Double startWaitingTime = agentsWaitingData.get(event.getPersonId().toString());
		if(startWaitingTime!=null) {
			Tuple<String, String> lineRoute = linesRoutesOfVehicle.get(event.getVehicleId().toString());
			WaitTimeData data = waitTimes.get(lineRoute).get(stopOfVehicle.get(event.getVehicleId().toString()));
			data.addWaitTime((int) (startWaitingTime/timeSlot), event.getTime()-startWaitingTime);
			agentsWaitingData.remove(event.getPersonId().toString());
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if(linesRoutesOfVehicle.get(event.getVehicleId().toString())!=null)
			stopOfVehicle.put(event.getVehicleId().toString(), event.getFacilityId().toString());
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		linesRoutesOfVehicle.put(event.getVehicleId().toString(), new Tuple<String, String>(event.getTransitLineId().toString(), event.getTransitRouteId().toString()));
	}

}
