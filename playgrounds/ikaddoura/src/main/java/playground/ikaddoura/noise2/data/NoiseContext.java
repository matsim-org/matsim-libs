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
package playground.ikaddoura.noise2.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.matsim.pt.PtConstants;

import playground.ikaddoura.noise2.NoiseParameters;
import playground.ikaddoura.noise2.handler.NoiseEquations;

/**
 * Computes a grid of receiver points and further spatial data which is required during the computation of noise immissions and damages.
 * Contains the spatial data as well as time-specific data.
 * 
 * @author lkroeger, ikaddoura
 *
 */
public class NoiseContext {
	
	private static final Logger log = Logger.getLogger(NoiseContext.class);
			
	private final Scenario scenario;
	private final NoiseParameters noiseParams;
		
	private final Map<Id<Person>, List<Coord>> personId2consideredActivityCoords = new HashMap<Id<Person>, List<Coord>>();
	private final List <Coord> consideredActivityCoordsForDamages = new ArrayList <Coord>();
	private final List <Coord> consideredActivityCoordsForReceiverPointGrid = new ArrayList <Coord>();
	
	private final List<String> consideredActivitiesForDamages = new ArrayList<String>();
	private final List<String> consideredActivitiesForReceiverPointGrid = new ArrayList<String>();
	
	private double xCoordMin = Double.MAX_VALUE;
	private double xCoordMax = Double.MIN_VALUE;
	private double yCoordMin = Double.MAX_VALUE;
	private double yCoordMax = Double.MIN_VALUE;
	
	private final Map<Tuple<Integer,Integer>,List<Id<ReceiverPoint>>> zoneTuple2listOfReceiverPointIds = new HashMap<Tuple<Integer, Integer>, List<Id<ReceiverPoint>>>();
	private final Map<Coord,Id<ReceiverPoint>> activityCoord2receiverPointId = new HashMap<Coord, Id<ReceiverPoint>>();
	
	private double xCoordMinLinkNode = Double.MAX_VALUE;
	private double xCoordMaxLinkNode = Double.MIN_VALUE;
	private double yCoordMinLinkNode = Double.MAX_VALUE;
	private double yCoordMaxLinkNode = Double.MIN_VALUE;
	
	private final Map<Tuple<Integer,Integer>, List<Id<Link>>> zoneTuple2listOfLinkIds = new HashMap<Tuple<Integer, Integer>, List<Id<Link>>>();
	
	// only required for routing purposes
	private Map<Double, Map<Id<Link>, NoiseLink>> timeInterval2linkId2noiseLinks = new HashMap<>();
	
	// time interval specific information
	private double currentTimeBinEndTime;
	private final Map<Id<ReceiverPoint>, ReceiverPoint> receiverPoints;
	private final Map<Id<Link>, NoiseLink> noiseLinks;
	private double eventTime = Double.MIN_VALUE;
					
	public NoiseContext(Scenario scenario, NoiseParameters noiseParams) {
		
		this.scenario = scenario;
		
		this.noiseParams = noiseParams;
		this.noiseParams.checkForConsistency();
		
		this.currentTimeBinEndTime = noiseParams.getTimeBinSizeNoiseComputation();
		
		this.receiverPoints = new HashMap<Id<ReceiverPoint>, ReceiverPoint>();
		this.noiseLinks = new HashMap<Id<Link>, NoiseLink>();
		
		String[] consideredActTypesForDamagesArray = noiseParams.getConsideredActivitiesForDamages();
		for (int i = 0; i < consideredActTypesForDamagesArray.length; i++) {
			this.consideredActivitiesForDamages.add(consideredActTypesForDamagesArray[i]);
		}
		
		String[] consideredActTypesForReceiverPointGridArray = noiseParams.getConsideredActivitiesForReceiverPointGrid();
		for (int i = 0; i < consideredActTypesForReceiverPointGridArray.length; i++) {
			this.consideredActivitiesForReceiverPointGrid.add(consideredActTypesForReceiverPointGridArray[i]);
		}
	}
	
	// for routing purposes
	public void storeTimeInterval() {
		
		Map<Id<Link>, NoiseLink> noiseLinksThisTimeBinCopy = new HashMap<>();
		noiseLinksThisTimeBinCopy.putAll(this.noiseLinks);
		
		double currentTimeIntervalCopy = this.currentTimeBinEndTime;
		
		this.timeInterval2linkId2noiseLinks.put(currentTimeIntervalCopy, noiseLinksThisTimeBinCopy);
	}
	
	public void initialize() {
		setActivityCoords();
		createGrid();
		setActivityCoord2NearestReceiverPointId();
		setRelevantLinkInfo();
		
		// delete unnecessary information
		this.zoneTuple2listOfLinkIds.clear();
		this.zoneTuple2listOfReceiverPointIds.clear();
		this.consideredActivityCoordsForReceiverPointGrid.clear();
		this.consideredActivityCoordsForDamages.clear();
	}
	
	public void initialize(String gridFileCSV) {
		setActivityCoords();
		createGrid(gridFileCSV);
		setActivityCoord2NearestReceiverPointId();
		setRelevantLinkInfo();
		
		// delete unnecessary information
		this.zoneTuple2listOfLinkIds.clear();
		this.zoneTuple2listOfReceiverPointIds.clear();
		this.consideredActivityCoordsForReceiverPointGrid.clear();
		this.consideredActivityCoordsForDamages.clear();
	}

	private void setActivityCoords () {
		
		for (Person person: scenario.getPopulation().getPersons().values()) {
				
			for (PlanElement planElement: person.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;
					
					if (!activity.getType().equalsIgnoreCase(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
						
						if (this.consideredActivitiesForDamages.contains(activity.getType())) {
							List<Coord> activityCoordinates = new ArrayList<Coord>();
							
							if (personId2consideredActivityCoords.containsKey(person.getId())) {
								activityCoordinates = personId2consideredActivityCoords.get(person.getId());
							}
							
							activityCoordinates.add(activity.getCoord());
							personId2consideredActivityCoords.put(person.getId(), activityCoordinates);
							
							consideredActivityCoordsForDamages.add(activity.getCoord());
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
				Coord coord = new CoordImpl(x, y);
				
				ReceiverPoint rp = new ReceiverPoint(id);
				rp.setCoord(coord);
				
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
	
	private void createGrid(String gridFileCSV) {
		// TODO Auto-generated method stub
		throw new RuntimeException("Not yet implemented. Aborting...");
		
	}
		
	private void setActivityCoord2NearestReceiverPointId () {
		
		int counter = 0;
		for (Coord coord : consideredActivityCoordsForDamages) {
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
	
	private void setRelevantLinkInfo() {
		
		setLinksMinMax();
		setLinksToZones();
				
		int counter = 0;
		
		for (ReceiverPoint rp : this.receiverPoints.values()) {
			counter++;
			
			if (counter % 10000. == 0.) {
				log.info("Setting relevant link information for receiver point # " + counter);
			}
			
			double pointCoordX = this.receiverPoints.get(rp.getId()).getCoord().getX();
			double pointCoordY = this.receiverPoints.get(rp.getId()).getCoord().getY();

			Map<Id<Link>,Double> relevantLinkIds2Ds = new HashMap<>();
			Map<Id<Link>,Double> relevantLinkIds2angleImmissionCorrection = new HashMap<>();
		
			// get the zone grid cell around the receiver point
			Tuple<Integer,Integer> zoneTuple = getZoneTupleForLinks(this.receiverPoints.get(rp.getId()).getCoord());
			
			// collect all Ids of links in this zone grid cell...
			List<Id<Link>> potentialLinks = new ArrayList<>();
			if(zoneTuple2listOfLinkIds.containsKey(zoneTuple)) {
				potentialLinks.addAll(zoneTuple2listOfLinkIds.get(zoneTuple));
			}
			// collect all Ids of links in all surrounding zone grid cells
			int x = zoneTuple.getFirst();
			int y = zoneTuple.getSecond();
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

			// go through these potential relevant link Ids
			List<Id<Link>> relevantLinkIds = new ArrayList<>();
			for (Id<Link> linkId : potentialLinks){
				if (!(relevantLinkIds.contains(linkId))) {
					double fromCoordX = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX();
					double fromCoordY = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY();
					double toCoordX = scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord().getX();
					double toCoordY = scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord().getY();
				
					double lotPointX = 0.;
					double lotPointY = 0.;
				
					double vectorX = toCoordX - fromCoordX;
					if (vectorX == 0.) {
						vectorX = 0.00000001;
						// dividing by zero is not possible
					}
					
					double vectorY = toCoordY - fromCoordY;
					double vector = vectorY/vectorX;
					if (vector == 0.) {
						vector = 0.00000001;
						// dividing by zero is not possible
					}
				
					double vector2 = (-1) * (1/vector);
					double yAbschnitt = fromCoordY - (fromCoordX * vector);
					double yAbschnittOriginal = fromCoordY - (fromCoordX * vector);
				
					double yAbschnitt2 = pointCoordY - (pointCoordX * vector2);
				
					double xValue = 0.;
					double yValue = 0.;
				
					if (yAbschnitt<yAbschnitt2) {
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
						// no edge solution
						distance = Math.sqrt((Math.pow(lotPointX-pointCoordX, 2))+(Math.pow(lotPointY-pointCoordY, 2)));
					} else {
						// edge solution (Randloesung)
						double distanceToFromNode = Math.sqrt((Math.pow(fromCoordX-pointCoordX, 2))+(Math.pow(fromCoordY-pointCoordY, 2)));
						double distanceToToNode = Math.sqrt((Math.pow(toCoordX-pointCoordX, 2))+(Math.pow(toCoordY-pointCoordY, 2)));
						if (distanceToFromNode > distanceToToNode) {
							distance = distanceToToNode;
						} else {
							distance = distanceToFromNode;
						}
					}
					
					if (distance < noiseParams.getRelevantRadius()){
						
						relevantLinkIds.add(linkId);
						
						if (distance == 0) {
							double minimumDistance = 5.;
							distance = minimumDistance;
							log.warn("Distance between " + linkId + " and " + rp.getId() + " is 0. The calculation of the correction term Ds requires a distance > 0. Therefore, setting the distance to a minimum value of " + minimumDistance + ".");
						}
						double correctionTermDs = NoiseEquations.calculateDistanceCorrection(distance);
						double correctionTermAngle = calculateAngleImmissionCorrection(this.receiverPoints.get(rp.getId()).getCoord(), scenario.getNetwork().getLinks().get(linkId));
						
						relevantLinkIds2Ds.put(linkId, correctionTermDs);
						relevantLinkIds2angleImmissionCorrection.put(linkId, correctionTermAngle);						
					}
				}
			}
			
			rp.setLinkId2distanceCorrection(relevantLinkIds2Ds);
			rp.setLinkId2angleCorrection(relevantLinkIds2angleImmissionCorrection);
		}
	}
	
	private void setLinksMinMax() {
		
		for (Id<Link> linkId : scenario.getNetwork().getLinks().keySet()){
			if ((scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX()) < xCoordMinLinkNode) {
				xCoordMinLinkNode = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX();
			}
			if ((scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY()) < yCoordMinLinkNode) {
				yCoordMinLinkNode = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY();
			}
			if ((scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX()) > xCoordMaxLinkNode) {
				xCoordMaxLinkNode = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX();
			}
			if ((scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY()) > yCoordMaxLinkNode) {
				yCoordMaxLinkNode = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY();
			}
		}		
	}
	
	private void setLinksToZones() {
		
		for (Id<Link> linkId : scenario.getNetwork().getLinks().keySet()){
			
			// split up the link into link segments with the following length
			double partLength = 0.25 * noiseParams.getRelevantRadius();
			int parts = (int) ((scenario.getNetwork().getLinks().get(linkId).getLength())/(partLength));

			double fromX = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX();
			double fromY = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY();
			double toX = scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord().getX();
			double toY = scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord().getY();
			double vectorX = toX - fromX;
			double vectorY = toY - fromY;
			
			// collect the coordinates of this link
			List<Coord> coords = new ArrayList<Coord>();
			coords.add(scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord());
			coords.add(scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord());
			for (int i = 1 ; i<parts ; i++) {
				double x = fromX + (i*((1./(parts))*vectorX));
				double y = fromY + (i*((1./(parts))*vectorY));
				Coord  coordTmp = new CoordImpl(x,y);
				coords.add(coordTmp);
			}
			
			// get zone grid cells for these coordinates
			List<Tuple<Integer,Integer>> relevantTuples = new ArrayList<Tuple<Integer,Integer>>();
			for (Coord coord : coords) {
				Tuple<Integer,Integer> tupleTmp = getZoneTupleForLinks(coord);
				if (!(relevantTuples.contains(tupleTmp))) {
					relevantTuples.add(tupleTmp);
				}
			}
			
			// go through these zone grid cells and save the link Id 			
			for(Tuple<Integer,Integer> tuple : relevantTuples) {
				if(zoneTuple2listOfLinkIds.containsKey(tuple)) {
					List<Id<Link>> linkIds = zoneTuple2listOfLinkIds.get(tuple);
					linkIds.add(linkId);
					zoneTuple2listOfLinkIds.put(tuple, linkIds);
				} else {
					List<Id<Link>> linkIds = new ArrayList<>();
					linkIds.add(linkId);
					zoneTuple2listOfLinkIds.put(tuple, linkIds);
				}
			}
		}
	}
	
	private Tuple<Integer,Integer> getZoneTupleForLinks(Coord coord) {
		 
		double xCoord = coord.getX();
		double yCoord = coord.getY();
		
		int xDirection = (int) ((xCoord - xCoordMinLinkNode) / (noiseParams.getRelevantRadius() / 1.));	
		int yDirection = (int) ((yCoordMaxLinkNode - yCoord) / noiseParams.getRelevantRadius() / 1.);
		
		Tuple<Integer,Integer> zoneDefinition = new Tuple<Integer, Integer>(xDirection, yDirection);
		return zoneDefinition;
	}
	
	private double calculateAngleImmissionCorrection(Coord receiverPointCoord, Link link) {

		double angle = 0;
		
		double pointCoordX = receiverPointCoord.getX();
		double pointCoordY = receiverPointCoord.getY();
		
		double fromCoordX = link.getFromNode().getCoord().getX();
		double fromCoordY = link.getFromNode().getCoord().getY();
		double toCoordX = link.getToNode().getCoord().getX();
		double toCoordY = link.getToNode().getCoord().getY();

		if (pointCoordX == fromCoordX && pointCoordY == fromCoordY) {
			// receiver point is situated on the link (fromNode)
			// assume a maximum angle immission correction for this case
			angle = 0;
			
		} else if (pointCoordX == toCoordX && pointCoordY == toCoordY) {
			// receiver point is situated on the link (toNode)
			// assume a zero angle immission correction for this case
			angle = 180;		
			
		} else {
			// all other cases
			double sc = (fromCoordX - pointCoordX) * (toCoordX - pointCoordX) + (fromCoordY - pointCoordY) * (toCoordY - pointCoordY);
			double cosAngle = sc / (
					Math.sqrt(
							Math.pow(fromCoordX - pointCoordX, 2) + Math.pow(fromCoordY - pointCoordY, 2)
							)
							*
					Math.sqrt(
							Math.pow(toCoordX - pointCoordX, 2) + Math.pow(toCoordY - pointCoordY, 2)
							)
					);
			angle = Math.toDegrees(Math.acos(cosAngle));

			if (sc > 0) {
				// spitzer winkel
						
				if (angle > 90) {
					angle = 180 - angle;
				}
				
			} else if (sc < 0) {
				// stumpfer winkel
				
				if (angle < 90) {
					angle = 180 - angle;
				}
				
			} else {
				angle = 0.;
			}
		}
			
		// since zero is not defined
		if (angle == 0.) {
			// zero degrees is not defined
			angle = 0.0000000001;
		}
					
//		System.out.println(receiverPointCoord + " // " + link.getId() + "(" + link.getFromNode().getCoord() + "-->" + link.getToNode().getCoord() + " // " + angle);
		double immissionCorrection = NoiseEquations.calculateAngleCorrection(angle);
		return immissionCorrection;
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
	
	public Scenario getScenario() {
		return scenario;
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
	
	public NoiseParameters getNoiseParams() {
		return noiseParams;
	}

	public double getCurrentTimeBinEndTime() {
		return currentTimeBinEndTime;
	}

	public void setCurrentTimeBinEndTime(double currentTimeBinEndTime) {
		this.currentTimeBinEndTime = currentTimeBinEndTime;
	}

	public Map<Id<Link>, NoiseLink> getNoiseLinks() {
		return noiseLinks;
	}

	public Map<Double, Map<Id<Link>, NoiseLink>> getTimeInterval2linkId2noiseLinks() {
		return timeInterval2linkId2noiseLinks;
	}

	public void setEventTime(double time) {
		this.eventTime = time;
	}

	public double getEventTime() {
		return eventTime;
	}

}
