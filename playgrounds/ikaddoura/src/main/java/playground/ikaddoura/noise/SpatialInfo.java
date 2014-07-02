/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * Contains spatial information for the RLS approach 'Lange gerade Fahrstreifen'
 * 
 * @author lkroeger, ikaddoura
 *
 */
public class SpatialInfo {
	
	private double receiverPointGap = 200; // distance between two receiver points along x- and y-axes
	private double relevantRadius = 500.; // radius around a receiver point in which all links are considered as relevant
	
	private ScenarioImpl scenario;
	private Map<Id,Coord> receiverPointId2Coord = new HashMap<Id,Coord>();
	
	private Map<Id,List<Id>> receiverPointId2relevantLinkIds = new HashMap<Id, List<Id>>();
	private Map<Id, Map<Id,Double>> receiverPointId2RelevantLinkIds2Distances = new HashMap<Id, Map<Id,Double>>();
	private Map<Coord,Id> activityCoord2receiverPointId = new HashMap<Coord,Id>();
		
	private List <Coord> allActivityCoords = new ArrayList <Coord>();
	private Map <Coord,Double> activityCoords2densityValue = new HashMap<Coord, Double>();
	private Map<Id,Double> linkId2streetWidth = new HashMap<Id, Double>();
	
	private Map<Id, Queue<Coord>> personId2coordsOfActivities = new HashMap<Id, Queue<Coord>>();
	private Map<Id,List<Coord>> personId2listOfCoords = new HashMap<Id, List<Coord>>();
	private Map <Coord,Id> activityCoord2NearestReceiverPointId = new HashMap<Coord, Id>();
	private Map <Id,Integer> receiverPointId2initialAssignmentInt = new HashMap<Id, Integer>();
	
	private Map<Id , Coord> allReceiverPointIds2Coord = new HashMap<Id, Coord>();

	// additional info required for (RLS-Teilstueckverfahren)
	private Map<Id, Map<Id,Double>> receiverPoint2RelevantlinkIds2lengthOfPartOfLinks = new HashMap<Id, Map<Id,Double>>();
	private Map<Id, Map<Id, Map<Integer, Double>>> receiverPointId2RelevantLinkIds2partOfLinks2distance = new HashMap<Id, Map<Id,Map<Integer,Double>>>();	
	
	public SpatialInfo(ScenarioImpl scenario) {
		this.scenario = scenario;
	}

	public void setReceiverPoints() {
		double xMin = Double.MAX_VALUE;
		double xMax = Double.MIN_VALUE;
		double yMin = Double.MAX_VALUE;
		double yMax = Double.MIN_VALUE;
		
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
			Link link = scenario.getNetwork().getLinks().get(linkId);
			Coord fromNodeCoord = link.getFromNode().getCoord();
			Coord toNodeCoord = link.getToNode().getCoord();
			
			if(fromNodeCoord.getX()<xMin) {
				xMin = fromNodeCoord.getX();
			}
			if(toNodeCoord.getX()<xMin) {
				xMin = toNodeCoord.getX();
			}
			if(fromNodeCoord.getY()<yMin) {
				yMin = fromNodeCoord.getY();
			}
			if(toNodeCoord.getY()<yMin) {
				yMin = toNodeCoord.getY();
			}
			
			if(fromNodeCoord.getX()>xMax) {
				xMax = fromNodeCoord.getX();
			}
			if(toNodeCoord.getX()>xMax) {
				xMax = toNodeCoord.getX();
			}
			if(fromNodeCoord.getY()>yMax) {
				yMax = fromNodeCoord.getY();
			}
			if(toNodeCoord.getY()>yMax) {
				yMax = toNodeCoord.getY();
			}
		}
		
		int counter = 0;
		for(double y = yMax + receiverPointGap ; y > yMin - receiverPointGap ; y = y - receiverPointGap) {
			for(double x = xMin - receiverPointGap ; x < xMax + receiverPointGap ; x = x + receiverPointGap) {
				Coord newCoord = new CoordImpl(x, y);
				receiverPointId2Coord.put(new IdImpl("coordId"+counter), newCoord);
				counter++;
			}
		}
		
//		System.out.println(receiverPointId2Coord.toString());
		
		// Alternative approach: Use the spatial distribution of the activity location to define the receiver points instead of regular grid.
//		for(Coord newCoord : GetActivityCoords.allActivityCoords) {
//			int b = 0;
//			int c = 0;
//			int d = 0;
//			for (Id coordId : receiverPoints.keySet()) {
//				double x1 = receiverPoints.get(coordId).getX();
//				double y1 = receiverPoints.get(coordId).getY();
//				double x2 = newCoord.getX();
//				double y2 = newCoord.getY();
//				double distance = (Math.sqrt((Math.pow((x1-x2),2)+Math.pow((y1-y2),2))));
//				
//				if(distance<50.0) {
//					b = 1;
//				}else if(distance<150.0) {
//					c = 1;
//				}else if(distance<250.0) {
//					d = 1;
//				}
//			}
//			if ((b==0)&&(c==0)&&(d==0)) {
//				receiverPoints.put(new IdImpl("coordId"+counter+"_"+newCoord.getX()+"_"+newCoord.getY()), newCoord);
//				counter++;
//				System.out.println("receiverPointCounter: "+counter);
//			} else if ((b==0)&&(c==1)) {
//				double random = Math.random();
//				if(random<0.02) {
//					receiverPoints.put(new IdImpl("coordId"+counter+"_"+newCoord.getX()+"_"+newCoord.getY()), newCoord);
//					counter++;
//					System.out.println("receiverPointCounter: "+counter);
//				}
//			} else if ((b==0)&&(c==0)&&(d==1)) {
//				double random = Math.random();
//				if(random<0.005) {
//					receiverPoints.put(new IdImpl("coordId"+counter+"_"+newCoord.getX()+"_"+newCoord.getY()), newCoord);
//					counter++;
//					System.out.println("receiverPointCounter: "+counter);
//				}
//			}
//		}
	}
	
	public void setActivityCoords () {
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
		for(Id coordId : this.receiverPointId2Coord.keySet()) {
			Coord coord = this.receiverPointId2Coord.get(coordId);
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
				counterCoords = (int) (counterCoords*NoiseConfig.getScaleFactor());
				int counter = counterCoords;
				
				double densityValue = counter/10.;
				// a value of 100 is high (high density of activity locations: 10000/square kilometre)

				for(Coord coordTmp : listTmp) {
					activityCoords2densityValue.put(coordTmp, densityValue);
					for(Id coordId : this.receiverPointId2Coord.keySet()) {
						Coord receiverPointCoord = this.receiverPointId2Coord.get(coordId);
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
	
//	public double getActivityDensityValue (Scenario scenario , Coord coord) {
//		double radius = 178.41; // one square kilometre/10
//		
//		double densityValue = 0.;
//		
//		double coordX = coord.getX();
//		double coordY = coord.getY();
//		
//		int counter = 1;
//		
//		for(Coord c : allActivityCoords) {	
//			double xValue = c.getX();
//			double yValue = c.getY();
//			
//			if(Math.sqrt((Math.pow(coordX-xValue,2))+(Math.pow(coordY-yValue,2)))<= radius) {
//				counter = counter + 1;
//			}
//			counter = (int) (counter*NoiseConfig.getScaleFactor());
//		}
//		
//		densityValue = counter/10.;
//		// a value of 100 is high (high density of activity locations: 10000/square kilometre)
//		
//		return densityValue;
//	}
	
	private double getStreetWidth (Scenario scenario , Id linkId) {
		double capacity = scenario.getNetwork().getLinks().get(linkId).getCapacity();
		double streetWidth = 8 + capacity/200;
		return streetWidth;
	}
	
	public void setActivityCoord2NearestReceiverPointId () {
		
		// Each activity coordinate is assigned to a receiver point, the coodinates might be identical
		
		for(Coord coord : allActivityCoords) {
			Id receiverPointId = this.getNearestReceiverPoint(coord);
			activityCoord2NearestReceiverPointId.put(coord, receiverPointId);
		}
	}
	
	public void setInitialAssignment () {
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
	
	public void setRelevantLinkIds() {
		// for faster computation, for noise immission-calculation, not all network links must be considered.
		for(Id pointId : receiverPointId2Coord.keySet()) {
			double pointCoordX = receiverPointId2Coord.get(pointId).getX();
			double pointCoordY = receiverPointId2Coord.get(pointId).getY();
			List<Id> relevantLinkIds = new ArrayList<Id>();
			Map<Id,Double> relevantLinkIds2distance = new HashMap<Id, Double>();
		
			for (Id linkId : scenario.getNetwork().getLinks().keySet()){
				double fromCoordX = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX();
				double fromCoordY = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY();
				double toCoordX = scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord().getX();
				double toCoordY = scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord().getY();
			
				double lotPointX = 0.;
				double lotPointY = 0.;
			
				double vectorX = toCoordX - fromCoordX;
				if(vectorX==0.) {
					vectorX=0.00000001;
					// dividing by zero is not possible
				}
				double vectorY = toCoordY - fromCoordY;
				
				double vector = vectorY/vectorX;
				if(vector==0.) {
					vector=0.00000001;
					// dividing by zero is not possible
				}
			
				double vector2 = (-1) * (1/vector);
				double yAbschnitt = fromCoordY - (fromCoordX*vector);
				double yAbschnittOriginal = fromCoordY - (fromCoordX*vector);
			
				double yAbschnitt2 = pointCoordY - (pointCoordX*vector2);
			
				double xValue = 0.;
				double yValue = 0.;
			
				if(yAbschnitt<yAbschnitt2) {
					yAbschnitt2 = yAbschnitt2 - yAbschnitt;
					yAbschnitt = 0;
					xValue = yAbschnitt2 / (vector - vector2);
					yValue = yAbschnittOriginal + (xValue*vector);
				} else if(yAbschnitt2<yAbschnitt) {
					yAbschnitt = yAbschnitt - yAbschnitt2;
					yAbschnitt2 = 0;
					xValue = yAbschnitt / (vector2 - vector);
					yValue = yAbschnittOriginal + (xValue*vector);
				}
			
				lotPointX = xValue;
				lotPointY = yValue;
				double distance = 0.;
				
				if(((xValue>fromCoordX)&&(xValue<toCoordX))||((xValue>toCoordX)&&(xValue<fromCoordX))||((yValue>fromCoordY)&&(yValue<toCoordY))||((yValue>toCoordY)&&(yValue<fromCoordY))) {
					// no edge solution (keine Randloesung)
					distance = Math.sqrt((Math.pow(lotPointX-pointCoordX, 2))+(Math.pow(lotPointY-pointCoordY, 2)));
				} else {
					// edge solution (Randloesung)
					double distanceToFromNode = Math.sqrt((Math.pow(fromCoordX-pointCoordX, 2))+(Math.pow(fromCoordY-pointCoordY, 2)));
					double distanceToToNode = Math.sqrt((Math.pow(toCoordX-pointCoordX, 2))+(Math.pow(toCoordY-pointCoordY, 2)));
					if (distanceToFromNode>distanceToToNode) {
						distance = distanceToToNode;
					} else {
						distance = distanceToFromNode;
					}
				}
				
				if(distance <= relevantRadius){
					relevantLinkIds.add(linkId);
					double minDistance = (getStreetWidth(scenario, linkId))/3;
					if(distance<minDistance) {
						distance = minDistance;
					}
					relevantLinkIds2distance.put(linkId, distance);
				}
			}
			receiverPointId2relevantLinkIds.put(pointId, relevantLinkIds);
			receiverPointId2RelevantLinkIds2Distances.put(pointId,relevantLinkIds2distance);
		}
		if (NoiseConfig.getRLSMethod().equals("parts")){
			getDistancesToPartsOfLink(scenario,receiverPointId2RelevantLinkIds2Distances);
		} else {
			// no additional computation
		}
	}
	
	public Map<Id, Map<Id, Map<Integer, Double>>> getDistancesToPartsOfLink (Scenario scenario , Map<Id, Map<Id,Double>> receiverpointId2RelevantlinkIds2Distances) {
		
		for(Id pointId : receiverPointId2RelevantLinkIds2Distances.keySet()) {
		
			Map<Id,Map<Integer,Double>> relevantLinkIds2partOfLinks2distance = new HashMap<Id, Map<Integer,Double>>();
			Map<Id,Double> relevantLinkIds2lengthOfPartOfLinks = new HashMap<Id, Double>();
			
			for(Id linkId : receiverPointId2RelevantLinkIds2Distances.get(pointId).keySet()) {
				
				double shortestDistance = receiverPointId2RelevantLinkIds2Distances.get(pointId).get(linkId);
				double geoLinkLength = Math.sqrt((Math.pow(scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX() - scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord().getX(), 2))+(Math.pow(scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY() - scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord().getY(), 2))); 
				double maxPartLinkLength = shortestDistance/4;
				
				int numberOfParts = (int) (geoLinkLength/maxPartLinkLength);
				numberOfParts = numberOfParts + 1;
			
				double lengthOfParts = geoLinkLength/numberOfParts;
				
				relevantLinkIds2lengthOfPartOfLinks.put(linkId, lengthOfParts);
				
				double fromCoordX = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX();
				double fromCoordY = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY();
				double toCoordX = scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord().getX();
				double toCoordY = scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord().getY();
				
				double vectorX = (toCoordX - fromCoordX)/(numberOfParts);
				double vectorY = (toCoordY - fromCoordY)/(numberOfParts);
				
				double xValue = receiverPointId2Coord.get(pointId).getX();
				double yValue = receiverPointId2Coord.get(pointId).getY();
				
				Map<Integer,Double> partOfLink2distance = new HashMap<Integer, Double>();
				
				for(int i = 0 ; i < numberOfParts ; i++) {
					double relevantX = fromCoordX + (i+0.5)*vectorX;
					double relevantY = fromCoordY + (i+0.5)*vectorY;
					
					double distance = Math.sqrt((Math.pow(xValue - relevantX, 2))+(Math.pow(yValue - relevantY, 2)));
				
					partOfLink2distance.put(i, distance);
				}
				relevantLinkIds2partOfLinks2distance.put(linkId, partOfLink2distance);
			}		
			receiverPointId2RelevantLinkIds2partOfLinks2distance.put(pointId, relevantLinkIds2partOfLinks2distance);
			receiverPoint2RelevantlinkIds2lengthOfPartOfLinks.put(pointId, relevantLinkIds2lengthOfPartOfLinks);
		}
		return receiverPointId2RelevantLinkIds2partOfLinks2distance;
	}
	
	public Id getNearestReceiverPoint (Coord coord) {
		Id nearestReceiverPointId = null;
		
		double xCoord = coord.getX();
		double yCoord = coord.getY();
		double distance = Double.MAX_VALUE;
		
		for(Id receiverPointId : receiverPointId2Coord.keySet()) {
			double xValue = receiverPointId2Coord.get(receiverPointId).getX();
			double yValue = receiverPointId2Coord.get(receiverPointId).getY();
			
			double a = xCoord - xValue;
			double b = yCoord - yValue;
			
			double distanceTmp = Math.sqrt((Math.pow(a, 2))+(Math.pow(b, 2)));
			if(distanceTmp < distance) {
				// update the nearest receiver point
				distance = distanceTmp;
				nearestReceiverPointId = receiverPointId;
			} else {}
		}
		activityCoord2receiverPointId.put(coord, nearestReceiverPointId);
		return nearestReceiverPointId;
	}

	public Map<Id, Coord> getReceiverPoints() {
		return receiverPointId2Coord;
	}

	public Map<Id, Map<Id, Double>> getReceiverPointId2RelevantLinkIds2Distances() {
		return receiverPointId2RelevantLinkIds2Distances;
	}

	public Map<Id, List<Id>> getReceiverPointId2relevantLinkIds() {
		return receiverPointId2relevantLinkIds;
	}

	public Map<Id, List<Coord>> getPersonId2listOfCoords() {
		return personId2listOfCoords;
	}

	public Map<Coord, Id> getActivityCoord2receiverPointId() {
		return activityCoord2receiverPointId;
	}

	public double getReceiverPointGap() {
		return receiverPointGap;
	}

	public double getRelevantRadius() {
		return relevantRadius;
	}

	public Map<Id, Coord> getReceiverPointId2Coord() {
		return receiverPointId2Coord;
	}

	public List<Coord> getAllActivityCoords() {
		return allActivityCoords;
	}

	public Map<Coord, Double> getActivityCoords2densityValue() {
		return activityCoords2densityValue;
	}

	public Map<Id, Double> getLinkId2streetWidth() {
		return linkId2streetWidth;
	}

	public Map<Id, Queue<Coord>> getPersonId2coordsOfActivities() {
		return personId2coordsOfActivities;
	}

	public Map<Coord, Id> getActivityCoord2NearestReceiverPointId() {
		return activityCoord2NearestReceiverPointId;
	}

	public Map<Id, Integer> getReceiverPointId2initialAssignmentInt() {
		return receiverPointId2initialAssignmentInt;
	}

	public Map<Id, Coord> getAllReceiverPointIds2Coord() {
		return allReceiverPointIds2Coord;
	}

	public Map<Id, Map<Id, Double>> getReceiverPoint2RelevantlinkIds2lengthOfPartOfLinks() {
		return receiverPoint2RelevantlinkIds2lengthOfPartOfLinks;
	}

	public Map<Id, Map<Id, Map<Integer, Double>>> getReceiverPointId2RelevantLinkIds2partOfLinks2distance() {
		return receiverPointId2RelevantLinkIds2partOfLinks2distance;
	}
	
}
