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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.handler.NoiseEquations;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

/**
 * Contains the grid and further noise-specific information.
 * 
 * @author lkroeger, ikaddoura
 *
 */
public class NoiseContext {
	
	private static final Logger log = Logger.getLogger(NoiseContext.class);
			
	private final Scenario scenario;
	private final NoiseConfigGroup noiseParams;
	private final Grid grid;
			
	private final Map<Tuple<Integer,Integer>, List<Id<Link>>> zoneTuple2listOfLinkIds = new HashMap<Tuple<Integer, Integer>, List<Id<Link>>>();
	private double xCoordMinLinkNode = Double.MAX_VALUE;
	private double xCoordMaxLinkNode = Double.MIN_VALUE;
	private double yCoordMinLinkNode = Double.MAX_VALUE;
	private double yCoordMaxLinkNode = Double.MIN_VALUE;
	
	private Set<Id<Vehicle>> busVehicleIDs = new HashSet<Id<Vehicle>>();
	
	// for routing purposes
	
	private final Map<Double, Map<Id<Link>, NoiseLink>> timeInterval2linkId2noiseLinks = new HashMap<>();
	
	// time interval specific information
	
	private double currentTimeBinEndTime;
	private final Map<Id<Link>, NoiseLink> noiseLinks;
	private double eventTime = Double.MIN_VALUE;

	private final Map<Id<ReceiverPoint>, NoiseReceiverPoint> noiseReceiverPoints;
	
	// ############################################
	
	public NoiseContext(Scenario scenario) {
		this.scenario = scenario;
				
		if ((NoiseConfigGroup) this.scenario.getConfig().getModule("noise") == null) {
			throw new RuntimeException("Could not find a noise config group. "
					+ "Check if the custom module is loaded, e.g. 'ConfigUtils.loadConfig(configFile, new NoiseConfigGroup())'"
					+ " Aborting...");
		}
		
		this.noiseParams = (NoiseConfigGroup) this.scenario.getConfig().getModule("noise");
		this.noiseParams.checkNoiseParametersForConsistency();
		
		this.grid = new Grid(scenario);
				
		this.currentTimeBinEndTime = noiseParams.getTimeBinSizeNoiseComputation();
		
		this.noiseReceiverPoints = new HashMap<Id<ReceiverPoint>, NoiseReceiverPoint>();
		this.noiseLinks = new HashMap<Id<Link>, NoiseLink>();
		
		checkConsistency();
		setRelevantLinkInfo();
	}

	// for routing purposes
	public final void storeTimeInterval() {
		
		Map<Id<Link>, NoiseLink> noiseLinksThisTimeBinCopy = new HashMap<>();
		noiseLinksThisTimeBinCopy.putAll(this.noiseLinks);
		
		double currentTimeIntervalCopy = this.currentTimeBinEndTime;
		
		this.timeInterval2linkId2noiseLinks.put(currentTimeIntervalCopy, noiseLinksThisTimeBinCopy);
	}
	
	private void checkConsistency() {
		List<String> consideredActivitiesForDamagesList = new ArrayList<String>();
		List<String> consideredActivitiesForReceiverPointGridList = new ArrayList<String>();

		for (int i = 0; i < this.grid.getGridParams().getConsideredActivitiesForDamageCalculationArray().length; i++) {
			consideredActivitiesForDamagesList.add(this.grid.getGridParams().getConsideredActivitiesForDamageCalculationArray()[i]);
		}
		
		for (int i = 0; i < this.grid.getGridParams().getConsideredActivitiesForReceiverPointGridArray().length; i++) {
			consideredActivitiesForReceiverPointGridList.add(this.grid.getGridParams().getConsideredActivitiesForReceiverPointGridArray()[i]);
		}
		
		if (this.noiseParams.isComputeNoiseDamages()) {
			
			if (consideredActivitiesForDamagesList.size() == 0) {
				log.warn("Not considering any activity type for the noise damage computation."
						+ "The computation of noise damages should be disabled.");
			}
			
			if (this.grid.getGridParams().getReceiverPointsGridMaxX() != 0.
					|| this.grid.getGridParams().getReceiverPointsGridMinX() != 0.
					|| this.grid.getGridParams().getReceiverPointsGridMaxY() != 0.
					|| this.grid.getGridParams().getReceiverPointsGridMinY() != 0.) {
				log.warn("In order to keep track of the agent activities, the grid of receiver points should not be limited to a set of predefined coordinates."
						+ "For a grid covering all activity locations, set the minimum and maximum x/y parameters to 0.0. "
						+ "There will be more agents mapped to the receiver points at the edges. Only the inner receiver points should be used for analysis.");
			}
						
			if (this.grid.getGridParams().getReceiverPointsGridMinX() == 0. && this.grid.getGridParams().getReceiverPointsGridMinY() == 0. && this.grid.getGridParams().getReceiverPointsGridMaxX() == 0. && this.grid.getGridParams().getReceiverPointsGridMaxY() == 0.) {
				for (String type : consideredActivitiesForDamagesList) {
					if (!consideredActivitiesForReceiverPointGridList.contains(type)) {
						throw new RuntimeException("An activity type which is considered for the damage calculation (" + type
								+ ") should also be considered for the minimum and maximum coordinates of the receiver point grid area. Aborting...");
					}
				}
			}
		}
	}

	private void setRelevantLinkInfo() {
		
		setLinksMinMax();
		setLinksToZones();
				
		int counter = 0;
		
		for (ReceiverPoint rp : this.grid.getReceiverPoints().values()) {
			
			counter++;
			if (counter % 10000. == 0.) {
				log.info("Setting relevant link information for receiver point # " + counter);
			}
			
			NoiseReceiverPoint nrp = new NoiseReceiverPoint(rp.getId(), rp.getCoord());
			
			double pointCoordX = nrp.getCoord().getX();
			double pointCoordY = nrp.getCoord().getY();

			Map<Id<Link>,Double> relevantLinkIds2Ds = new HashMap<>();
			Map<Id<Link>,Double> relevantLinkIds2angleImmissionCorrection = new HashMap<>();
		
			// get the zone grid cell around the receiver point
			Tuple<Integer,Integer> zoneTuple = getZoneTupleForLinks(nrp.getCoord());
			
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
						double correctionTermAngle = calculateAngleImmissionCorrection(nrp.getCoord(), scenario.getNetwork().getLinks().get(linkId));
						
						relevantLinkIds2Ds.put(linkId, correctionTermDs);
						relevantLinkIds2angleImmissionCorrection.put(linkId, correctionTermAngle);						
					}
				}
			}
			
			nrp.setLinkId2distanceCorrection(relevantLinkIds2Ds);
			nrp.setLinkId2angleCorrection(relevantLinkIds2angleImmissionCorrection);
			
			this.noiseReceiverPoints.put(nrp.getId(), nrp);
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
				Coord  coordTmp = new Coord(x, y);
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
	
	public final Scenario getScenario() {
		return scenario;
	}
	
	public final Map<Id<ReceiverPoint>, NoiseReceiverPoint> getReceiverPoints() {
		return noiseReceiverPoints;
	}
	
	public final NoiseConfigGroup getNoiseParams() {
		return noiseParams;
	}

	public final double getCurrentTimeBinEndTime() {
		return currentTimeBinEndTime;
	}

	public final void setCurrentTimeBinEndTime(double currentTimeBinEndTime) {
		this.currentTimeBinEndTime = currentTimeBinEndTime;
	}

	public final Map<Id<Link>, NoiseLink> getNoiseLinks() {
		return noiseLinks;
	}

	public final Map<Double, Map<Id<Link>, NoiseLink>> getTimeInterval2linkId2noiseLinks() {
		return timeInterval2linkId2noiseLinks;
	}

	public final void setEventTime(double time) {
		this.eventTime = time;
	}

	public final double getEventTime() {
		return eventTime;
	}

	public final Grid getGrid() {
		return grid;
	}

	public Set<Id<Vehicle>> getBusVehicleIDs() {
		return busVehicleIDs;
	}

	public void setBusVehicleIDs(Set<Id<Vehicle>> busVehicleIDs) {
		this.busVehicleIDs = busVehicleIDs;
	}

}
