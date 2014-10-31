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
package playground.ikaddoura.noise2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.core.utils.collections.Tuple;

/**
 * 
 * Collects each agent's performance of activities throughout the day.
 * 
 * @author lkroeger, ikaddoura
 *
 */

public class PersonActivityHandler implements ActivityEndEventHandler , ActivityStartEventHandler{

	private static final Logger log = Logger.getLogger(PersonActivityHandler.class);
	
	private Scenario scenario;
	
	private Map<Id,Map<Id,Map<Integer,Tuple<Double,Double>>>> receiverPointId2personId2actNumber2activityStartAndActivityEnd = new HashMap<Id,Map<Id,Map<Integer,Tuple<Double,Double>>>>();
	private Map<Id,Map<Integer,Map<Id,Tuple<Double, Double>>>> personId2actNumber2receiverPointId2activityStartAndActivityEnd = new HashMap<Id,Map<Integer,Map<Id,Tuple<Double, Double>>>>();
	private Map<Id,Map<Integer,String>> personId2actNumber2actType = new HashMap<Id, Map<Integer,String>>();
	private Map<Id,Integer> personId2actualActNumber = new HashMap<Id, Integer>();
	private Map<Id,Map<Double,Double>> receiverPointId2timeInterval2affectedAgentUnits = new HashMap<Id, Map<Double,Double>>();
	private Map<Id,Map<Double,Map<Id,Map<Integer,Tuple<Double,String>>>>> receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType = new HashMap<Id, Map<Double,Map<Id,Map<Integer,Tuple<Double,String>>>>>();
	private Map<Id,List<Id>> receiverPointId2ListOfHomeAgents = new HashMap<Id, List<Id>>();
				
	private NoiseSpatialInfo spatialInfo;
	
	public PersonActivityHandler (Scenario scenario , NoiseSpatialInfo spatialInfo) {
		this.scenario = scenario;
		this.spatialInfo = spatialInfo;		
	}
	
	@Override
	public void reset(int iteration) {
		this.receiverPointId2personId2actNumber2activityStartAndActivityEnd.clear();
		this.personId2actNumber2receiverPointId2activityStartAndActivityEnd.clear();
		this.personId2actNumber2actType.clear();
		this.personId2actualActNumber.clear();
		this.receiverPointId2timeInterval2affectedAgentUnits.clear();
		this.receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.clear();
		this.receiverPointId2ListOfHomeAgents.clear();
	}
	
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (!(scenario.getPopulation().getPersons().containsKey(event.getPersonId()))) {
		} else {
		
			if (!event.getActType().toString().equals("pt_interaction")) {
				Id personId = event.getPersonId();
				
				personId2actualActNumber.put(event.getPersonId(), personId2actualActNumber.get(event.getPersonId())+1);
				int actNumber = personId2actualActNumber.get(personId);
				double time = event.getTime();
				Coord coord = spatialInfo.getPersonId2listOfCoords().get(personId).get(actNumber-1);
				Id receiverPointId = spatialInfo.getActivityCoord2receiverPointId().get(coord);
				
				double startTime = time;
				double endTime = 30 * 3600;
				Tuple<Double,Double> activityStartAndActivityEnd = new Tuple<Double, Double>(startTime, endTime);
				Map<Id,Tuple<Double,Double>> receiverPointId2activityStartAndActivityEnd = new HashMap<Id, Tuple<Double,Double>>();
				receiverPointId2activityStartAndActivityEnd.put(receiverPointId, activityStartAndActivityEnd);
				Map<Integer,Map<Id,Tuple<Double,Double>>> actNumber2receiverPointId2activityStartAndActivityEnd = personId2actNumber2receiverPointId2activityStartAndActivityEnd.get(personId);
				actNumber2receiverPointId2activityStartAndActivityEnd.put(actNumber, receiverPointId2activityStartAndActivityEnd);
				personId2actNumber2receiverPointId2activityStartAndActivityEnd.put(personId, actNumber2receiverPointId2activityStartAndActivityEnd);
				
				String actType = event.getActType();
				Map <Integer,String> actNumber2actType = personId2actNumber2actType.get(personId);
				actNumber2actType.put(actNumber,actType);
				personId2actNumber2actType.put(personId,actNumber2actType);
				
				if (receiverPointId2personId2actNumber2activityStartAndActivityEnd.containsKey(receiverPointId)) {
					// already at least one activity at this receiverPoint
					if (receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).containsKey(personId)) {
						// already at least the second activity of this person at this receiverPoint
						Map<Integer,Tuple<Double,Double>> actNumber2activityStartAndActivityEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId);
						actNumber2activityStartAndActivityEnd.put(actNumber, activityStartAndActivityEnd);
						Map<Id,Map<Integer,Tuple<Double,Double>>> personId2actNumber2activityStartAndActivityEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId);
						personId2actNumber2activityStartAndActivityEnd.put(personId, actNumber2activityStartAndActivityEnd);
						receiverPointId2personId2actNumber2activityStartAndActivityEnd.put(receiverPointId, personId2actNumber2activityStartAndActivityEnd);
					} else {
						// the first activity of this person at this receiverPoint
						Map<Integer,Tuple<Double,Double>> actNumber2activityStartAndActivityEnd = new HashMap<Integer, Tuple<Double,Double>>();
						actNumber2activityStartAndActivityEnd.put(actNumber, activityStartAndActivityEnd);
						Map<Id,Map<Integer,Tuple<Double,Double>>> personId2actNumber2activityStartAndActivityEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId);
						personId2actNumber2activityStartAndActivityEnd.put(personId, actNumber2activityStartAndActivityEnd);
						receiverPointId2personId2actNumber2activityStartAndActivityEnd.put(receiverPointId, personId2actNumber2activityStartAndActivityEnd);
					}
				} else {
					// the first activity at this receiver Point
					Map<Integer,Tuple<Double,Double>> actNumber2activityStartAndActivityEnd = new HashMap<Integer, Tuple<Double,Double>>();
					actNumber2activityStartAndActivityEnd.put(actNumber, activityStartAndActivityEnd);
					Map<Id,Map<Integer,Tuple<Double,Double>>> personId2actNumber2activityStartAndActivityEnd = new HashMap<Id, Map<Integer,Tuple<Double,Double>>>();
					personId2actNumber2activityStartAndActivityEnd.put(personId, actNumber2activityStartAndActivityEnd);
					receiverPointId2personId2actNumber2activityStartAndActivityEnd.put(receiverPointId, personId2actNumber2activityStartAndActivityEnd);
				}
			}
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		
		if (!(scenario.getPopulation().getPersons().containsKey(event.getPersonId()))) {
			// probably public transport
		} else {
			
			if (!event.getActType().toString().equals("pt_interaction")) {
				Id personId = event.getPersonId();
				
				if (!(personId2actualActNumber.containsKey(personId))) {
					personId2actualActNumber.put(personId, 1);
				}
				int actNumber = personId2actualActNumber.get(personId);
				double time = event.getTime();
	
				Coord coord = spatialInfo.getPersonId2listOfCoords().get(personId).get(actNumber-1);
				Id receiverPointId = spatialInfo.getActivityCoord2receiverPointId().get(coord);
				
				if (personId2actNumber2receiverPointId2activityStartAndActivityEnd.containsKey(personId)) {
					// not the first activity
					double startTime = personId2actNumber2receiverPointId2activityStartAndActivityEnd.get(personId).get(actNumber).get(receiverPointId).getFirst();
					double EndTime = time;
					Tuple<Double,Double> activityStartAndActivityEnd = new Tuple<Double, Double>(startTime, EndTime);
					Map<Id,Tuple<Double,Double>> receiverPointId2activityStartAndActivityEnd = new HashMap<Id, Tuple<Double,Double>>();
					receiverPointId2activityStartAndActivityEnd.put(receiverPointId, activityStartAndActivityEnd);
					Map<Integer,Map<Id,Tuple<Double,Double>>> actNumber2receiverPointId2activityStartAndActivityEnd = personId2actNumber2receiverPointId2activityStartAndActivityEnd.get(personId);
					actNumber2receiverPointId2activityStartAndActivityEnd.put(actNumber, receiverPointId2activityStartAndActivityEnd);
					personId2actNumber2receiverPointId2activityStartAndActivityEnd.put(personId, actNumber2receiverPointId2activityStartAndActivityEnd);
					
				} else {
					// the first activity
					double startTime = 0.;
					double EndTime = time;
					Tuple<Double,Double> activityStartAndActivityEnd = new Tuple<Double, Double>(startTime, EndTime);
					Map<Id,Tuple<Double,Double>> receiverPointId2activityStartAndActivityEnd = new HashMap<Id, Tuple<Double,Double>>();
					receiverPointId2activityStartAndActivityEnd.put(receiverPointId, activityStartAndActivityEnd);
					Map<Integer,Map<Id,Tuple<Double,Double>>> actNumber2receiverPointId2activityStartAndActivityEnd = new HashMap<Integer, Map<Id,Tuple<Double,Double>>>();
					actNumber2receiverPointId2activityStartAndActivityEnd.put(actNumber, receiverPointId2activityStartAndActivityEnd);
					personId2actNumber2receiverPointId2activityStartAndActivityEnd.put(personId, actNumber2receiverPointId2activityStartAndActivityEnd);
	
					String actType = event.getActType();
					Map <Integer,String> actNumber2actType = new HashMap<Integer, String>();
					actNumber2actType.put(actNumber,actType);
					personId2actNumber2actType.put(personId, actNumber2actType);
					
					if (receiverPointId2ListOfHomeAgents.containsKey(receiverPointId)) {
						List <Id> listOfHomeAgents = receiverPointId2ListOfHomeAgents.get(receiverPointId);
						listOfHomeAgents.add(personId);
						receiverPointId2ListOfHomeAgents.put(receiverPointId, listOfHomeAgents);
					} else {
						List <Id> listOfHomeAgents = new ArrayList<Id>();
						listOfHomeAgents.add(personId);
						receiverPointId2ListOfHomeAgents.put(receiverPointId, listOfHomeAgents);
					}
				}
				
				if (receiverPointId2personId2actNumber2activityStartAndActivityEnd.containsKey(receiverPointId)) {
					// already at least one activity at this receiver point
					if(receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).containsKey(personId)) {
						// at least the second activity of this person at this receiver point
						double startTime = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).get(actNumber).getFirst();
						double EndTime = time;
						Tuple<Double,Double> activityStartAndActivityEnd = new Tuple<Double, Double>(startTime, EndTime);
						Map<Integer,Tuple<Double,Double>> actNumber2activityStartAndActivityEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId);
						actNumber2activityStartAndActivityEnd.put(actNumber, activityStartAndActivityEnd);
						Map<Id,Map<Integer,Tuple<Double,Double>>> personId2actNumber2activityStartAndActivityEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId);
						personId2actNumber2activityStartAndActivityEnd.put(personId, actNumber2activityStartAndActivityEnd);
						receiverPointId2personId2actNumber2activityStartAndActivityEnd.put(receiverPointId, personId2actNumber2activityStartAndActivityEnd);
	
					} else {
						// the first activity of this person at this receiver point
						double startTime = 0.; // this must be the home activity in the morning;
						double EndTime = time;
						Tuple<Double,Double> activityStartAndActivityEnd = new Tuple<Double, Double>(startTime, EndTime);
						Map<Integer,Tuple<Double,Double>> actNumber2activityStartAndActivityEnd = new HashMap<Integer, Tuple<Double,Double>>();
						actNumber2activityStartAndActivityEnd.put(actNumber, activityStartAndActivityEnd);
						Map<Id,Map<Integer,Tuple<Double,Double>>> personId2actNumber2activityStartAndActivityEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId);
						personId2actNumber2activityStartAndActivityEnd.put(personId, actNumber2activityStartAndActivityEnd);
						receiverPointId2personId2actNumber2activityStartAndActivityEnd.put(receiverPointId, personId2actNumber2activityStartAndActivityEnd);
	
						String actType = event.getActType();
						Map <Integer,String> actNumber2actType = new HashMap<Integer, String>();
						actNumber2actType.put(actNumber,actType);
						personId2actNumber2actType.put(personId, actNumber2actType);
					}
					
				} else {
					// the first activity at this receiver point
					double startTime = 0.; // this must be the home activity in the morning;
					double EndTime = time;
					Tuple<Double,Double> activityStartAndActivityEnd = new Tuple<Double, Double>(startTime, EndTime);
					Map<Integer,Tuple<Double,Double>> actNumber2activityStartAndActivityEnd = new HashMap<Integer, Tuple<Double,Double>>();
					actNumber2activityStartAndActivityEnd.put(actNumber, activityStartAndActivityEnd);
					Map<Id,Map<Integer,Tuple<Double,Double>>> personId2actNumber2activityStartAndActivityEnd = new HashMap<Id, Map<Integer,Tuple<Double,Double>>>();
					personId2actNumber2activityStartAndActivityEnd.put(personId, actNumber2activityStartAndActivityEnd);
					receiverPointId2personId2actNumber2activityStartAndActivityEnd.put(receiverPointId, personId2actNumber2activityStartAndActivityEnd);
	
					String actType = event.getActType();
					Map <Integer,String> actNumber2actType = new HashMap<Integer, String>();
					actNumber2actType.put(actNumber,actType);
					personId2actNumber2actType.put(personId, actNumber2actType);
				}
			} 
		}
	}

	public void calculateDurationOfStay() {
		for(Id receiverPointId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.keySet()) {
			for(Id personId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).keySet()) {
				for(Integer actNumber : receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).keySet()) {
					Tuple<Double,Double> actStartAndActEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).get(actNumber);
					String actType = personId2actNumber2actType.get(personId).get(actNumber);
					double actStart = Double.MAX_VALUE;
					double actEnd = Double.MIN_VALUE;
					if(actStartAndActEnd.getFirst() == 0.) {
						// home activity (morning)
						actStart = 0.;
						actEnd = actStartAndActEnd.getSecond();
					} else if(actStartAndActEnd.getSecond() == 30*3600) {
						// home activity (evening)
						actStart = actStartAndActEnd.getFirst();
						actEnd = 30*3600;
					} else {
						// other activity
						actStart = actStartAndActEnd.getFirst();
						actEnd = actStartAndActEnd.getSecond();
					}
					// now calculation for the time shares of the intervals
					for(double intervalEnd = NoiseConfigParameters.getTimeBinSizeNoiseComputation() ; intervalEnd <= 30*3600 ; intervalEnd = intervalEnd + NoiseConfigParameters.getTimeBinSizeNoiseComputation()) {
						double intervalStart = intervalEnd - NoiseConfigParameters.getTimeBinSizeNoiseComputation();
//						
						double durationOfStay = 0.;
//						
						if(actEnd <= intervalStart || actStart >= intervalEnd) {
							durationOfStay = 0.;
						} else if(actStart <= intervalStart && actEnd >= intervalEnd) {
							durationOfStay = NoiseConfigParameters.getTimeBinSizeNoiseComputation();
						} else if(actStart <= intervalStart && actEnd <= intervalEnd) {
							durationOfStay = actEnd - intervalStart;
						} else if(actStart >= intervalStart && actEnd >= intervalEnd) {
							durationOfStay = intervalEnd - actStart;
						} else if(actStart >= intervalStart && actEnd <= intervalEnd) {
							durationOfStay = actEnd - actStart;
						}
						
						// calculation for the individual noiseEventsAffected
						// list for all receiver points and all time intervals for each agent the time, ...
						Map <Double , Map <Id,Map<Integer,Tuple<Double,String>>>> timeInterval2personId2actNumber2affectedAgentUnitsAndActType = new HashMap <Double , Map <Id,Map<Integer,Tuple<Double,String>>>>();
						if(receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.containsKey(receiverPointId)) {
							timeInterval2personId2actNumber2affectedAgentUnitsAndActType = receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.get(receiverPointId);
						} else {
						}
						Map <Id,Map<Integer,Tuple<Double,String>>> personId2actNumber2affectedAgentUnitsAndActType = new HashMap <Id,Map<Integer,Tuple<Double,String>>>();
						if(timeInterval2personId2actNumber2affectedAgentUnitsAndActType.containsKey(intervalEnd)) {
							personId2actNumber2affectedAgentUnitsAndActType = timeInterval2personId2actNumber2affectedAgentUnitsAndActType.get(intervalEnd);
						} else {
						}
						Map<Integer,Tuple<Double,String>> actNumber2affectedAgentUnitsAndActType = new HashMap <Integer,Tuple<Double,String>>();
						if(personId2actNumber2affectedAgentUnitsAndActType.containsKey(personId)) {
							actNumber2affectedAgentUnitsAndActType = personId2actNumber2affectedAgentUnitsAndActType.get(personId);
						} else {
						}
						Tuple <Double,String> affectedAgentUnitsAndActType = new Tuple<Double, String>((durationOfStay/3600.), actType);
						actNumber2affectedAgentUnitsAndActType.put(actNumber,affectedAgentUnitsAndActType);
						personId2actNumber2affectedAgentUnitsAndActType.put(personId,actNumber2affectedAgentUnitsAndActType);
						timeInterval2personId2actNumber2affectedAgentUnitsAndActType.put(intervalEnd,personId2actNumber2affectedAgentUnitsAndActType);
						receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.put(receiverPointId,timeInterval2personId2actNumber2affectedAgentUnitsAndActType);
						
						// calculation for the individual noiseEventsAffected (home-based-oriented)
						
						
						// calculation for the damage
						// the adaption of the intervalLength is considered here implicitly,
						// a further correction is not necessary
						double affectedAgentUnits = (NoiseConfigParameters.getScaleFactor())* (durationOfStay/3600.);
						if(receiverPointId2timeInterval2affectedAgentUnits.containsKey(receiverPointId)) {
							if(receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId).containsKey(intervalEnd)) {
								Map<Double,Double> timeInterval2affectedAgentUnits = receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId);
								timeInterval2affectedAgentUnits.put(intervalEnd, timeInterval2affectedAgentUnits.get(intervalEnd)+affectedAgentUnits);
								receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId, timeInterval2affectedAgentUnits);
							} else {
								Map<Double,Double> timeInterval2affectedAgentUnits = receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId);
								timeInterval2affectedAgentUnits.put(intervalEnd, affectedAgentUnits);
								receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId, timeInterval2affectedAgentUnits);
							}
						} else {
							Map<Double,Double> timeInterval2affectedAgentUnits = new HashMap<Double, Double>();
							timeInterval2affectedAgentUnits.put(intervalEnd, affectedAgentUnits);
							receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId, timeInterval2affectedAgentUnits);
						}	
					}
				}
			}
		}
	}
	
	public void calculateDurationOfStayOnlyHomeActivity() {
		for(Id receiverPointId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.keySet()) {
			for(Id personId : receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).keySet()) {
				for(Integer actNumber : receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).keySet()) {
					Tuple<Double,Double> actStartAndActEnd = receiverPointId2personId2actNumber2activityStartAndActivityEnd.get(receiverPointId).get(personId).get(actNumber);
					String actType = personId2actNumber2actType.get(personId).get(actNumber);
					double actStart = Double.MAX_VALUE;
					double actEnd = Double.MIN_VALUE;
					if(actStartAndActEnd.getFirst() == 0.) {
						// home activity (morning)
						actStart = 0.;
						actEnd = actStartAndActEnd.getSecond();
					} else if(actStartAndActEnd.getSecond() == 30*3600) {
						// TODO: !!! actStartAndActEnd.getSecond() == 30*3600 ?? Right Adaption before?!
						// home activity (evening)
						actStart = actStartAndActEnd.getFirst();
						actEnd = 30*3600;
					} else {
						// other activity
						actStart = actStartAndActEnd.getFirst();
						actEnd = actStartAndActEnd.getSecond();
					}
					
					if(!(actType.toString().equals("home"))) {
						// activity duration is zero if it is a home activity
						actEnd = actStart;
					}
					
					// now calculation for the time shares of the intervals
					for(double intervalEnd = NoiseConfigParameters.getTimeBinSizeNoiseComputation() ; intervalEnd <= 30*3600 ; intervalEnd = intervalEnd + NoiseConfigParameters.getTimeBinSizeNoiseComputation()) {
						double intervalStart = intervalEnd - NoiseConfigParameters.getTimeBinSizeNoiseComputation();
					
						double durationOfStay = 0.;
					
						if(actEnd <= intervalStart || actStart >= intervalEnd) {
							durationOfStay = 0.;
						} else if(actStart <= intervalStart && actEnd >= intervalEnd) {
							durationOfStay = NoiseConfigParameters.getTimeBinSizeNoiseComputation();
						} else if(actStart <= intervalStart && actEnd <= intervalEnd) {
							durationOfStay = actEnd - intervalStart;
						} else if(actStart >= intervalStart && actEnd >= intervalEnd) {
							durationOfStay = intervalEnd - actStart;
						} else if(actStart <= intervalStart && actEnd <= intervalEnd) {
							durationOfStay = actEnd - actStart;
						}
						
						// calculation for the individual noiseEventsAffected
						// list for all receiver points and all time intervals for each agent the time, ...
						Map <Double , Map <Id,Map<Integer,Tuple<Double,String>>>> timeInterval2personId2actNumber2affectedAgentUnitsAndActType = new HashMap <Double , Map <Id,Map<Integer,Tuple<Double,String>>>>();
						if(receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.containsKey(receiverPointId)) {
							timeInterval2personId2actNumber2affectedAgentUnitsAndActType = receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.get(receiverPointId);
						} else {
						}
						Map <Id,Map<Integer,Tuple<Double,String>>> personId2actNumber2affectedAgentUnitsAndActType = new HashMap <Id,Map<Integer,Tuple<Double,String>>>();
						if(timeInterval2personId2actNumber2affectedAgentUnitsAndActType.containsKey(intervalEnd)) {
							personId2actNumber2affectedAgentUnitsAndActType = timeInterval2personId2actNumber2affectedAgentUnitsAndActType.get(intervalEnd);
						} else {
						}
						Map<Integer,Tuple<Double,String>> actNumber2affectedAgentUnitsAndActType = new HashMap <Integer,Tuple<Double,String>>();
						if(personId2actNumber2affectedAgentUnitsAndActType.containsKey(personId)) {
							actNumber2affectedAgentUnitsAndActType = personId2actNumber2affectedAgentUnitsAndActType.get(personId);
						} else {
						}
						Tuple <Double,String> affectedAgentUnitsAndActType = new Tuple<Double, String>((durationOfStay/3600.), actType);
						actNumber2affectedAgentUnitsAndActType.put(actNumber,affectedAgentUnitsAndActType);
						personId2actNumber2affectedAgentUnitsAndActType.put(personId,actNumber2affectedAgentUnitsAndActType);
						timeInterval2personId2actNumber2affectedAgentUnitsAndActType.put(intervalEnd,personId2actNumber2affectedAgentUnitsAndActType);
						receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType.put(receiverPointId,timeInterval2personId2actNumber2affectedAgentUnitsAndActType);
						
						// calculation for the individual noiseEventsAffected (home-based-oriented)
						
						// calculation for the damage
						double affectedAgentUnits = (NoiseConfigParameters.getScaleFactor())* (durationOfStay/3600.);
						if(receiverPointId2timeInterval2affectedAgentUnits.containsKey(receiverPointId)) {
							if(receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId).containsKey(intervalEnd)) {
								Map<Double,Double> timeInterval2affectedAgentUnits = receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId);
								timeInterval2affectedAgentUnits.put(intervalEnd, timeInterval2affectedAgentUnits.get(intervalEnd)+affectedAgentUnits);
								receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId, timeInterval2affectedAgentUnits);
							} else {
								Map<Double,Double> timeInterval2affectedAgentUnits = receiverPointId2timeInterval2affectedAgentUnits.get(receiverPointId);
								timeInterval2affectedAgentUnits.put(intervalEnd, affectedAgentUnits);
								receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId, timeInterval2affectedAgentUnits);
							}
						} else {
							Map<Double,Double> timeInterval2affectedAgentUnits = new HashMap<Double, Double>();
							timeInterval2affectedAgentUnits.put(intervalEnd, affectedAgentUnits);
							receiverPointId2timeInterval2affectedAgentUnits.put(receiverPointId, timeInterval2affectedAgentUnits);
						}	
					}
				}
			}
		}
	}

	public Map<Id, Map<Id, Map<Integer, Tuple<Double, Double>>>> getReceiverPointId2personId2actNumber2activityStartAndActivityEnd() {
		return receiverPointId2personId2actNumber2activityStartAndActivityEnd;
	}

	public Map<Id, Map<Double, Double>> getReceiverPointId2timeInterval2affectedAgentUnits() {
		return receiverPointId2timeInterval2affectedAgentUnits;
	}

	public Map<Id, Map<Double, Map<Id, Map<Integer, Tuple<Double, String>>>>> getReceiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType() {
		return receiverPointId2timeInterval2personId2actNumber2affectedAgentUnitsAndActType;
	}

	public Map<Id, List<Id>> getReceiverPointId2ListOfHomeAgents() {
		return receiverPointId2ListOfHomeAgents;
	}
	
}
