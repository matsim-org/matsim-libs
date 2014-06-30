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
package playground.ikaddoura.noise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

//import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

public class GetActivityCoords {
	
//	private static final Logger log = Logger.getLogger(GetActivityCoords.class);
	
	static Scenario scenario;
	
	static List <Coord> allActivityCoords = new ArrayList <Coord>();
	static Map <Coord,Double> activityCoords2densityValue = new HashMap<Coord, Double>();
	static Map<Id,Double> linkId2streetWidth = new HashMap<Id, Double>();
	
	static Map<Id, Queue<Coord>> personId2coordsOfActivities = new HashMap<Id, Queue<Coord>>();
	static Map<Id,List<Coord>> personId2listOfCoords = new HashMap<Id, List<Coord>>();
	static Map <Coord,Id> activityCoord2NearestReceiverPointId = new HashMap<Coord, Id>();
	static Map <Id,Integer> receiverPointId2initialAssignmentInt = new HashMap<Id, Integer>();
	
	Map<Id , Coord> allReceiverPointIds2Coord = new HashMap<Id, Coord>();
	
	public GetActivityCoords(Scenario scenario) {
//		this.scenario = scenario;
	}
	
	public static void getActivityCoords (Scenario scenario) {
		// first of all, list all activity-coordinates
		// and assign all coordinates of a plan to a person
		for(Person person: scenario.getPopulation().getPersons().values()) {
	
			personId2coordsOfActivities.put(person.getId(), new LinkedList<Coord>());
			
			for(PlanElement planElement: person.getSelectedPlan().getPlanElements()) {
				if(planElement instanceof Activity) {
					Activity currentActivity = (Activity) planElement;
					// Exclude "pt interaction" pseudo-activities
					if(!currentActivity.getType().equalsIgnoreCase("pt interaction")) {
						personId2coordsOfActivities.get(person.getId()).add(currentActivity.getCoord());
						List<Coord> listTmp = new ArrayList<Coord>();
						if(personId2listOfCoords.containsKey(person.getId())) {
							listTmp = personId2listOfCoords.get(person.getId());
						}
						Coord coordTmp = currentActivity.getCoord();
						listTmp.add(coordTmp);
						personId2listOfCoords.put(person.getId(), listTmp);
						allActivityCoords.add(currentActivity.getCoord());
					}
				}
			}
		}
		
//		classify the activityCoords on the basis of the density
		double xCoordMin = Double.MAX_VALUE;
		double xCoordMax = Double.MIN_VALUE;
		double yCoordMin = Double.MAX_VALUE;
		double yCoordMax = Double.MIN_VALUE;
		for(Coord coord : allActivityCoords) {
			if(coord.getX()<xCoordMin) {
				xCoordMin = coord.getX();
			}
			if(coord.getX()>xCoordMax) {
				xCoordMax = coord.getX();
			}
			if(coord.getY()<yCoordMin) {
				yCoordMin = coord.getY();
			}
			if(coord.getY()>yCoordMax) {
				yCoordMax = coord.getY();
			}	
		}
		for(Id coordId : GetNearestReceiverPoint.receiverPoints.keySet()) {
			Coord coord = GetNearestReceiverPoint.receiverPoints.get(coordId);
			if(coord.getX()<xCoordMin) {
				xCoordMin = coord.getX();
			}
			if(coord.getX()>xCoordMax) {
				xCoordMax = coord.getX();
			}
			if(coord.getY()<yCoordMin) {
				yCoordMin = coord.getY();
			}
			if(coord.getY()>yCoordMax) {
				yCoordMax = coord.getY();
			}	
		}
		
		for(double y = yCoordMax+500 ; y > yCoordMin-800  ; y = y-316.225) {
			for(double x = xCoordMin-500 ; x < xCoordMax+800  ; x = x+316.225) {
				double xMinTmp = x;
				double xMaxTmp = x+316.225;
				double yMaxTmp = y;
				double yMinTmp = y-316.225;
				
				List<Coord> listTmp = new ArrayList<Coord>();
				int counterCoords = 0;
				
				for(Coord coord : allActivityCoords) {
					if((coord.getX()>=xMinTmp)&&(coord.getX()<xMaxTmp)&&(coord.getY()>=yMinTmp)&&(coord.getY()<yMaxTmp)) {
						listTmp.add(coord);
						counterCoords++;
					}
				}
				counterCoords = (int) (counterCoords*Configurations.getScaleFactor());
				int counter = counterCoords;
				
				double densityValue = counter/10.;
				// a value of 100 is high (high density of activity locations: 10000/square kilometre)

				for(Coord coordTmp : listTmp) {
					activityCoords2densityValue.put(coordTmp, densityValue);
					for(Id coordId : GetNearestReceiverPoint.receiverPoints.keySet()) {
						Coord receiverPointCoord = GetNearestReceiverPoint.receiverPoints.get(coordId);
						if((receiverPointCoord.getX()>=xMinTmp)&&(receiverPointCoord.getX()<xMaxTmp)&&(receiverPointCoord.getY()>=yMinTmp)&&(receiverPointCoord.getY()<yMaxTmp)) {
							activityCoords2densityValue.put(receiverPointCoord, densityValue);
						}
					}
				}
			}
		}
		
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
			double streetWidth = getStreetWidth(scenario,linkId);
			linkId2streetWidth.put(linkId, streetWidth);
		}
	}
	
	public static double getActivityDensityValue (Scenario scenario , Coord coord) {
		double radius = 178.41; // one square kilometre/10
		
		double densityValue = 0.;
		
		double coordX = coord.getX();
		double coordY = coord.getY();
		
		int counter = 0;
		
		for(Coord c : allActivityCoords) {	
			double xValue = c.getX();
			double yValue = c.getY();
			
			if(Math.sqrt((Math.pow(coordX-xValue,2))+(Math.pow(coordY-yValue,2)))<= radius) {
				counter = counter + 1;
			}
			counter = (int) (counter*Configurations.getScaleFactor());
		}
		
		densityValue = counter/10.;
		// a value of 100 is high (high density of activity locations: 10000/square kilometre)
		
		return densityValue;
	}
	
	public static double getStreetWidth (Scenario scenario , Id linkId) {
		double capacity = scenario.getNetwork().getLinks().get(linkId).getCapacity();
		
		double streetWidth = 8 + capacity/200;
		
		return streetWidth;
	}
	
	
	public static void getActivityCoord2NearestReceiverPointId (Scenario scenario) {
		
		// Each activity coordinate is assigned to a receiver point, the coodinates might be identical
		
		for(Coord coord : allActivityCoords) {
			Id receiverPointId = GetNearestReceiverPoint.getNearestReceiverPoint(coord);
			activityCoord2NearestReceiverPointId.put(coord, receiverPointId);
		}
	}
	
	public static void getInitialAssignment (Scenario scenario) {
		for(Id personId : personId2coordsOfActivities.keySet()) {
			
			Coord coord = personId2coordsOfActivities.get(personId).peek();
			
			Id receiverPointId = activityCoord2NearestReceiverPointId.get(coord);
			
			int x = 0;
			if(receiverPointId2initialAssignmentInt.containsKey(receiverPointId)) {
				x = receiverPointId2initialAssignmentInt.get(receiverPointId) + 1;
			} else {
				x = 1;
			}
			receiverPointId2initialAssignmentInt.put(receiverPointId , x);
		}
		
		
	}

}
