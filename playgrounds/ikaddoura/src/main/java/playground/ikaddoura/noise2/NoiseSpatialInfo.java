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
package playground.ikaddoura.noise2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.PtConstants;

/**
 * Contains all spatial information for the RLS approach 'Lange gerade Fahrstreifen' to calculate noise immission.
 * 
 * @author lkroeger, ikaddoura
 *
 */
public class NoiseSpatialInfo {
	
	private static final Logger log = Logger.getLogger(NoiseSpatialInfo.class);
		
	private final double receiverPointGap = 250.; // distance between two receiver points along x- and y-axes
	private final double relevantRadius = 500.; // radius around a receiver point in which all links are considered as relevant
	private final double gridCentroidGapDensity = 250.; // distance between two grid cell centroids along x- and y-axes (relevant for computation of activity density)
	
	private Scenario scenario;
	private Map<Id,List<Coord>> personId2listOfCoords = new HashMap<Id, List<Coord>>();
	private List <Coord> allActivityCoords = new ArrayList <Coord>();
	
	private double xCoordMin = Double.MAX_VALUE;
	private double xCoordMax = Double.MIN_VALUE;
	private double yCoordMin = Double.MAX_VALUE;
	private double yCoordMax = Double.MIN_VALUE;
	
	private Map<Tuple<Integer,Integer>,List<Coord>> zoneTuple2listOfActivityCoords = new HashMap<Tuple<Integer,Integer>, List<Coord>>();

	private Map<Id,Coord> receiverPointId2Coord = new HashMap<Id,Coord>();
	private Map<Tuple<Integer,Integer>,List<Id>> zoneTuple2listOfReceiverPointIds = new HashMap<Tuple<Integer,Integer>, List<Id>>();

	private Map<Coord,Id> activityCoord2receiverPointId = new HashMap<Coord,Id>();
	private Map<Coord,Id> coord2receiverPointId = new HashMap<Coord,Id>();
	
	private double xCoordMinLinkNodes = Double.MAX_VALUE;
	private double xCoordMaxLinkNodes = Double.MIN_VALUE;
	private double yCoordMinLinkNodes = Double.MAX_VALUE;
	private double yCoordMaxLinkNodes = Double.MIN_VALUE;
	private Map<Tuple<Integer,Integer>,List<Id>> zoneTuple2listOfLinkIds = new HashMap<Tuple<Integer,Integer>, List<Id>>();
	
	private Map<Id,List<Id>> receiverPointId2relevantLinkIds = new HashMap<Id, List<Id>>();
	private Map<Id, Map<Id,Double>> receiverPointId2relevantLinkId2correctionTermDs = new HashMap<Id, Map<Id,Double>>();
	private Map<Id, Map<Id,Double>> receiverPointId2relevantLinkId2correctionTermAngle = new HashMap<Id, Map<Id,Double>>();
				
	public NoiseSpatialInfo(Scenario scenario) {
		this.scenario = scenario;
	}	
	
	public void setActivityCoords () {
		Map<Id, Queue<Coord>> personId2coordsOfActivities = new HashMap<Id, Queue<Coord>>();

		for (Person person: scenario.getPopulation().getPersons().values()) {
	
			personId2coordsOfActivities.put(person.getId(), new LinkedList<Coord>());
			Map<Coord,String> tmpMapCoord2Activity = new HashMap<Coord, String>();
			
			for (PlanElement planElement: person.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof Activity) {
					Activity currentActivity = (Activity) planElement;
					
					// exclude "pt interaction" pseudo-activities
					if (!currentActivity.getType().equalsIgnoreCase(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
						personId2coordsOfActivities.get(person.getId()).add(currentActivity.getCoord());
						List<Coord> listTmp = new ArrayList<Coord>();
						if(personId2listOfCoords.containsKey(person.getId())) {
							listTmp = personId2listOfCoords.get(person.getId());
						}
						Coord coordTmp = currentActivity.getCoord();
						listTmp.add(coordTmp);
						personId2listOfCoords.put(person.getId(), listTmp);
						
						if (tmpMapCoord2Activity.containsKey(currentActivity.getCoord())) {
							if (tmpMapCoord2Activity.get(currentActivity.getCoord())==currentActivity.getType().toString()) {
							} else {
								allActivityCoords.add(currentActivity.getCoord());
								tmpMapCoord2Activity.put(currentActivity.getCoord(), currentActivity.getType().toString());
							}
						} else {
							allActivityCoords.add(currentActivity.getCoord());
							tmpMapCoord2Activity.put(currentActivity.getCoord(), currentActivity.getType().toString());
						}
					}
				}
			}
		}
		
		for(Coord coord : allActivityCoords) {
			if(coord.getX() < xCoordMin) {
				xCoordMin = coord.getX();
			}
			if(coord.getX() > xCoordMax) {
				xCoordMax = coord.getX();
			}
			if(coord.getY() < yCoordMin) {
				yCoordMin = coord.getY();
			}
			if(coord.getY() > yCoordMax) {
				yCoordMax = coord.getY();
			}
		}
			
		//	classify the activityCoords on the basis of the density
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
	
	public void setReceiverPoints() {
		
		int counter = 0;
		
		// a grid of receiver points
		for (double y = yCoordMax + 100. ; y > yCoordMin - 100. - receiverPointGap ; y = y - receiverPointGap) {
			for (double x = xCoordMin - 100. ; x < xCoordMax + 100. + receiverPointGap ; x = x + receiverPointGap) {
				Coord coord = new CoordImpl(x, y);
				Id rpId = new IdImpl(counter);
				receiverPointId2Coord.put(rpId, coord);
				counter++;
							
				Tuple<Integer,Integer> zoneTuple = getZoneTuple(coord);
				List<Id> listOfReceiverPointIDs = new ArrayList<Id>();
				if (zoneTuple2listOfReceiverPointIds.containsKey(zoneTuple)) {
					listOfReceiverPointIDs = zoneTuple2listOfReceiverPointIds.get(zoneTuple);
				}
				listOfReceiverPointIDs.add(rpId);
				zoneTuple2listOfReceiverPointIds.put(zoneTuple, listOfReceiverPointIDs);
			}
		}
		
		log.info("number of receiver points: " + receiverPointId2Coord.size());
		writeReceiverPoints();
	}
	
	private void writeReceiverPoints() {

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
		headers.add("receiverPointId");
		headers.add("xCoord");
		headers.add("yCoord");
		
		List<HashMap<Id,Double>> values = new ArrayList<HashMap<Id,Double>>();
		values.add(id2xCoord);
		values.add(id2yCoord);
		
		write(scenario.getConfig().controler().getOutputDirectory() + "/", 3, headers, values);
	}
	
	private void write (String fileName , int columns , List<String> headers , List<HashMap<Id,Double>> values) {
		
		File file = new File(fileName);
		file.mkdirs();
		
		File file2 = new File(fileName+"receiverPoints.csv");
			
		// For all maps, the number of keys should be the same
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
			bw.write(headers.get(0));
			for(int i = 1 ; i < columns ; i++) {
				bw.write(";"+headers.get(i));
			}
			bw.newLine();
			
			for(Id id : values.get(0).keySet()) {
				bw.write(id.toString());
				for(int i = 0 ; i < (columns-1) ; i++) {
					bw.write(";"+values.get(i).get(id));
				}
				bw.newLine();
			}
				
			bw.close();
				log.info("Output written to " + fileName);
				
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setActivityCoord2NearestReceiverPointId () {
		
		int xi = 0;
		for (Coord coord : allActivityCoords) {
			xi++;
			if (xi % 20000 == 0) {
				log.info("activity coordinates " + xi + " ...");
			}
			
			if (!(activityCoord2receiverPointId.containsKey(coord))) {
			
				Id receiverPointId = this.getNearestReceiverPoint(coord);
				activityCoord2receiverPointId.put(coord, receiverPointId);
			}
		}
				
		for (Id id : receiverPointId2Coord.keySet()) {
			
			xi++;
			
			if (xi % 20000 == 0) {
				log.info("receiver point " + xi + " ...");
			}
			coord2receiverPointId.put(receiverPointId2Coord.get(id), id);
		}
	}
	
	private Tuple<Integer,Integer> getZoneTupleForLinks(Coord coord) {
		 
		double xCoord = coord.getX();
		double yCoord = coord.getY();
		
		int xDirection = (int) ((xCoord-xCoordMinLinkNodes) / (relevantRadius/1.));	
		int yDirection = (int) ((yCoordMaxLinkNodes-yCoord) / relevantRadius/1.);
		
		Tuple<Integer,Integer> zoneDefinition = new Tuple<Integer, Integer>(xDirection, yDirection);
		return zoneDefinition;
	}
	
	public void setRelevantLinkIds() {
		
		setLinksMinMax();
		setLinksToZones();
				
		int counter = 0;
		
		for (Id pointId : receiverPointId2Coord.keySet()) {

			counter++;
			if (counter % 1000. == 0.) {
				log.info("receiver point ... " + counter);
			}
			
			double pointCoordX = receiverPointId2Coord.get(pointId).getX();
			double pointCoordY = receiverPointId2Coord.get(pointId).getY();
			
			Map<Id,Double> relevantLinkIds2Ds = new HashMap<Id, Double>();
			Map<Id,Double> relevantLinkIds2angleImmissionCorrection = new HashMap<Id, Double>();
		
			// get the zone grid cell around the receiver point
			Tuple<Integer,Integer> zoneTuple = getZoneTupleForLinks(receiverPointId2Coord.get(pointId));
			
			// collect all Ids of links in this zone grid cell...
			List<Id> potentialLinks = new ArrayList<Id>();
			if(zoneTuple2listOfLinkIds.containsKey(zoneTuple)) {
				potentialLinks.addAll(zoneTuple2listOfLinkIds.get(zoneTuple));
			}
			// ... and in all surrounding zone grid cells
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

			// go through these (potential) links
			List<Id> relevantLinkIds = new ArrayList<Id>();
			for (Id linkId : potentialLinks){
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
					
					if (distance < relevantRadius){
						
						relevantLinkIds.add(linkId);
						
						if (distance == 0) {
							double minimumDistance = 5.;
							distance = minimumDistance;
							log.warn("Distance between " + linkId + " and " + pointId + " is 0. The calculation of the correction term Ds requires a distance > 0. Therefore, setting the distance to a minimum value of " + minimumDistance + ".");
						}
						double correctionTermDs = calculateDs(distance);
						double correctionTermAngle = calculateAngleImmissionCorrection(receiverPointId2Coord.get(pointId), scenario.getNetwork().getLinks().get(linkId));
						
						relevantLinkIds2Ds.put(linkId, correctionTermDs);
						relevantLinkIds2angleImmissionCorrection.put(linkId, correctionTermAngle);						
					}
				}
			}
			
			receiverPointId2relevantLinkIds.put(pointId, relevantLinkIds);
			receiverPointId2relevantLinkId2correctionTermDs.put(pointId, relevantLinkIds2Ds);
			receiverPointId2relevantLinkId2correctionTermAngle.put(pointId, relevantLinkIds2angleImmissionCorrection);
		}
	}
	
	private void setLinksMinMax() {
		
		for (Id linkId : scenario.getNetwork().getLinks().keySet()){
			if ((scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX()) < xCoordMinLinkNodes) {
				xCoordMinLinkNodes = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX();
			}
			if ((scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY()) < yCoordMinLinkNodes) {
				yCoordMinLinkNodes = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY();
			}
			if ((scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX()) > xCoordMaxLinkNodes) {
				xCoordMaxLinkNodes = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX();
			}
			if ((scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY()) > yCoordMaxLinkNodes) {
				yCoordMaxLinkNodes = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY();
			}
		}		
	}
	
	private void setLinksToZones() {
		
		for (Id linkId : scenario.getNetwork().getLinks().keySet()){
			
			// split up the link into link segments with the following length
			double partLength = 0.25 * relevantRadius;
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
				double x = fromX + (i*((1./((double)parts))*vectorX));
				double y = fromY + (i*((1./((double)parts))*vectorY));
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
	
	private double calculateDs (double distanceToRoad){
		double Ds = 0.;
		Ds = 15.8 - (10 * Math.log10(distanceToRoad)) - (0.0142*(Math.pow(distanceToRoad,0.9)));
		return Ds;
	}
	
	private double calculateAngleImmissionCorrection(Coord receiverPointCoord, Link link) {
		System.out.println("Link: " + link.getId().toString());
		double immissionCorrection = 0.;
		double angle = 0.;
		
		double lotPointX = 0.;
		double lotPointY = 0.;
		
		double pointCoordX = receiverPointCoord.getX();
		double pointCoordY = receiverPointCoord.getY();
		
		double fromCoordX = link.getFromNode().getCoord().getX();
		double fromCoordY = link.getFromNode().getCoord().getY();
		double toCoordX = link.getToNode().getCoord().getX();
		double toCoordY = link.getToNode().getCoord().getY();
		
		double vectorX = toCoordX - fromCoordX;
		if (vectorX == 0.) {
			vectorX = 0.00000001;
			// dividing by zero is not possible
		}
		System.out.println("x-vector: " + vectorX);

		double vectorY = toCoordY - fromCoordY;
		System.out.println("y-vector: " + vectorY);

		double vector = vectorY / vectorX;
		if (vector == 0.) {
			vector = 0.00000001;
			// dividing by zero is not possible
		}
		System.out.println("vector: " + vector);
		
		double vector2 = (-1) * (1/vector);
		System.out.println("vector 2: " + vector2);

		double yAbschnitt = fromCoordY - (fromCoordX * vector);
		double yAbschnittOriginal = fromCoordY - (fromCoordX * vector);
		
		double yAbschnitt2 = pointCoordY - (pointCoordX * vector2);
		
		double xValue = 0.;
		double yValue = 0.;
		
		if(yAbschnitt < yAbschnitt2) {
			yAbschnitt2 = yAbschnitt2 - yAbschnitt;
			yAbschnitt = 0;
			xValue = yAbschnitt2 / (vector - vector2);
			yValue = yAbschnittOriginal + (xValue * vector);
		} else if (yAbschnitt2 < yAbschnitt) {
			yAbschnitt = yAbschnitt - yAbschnitt2;
			yAbschnitt2 = 0;
			xValue = yAbschnitt / (vector2 - vector);
			yValue = yAbschnittOriginal + (xValue * vector);
		}
		
		lotPointX = xValue;
		lotPointY = yValue;
		
		double angle1 = 0.;
		double angle2 = 0.;
		
		double kath1 = Math.abs(Math.sqrt(Math.pow(lotPointX - fromCoordX, 2) + Math.pow(lotPointY - fromCoordY, 2)));
		double hypo1 = Math.abs(Math.sqrt(Math.pow(pointCoordX - fromCoordX, 2) + Math.pow(pointCoordY - fromCoordY, 2)));
		double kath2 = Math.abs(Math.sqrt(Math.pow(lotPointX - toCoordX, 2) + Math.pow(lotPointY - toCoordY, 2)));
		double hypo2 = Math.abs(Math.sqrt(Math.pow(pointCoordX - toCoordX, 2) + Math.pow(pointCoordY - toCoordY, 2)));
		
		if (kath1 == 0) {
			kath1 = 0.0000001;
		}
		if (kath2 == 0) {
			kath2 = 0.0000001;
		}
		if (hypo1 == 0) {
			hypo1 = 0.0000001;
		}
		if (hypo2 == 0) {
			hypo2 = 0.0000001;
		}
		
		if (kath1 > hypo1) {
			kath1 = hypo1;
		}
		if (kath2 > hypo2) {
			kath2 = hypo2;
		}
		
		angle1 = Math.asin(kath1 / hypo1);
		angle2 = Math.asin(kath2 / hypo2);
		
		angle1 = Math.toDegrees(angle1);
		angle2 = Math.toDegrees(angle2);
		
		if((((fromCoordX < lotPointX) && (toCoordX > lotPointX)) || ((fromCoordX > lotPointX) && (toCoordX < lotPointX))) || (((fromCoordY < lotPointY) && (toCoordY > lotPointY)) || ((fromCoordY > lotPointY) && (toCoordY < lotPointY)))) {
			angle = Math.abs(angle1 + angle2);
		} else {
			angle = Math.abs(angle1 - angle2);
		}
		immissionCorrection = 10 * Math.log10((angle) / (180));
		return immissionCorrection;
	}
	
	private Tuple<Integer,Integer> getZoneTuple(Coord coord) {
		 
		double xCoord = coord.getX();
		double yCoord = coord.getY();
		
		int xDirection = (int) ((xCoord - xCoordMin) / (receiverPointGap/1.));	
		int yDirection = (int) ((yCoordMax - yCoord) / receiverPointGap/1.);
		
		Tuple<Integer,Integer> zoneDefinition = new Tuple<Integer, Integer>(xDirection, yDirection);
		return zoneDefinition;
	}
	
	private Tuple<Integer,Integer> getZoneTupleDensityZones(Coord coord) {
		
		double xCoord = coord.getX();
		double yCoord = coord.getY();
		
		int xDirection = (int) ((xCoord - xCoordMin) / (gridCentroidGapDensity/1.));	
		int yDirection = (int) ((yCoordMax - yCoord) / (gridCentroidGapDensity/1.));
		
		Tuple<Integer,Integer> zoneDefinition = new Tuple<Integer, Integer>(xDirection, yDirection);
		
		return zoneDefinition;
	}
	
	private Id getNearestReceiverPoint (Coord coord) {
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

	public Map<Id, Coord> getReceiverPointId2Coord() {
		return receiverPointId2Coord;
	}
	
	public Map<Id, List<Coord>> getPersonId2listOfCoords() {
		return personId2listOfCoords;
	}

	public Map<Coord, Id> getActivityCoord2receiverPointId() {
		return activityCoord2receiverPointId;
	}
	
	public Map<Coord,Id> getCoord2receiverPointId() {
		return coord2receiverPointId;
	}	
	
	public Map<Id, List<Id>> getReceiverPointId2relevantLinkIds() {
		return receiverPointId2relevantLinkIds;
	}
	
	public Map<Id, Map<Id, Double>> getReceiverPointId2relevantLinkId2correctionTermAngle() {
		return receiverPointId2relevantLinkId2correctionTermAngle;
	}

	public Map<Id, Map<Id, Double>> getReceiverPointId2relevantLinkId2correctionTermDs() {
		return receiverPointId2relevantLinkId2correctionTermDs;
	}

	// for testing purposes
	public Map<Tuple<Integer, Integer>, List<Coord>> getZoneTuple2listOfActivityCoords() {
		return zoneTuple2listOfActivityCoords;
	}

	// for testing purposes
	public Map<Tuple<Integer, Integer>, List<Id>> getZoneTuple2listOfReceiverPointIds() {
		return zoneTuple2listOfReceiverPointIds;
	}
		
}
