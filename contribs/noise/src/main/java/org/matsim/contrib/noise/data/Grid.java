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
package org.matsim.contrib.noise.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.PtConstants;

/**
 * Computes a grid of receiver points and provides some basic spatial functionality,
 * e.g. the nearest receiver point for the coordinates of each 'considered' activity type. 
 * 
 * @author lkroeger, ikaddoura
 *
 */
public class Grid {
	
	private static final Logger log = Logger.getLogger(Grid.class);
			
	private final Scenario scenario;
	private final NoiseConfigGroup noiseParams;
		
	private final Map<Id<Person>, List<Coord>> personId2consideredActivityCoords = new HashMap<Id<Person>, List<Coord>>();
	
	private final List <Coord> consideredActivityCoordsForSpatialFunctionality = new ArrayList <Coord>();
	private final List <Coord> consideredActivityCoordsForReceiverPointGrid = new ArrayList <Coord>();
	
	private final List<String> consideredActivitiesForSpatialFunctionality = new ArrayList<String>();
	private final List<String> consideredActivitiesForReceiverPointGrid = new ArrayList<String>();
	
	private double xCoordMin = Double.MAX_VALUE;
	private double xCoordMax = Double.MIN_VALUE;
	private double yCoordMin = Double.MAX_VALUE;
	private double yCoordMax = Double.MIN_VALUE;
	
	private final Map<Tuple<Integer,Integer>,List<Id<ReceiverPoint>>> zoneTuple2listOfReceiverPointIds = new HashMap<Tuple<Integer, Integer>, List<Id<ReceiverPoint>>>();
	private final Map<Coord,Id<ReceiverPoint>> activityCoord2receiverPointId = new HashMap<Coord, Id<ReceiverPoint>>();
	
	private final Map<Id<ReceiverPoint>, ReceiverPoint> receiverPoints;
	
	public Grid(Scenario scenario) {
		this.scenario = scenario;	

		if ((NoiseConfigGroup) this.scenario.getConfig().getModule("noise") == null) {
			throw new RuntimeException("Could not find a noise config group. "
					+ "Check if the custom module is loaded, e.g. 'ConfigUtils.loadConfig(configFile, new NoiseConfigGroup())'"
					+ " Aborting...");
		}
		
		this.noiseParams = (NoiseConfigGroup) this.scenario.getConfig().getModule("noise");
		
		this.receiverPoints = new HashMap<Id<ReceiverPoint>, ReceiverPoint>();
		
		String[] consideredActTypesForDamagesArray = noiseParams.getConsideredActivitiesForDamageCalculationArray();
		for (int i = 0; i < consideredActTypesForDamagesArray.length; i++) {
			this.consideredActivitiesForSpatialFunctionality.add(consideredActTypesForDamagesArray[i]);
		}
		
		String[] consideredActTypesForReceiverPointGridArray = noiseParams.getConsideredActivitiesForReceiverPointGridArray();
		for (int i = 0; i < consideredActTypesForReceiverPointGridArray.length; i++) {
			this.consideredActivitiesForReceiverPointGrid.add(consideredActTypesForReceiverPointGridArray[i]);
		}

		this.noiseParams.checkGridParametersForConsistency();
		initialize();
	}

	private void initialize() {
		setActivityCoords();
		createGrid();
		setActivityCoord2NearestReceiverPointId();
		
		// delete unnecessary information
		this.zoneTuple2listOfReceiverPointIds.clear();
		this.consideredActivityCoordsForReceiverPointGrid.clear();
		this.consideredActivityCoordsForSpatialFunctionality.clear();
	}

	private void setActivityCoords () {
		
		for (Person person: scenario.getPopulation().getPersons().values()) {
				
			for (PlanElement planElement: person.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;
					
					if (!activity.getType().equalsIgnoreCase(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
						
						if (this.consideredActivitiesForSpatialFunctionality.contains(activity.getType())) {
							List<Coord> activityCoordinates = new ArrayList<Coord>();
							
							if (personId2consideredActivityCoords.containsKey(person.getId())) {
								activityCoordinates = personId2consideredActivityCoords.get(person.getId());
							}
							
							activityCoordinates.add(activity.getCoord());
							personId2consideredActivityCoords.put(person.getId(), activityCoordinates);
							
							consideredActivityCoordsForSpatialFunctionality.add(activity.getCoord());
						}
						
						if (this.consideredActivitiesForReceiverPointGrid.contains(activity.getType())) {
							consideredActivityCoordsForReceiverPointGrid.add(activity.getCoord());
						}
					}
				}
			}
		}
	}
	
	private void createGrid() {
		
		if (this.noiseParams.getReceiverPointsGridMinX() == 0. && this.noiseParams.getReceiverPointsGridMinY() == 0. && this.noiseParams.getReceiverPointsGridMaxX() == 0. && this.noiseParams.getReceiverPointsGridMaxY() == 0.) {
			
			log.info("Creating receiver points for the entire area between the minimum and maximium x and y activity coordinates of all activity locations.");
						
			for (Coord coord : consideredActivityCoordsForReceiverPointGrid) {
				if (coord.getX() < xCoordMin) {
					xCoordMin = coord.getX();
				}
				if (coord.getX() > xCoordMax) {
					xCoordMax = coord.getX();
				}
				if (coord.getY() < yCoordMin) {
					yCoordMin = coord.getY();
				}
				if (coord.getY() > yCoordMax) {
					yCoordMax = coord.getY();
				}
			}
			
		} else {
			
			xCoordMin = this.noiseParams.getReceiverPointsGridMinX();
			xCoordMax = this.noiseParams.getReceiverPointsGridMaxX();
			yCoordMin = this.noiseParams.getReceiverPointsGridMinY();
			yCoordMax = this.noiseParams.getReceiverPointsGridMaxY();
			
			log.info("Creating receiver points for the area between the coordinates (" + xCoordMin + "/" + yCoordMin + ") and (" + xCoordMax + "/" + yCoordMax + ").");
			
			createReceiverPoints();
		}
		
		createReceiverPoints();		
	}
	
	private void createReceiverPoints() {

		int counter = 0;
		
		for (double y = yCoordMax + 100. ; y > yCoordMin - 100. - noiseParams.getReceiverPointGap() ; y = y - noiseParams.getReceiverPointGap()) {
		
			for (double x = xCoordMin - 100. ; x < xCoordMax + 100. + noiseParams.getReceiverPointGap() ; x = x + noiseParams.getReceiverPointGap()) {
				
				Id<ReceiverPoint> id = Id.create(counter, ReceiverPoint.class);
				Coord coord = new Coord(x, y);
				
				ReceiverPoint rp = new ReceiverPoint(id, coord);
								
				receiverPoints.put(id, rp);
				
				counter++;
						
				Tuple<Integer,Integer> zoneTuple = getZoneTuple(coord);
				List<Id<ReceiverPoint>> listOfReceiverPointIDs = new ArrayList<Id<ReceiverPoint>>();
				if (zoneTuple2listOfReceiverPointIds.containsKey(zoneTuple)) {
					listOfReceiverPointIDs = zoneTuple2listOfReceiverPointIds.get(zoneTuple);
				}
				listOfReceiverPointIDs.add(id);
				zoneTuple2listOfReceiverPointIds.put(zoneTuple, listOfReceiverPointIDs);
			}
		}
		log.info("Total number of receiver points: " + receiverPoints.size());
	}
	
	private void setActivityCoord2NearestReceiverPointId () {
		
		int counter = 0;
		for (Coord coord : consideredActivityCoordsForSpatialFunctionality) {
			if (counter % 100000 == 0) {
				log.info("Setting activity coordinates to nearest receiver point. activity location # " + counter);
			}
			
			if (!(activityCoord2receiverPointId.containsKey(coord))) {
			
				Id<ReceiverPoint> receiverPointId = identifyNearestReceiverPoint(coord);
				activityCoord2receiverPointId.put(coord, receiverPointId);
			}
			
			counter++;
		}				
	}
	
	private Tuple<Integer, Integer> getZoneTuple(Coord coord) {
		 
		double xCoord = coord.getX();
		double yCoord = coord.getY();
		
		int xDirection = (int) ((xCoord - xCoordMin) / (noiseParams.getReceiverPointGap() / 1.));	
		int yDirection = (int) ((yCoordMax - yCoord) / noiseParams.getReceiverPointGap() / 1.);
		
		Tuple<Integer, Integer> zoneDefinition = new Tuple<Integer, Integer>(xDirection, yDirection);
		return zoneDefinition;
	}
	
	private Id<ReceiverPoint> identifyNearestReceiverPoint (Coord coord) {
		Id<ReceiverPoint> nearestReceiverPointId = null;
		
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
		
		List<Id<ReceiverPoint>> relevantReceiverPointIds = new ArrayList<Id<ReceiverPoint>>();
		
		for (Tuple<Integer,Integer> tuple : tuples) {
			if (zoneTuple2listOfReceiverPointIds.containsKey(tuple)) {
				for (Id<ReceiverPoint> id : zoneTuple2listOfReceiverPointIds.get(tuple)) {
					relevantReceiverPointIds.add(id);
				}
			}
		}
		
		double minDistance = Double.MAX_VALUE;

		for (Id<ReceiverPoint> receiverPointId : relevantReceiverPointIds) {
			double xValue = this.receiverPoints.get(receiverPointId).getCoord().getX();
			double yValue = this.receiverPoints.get(receiverPointId).getCoord().getY();
			
			double a = coord.getX() - xValue;
			double b = coord.getY() - yValue;
			
			double distance = Math.sqrt((Math.pow(a, 2))+(Math.pow(b, 2)));
			if (distance < minDistance) {
				minDistance = distance;
				nearestReceiverPointId = receiverPointId;
			}
		}

		return nearestReceiverPointId;
	}
	
	public Map<Id<Person>, List<Coord>> getPersonId2listOfConsideredActivityCoords() {
		return personId2consideredActivityCoords;
	}

	public Map<Coord, Id<ReceiverPoint>> getActivityCoord2receiverPointId() {
		return activityCoord2receiverPointId;
	}
	
	public Map<Id<ReceiverPoint>, ReceiverPoint> getReceiverPoints() {
		return receiverPoints;
	}
	
	public NoiseConfigGroup getGridParams() {
		return noiseParams;
	}

}
