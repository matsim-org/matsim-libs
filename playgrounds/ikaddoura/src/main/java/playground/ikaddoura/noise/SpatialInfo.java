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
	
//	private double receiverPointGap = 100.; // distance between two receiver points along x- and y-axes
//	private double relevantRadius = 500.; // radius around a receiver point in which all links are considered as relevant
	private double receiverPointGap = 250.; // distance between two receiver points along x- and y-axes
	private double relevantRadius = 500.; // radius around a receiver point in which all links are considered as relevant
	
	private Scenario scenario;
	private Map<Id,Coord> receiverPointId2Coord = new HashMap<Id,Coord>();
	private Map<Coord,Id> coord2receiverPointId = new HashMap<Coord,Id>();
	private Map<Tuple<Integer,Integer>,List<Id>> zoneTuple2listOfReceiverPointIds = new HashMap<Tuple<Integer,Integer>, List<Id>>();
	private Map<Tuple<Integer,Integer>,List<Coord>> zoneTuple2listOfActivityCoords = new HashMap<Tuple<Integer,Integer>, List<Coord>>();
	private Map<Tuple<Integer,Integer>,List<Id>> zoneTuple2listOfLinkIds = new HashMap<Tuple<Integer,Integer>, List<Id>>();
	
	private Map<Id,List<Id>> receiverPointId2relevantLinkIds = new HashMap<Id, List<Id>>();
	private Map<Id, Map<Id,Double>> receiverPointId2RelevantLinkIds2Distances = new HashMap<Id, Map<Id,Double>>();
	private Map<Id, Map<Id,Double>> receiverPointId2RelevantLinkIds2Drefl = new HashMap<Id, Map<Id,Double>>();
	private Map<Id, Map<Id,Double>> receiverPointId2RelevantLinkIds2DbmDz = new HashMap<Id, Map<Id,Double>>();
	private Map<Id, Map<Id,Double>> receiverPointId2RelevantLinkIds2Ds = new HashMap<Id, Map<Id,Double>>();
	private Map<Id, Map<Id, Map<Integer,Double>>> receiverPointId2RelevantLinkIds2PartOfLinks2DreflParts = new HashMap<Id, Map<Id,Map<Integer,Double>>>();
	private Map<Id, Map<Id, Map<Integer,Double>>> receiverPointId2RelevantLinkIds2PartOfLinks2DbmDzParts = new HashMap<Id, Map<Id,Map<Integer,Double>>>();
	private Map<Id, Map<Id, Map<Integer,Double>>> receiverPointId2RelevantLinkIds2PartOfLinks2DsParts = new HashMap<Id, Map<Id,Map<Integer,Double>>>();
	private Map<Id, Map<Id, Map<Integer,Double>>> receiverPointId2RelevantLinkIds2PartOfLinks2DlParts = new HashMap<Id, Map<Id,Map<Integer,Double>>>();

	private Map<Id, Map<Id,Double>> receiverPointId2RelevantLinkIds2AngleImmissionCorrection = new HashMap<Id, Map<Id,Double>>();
	private Map<Id, Map<Id,Double>> receiverPointId2RelevantLinkIds2AdditionalValue = new HashMap<Id, Map<Id,Double>>();
	private Map<Coord,Id> activityCoord2receiverPointId = new HashMap<Coord,Id>();
	
	private List <Coord> allActivityCoords = new ArrayList <Coord>();
	private Map <Coord,Double> activityCoords2densityValue = new HashMap<Coord, Double>();
	private Map<Id,Double> linkId2streetWidth = new HashMap<Id, Double>();
	
	private Map<Id, Queue<Coord>> personId2coordsOfActivities = new HashMap<Id, Queue<Coord>>();
	private Map<Id,List<Coord>> personId2listOfCoords = new HashMap<Id, List<Coord>>();
	private Map <Id,Integer> receiverPointId2initialAssignmentInt = new HashMap<Id, Integer>();
	
	private Map<Id , Coord> allReceiverPointIds2Coord = new HashMap<Id, Coord>();

	private List<Id> ListOfLinksWithOppositeLink = new ArrayList<Id>();
	
	// additional info required for (RLS-Teilstueckverfahren)
	private Map<Id, Map<Id,Double>> receiverPoint2RelevantlinkIds2lengthOfPartOfLinks = new HashMap<Id, Map<Id,Double>>();
	private Map<Id, Map<Id, Map<Integer, Double>>> receiverPointId2RelevantLinkIds2partOfLinks2distance = new HashMap<Id, Map<Id,Map<Integer,Double>>>();	
	
	public SpatialInfo(Scenario scenario) {
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
		for(Person person: scenario.getPopulation().getPersons().values()) {
	
			personId2coordsOfActivities.put(person.getId(), new LinkedList<Coord>());
			Map<Coord,String> tmpMapCoord2Activity = new HashMap<Coord, String>();
			
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
						
						if(tmpMapCoord2Activity.containsKey(currentActivity.getCoord())) {
							if(tmpMapCoord2Activity.get(currentActivity.getCoord())==currentActivity.getType().toString()) {
							} else {
								allActivityCoords.add(currentActivity.getCoord());
								tmpMapCoord2Activity.put(currentActivity.getCoord(), currentActivity.getType().toString());
							}
						}else {
							allActivityCoords.add(currentActivity.getCoord());
							tmpMapCoord2Activity.put(currentActivity.getCoord(), currentActivity.getType().toString());
						}
					}
				}
			}
		}
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
		
		for(Coord coord : allActivityCoords) {
			Tuple<Integer,Integer> zoneTuple = getZoneTupleDensityZones(coord);
			if(!(zoneTuple2listOfActivityCoords.containsKey(zoneTuple))) {
				List<Coord> activityCoords = new ArrayList<Coord>();
				activityCoords.add(coord);
				zoneTuple2listOfActivityCoords.put(zoneTuple, activityCoords);
			} else {
				List<Coord> activityCoords = zoneTuple2listOfActivityCoords.get(zoneTuple);
				activityCoords.add(coord);
				zoneTuple2listOfActivityCoords.put(zoneTuple, activityCoords);
			}
		}
	}
	
	public void setDensityAndStreetWidth() {
		
		for(double y = yCoordMax+100. ; y > yCoordMin-100.-316.225  ; y = y-316.225) {
			for(double x = xCoordMin-100. ; x < xCoordMax+100.+316.225  ; x = x+316.225) {
				double xMinTmp = x;
				double xMaxTmp = x+316.225;
				double yMaxTmp = y;
				double yMinTmp = y-316.225;
				
				double xCoord = x+(316.225/2.);
				double yCoord = y-(316.225/2.);
				Coord actCoord = new CoordImpl(xCoord,yCoord);
				
				List<Coord> listTmp = new ArrayList<Coord>();
				
				Tuple<Integer,Integer> zoneTupleActCoord = getZoneTupleDensityZones(actCoord);
				List<Coord> relevantCoords = new ArrayList<Coord>();
				if(zoneTuple2listOfActivityCoords.containsKey(zoneTupleActCoord)) {
					relevantCoords.addAll(zoneTuple2listOfActivityCoords.get(zoneTupleActCoord));
				}
				int xInt = zoneTupleActCoord.getFirst();
				int yInt = zoneTupleActCoord.getSecond();
				Tuple<Integer,Integer> TupleNW = new Tuple<Integer, Integer>(xInt-1, yInt-1);
				if(zoneTuple2listOfActivityCoords.containsKey(TupleNW)) {
					relevantCoords.addAll(zoneTuple2listOfActivityCoords.get(TupleNW));
				}
				Tuple<Integer,Integer> TupleN = new Tuple<Integer, Integer>(xInt, yInt-1);
				if(zoneTuple2listOfActivityCoords.containsKey(TupleN)) {
					relevantCoords.addAll(zoneTuple2listOfActivityCoords.get(TupleN));
				}
				Tuple<Integer,Integer> TupleNO = new Tuple<Integer, Integer>(xInt+1, yInt-1);
				if(zoneTuple2listOfActivityCoords.containsKey(TupleNO)) {
					relevantCoords.addAll(zoneTuple2listOfActivityCoords.get(TupleNO));
				}
				Tuple<Integer,Integer> TupleW = new Tuple<Integer, Integer>(xInt-1, yInt);
				if(zoneTuple2listOfActivityCoords.containsKey(TupleW)) {
					relevantCoords.addAll(zoneTuple2listOfActivityCoords.get(TupleW));
				}
				Tuple<Integer,Integer> TupleO = new Tuple<Integer, Integer>(xInt+1, yInt);
				if(zoneTuple2listOfActivityCoords.containsKey(TupleO)) {
					relevantCoords.addAll(zoneTuple2listOfActivityCoords.get(TupleO));
				}
				Tuple<Integer,Integer> TupleSW = new Tuple<Integer, Integer>(xInt-1, yInt+1);
				if(zoneTuple2listOfActivityCoords.containsKey(TupleSW)) {
					relevantCoords.addAll(zoneTuple2listOfActivityCoords.get(TupleSW));
				}
				Tuple<Integer,Integer> TupleS = new Tuple<Integer, Integer>(xInt, yInt+1);
				if(zoneTuple2listOfActivityCoords.containsKey(TupleS)) {
					relevantCoords.addAll(zoneTuple2listOfActivityCoords.get(TupleS));
				}
				Tuple<Integer,Integer> TupleSO = new Tuple<Integer, Integer>(xInt+1, yInt+1);
				if(zoneTuple2listOfActivityCoords.containsKey(TupleSO)) {
					relevantCoords.addAll(zoneTuple2listOfActivityCoords.get(TupleSO));
				}
				
				int counterCoords = 0;
				
				for(Coord coord : relevantCoords) {
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
				double densityValue = counter/25.;
					
//				if(densityValue>10.) {
//					log.info("densityValue: "+densityValue);
//					log.info(xCoord);
//					log.info(yCoord);
//				}
				// a value of 100 is high (high density of activity locations: 25000/square kilometre)
					
				for(Coord coordTmp : listTmp) {
					activityCoords2densityValue.put(coordTmp, densityValue);
				}
				for(Id coordId : this.receiverPointId2Coord.keySet()) {
					Coord receiverPointCoord = this.receiverPointId2Coord.get(coordId);
					if(!(activityCoords2densityValue.containsKey(coordId))) {
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
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
			double streetWidth = getStreetWidth(scenario,linkId);
			linkId2streetWidth.put(linkId, streetWidth);
		}
		
//TODO: Writing a csv-File...		
//		HashMap<Id,Double> id2xCoord = new HashMap<Id, Double>();
//		HashMap<Id,Double> id2yCoord = new HashMap<Id, Double>();
//		HashMap<Id,Double> id2density = new HashMap<Id, Double>();
//		for(Coord coord : activityCoords2densityValue.keySet()) {
//			Id id = Id.create(coord.toString());
//			double x = coord.getX();
//			double y = coord.getY();
//			double density = activityCoords2densityValue.get(coord);
//			id2xCoord.put(id,x);
//			id2yCoord.put(id,y);
//			id2density.put(id,density);
//		}
//		List<String> headers = new ArrayList<String>();
//		headers.add("actCoordId");
//		headers.add("xCoord");
//		headers.add("yCoord");
//		headers.add("densityValue");
//		
//		List<HashMap<Id,Double>> values = new ArrayList<HashMap<Id,Double>>();
//		values.add(id2xCoord);
//		values.add(id2yCoord);
//		values.add(id2density);
//		LKCsvWriterId2DoubleMaps writer = new LKCsvWriterId2DoubleMaps("/Users/Lars/Desktop/VERSUCH/densityZonesSiouxFalls.csv", 4, headers, values);
//		writer.write();
	}
	
	public void setReceiverPoints() {
		
		// for a 
		HashMap<Id,Double> pointId2counter = new HashMap<Id, Double>();

		int counter = 0;
		
//		//TODO: First possibility: a grid of receiver points
//		for(double y = yCoordMax + 100. ; y > yCoordMin - 100. - receiverPointGap ; y = y - receiverPointGap) {
//			for(double x = xCoordMin - 100. ; x < xCoordMax + 100. + receiverPointGap ; x = x + receiverPointGap) {
//				Coord newCoord = new CoordImpl(x, y);
//				Id newId = Id.create("coordId"+counter);
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
//			}
//		}
		
		//TODO: Optional, the receiver points can be set on basis of the activity coords
		for(Coord actCoord : allActivityCoords) {
			if(counter%20000. == 0.) {
				log.info("activity coords ... "+counter);
			}
			Tuple<Integer,Integer> zoneTupleActCoord = getZoneTuple(actCoord);
			
//			List<Tuple<Integer,Integer>> tuples = new ArrayList<Tuple<Integer,Integer>>();
			List<Id> relevantReceiverPointIds = new ArrayList<Id>();
			if(zoneTuple2listOfReceiverPointIds.containsKey(zoneTupleActCoord)) {
				relevantReceiverPointIds.addAll(zoneTuple2listOfReceiverPointIds.get(zoneTupleActCoord));
			}
			
			int x = zoneTupleActCoord.getFirst();
			int y = zoneTupleActCoord.getSecond();
			Tuple<Integer,Integer> TupleNW = new Tuple<Integer, Integer>(x-1, y-1);
			if(zoneTuple2listOfReceiverPointIds.containsKey(TupleNW)) {
				relevantReceiverPointIds.addAll(zoneTuple2listOfReceiverPointIds.get(TupleNW));
			}
			Tuple<Integer,Integer> TupleN = new Tuple<Integer, Integer>(x, y-1);
			if(zoneTuple2listOfReceiverPointIds.containsKey(TupleN)) {
				relevantReceiverPointIds.addAll(zoneTuple2listOfReceiverPointIds.get(TupleN));
			}
			Tuple<Integer,Integer> TupleNO = new Tuple<Integer, Integer>(x+1, y-1);
			if(zoneTuple2listOfReceiverPointIds.containsKey(TupleNO)) {
				relevantReceiverPointIds.addAll(zoneTuple2listOfReceiverPointIds.get(TupleNO));
			}
			Tuple<Integer,Integer> TupleW = new Tuple<Integer, Integer>(x-1, y);
			if(zoneTuple2listOfReceiverPointIds.containsKey(TupleW)) {
				relevantReceiverPointIds.addAll(zoneTuple2listOfReceiverPointIds.get(TupleW));
			}
			Tuple<Integer,Integer> TupleO = new Tuple<Integer, Integer>(x+1, y);
			if(zoneTuple2listOfReceiverPointIds.containsKey(TupleO)) {
				relevantReceiverPointIds.addAll(zoneTuple2listOfReceiverPointIds.get(TupleO));
			}
			Tuple<Integer,Integer> TupleSW = new Tuple<Integer, Integer>(x-1, y+1);
			if(zoneTuple2listOfReceiverPointIds.containsKey(TupleSW)) {
				relevantReceiverPointIds.addAll(zoneTuple2listOfReceiverPointIds.get(TupleSW));
			}
			Tuple<Integer,Integer> TupleS = new Tuple<Integer, Integer>(x, y+1);
			if(zoneTuple2listOfReceiverPointIds.containsKey(TupleS)) {
				relevantReceiverPointIds.addAll(zoneTuple2listOfReceiverPointIds.get(TupleS));
			}
			Tuple<Integer,Integer> TupleSO = new Tuple<Integer, Integer>(x+1, y+1);
			if(zoneTuple2listOfReceiverPointIds.containsKey(TupleSO)) {
				relevantReceiverPointIds.addAll(zoneTuple2listOfReceiverPointIds.get(TupleSO));
			}
			if(relevantReceiverPointIds.size()==0) {
				Coord newCoord = new CoordImpl(actCoord.getX(), actCoord.getY());
				Id<Coord> newId = Id.create("coordId"+counter, Coord.class);
				receiverPointId2Coord.put(newId, newCoord);
				counter++;
				
				Tuple<Integer,Integer> zoneTuple = getZoneTuple(newCoord);
				List<Id> listOfCoords = new ArrayList<Id>();
				if(zoneTuple2listOfReceiverPointIds.containsKey(zoneTuple)) {
					listOfCoords = zoneTuple2listOfReceiverPointIds.get(zoneTuple);
				}
				listOfCoords.add(newId);
				zoneTuple2listOfReceiverPointIds.put(zoneTuple,listOfCoords);
			}else{
			
				boolean newCoordNecessary = true;
				for(Id id : relevantReceiverPointIds) {

					double distance = 0.;
					double x1 = actCoord.getX();
					double x2 = receiverPointId2Coord.get(id).getX();
					double y1 = actCoord.getY();
					double y2 = receiverPointId2Coord.get(id).getY();
					
					distance = Math.sqrt((Math.pow(x2-x1, 2))+(Math.pow(y2-y1, 2)));
					
					if(distance <= 25.) {
						newCoordNecessary = false;
						if(pointId2counter.containsKey(id)) {
							pointId2counter.put(id, pointId2counter.get(id)+1.0);
						} else {
							pointId2counter.put(id, 1.0);
						}
					}
				}
				
				if (newCoordNecessary == true) {
					Coord newCoord = new CoordImpl(actCoord.getX(), actCoord.getY());
					Id<Coord> newId = Id.create("coordId"+counter, Coord.class);
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
		}
		log.info("number of receiver points: "+receiverPointId2Coord.size());
		HashMap<Id,Double> id2xCoord = new HashMap<Id, Double>();
		HashMap<Id,Double> id2yCoord = new HashMap<Id, Double>();
		int c = 0;
		for(Id id : receiverPointId2Coord.keySet()) {
			c++;
			if(c%1000 == 0) {
				log.info("receiver points "+c);
			}
			id2xCoord.put(id, receiverPointId2Coord.get(id).getX());
			id2yCoord.put(id, receiverPointId2Coord.get(id).getY());
		}
		List<String> headers = new ArrayList<String>();
		headers.add("pointId");
		headers.add("xCoord");
		headers.add("yCoord");
		headers.add("counter");
		for(Id id : id2xCoord.keySet()) {
			if(!(pointId2counter.containsKey(id))) {
				pointId2counter.put(id,0.);
			}
		}
		List<HashMap<Id,Double>> values = new ArrayList<HashMap<Id,Double>>();
		values.add(id2xCoord);
		values.add(id2yCoord);
		values.add(pointId2counter);
//		LKCsvWriterId2DoubleMaps writer = new LKCsvWriterId2DoubleMaps("/Users/Lars/Desktop/VERSUCH/BerlinCoords90.csv", 4, headers, values);
//		LKCsvWriterId2DoubleMaps writer = new LKCsvWriterId2DoubleMaps("/Users/Lars/Desktop/VERSUCH/Sioux250.csv", 4, headers, values);
		LKCsvWriterId2DoubleMaps writer = new LKCsvWriterId2DoubleMaps(scenario.getConfig().controler().getOutputDirectory()+"/receiverPoints/receiverPoints.csv", 4, headers, values);
		writer.write();
		// For a post-analysis of the events-file the real locations of the ceiver points should be known!
	}
	
//	//TODO: Alternatively, a grid is used
//	public double setActivityDensityValueFromRadius (Scenario scenario , Coord coord) {
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
		List<Id> receiverPointsToRemove = new ArrayList<Id>();
		if(removeUnusedReceiverPoints == true) {
			for(Id id : receiverPointId2Coord.keySet()) {
				receiverPointsToRemove.add(id);
			}
		}
		int xi = 0;
		
		for(Coord coord : allActivityCoords) {
			xi++;
			if(xi%20000 == 0) {
				log.info("activity coordinates "+xi+" ...");
			}
			
			if (!(activityCoord2receiverPointId.containsKey(coord))) {
			
				// A pre-fixing of zones to consider would be helpful for reducing the computational time
				Id receiverPointId = this.getNearestReceiverPoint(coord);
				
				activityCoord2receiverPointId.put(coord, receiverPointId);
				
				if(removeUnusedReceiverPoints == true) {
					if(receiverPointsToRemove.contains(receiverPointId)) {
						receiverPointsToRemove.remove(receiverPointId);
					} else {
						// receiver pointId already removed from list
					}
				}
			} else {
			}
		}
		if(removeUnusedReceiverPoints == true) {
			for(Id id : receiverPointsToRemove) {
				xi++;
				if(xi%20000 == 0) {
					log.info("receiver point "+xi+" ...");
				}
				UnusedReceiverPointId2Coord.put(id, receiverPointId2Coord.get(id));
				receiverPointId2Coord.remove(id);
			}
		}
		log.info("number of receiver points: "+receiverPointId2Coord.size());
		for(Id id : receiverPointId2Coord.keySet()) {
			xi++;
			if(xi%20000 == 0) {
				log.info("receiver point "+xi+" ...");
			}
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
				double x = fromX + (i*((1./(parts))*vectorX));
				double y = fromY + (i*((1./(parts))*vectorY));
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
		
		setListLinksWithOppositeLink();
		
		int linknumber = 0;
		
		// for faster computation, for noise immission-calculation, not all network links are considered.
		for(Id pointId : receiverPointId2Coord.keySet()) {

			linknumber++;
			if(linknumber%1000. == 0.) {
				log.info("receiver point ... "+linknumber);
			}
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
					
					//TODO: The reason for the following adaption depends on the scenario,
					//for the Sioux-Falls-scenario, it is relevant
					// activityCoords are set in the centre point of the buildings,
					// the relevant noise measurement has to be done on the facade.
//					distance = distance - (5. + (Math.random()*5.));
					distance = distance - 7.5;
					
					// in direction or in opposite direction or a one-way-street
					boolean oneWayStreet = false;
					if(!(ListOfLinksWithOppositeLink.contains(linkId))) {
						oneWayStreet = true;
					}
					
					boolean oppositeDirection = true;
					if(oneWayStreet == false) {
						oppositeDirection = true;
						
						double lotVectorX = lotPointX - pointCoordX;
						double lotVectorY = lotPointY - pointCoordY;
						double linkVectorX = toCoordX - fromCoordX;
						double linkVectorY = toCoordY - fromCoordY;
						
						if(lotVectorX<0.){
							if(lotVectorY<0.) {
								if(linkVectorX<0.) {
									oppositeDirection = false;
								}
							} else if(lotVectorY>0.) {
								if(linkVectorX>0.) {
									oppositeDirection = false;
								}
							} else if(lotVectorY==0.) {
								if((linkVectorX==0.)&&(linkVectorY>0.)) {
									oppositeDirection = false;
								}
							} 
						} else if (lotVectorX>0.){
							if(lotVectorY<0.) {
								if(linkVectorX<0.) {
									oppositeDirection = false;
								}
							} else if(lotVectorY>0.) {
								if(linkVectorX>0.) {
									oppositeDirection = false;
								}
							} else if(lotVectorY==0.) {
								if((linkVectorX==0.)&&(linkVectorY<0.)) {
									oppositeDirection = false;
								}
							}
						} else if (lotVectorX==0.){
							if(lotVectorY<0.) {
								if(linkVectorX<0.) {
									oppositeDirection = false;
								}
							} else if(lotVectorY>0.) {
								if(linkVectorX>0.) {
									oppositeDirection = false;
								}
							} 
						}
						
						if(oppositeDirection == true) {
							distance = distance + (0.3*(getStreetWidth(scenario, linkId)));
						} else {
							distance = distance - (0.3*(getStreetWidth(scenario, linkId)));
						}
					}
					
					if(distance < relevantRadius){
						
						relevantLinkIds.add(linkId);
						
						double minDistance = (getStreetWidth(scenario, linkId))/3;
						
						double additionalValue = 0.;
						if(oneWayStreet == true) {
							additionalValue = (linkId2streetWidth.get(linkId)/6.);
						} else if(oppositeDirection == true) {
							additionalValue = (linkId2streetWidth.get(linkId)/3.);
						}
						
						if(receiverPointId2RelevantLinkIds2AdditionalValue.containsKey(pointId)) {
							Map<Id,Double> relevantLinkIds2AdditionalValue = receiverPointId2RelevantLinkIds2AdditionalValue.get(pointId);
							relevantLinkIds2AdditionalValue.put(linkId, additionalValue);
							receiverPointId2RelevantLinkIds2AdditionalValue.put(pointId, relevantLinkIds2AdditionalValue);
						} else {
							Map<Id,Double> relevantLinkIds2AdditionalValue = new HashMap<Id, Double>();
							relevantLinkIds2AdditionalValue.put(linkId, additionalValue);
							receiverPointId2RelevantLinkIds2AdditionalValue.put(pointId, relevantLinkIds2AdditionalValue);
						}
						
						if(distance<(minDistance+additionalValue)) {
							distance = (minDistance+additionalValue);
						}
						
						String direction = null;
						
						if(oneWayStreet==true) {
							direction = "oneWay";
						} else if(oppositeDirection==true) {
							direction = "oppositeDirection";
						} else {
							direction = "inDirection";
						}
						
						relevantLinkIds2distance.put(linkId, distance);
						
						// the following calculations for the correction terms of the noise immission calculation
						double Drefl = calculateDreflection(receiverPointId2Coord.get(pointId), linkId);
						double DbmDz = calculateDbmDz(distance, receiverPointId2Coord.get(pointId), pointId, linkId, direction);
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
			getCorrectionTermsForPartsOfLink(scenario,receiverPointId2RelevantLinkIds2Distances);
		} else {
			// no additional computation
		}
	}
	
	public void setListLinksWithOppositeLink() {
		for(Id linkId : scenario.getNetwork().getLinks().keySet()) {
			Coord fromCoord = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord();
			Coord toCoord = scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord();
			
			Tuple<Integer,Integer> tupleOfPointCoords = getZoneTupleForLinks(fromCoord);
			
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
			
			for(Id possibleOppositeLinkId : potentialLinks) {
				Coord fromCoord2 = scenario.getNetwork().getLinks().get(possibleOppositeLinkId).getFromNode().getCoord();
				Coord toCoord2 = scenario.getNetwork().getLinks().get(possibleOppositeLinkId).getToNode().getCoord();
				if((fromCoord == toCoord2)&&(toCoord==fromCoord2)) {
					ListOfLinksWithOppositeLink.add(linkId);
				}
			}
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
		
		Dreflection = Dreflection*1.5;
		
		// For the consideration of the singualar-reflection-effects,
		// in dependence of the streetWdith, the height of the buildings
		// and the structure (in particular the gaps between the buildings),
		// and also the distance to the emission-source
		// an additional effect of 0-3 dB(A) ispossible,
		// effectively much smaller than 3 dB(A).
		// Therefore the reflection-effect calculated for the multiple reflection effects is multiplied by 1.5
		
		// Potential absorbing properties of the buildings are not considered here
		
		return Dreflection;
	}
	
	public double calculateDbmDz (double distanceToRoad , Coord coord, Id coordId, Id linkId, String direction) {
		double DbmDz = 0.;
		if(distanceToRoad==0.) {
			distanceToRoad = 0.00000001;
			// dividing by zero is not possible
		}

		double densityValue = 0.;
		if(activityCoords2densityValue.containsKey(coord)) {
			densityValue = activityCoords2densityValue.get(coord);
		}
//		double averagedHeight = 1. + ((densityValue*0.1)*(Math.random()*2.));
		double averagedHeight = 4.; // For the noise immission calculation the receiver points shall be set in a height of 4 metres.
		double s = Math.sqrt((Math.pow(distanceToRoad, 2))+(Math.pow(averagedHeight, 2)));
		//Kw between (0.95 and 1.0 for high and low densities, approximated)
		double Kw = 1.0 - (0.05*(densityValue/100.));
		double z = 0.;
		// structure of buildings is necessary to calculate		}
//		if(distanceToRoad<=20.) {
//			z = 0.;
//		} else if (distanceToRoad<=119.) {
//			z = ((Math.log10(distanceToRoad-19.))/2.) * (((Math.sqrt(densityValue))*10.)/100.) * 40.;
//		} else {
//			z = ((Math.log10(100.))/2.) * (((Math.sqrt(densityValue))*10.)/100.) * 40.;
//		}
		double additionalValue = receiverPointId2RelevantLinkIds2AdditionalValue.get(coordId).get(linkId); 
		
		if(distanceToRoad<=(30.+additionalValue)) {
			z = 0.;
		} else if (distanceToRoad<=(129.+additionalValue)) {
//			z = (Math.sqrt(Math.random())) * ((Math.log10(distanceToRoad-(29.+additionalValue)))/2.) * (((Math.sqrt(densityValue))*10.)/100.) * 40.;
			z = ((Math.log10(distanceToRoad-(29.+additionalValue)))/2.) * (((Math.sqrt(densityValue))*10.)/100.) * 40.;
		} else {
//			z = (Math.sqrt(Math.random())) * ((Math.random() * ((Math.log10(100.))/2.) * (((Math.sqrt(densityValue))*10.)/100.) * 40.));
			z = ((Math.log10(100.))/2.) * (((Math.sqrt(densityValue))*10.)/100.) * 40.;
		}
		
		// D_BM is relevant if there are no buildings which provoke shielding effects
//		double Dbm = -4.8* Math.exp((-1)*(Math.pow((10*(densityValue*0.01)/distanceToRoad)*(8.5+(100/distanceToRoad)),1.3)));
//		double Dbm = -4.8* Math.exp((-1)*(Math.pow(((2+(densityValue*0.1)/distanceToRoad)*(8.5+(100/distanceToRoad))),1.3)));
		double Dbm = -4.8* Math.exp((-1)*(Math.pow((((2+(averagedHeight))/distanceToRoad)*(8.5+(100./distanceToRoad))),1.3)));
//		log.info("");log.info("++++++++++++++");
//		log.info("Dbm: "+Dbm);
		double Dz = 0.;
		double z2 = (distanceToRoad/3)*(densityValue*0.01)/100;
//		z = z - 1./30.;
//		Dz = -10*Math.log10(3+(60*z));
		
		if(distanceToRoad<=(30.+additionalValue)) {
			Dz = 0.;
		} else {
			Dz = -7 * Math.log10(5+(((70+(0.25*s))/(1+(0.2*z)))*z*Kw));
		}
		
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
	
	public void getCorrectionTermsForPartsOfLink (Scenario scenario , Map<Id, Map<Id,Double>> receiverpointId2RelevantlinkIds2Distances) {
		
		for(Id pointId : receiverPointId2RelevantLinkIds2Distances.keySet()) {
		
			Map<Id,Map<Integer,Double>> relevantLinkIds2partOfLinks2distance = new HashMap<Id, Map<Integer,Double>>();
			Map<Id,Double> relevantLinkIds2lengthOfPartOfLinks = new HashMap<Id, Double>();
			Map<Id,Map<Integer,Double>> relevantLinkIds2PartOfLinks2DreflParts = new HashMap<Id, Map<Integer,Double>>();
			Map<Id,Map<Integer,Double>> relevantLinkIds2PartOfLinks2DbmDzParts = new HashMap<Id, Map<Integer,Double>>();
			Map<Id,Map<Integer,Double>> relevantLinkIds2PartOfLinks2DsParts = new HashMap<Id, Map<Integer,Double>>();
			Map<Id,Map<Integer,Double>> relevantLinkIds2PartOfLinks2DlParts = new HashMap<Id, Map<Integer,Double>>();
			
			for(Id linkId : receiverPointId2RelevantLinkIds2Distances.get(pointId).keySet()) {
				
//				double shortestDistance = receiverPointId2RelevantLinkIds2Distances.get(pointId).get(linkId);
				double geoLinkLength = Math.sqrt((Math.pow(scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX() - scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord().getX(), 2))+(Math.pow(scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY() - scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord().getY(), 2))); 
//				double maxPartLinkLength = shortestDistance/4;
				double maxPartLinkLength = 5.;
				
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
				Map<Integer,Double> partOfLinks2DreflParts = new HashMap<Integer, Double>();
				Map<Integer,Double> partOfLinks2DbmDzParts = new HashMap<Integer, Double>();
				Map<Integer,Double> partOfLinks2DsParts = new HashMap<Integer, Double>();
				Map<Integer,Double> partOfLinks2DlParts = new HashMap<Integer, Double>();
				
				for(int i = 0 ; i < numberOfParts ; i++) {
					double relevantX = fromCoordX + (i+0.5)*vectorX;
					double relevantY = fromCoordY + (i+0.5)*vectorY;
					
					double distance = Math.sqrt((Math.pow(xValue - relevantX, 2))+(Math.pow(yValue - relevantY, 2)));
				
					partOfLink2distance.put(i, distance);
					
					//+++++++++++++++++++++++++++++++++++++
					//calculate partOfLinks2DreflParts
					
					double Dreflection = 0.;	
					double densityValue = 0.;
					if(activityCoords2densityValue.containsKey(receiverPointId2Coord.get(pointId))) {
						densityValue = activityCoords2densityValue.get(receiverPointId2Coord.get(pointId));
					}
					
					double streetWidth = linkId2streetWidth.get(linkId);
					
					Dreflection = densityValue/streetWidth;
					
					if(Dreflection>3.2) {
						Dreflection = 3.2;
					}
					
					Dreflection = Dreflection*1.5;
					
					// For the consideration of the singualar-reflection-effects,
					// in dependence of the streetWdith, the height of the buildings
					// and the structure (in particular the gaps between the buildings),
					// and also the distance to the emission-source
					// an additional effect of 0-3 dB(A) ispossible,
					// effectively much smaller than 3 dB(A).
					// Therefore the reflection-effect calculated for the multiple reflection effects is multiplied by 1.5
					
					// Potential absorbing properties of the buildings are not considered here
					
					partOfLinks2DreflParts.put(i,Dreflection);
					
					//+++++++++++++++++++++++++++++++++++++
					//calculate partOfLinks2DbmDzParts
					
					double Dbm = 0.;
					
					Dbm = 4.8 - ((2.25/distance)*(34 + (600/distance)));
					
					if(Dbm<0.) {
						Dbm=0.;
					}
					
					double Dz = 0.;
					
					double z = 0.;
					
					if(distance<=(30.+receiverPointId2RelevantLinkIds2AdditionalValue.get(pointId).get(linkId))) {
//						log.info("111");
						z = 0.;
					} else if (distance<=(130.+receiverPointId2RelevantLinkIds2AdditionalValue.get(pointId).get(linkId))) {
//						log.info("222");
//						z = (Math.sqrt(Math.random())) * ((Math.log10(distance-(29.+receiverPointId2RelevantLinkIds2AdditionalValue.get(pointId).get(linkId))))/2.) * (4. + 26.*(activityCoords2densityValue.get(receiverPointId2Coord.get(pointId))/100.));
						z = ((Math.log10(distance-(29.+receiverPointId2RelevantLinkIds2AdditionalValue.get(pointId).get(linkId))))/2.) * (4. + 26.*(activityCoords2densityValue.get(receiverPointId2Coord.get(pointId))/100.));
					} else {
//						log.info("333");
//						log.info(pointId);
//						log.info(activityCoords2densityValue);
//						log.info(activityCoords2densityValue.get(pointId));
//						z = (Math.sqrt(Math.random())) * (4. + 26.*(activityCoords2densityValue.get(receiverPointId2Coord.get(pointId))/100.));
						z = (4. + 26.*(activityCoords2densityValue.get(receiverPointId2Coord.get(pointId))/100.));
					}
					
					// for the correct calculation of the height correction
//					double gamma = 1000.;
//					if(distanceToRoad>125.) {
//						gamma = distanceToRoad*8.;
//					}
					
					if(distance<(30.+receiverPointId2RelevantLinkIds2AdditionalValue.get(pointId).get(linkId))) {
						Dz = 0.;
					} else {
						Dz = 10*(Math.log10(3+(60*z)));
					}
					
					double DbmDz = 0.;
					
					if((Math.abs(Dbm))>(Math.abs(Dz))) {
						DbmDz = Dbm;
					} else {
						DbmDz = Dz;
					}
					
					partOfLinks2DbmDzParts.put(i, DbmDz);
					
					//+++++++++++++++++++++++++++++++++++++
					//calculate partOfLinks2DsParts
					
					double Ds = 0.;
					
					Ds = (20*Math.log10(distance)) + (distance/200.) - 11.2;
					
					partOfLinks2DsParts.put(i,Ds);
					
					//+++++++++++++++++++++++++++++++++++++
					//calculate partOfLinks2DlParts
					
					double Dl = 0.;
					
					Dl = 10*(Math.log10(lengthOfParts));
					
					partOfLinks2DlParts.put(i,Dl);
				}
				relevantLinkIds2partOfLinks2distance.put(linkId, partOfLink2distance);
				relevantLinkIds2PartOfLinks2DreflParts.put(linkId,partOfLinks2DreflParts);
				relevantLinkIds2PartOfLinks2DbmDzParts.put(linkId,partOfLinks2DbmDzParts);
				relevantLinkIds2PartOfLinks2DsParts.put(linkId,partOfLinks2DsParts);
				relevantLinkIds2PartOfLinks2DlParts.put(linkId,partOfLinks2DlParts);
			}		
			receiverPointId2RelevantLinkIds2PartOfLinks2DreflParts.put(pointId, relevantLinkIds2PartOfLinks2DreflParts);
			receiverPointId2RelevantLinkIds2PartOfLinks2DbmDzParts.put(pointId, relevantLinkIds2PartOfLinks2DbmDzParts);
			receiverPointId2RelevantLinkIds2PartOfLinks2DsParts.put(pointId, relevantLinkIds2PartOfLinks2DsParts);
			receiverPointId2RelevantLinkIds2PartOfLinks2DlParts.put(pointId, relevantLinkIds2PartOfLinks2DlParts);
			receiverPointId2RelevantLinkIds2partOfLinks2distance.put(pointId, relevantLinkIds2partOfLinks2distance);
			receiverPoint2RelevantlinkIds2lengthOfPartOfLinks.put(pointId, relevantLinkIds2lengthOfPartOfLinks);

		}
	}
	
	public Tuple<Integer,Integer> getZoneTuple(Coord coord) {
		 
		double xCoord = coord.getX();
		double yCoord = coord.getY();
		
		int xDirection = (int) ((xCoord-xCoordMin)/(receiverPointGap/1.));	
		int yDirection = (int) ((yCoordMax-yCoord)/receiverPointGap/1.);
		
		Tuple<Integer,Integer> zoneDefinition = new Tuple<Integer, Integer>(xDirection, yDirection);
		return zoneDefinition;
	}
	
	public Tuple<Integer,Integer> getZoneTupleDensityZones(Coord coord) {
		
		double xCoord = coord.getX();
		double yCoord = coord.getY();
		
		int xDirection = (int) ((xCoord-xCoordMin)/(400./1.));	
		int yDirection = (int) ((yCoordMax-yCoord)/(400./1.));
		
		Tuple<Integer,Integer> zoneDefinition = new Tuple<Integer, Integer>(xDirection, yDirection);
		
		return zoneDefinition;
	}
	
	public Id getNearestReceiverPoint (Coord coord) {
		Id nearestReceiverPointId = null;

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
	
	public Map<Id,Map<Id,Double>> getReceiverPointId2RelevantLinkIds2AdditionalValue() {
		return receiverPointId2RelevantLinkIds2AdditionalValue;
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
	
	public Map<Id, Map<Id, Map<Integer,Double>>> getReceiverPointId2RelevantLinkIds2PartOfLinks2DreflParts() {
		return receiverPointId2RelevantLinkIds2PartOfLinks2DreflParts;
	}
	
	public Map<Id, Map<Id, Double>> getReceiverPointId2RelevantLinkIds2DbmDz() {
		return receiverPointId2RelevantLinkIds2DbmDz;
	}
	
	public Map<Id, Map<Id, Map<Integer,Double>>> getReceiverPointId2RelevantLinkIds2PartOfLinks2DbmDzParts() {
		return receiverPointId2RelevantLinkIds2PartOfLinks2DbmDzParts;
	}
	
	public Map<Id, Map<Id, Double>> getReceiverPointId2RelevantLinkIds2Ds() {
		return receiverPointId2RelevantLinkIds2Ds;
	}
	
	public Map<Id, Map<Id, Map<Integer,Double>>> getReceiverPointId2RelevantLinkIds2PartOfLinks2DsParts() {
		return receiverPointId2RelevantLinkIds2PartOfLinks2DsParts;
	}
	
	public Map<Id, Map<Id, Map<Integer,Double>>> getReceiverPointId2RelevantLinkIds2PartOfLinks2DlParts() {
		return receiverPointId2RelevantLinkIds2PartOfLinks2DlParts;
	}
	
	public Map<Id, Map<Id, Double>> getReceiverPointId2RelevantLinkIds2AngleImmissionCorrection() {
		return receiverPointId2RelevantLinkIds2AngleImmissionCorrection;
	}
	
	public Map<Coord,Id> getCoord2receiverPointId() {
		return coord2receiverPointId;
	}
}
