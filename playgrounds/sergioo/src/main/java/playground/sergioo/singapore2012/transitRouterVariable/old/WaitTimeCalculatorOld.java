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

package playground.sergioo.singapore2012.transitRouterVariable.old;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.sergioo.singapore2012.transitRouterVariable.waitTimes.WaitTimeData;
import playground.sergioo.singapore2012.transitRouterVariable.waitTimes.WaitTimeDataArray;

/**
 * Save waiting times of agents while mobsim is running
 * 
 * @author sergioo
 */

public class WaitTimeCalculatorOld implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler, PersonStuckEventHandler {
	
	//Constants
	private final static String SEPARATOR = "===";

	//Attributes
	private final double timeSlot;
	private final Map<String, WaitTimeData> waitTimes = new ConcurrentHashMap<String, WaitTimeData>(26883);
	private final Map<String, double[]> scheduledWaitTimes = new ConcurrentHashMap<String, double[]>(26883);
	private final Map<Id<Person>, Double> agentsWaitingData = new ConcurrentHashMap<Id<Person>, Double>();
	private final Map<Id<Person>, Integer> agentsCurrentLeg = new ConcurrentHashMap<Id<Person>, Integer>();
	private final Population population;

	//Constructors
	public WaitTimeCalculatorOld(final Population population, final TransitSchedule transitSchedule, final int timeSlot, final int totalTime) {
		this.population = population;
		this.timeSlot = timeSlot;
		for(TransitLine line:transitSchedule.getTransitLines().values())
			for(TransitRoute route:line.getRoutes().values()) {
				double[] sortedDepartures = new double[route.getDepartures().size()];
				int d=0;
				for(Departure departure:route.getDepartures().values())
					sortedDepartures[d++] = departure.getDepartureTime();
				Arrays.sort(sortedDepartures);
				for(TransitRouteStop stop:route.getStops()) {
					String key = line.getId()+")["+route.getId()+"]"+stop.getStopFacility().getId();
					waitTimes.put(key, new WaitTimeDataArray(totalTime/timeSlot+1));
					double[] cacheWaitTimes = new double[totalTime/timeSlot+1];
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
					scheduledWaitTimes.put(key, cacheWaitTimes);
				}
			}
	}

	//Methods
	public WaitTimeOld getWaitTimes() {
		return new WaitTimeOld() {
			
			@Override
			public double getRouteStopWaitTime(Id<TransitLine> lineId, Id<TransitRoute> routeId, Id<TransitStopFacility> stopId, double time) {
				return WaitTimeCalculatorOld.this.getRouteStopWaitTime(lineId, routeId, stopId, time);
			}
		
		};
	}
	private double getRouteStopWaitTime(Id<TransitLine> lineId, Id<TransitRoute> routeId, Id<TransitStopFacility> stopId, double time) {
		String key = lineId.toString()+")["+routeId.toString()+"]"+stopId.toString();
		WaitTimeData waitTimeData = waitTimes.get(key);
		if(waitTimeData.getNumData((int) (time/timeSlot))==0) {
			double[] waitTimes = scheduledWaitTimes.get(key);
			return waitTimes[(int) (time/timeSlot)<waitTimes.length?(int) (time/timeSlot):(waitTimes.length-1)];
		}
		else
			return waitTimeData.getWaitTime((int) (time/timeSlot));
	}
	@Override
	public void reset(int iteration) {
		for(WaitTimeData waitTimeData:waitTimes.values())
			waitTimeData.resetWaitTimes();
		agentsWaitingData.clear();
		agentsCurrentLeg.clear();
	}
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Integer currentLeg = agentsCurrentLeg.get(event.getPersonId());
		if(currentLeg == null)
			currentLeg = 0;
		else
			currentLeg++;
		agentsCurrentLeg.put(event.getPersonId(), currentLeg);
		if(event.getLegMode().equals("pt") && agentsWaitingData.get(event.getPersonId())==null)
			agentsWaitingData.put(event.getPersonId(), event.getTime());
		else if(agentsWaitingData.get(event.getPersonId())!=null)
			new RuntimeException("Departing with old data");
	}
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Double startWaitingTime = agentsWaitingData.get(event.getPersonId());
		if(startWaitingTime!=null) {
			int legs = 0, currentLeg = agentsCurrentLeg.get(event.getPersonId());
			PLAN_ELEMENTS:
			for(PlanElement planElement:population.getPersons().get(event.getPersonId()).getSelectedPlan().getPlanElements())
				if(planElement instanceof Leg) {
					if(currentLeg==legs) {
						String[] leg = ((Leg)planElement).getRoute().getRouteDescription().split(SEPARATOR);
						WaitTimeData data = waitTimes.get(leg[2]+")["+leg[3]+"]"+leg[1]);
						if(data!=null)
							data.addWaitTime((int) (startWaitingTime/timeSlot), event.getTime()-startWaitingTime);
						agentsWaitingData.remove(event.getPersonId());
						break PLAN_ELEMENTS;
					}
					else
						legs++;
				}
		}
	}
	
	/*@Override
	public void handleEvent(AdditionalTeleportationDepartureEvent event) {
		Double startWaitingTime = agentsWaitingData.get(event.getAgentId());
		if(startWaitingTime!=null) {
			int legs = 0, currentLeg = agentsCurrentLeg.get(event.getAgentId());
			PLAN_ELEMENTS:
			for(PlanElement planElement:population.getPersons().get(event.getAgentId()).getSelectedPlan().getPlanElements())
				if(planElement instanceof Leg) {
					if(currentLeg==legs) {
						String[] leg = ((GenericRoute)((Leg)planElement).getRoute()).getRouteDescription().split(SEPARATOR);
						String key = "("+leg[2]+")["+leg[3]+"]"+leg[1];
						waitTimes.get(key).addWaitTime((int) (startWaitingTime/timeSlot), event.getTime()-startWaitingTime);
						agentsWaitingData.remove(event.getAgentId());
						break PLAN_ELEMENTS;
					}
					else
						legs++;
				}
		}
	}*/
	
	@Override
	public void handleEvent(PersonStuckEvent event) {
		Double startWaitingTime = agentsWaitingData.get(event.getPersonId());
		if(startWaitingTime!=null) {
			int legs = 0, currentLeg = agentsCurrentLeg.get(event.getPersonId());
			PLAN_ELEMENTS:
			for(PlanElement planElement:population.getPersons().get(event.getPersonId()).getSelectedPlan().getPlanElements())
				if(planElement instanceof Leg) {
					if(currentLeg==legs) {
						String[] leg = (((Leg)planElement).getRoute()).getRouteDescription().split(SEPARATOR);
						WaitTimeData data = waitTimes.get(leg[2]+")["+leg[3]+"]"+leg[1]);
						if(data!=null)
							data.addWaitTime((int) (startWaitingTime/timeSlot), event.getTime()-startWaitingTime);
						agentsWaitingData.remove(event.getPersonId());
						break PLAN_ELEMENTS;
					}
					else
						legs++;
				}
		}
	}

}
