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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * Contains spatial information for the RLS approach 'Lange gerade Fahrstreifen'
 * 
 * @author lkroeger, ikaddoura
 *
 */
public class SpatialInfo {
	
	private static final Logger log = Logger.getLogger(SpatialInfo.class);
	
	private double receiverPointGap = 100.; // distance between two receiver points along x- and y-axes
	private double relevantRadius = 500.; // radius around a receiver point in which all links are considered as relevant
	
	private ScenarioImpl scenario;
	private Map<Id,Coord> receiverPointId2Coord = new HashMap<Id,Coord>();
	private Map<Coord,Id> coord2receiverPointId = new HashMap<Coord,Id>();
	private Map<Tuple<Integer,Integer>,List<Id>> zoneTuple2listOfReceiverPointIds = new HashMap<Tuple<Integer,Integer>, List<Id>>();
	private Map<Tuple<Integer,Integer>,List<Id>> zoneTuple2listOfLinkIds = new HashMap<Tuple<Integer,Integer>, List<Id>>();
	
	private Map<Id,List<Id>> receiverPointId2relevantLinkIds = new HashMap<Id, List<Id>>();
	private Map<Id, Map<Id,Double>> receiverPointId2RelevantLinkIds2Distances = new HashMap<Id, Map<Id,Double>>();
	private Map<Id, Map<Id,Double>> receiverPointId2RelevantLinkIds2Drefl = new HashMap<Id, Map<Id,Double>>();
	private Map<Id, Map<Id,Double>> receiverPointId2RelevantLinkIds2DbmDz = new HashMap<Id, Map<Id,Double>>();
	private Map<Id, Map<Id,Double>> receiverPointId2RelevantLinkIds2Ds = new HashMap<Id, Map<Id,Double>>();
	private Map<Id, Map<Id,Double>> receiverPointId2RelevantLinkIds2AngleImmissionCorrection = new HashMap<Id, Map<Id,Double>>();
	private Map<Coord,Id> activityCoord2receiverPointId = new HashMap<Coord,Id>();
		
	private List <Coord> allActivityCoords = new ArrayList <Coord>();
	private Map <Coord,Double> activityCoords2densityValue = new HashMap<Coord, Double>();
	private Map<Id,Double> linkId2streetWidth = new HashMap<Id, Double>();
	
	private Map<Id, Queue<Coord>> personId2coordsOfActivities = new HashMap<Id, Queue<Coord>>();
	private Map<Id,List<Coord>> personId2listOfCoords = new HashMap<Id, List<Coord>>();
	private Map <Id,Integer> receiverPointId2initialAssignmentInt = new HashMap<Id, Integer>();
	
	private Map<Id , Coord> allReceiverPointIds2Coord = new HashMap<Id, Coord>();

	// additional info required for (RLS-Teilstueckverfahren)
	private Map<Id, Map<Id,Double>> receiverPoint2RelevantlinkIds2lengthOfPartOfLinks = new HashMap<Id, Map<Id,Double>>();
	private Map<Id, Map<Id, Map<Integer, Double>>> receiverPointId2RelevantLinkIds2partOfLinks2distance = new HashMap<Id, Map<Id,Map<Integer,Double>>>();	
	
	public SpatialInfo(ScenarioImpl scenario) {
		this.scenario = scenario;
	}

	double xCoordMin = Double.MAX_VALUE;
	double xCoordMax = Double.MIN_VALUE;
	double yCoordMin = Double.MAX_VALUE;
	double yCoordMax = Double.MIN_VALUE;
	
	double xCoordMinLinkNodes = Double.MAX_VALUE;
	double xCoordMaxLinkNodes = Double.MIN_VALUE;
	double yCoordMinLinkNodes = Double.MAX_VALUE;
	double yCoordMaxLinkNodes = Double.MIN_VALUE;
	
	public void setActivityCoords () {
		// first of all, list all activity-coordinates
		// and assign all coordinates of a plan to a person
//		log.info("a");
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
//		log.info("b");
//		classify the activityCoords on the basis of the density
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
//		log.info("c");
//		for(Id coordId : this.receiverPointId2Coord.keySet()) {
//			Coord coord = this.receiverPointId2Coord.get(coordId);
//			if(coord.getX()<xCoordMin) {
//				xCoordMin = coord.getX();
//			}
//			if(coord.getX()>xCoordMax) {
//				xCoordMax = coord.getX();
//			}
//			if(coord.getY()<yCoordMin) {
//				yCoordMin = coord.getY();
//			}
//			if(coord.getY()>yCoordMax) {
//				yCoordMax = coord.getY();
//			}	
//		}
		
//		log.info("d");
		
	}
	
	public void setDensityAndStreetWidth() {
		for(double y = yCoordMax+100. ; y > yCoordMin-100.-316.225  ; y = y-316.225) {
	//		log.info("aa");
			for(double x = xCoordMin-100. ; x < xCoordMax+100.+316.225  ; x = x+316.225) {
				
				double xMinTmp = x;
				double xMaxTmp = x+316.225;
				double yMaxTmp = y;
				double yMinTmp = y-316.225;
				
				List<Coord> listTmp = new ArrayList<Coord>();
				int counterCoords = 0;
				
				for(Coord coord : allActivityCoords) {
	//				if((coord.getX()>=xMinTmp)&&(coord.getX()<xMaxTmp)&&(coord.getY()>=yMinTmp)&&(coord.getY()<yMaxTmp)) {	
	//					listTmp.add(coord);
	//					counterCoords++;
	//				} // clearer but slower!?
					if(coord.getX()>=xMinTmp) {
						if(coord.getX()<xMaxTmp) {
							if(coord.getY()>=yMinTmp) {
								if(coord.getY()<yMaxTmp) {
									listTmp.add(coord);
									counterCoords++;
								}
							}
						}
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
	//					if((receiverPointCoord.getX()>=xMinTmp)&&(receiverPointCoord.getX()<xMaxTmp)&&(receiverPointCoord.getY()>=yMinTmp)&&(receiverPointCoord.getY()<yMaxTmp)) {
	//						activityCoords2densityValue.put(receiverPointCoord, densityValue);
	//					} // clearer but slower!?
						if(receiverPointCoord.getX()>=xMinTmp) {
							if(receiverPointCoord.getX()<xMaxTmp) {
								if(receiverPointCoord.getY()>=yMinTmp) {
									if(receiverPointCoord.getY()<yMaxTmp) {
										activityCoords2densityValue.put(receiverPointCoord, densityValue);
									}
								}
							}
						}
					}
				}
				
			}
		}
//		log.info("e");
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
			double streetWidth = getStreetWidth(scenario,linkId);
			linkId2streetWidth.put(linkId, streetWidth);
		}
	}
	
	public void setReceiverPoints() {
//		double xMin = Double.MAX_VALUE;
//		double xMax = Double.MIN_VALUE;
//		double yMin = Double.MAX_VALUE;
//		double yMax = Double.MIN_VALUE;
//		
//		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
//			Link link = scenario.getNetwork().getLinks().get(linkId);
//			Coord fromNodeCoord = link.getFromNode().getCoord();
//			Coord toNodeCoord = link.getToNode().getCoord();
//			
//			if(fromNodeCoord.getX()<xMin) {
//				xMin = fromNodeCoord.getX();
//			}
//			if(toNodeCoord.getX()<xMin) {
//				xMin = toNodeCoord.getX();
//			}
//			if(fromNodeCoord.getY()<yMin) {
//				yMin = fromNodeCoord.getY();
//			}
//			if(toNodeCoord.getY()<yMin) {
//				yMin = toNodeCoord.getY();
//			}
//			
//			if(fromNodeCoord.getX()>xMax) {
//				xMax = fromNodeCoord.getX();
//			}
//			if(toNodeCoord.getX()>xMax) {
//				xMax = toNodeCoord.getX();
//			}
//			if(fromNodeCoord.getY()>yMax) {
//				yMax = fromNodeCoord.getY();
//			}
//			if(toNodeCoord.getY()>yMax) {
//				yMax = toNodeCoord.getY();
//			}
//		}
		
		int counter = 0;
		
		//TODO: Optional, the receiver points can be set on basis of the activity coords
//		for(Coord actCoord : allActivityCoords) {
////			log.info(actCoord);
//			Tuple<Integer,Integer> zoneTupleActCoord = getZoneTuple(actCoord);
//			
//			List<Tuple<Integer,Integer>> tuples = new ArrayList<Tuple<Integer,Integer>>();
//			List<Id> relevantReceiverPointIds = new ArrayList<Id>();
//			if(zoneTuple2listOfReceiverPointIds.containsKey(zoneTupleActCoord)) {
//				relevantReceiverPointIds.addAll(zoneTuple2listOfReceiverPointIds.get(zoneTupleActCoord));
//			}
//			
//			int x = zoneTupleActCoord.getFirst();
//			int y = zoneTupleActCoord.getSecond();
//			Tuple<Integer,Integer> TupleNW = new Tuple<Integer, Integer>(x-1, y-1);
//			if(zoneTuple2listOfReceiverPointIds.containsKey(TupleNW)) {
//				relevantReceiverPointIds.addAll(zoneTuple2listOfReceiverPointIds.get(TupleNW));
//			}
//			Tuple<Integer,Integer> TupleN = new Tuple<Integer, Integer>(x, y-1);
//			if(zoneTuple2listOfReceiverPointIds.containsKey(TupleN)) {
//				relevantReceiverPointIds.addAll(zoneTuple2listOfReceiverPointIds.get(TupleN));
//			}
//			Tuple<Integer,Integer> TupleNO = new Tuple<Integer, Integer>(x+1, y-1);
//			if(zoneTuple2listOfReceiverPointIds.containsKey(TupleNO)) {
//				relevantReceiverPointIds.addAll(zoneTuple2listOfReceiverPointIds.get(TupleNO));
//			}
//			Tuple<Integer,Integer> TupleW = new Tuple<Integer, Integer>(x-1, y);
//			if(zoneTuple2listOfReceiverPointIds.containsKey(TupleW)) {
//				relevantReceiverPointIds.addAll(zoneTuple2listOfReceiverPointIds.get(TupleW));
//			}
//			Tuple<Integer,Integer> TupleO = new Tuple<Integer, Integer>(x+1, y);
//			if(zoneTuple2listOfReceiverPointIds.containsKey(TupleO)) {
//				relevantReceiverPointIds.addAll(zoneTuple2listOfReceiverPointIds.get(TupleO));
//			}
//			Tuple<Integer,Integer> TupleSW = new Tuple<Integer, Integer>(x-1, y+1);
//			if(zoneTuple2listOfReceiverPointIds.containsKey(TupleSW)) {
//				relevantReceiverPointIds.addAll(zoneTuple2listOfReceiverPointIds.get(TupleSW));
//			}
//			Tuple<Integer,Integer> TupleS = new Tuple<Integer, Integer>(x, y+1);
//			if(zoneTuple2listOfReceiverPointIds.containsKey(TupleS)) {
//				relevantReceiverPointIds.addAll(zoneTuple2listOfReceiverPointIds.get(TupleS));
//			}
//			Tuple<Integer,Integer> TupleSO = new Tuple<Integer, Integer>(x+1, y+1);
//			if(zoneTuple2listOfReceiverPointIds.containsKey(TupleSO)) {
//				relevantReceiverPointIds.addAll(zoneTuple2listOfReceiverPointIds.get(TupleSO));
//			}
////			log.info(relevantReceiverPointIds.size());
//			if(relevantReceiverPointIds.size()==0) {
////				log.info("aa");
//				Coord newCoord = new CoordImpl(actCoord.getX(), actCoord.getY());
//				Id newId = new IdImpl("coordId"+counter);
//				receiverPointId2Coord.put(newId, newCoord);
//				counter++;
//				
//				Tuple<Integer,Integer> zoneTuple = getZoneTuple(newCoord);
//				List<Id> listOfCoords = new ArrayList<Id>();
//				if(zoneTuple2listOfReceiverPointIds.containsKey(zoneTuple)) {
//					listOfCoords = zoneTuple2listOfReceiverPointIds.get(zoneTuple);
//				}
//				listOfCoords.add(newId);
//				zoneTuple2listOfReceiverPointIds.put(zoneTuple,listOfCoords);
//			}else{
//			
//				boolean newCoordNecessary = true;
//				for(Id id : relevantReceiverPointIds) {
//
//					double distance = 0.;
//					double x1 = actCoord.getX();
//					double x2 = receiverPointId2Coord.get(id).getX();
//					double y1 = actCoord.getY();
//					double y2 = receiverPointId2Coord.get(id).getY();
//					
//					distance = Math.sqrt((Math.pow(x2-x1, 2))+(Math.pow(y2-y1, 2)));
//					
//					if(distance <= 20.) {
//						newCoordNecessary = false;
//						
//					}
//				}
//				
//				if (newCoordNecessary == true) {
//					Coord newCoord = new CoordImpl(actCoord.getX(), actCoord.getY());
//					Id newId = new IdImpl("coordId"+counter);
//					receiverPointId2Coord.put(newId, newCoord);
//					counter++;
//					
//					Tuple<Integer,Integer> zoneTuple = getZoneTuple(newCoord);
//					List<Id> listOfCoords = new ArrayList<Id>();
//					if(zoneTuple2listOfReceiverPointIds.containsKey(zoneTuple)) {
//						listOfCoords = zoneTuple2listOfReceiverPointIds.get(zoneTuple);
//					}
//					listOfCoords.add(newId);
//					zoneTuple2listOfReceiverPointIds.put(zoneTuple,listOfCoords);
//				}
//			}
//		}
		
		//TODO: Normally a grid of receiver points is set, the distance in x- and y-direction has to be set above
		for(double y = yCoordMax + 100. ; y > yCoordMin - 100. - receiverPointGap ; y = y - receiverPointGap) {
			for(double x = xCoordMin - 100. ; x < xCoordMax + 100. + receiverPointGap ; x = x + receiverPointGap) {
				Coord newCoord = new CoordImpl(x, y);
				Id newId = new IdImpl("coordId"+counter);
				receiverPointId2Coord.put(newId, newCoord);
				counter++;
							
				Tuple<Integer,Integer> zoneTuple = getZoneTuple(newCoord);
				List<Id> listOfCoords = new ArrayList<Id>();
				if(zoneTuple2listOfReceiverPointIds.containsKey(zoneTuple)) {
					listOfCoords = zoneTuple2listOfReceiverPointIds.get(zoneTuple);
				}
				listOfCoords.add(newId);
				zoneTuple2listOfReceiverPointIds.put(zoneTuple,listOfCoords);
			}
		}
		log.info("number of receiver points: "+receiverPointId2Coord.size());
		
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
	
	boolean removeUnusedReceiverPoints = true;
	private Map<Id,Coord> UnusedReceiverPointId2Coord = new HashMap<Id,Coord>();
	
	public void setActivityCoord2NearestReceiverPointId () {
		
		// Each activity coordinate is assigned to a receiver point, the coodinates might be identical
		
		// After assigning the activity coordinates to the receiver points,
		// all receiver Points to which not even one activity coordinate is assigned to
		// are removed from the map due to performance reasons
		// if boolean removeUnusedReceiverPoints == true
		// TODO: If a replanning of the activity locations is activated,
		// the removing of the receiver points is not possible.
		
		// TODO: for graphical presentation of a noise map (noise-immission-map),
		// a post-analysis is necessary then
		// or the unused receiver Points are added back for the final x iterations
		// x is dependent from the number of iterations used for the averaged analysis
		log.info("aa");
		List<Id> receiverPointsToRemove = new ArrayList<Id>();
		if(removeUnusedReceiverPoints == true) {
			for(Id id : receiverPointId2Coord.keySet()) {
				receiverPointsToRemove.add(id);
			}
		}
		log.info("bb");
		int xi = 0;
		
		for(Coord coord : allActivityCoords) {
			xi++;
			if(xi%10000 == 0) {
				log.info(xi);
			}
			// A pre-fixing of zones to consider would be helpful for reducing the computational time
			Id receiverPointId = this.getNearestReceiverPoint(coord);
			activityCoord2receiverPointId.put(coord, receiverPointId);
			
			if(removeUnusedReceiverPoints == true) {
//				log.info("xxx");
				if(receiverPointsToRemove.contains(receiverPointId)) {
					receiverPointsToRemove.remove(receiverPointId);
				} else {
					// receiver pointId already removed from list
				}
			}	
		}
//		log.info("cc");
		if(removeUnusedReceiverPoints == true) {
			for(Id id : receiverPointsToRemove) {
				UnusedReceiverPointId2Coord.put(id, receiverPointId2Coord.get(id));
				receiverPointId2Coord.remove(id);
			}
		}
		log.info("receiverPointId2Coord: "+receiverPointId2Coord.size());
//		log.info("dd");
		for(Id id : receiverPointId2Coord.keySet()) {
			coord2receiverPointId.put(receiverPointId2Coord.get(id), id);
		}
	}
	
	public void addingBackUnusedReceiverPoints() {
		if(removeUnusedReceiverPoints == true) {
			for(Id id : UnusedReceiverPointId2Coord.keySet()) {
				receiverPointId2Coord.put(id, UnusedReceiverPointId2Coord.get(id));
				coord2receiverPointId.put(receiverPointId2Coord.get(id), id);
			}
		}
	}
	
//	public void setInitialAssignment () {
//		for(Id personId : personId2coordsOfActivities.keySet()) {
//			
//			Coord coord = personId2coordsOfActivities.get(personId).peek();
//			
//			Id receiverPointId = activityCoord2NearestReceiverPointId.get(coord);
//			
//			int x = 0;
//			if(receiverPointId2initialAssignmentInt.containsKey(receiverPointId)) {
//				x = receiverPointId2initialAssignmentInt.get(receiverPointId) + 1;
//			} else {
//				x = 1;
//			}
//			receiverPointId2initialAssignmentInt.put(receiverPointId , x);
//		}
//	}
	
	public void setLinksToZones() {
		// set min and max x and y
		for (Id linkId : scenario.getNetwork().getLinks().keySet()){
			if((scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX())<xCoordMinLinkNodes) {
				xCoordMinLinkNodes = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX();
			}
			if((scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY())<yCoordMinLinkNodes) {
				yCoordMinLinkNodes = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY();
			}
			if((scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX())>xCoordMaxLinkNodes) {
				xCoordMaxLinkNodes = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX();
			}
			if((scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY())>yCoordMaxLinkNodes) {
				yCoordMaxLinkNodes = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY();
			}
		}
		
		for (Id linkId : scenario.getNetwork().getLinks().keySet()){
			double partLength = 0.;
			partLength = 2.*(Math.sqrt(Math.pow((1. - Math.sin(Math.PI/4.)),2) + Math.pow((1. - Math.sin(Math.PI/4.)),2)));
			partLength = partLength * relevantRadius;
			int parts = (int) ((scenario.getNetwork().getLinks().get(linkId).getLength())/(partLength));
			
			double fromX = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX();
			double fromY = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY();
			double toX = scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord().getX();
			double toY = scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord().getY();
			double vectorX = toX - fromX;
			double vectorY = toY - fromY;
			List<Coord> coords = new ArrayList<Coord>();
			coords.add(scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord());
			coords.add(scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord());
			
			for(int i = 1 ; i<parts ; i++) {
				double x = fromX + (i*((1./((double)parts))*vectorX));
				double y = fromY + (i*((1./((double)parts))*vectorY));
				Coord  coordTmp = new CoordImpl(x,y);
				coords.add(coordTmp);
			}
			
			List<Tuple<Integer,Integer>> relevantTuples = new ArrayList<Tuple<Integer,Integer>>();
			for(Coord coord : coords) {
				Tuple<Integer,Integer> tupleTmp = getZoneTupleForLinks(coord);
				if(!(relevantTuples.contains(tupleTmp))) {
					relevantTuples.add(tupleTmp);
				}
			}
			for(Tuple<Integer,Integer> tuple :relevantTuples) {
				if(zoneTuple2listOfLinkIds.containsKey(tuple)) {
					List<Id> linkIds = zoneTuple2listOfLinkIds.get(tuple);
					linkIds.add(linkId);
					zoneTuple2listOfLinkIds.put(tuple, linkIds);
				} else {
					List<Id> linkIds = new ArrayList<Id>();
					linkIds.add(linkId);
					zoneTuple2listOfLinkIds.put(tuple, linkIds);
				}
			}
		}
	}
	
	public Tuple<Integer,Integer> getZoneTupleForLinks(Coord coord) {
		 
		double xCoord = coord.getX();
		double yCoord = coord.getY();
		
		int xDirection = (int) ((xCoord-xCoordMinLinkNodes)/(relevantRadius/1.));	
		int yDirection = (int) ((yCoordMaxLinkNodes-yCoord)/relevantRadius/1.);
		
		Tuple<Integer,Integer> zoneDefinition = new Tuple<Integer, Integer>(xDirection, yDirection);
		return zoneDefinition;
	}
	
	public void setRelevantLinkIds() {
		
		setLinksToZones();
		
		// for faster computation, for noise immission-calculation, not all network links are considered.
		for(Id pointId : receiverPointId2Coord.keySet()) {
			double pointCoordX = receiverPointId2Coord.get(pointId).getX();
			double pointCoordY = receiverPointId2Coord.get(pointId).getY();
			List<Id> relevantLinkIds = new ArrayList<Id>();
			Map<Id,Double> relevantLinkIds2distance = new HashMap<Id, Double>();
			Map<Id,Double> relevantLinkIds2Drefl = new HashMap<Id, Double>();
			Map<Id,Double> relevantLinkIds2DbmDz = new HashMap<Id, Double>();
			Map<Id,Double> relevantLinkIds2Ds = new HashMap<Id, Double>();
			Map<Id,Double> relevantLinkIds2angleImmissionCorrection = new HashMap<Id, Double>();
		
			Tuple<Integer,Integer> tupleOfPointCoords = getZoneTupleForLinks(receiverPointId2Coord.get(pointId));
			
			List<Id> potentialLinks = new ArrayList<Id>();
			if(zoneTuple2listOfLinkIds.containsKey(tupleOfPointCoords)) {
				potentialLinks.addAll(zoneTuple2listOfLinkIds.get(tupleOfPointCoords));
			}
			int x = tupleOfPointCoords.getFirst();
			int y = tupleOfPointCoords.getSecond();
			Tuple<Integer,Integer> TupleNW = new Tuple<Integer, Integer>(x-1, y-1);
			if(zoneTuple2listOfLinkIds.containsKey(TupleNW)) {
				potentialLinks.addAll(zoneTuple2listOfLinkIds.get(TupleNW));
			}
			Tuple<Integer,Integer> TupleN = new Tuple<Integer, Integer>(x, y-1);
			if(zoneTuple2listOfLinkIds.containsKey(TupleN)) {
				potentialLinks.addAll(zoneTuple2listOfLinkIds.get(TupleN));
			}
			Tuple<Integer,Integer> TupleNO = new Tuple<Integer, Integer>(x+1, y-1);
			if(zoneTuple2listOfLinkIds.containsKey(TupleNO)) {
				potentialLinks.addAll(zoneTuple2listOfLinkIds.get(TupleNO));
			}
			Tuple<Integer,Integer> TupleW = new Tuple<Integer, Integer>(x-1, y);
			if(zoneTuple2listOfLinkIds.containsKey(TupleW)) {
				potentialLinks.addAll(zoneTuple2listOfLinkIds.get(TupleW));
			}
			Tuple<Integer,Integer> TupleO = new Tuple<Integer, Integer>(x+1, y);
			if(zoneTuple2listOfLinkIds.containsKey(TupleO)) {
				potentialLinks.addAll(zoneTuple2listOfLinkIds.get(TupleO));
			}
			Tuple<Integer,Integer> TupleSW = new Tuple<Integer, Integer>(x-1, y+1);
			if(zoneTuple2listOfLinkIds.containsKey(TupleSW)) {
				potentialLinks.addAll(zoneTuple2listOfLinkIds.get(TupleSW));
			}
			Tuple<Integer,Integer> TupleS = new Tuple<Integer, Integer>(x, y+1);
			if(zoneTuple2listOfLinkIds.containsKey(TupleS)) {
				potentialLinks.addAll(zoneTuple2listOfLinkIds.get(TupleS));
			}
			Tuple<Integer,Integer> TupleSO = new Tuple<Integer, Integer>(x+1, y+1);
			if(zoneTuple2listOfLinkIds.containsKey(TupleSO)) {
				potentialLinks.addAll(zoneTuple2listOfLinkIds.get(TupleSO));
			}
			
			for (Id linkId : potentialLinks){
				if(!(relevantLinkIds.contains(linkId))) {
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
						
						// the following calculations for the correction terms of the noise immission calculation
						double Drefl = calculateDreflection(receiverPointId2Coord.get(pointId), linkId);
						double DbmDz = calculateDbmDz(distance, receiverPointId2Coord.get(pointId));
						double Ds = calculateDs (distance);
						double angleImmissionCorrection = calculateAngleImmissionCorrection(receiverPointId2Coord.get(pointId), scenario.getNetwork().getLinks().get(linkId));
						relevantLinkIds2Drefl.put(linkId, Drefl);
						relevantLinkIds2DbmDz.put(linkId, DbmDz);
						relevantLinkIds2Ds.put(linkId, Ds);
						relevantLinkIds2angleImmissionCorrection.put(linkId, angleImmissionCorrection);
					}
				}
			}
			receiverPointId2relevantLinkIds.put(pointId, relevantLinkIds);
			receiverPointId2RelevantLinkIds2Distances.put(pointId,relevantLinkIds2distance);
			receiverPointId2RelevantLinkIds2Drefl.put(pointId,relevantLinkIds2Drefl);
			receiverPointId2RelevantLinkIds2DbmDz.put(pointId,relevantLinkIds2DbmDz);
			receiverPointId2RelevantLinkIds2Ds.put(pointId,relevantLinkIds2Ds);
			receiverPointId2RelevantLinkIds2AngleImmissionCorrection.put(pointId,relevantLinkIds2angleImmissionCorrection);
		}
		if (NoiseConfig.getRLSMethod().equals("parts")){
			getDistancesToPartsOfLink(scenario,receiverPointId2RelevantLinkIds2Distances);
		} else {
			// no additional computation
		}
	}
	
	public double calculateDreflection (Coord coord , Id linkId) {
		double Dreflection = 0.;	
		double densityValue = 0.;
		if(activityCoords2densityValue.containsKey(coord)) {
			densityValue = activityCoords2densityValue.get(coord);
		}
		
		double streetWidth = linkId2streetWidth.get(linkId);
		
		Dreflection = densityValue/streetWidth;
		
		if(Dreflection>3.2) {
			Dreflection = 3.2;
		}
		
		return Dreflection;
	}
	
	public double calculateDbmDz (double distanceToRoad , Coord coord) {
		double DbmDz = 0.;
		if(distanceToRoad==0.) {
			distanceToRoad = 0.00000001;
			// dividing by zero is not possible
		}
		
		double densityValue = 0.;
		
		if(activityCoords2densityValue.containsKey(coord)) {
			densityValue = activityCoords2densityValue.get(coord);
		}
		
		// D_BM is relevant if there are no buildings which provoke shielding effects
		// The height is chosen to be dependent from the activity locations density
//		double Dbm = -4.8* Math.exp((-1)*(Math.pow((10*(densityValue*0.01)/distanceToRoad)*(8.5+(100/distanceToRoad)),1.3)));
		double Dbm = -4.8* Math.exp((-1)*(Math.pow((((2+(densityValue*0.1)/distanceToRoad)*(8.5+(100/distanceToRoad)))),1.3)));
		
		double Dz = 0.;
		double z = (distanceToRoad/3)*(densityValue*0.01)/100;
		z = z - 1./30.;
		Dz = -10*Math.log10(3+60*z);
		
		if((Math.abs(Dbm))>(Math.abs(Dz))) {
			DbmDz = Dbm;
		} else {
			DbmDz = Dz;	
		}
		return DbmDz;
	}
	
	public double calculateDs (double distanceToRoad){
		double Ds = 0.;
		
		Ds = 15.8 - (10 * Math.log10(distanceToRoad)) - (0.0142*(Math.pow(distanceToRoad,0.9)));
//		Ds = 15.8 - (12*(distanceToRoad/400)) - (10 * Math.log10(distanceToRoad)) - (0.0142*(Math.pow(distanceToRoad,0.9)));
//		Ds = 15.8 + 2 - (12*(distanceToRoad/400)) - (10 * Math.log10(distanceToRoad)) - (0.0142*(Math.pow(distanceToRoad,0.9)));
		
		return Ds;
	}
	
	public static double calculateAngleImmissionCorrection(Coord coord, Link link) {
		
		double immissionCorrection = 0.;
		
		double angle = 0.;
		
		double lotPointX = 0.;
		double lotPointY = 0.;
		
		double pointCoordX = coord.getX();
		double pointCoordY = coord.getY();
		
		double fromCoordX = link.getFromNode().getCoord().getX();
		double fromCoordY = link.getFromNode().getCoord().getY();
		double toCoordX = link.getToNode().getCoord().getX();
		double toCoordY = link.getToNode().getCoord().getY();
		
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
//		double yAbschnitt2Original = pointCoordY - (pointCoordX*vector2);
		
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
		
		double angle1 = 0.;
		double angle2 = 0.;
		
		double kath1 = Math.abs(Math.sqrt(Math.pow(lotPointX - fromCoordX, 2) + Math.pow(lotPointY - fromCoordY, 2)));
		double hypo1 = Math.abs(Math.sqrt(Math.pow(pointCoordX - fromCoordX, 2) + Math.pow(pointCoordY - fromCoordY, 2)));
		double kath2 = Math.abs(Math.sqrt(Math.pow(lotPointX - toCoordX, 2) + Math.pow(lotPointY - toCoordY, 2)));
		double hypo2 = Math.abs(Math.sqrt(Math.pow(pointCoordX - toCoordX, 2) + Math.pow(pointCoordY - toCoordY, 2)));
		
		if(kath1==0) {
			kath1 = 0.0000001;
		}
		if(kath2==0) {
			kath2 = 0.0000001;
		}
		if(hypo1==0) {
			hypo1 = 0.0000001;
		}
		if(hypo2==0) {
			hypo2 = 0.0000001;
		}
		
		if(kath1>hypo1) {
			kath1 = hypo1;
		}
		if(kath2>hypo2) {
			kath2 = hypo2;
		}
		
		angle1 = Math.asin(kath1/hypo1);
		angle2 = Math.asin(kath2/hypo2);
		
		angle1 = Math.toDegrees(angle1);
		angle2 = Math.toDegrees(angle2);
		
		if((((fromCoordX<lotPointX)&&(toCoordX>lotPointX))||((fromCoordX>lotPointX)&&(toCoordX<lotPointX)))||(((fromCoordY<lotPointY)&&(toCoordY>lotPointY))||((fromCoordY>lotPointY)&&(toCoordY<lotPointY)))) {
			angle = Math.abs(angle1 + angle2);
		} else {
			angle = Math.abs(angle1 - angle2);
		}
		
		immissionCorrection = 10*Math.log10((angle)/(180));
		
		return immissionCorrection;
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
	
	public Tuple<Integer,Integer> getZoneTuple(Coord coord) {
		 
		double xCoord = coord.getX();
		double yCoord = coord.getY();
		
		int xDirection = (int) ((xCoord-xCoordMin)/(receiverPointGap/1.));	
		int yDirection = (int) ((yCoordMax-yCoord)/receiverPointGap/1.);
		
		Tuple<Integer,Integer> zoneDefinition = new Tuple<Integer, Integer>(xDirection, yDirection);
		return zoneDefinition;
	}
	
	public Id getNearestReceiverPoint (Coord coord) {
		Id nearestReceiverPointId = null;
//		
		double xCoord = coord.getX();
		double yCoord = coord.getY();
		double distance = Double.MAX_VALUE;
		
		List<Tuple<Integer,Integer>> tuples = new ArrayList<Tuple<Integer,Integer>>();
		Tuple<Integer,Integer> centralTuple = getZoneTuple(coord);
		tuples.add(centralTuple);
		int x = centralTuple.getFirst();
		int y = centralTuple.getSecond();
		Tuple<Integer,Integer> TupleNW = new Tuple<Integer, Integer>(x-1, y-1);
		tuples.add(TupleNW);
		Tuple<Integer,Integer> TupleN = new Tuple<Integer, Integer>(x, y-1);
		tuples.add(TupleN);
		Tuple<Integer,Integer> TupleNO = new Tuple<Integer, Integer>(x+1, y-1);
		tuples.add(TupleNO);
		Tuple<Integer,Integer> TupleW = new Tuple<Integer, Integer>(x-1, y);
		tuples.add(TupleW);
		Tuple<Integer,Integer> TupleO = new Tuple<Integer, Integer>(x+1, y);
		tuples.add(TupleO);
		Tuple<Integer,Integer> TupleSW = new Tuple<Integer, Integer>(x-1, y+1);
		tuples.add(TupleSW);
		Tuple<Integer,Integer> TupleS = new Tuple<Integer, Integer>(x, y+1);
		tuples.add(TupleS);
		Tuple<Integer,Integer> TupleSO = new Tuple<Integer, Integer>(x+1, y+1);
		tuples.add(TupleSO);
		
		Map<Id,Coord> relevantId2Coord = new HashMap<Id, Coord>();
		for(Tuple<Integer,Integer> tuple : tuples) {
			if(zoneTuple2listOfReceiverPointIds.containsKey(tuple)) {
				for(Id id : zoneTuple2listOfReceiverPointIds.get(tuple)) {
					Coord relevantCoord = receiverPointId2Coord.get(id);
					relevantId2Coord.put(id,relevantCoord);
				}
			}
		}
		
		for(Id receiverPointId : relevantId2Coord.keySet()) {
			double xValue = relevantId2Coord.get(receiverPointId).getX();
			double yValue = relevantId2Coord.get(receiverPointId).getY();
			
			double a = xCoord - xValue;
			double b = yCoord - yValue;
			
			double distanceTmp = Math.sqrt((Math.pow(a, 2))+(Math.pow(b, 2)));
			if(distanceTmp < distance) {
				// update the nearest receiver point
				distance = distanceTmp;
				nearestReceiverPointId = receiverPointId;
			} else {
			}
		}
		
		// The following version is previous/older version
		// The distance to all receiver points is compared
		// Much more computational time (the factor is depending on the scenario)
//		for(Id receiverPointId : receiverPointId2Coord.keySet()) {
//			double xValue = receiverPointId2Coord.get(receiverPointId).getX();
//			double yValue = receiverPointId2Coord.get(receiverPointId).getY();
//			
//			double a = xCoord - xValue;
//			double b = yCoord - yValue;
//			
//			double distanceTmp = Math.sqrt((Math.pow(a, 2))+(Math.pow(b, 2)));
//			if(distanceTmp < distance) {
//				// update the nearest receiver point
//				distance = distanceTmp;
//				nearestReceiverPointId = receiverPointId;
//			} else {
//			}
//		}
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
	
	public Map<Id, Map<Id, Double>> getReceiverPointId2RelevantLinkIds2Drefl() {
		return receiverPointId2RelevantLinkIds2Drefl;
	}
	
	public Map<Id, Map<Id, Double>> getReceiverPointId2RelevantLinkIds2DbmDz() {
		return receiverPointId2RelevantLinkIds2DbmDz;
	}
	
	public Map<Id, Map<Id, Double>> getReceiverPointId2RelevantLinkIds2Ds() {
		return receiverPointId2RelevantLinkIds2Ds;
	}
	
	public Map<Id, Map<Id, Double>> getReceiverPointId2RelevantLinkIds2AngleImmissionCorrection() {
		return receiverPointId2RelevantLinkIds2AngleImmissionCorrection;
	}
	
	public Map<Coord,Id> getCoord2receiverPointId() {
		return coord2receiverPointId;
	}
}
