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
import java.util.List;
import java.util.Map;

//import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;

public class GetNearestReceiverPoint {
	
//	private static final Logger log = Logger.getLogger(GetNearestReceiverPoint.class);
	
	public static Map<Id,Coord> receiverPoints = new HashMap<Id,Coord>();
	
//	static Map<Id,Coord> allReceiverPointIds2Coord = new HashMap<Id, Coord>();
	
	static Map<Id,List<Id>> receiverPointId2relevantLinkIds = new HashMap<Id, List<Id>>();
	static Map<Id, Map<Id,Double>> receiverPointId2RelevantLinkIds2Distances = new HashMap<Id, Map<Id,Double>>();
	static Map<Id, Map<Id, Map<Integer, Double>>> receiverPointId2RelevantLinkIds2partOfLinks2distance = new HashMap<Id, Map<Id,Map<Integer,Double>>>();
	static Map<Id, Map<Id,Double>> receiverPoint2RelevantlinkIds2lengthOfPartOfLinks = new HashMap<Id, Map<Id,Double>>();
	
	static Map<Id,List<Coord>> linkId2CoordinatesOfTheHelpfulNodes = new HashMap<Id,List<Coord>>();
	
	static Map<Coord,Id> activityCoord2receiverPointId = new HashMap<Coord,Id>();
	
	public static void getReceiverPoints(Scenario scenario) {
//		double receiverPointGap = 200.; //TODO
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
		
//		int counter = 0;
//		for(double y = yMax ; y > yMin ; y = y - receiverPointGap) {
//			for(double x = xMin ; x < xMax ; x = x + receiverPointGap) {
//				Coord newCoord = new CoordImpl(x, y);
//				receiverPoints.put(new IdImpl("coordId"+counter), newCoord);
//				counter++;
//			}
//		}
		
//		// Berlin_Test
//		int counter = 0;
//		for(double y = 5830000. ; y > 5810000 ; y = y - receiverPointGap) {
//			for(double x = 4580000 ; x < 4610000 ; x = x + receiverPointGap) {
//				Coord newCoord = new CoordImpl(x, y);
//				receiverPoints.put(new IdImpl("coordId"+counter), newCoord);
//				counter++;
		
		// SiouxFalls
		int counter = 0;
//		for(double y = 4832500. ; y > 4818500 ; y = y - (receiverPointGap)) {
//			for(double x = 678000 ; x < 688000 ; x = x + (receiverPointGap)) {
//				Coord newCoord = new CoordImpl(x, y);
//				receiverPoints.put(new IdImpl("coordId"+counter), newCoord);
////				System.out.println("receiverPoints: "+receiverPoints);
//				counter++;
//			}
//		}
		
		//TODO: only for samples
		for(Coord newCoord : GetActivityCoords.allActivityCoords) {
			int b = 0;
			int c = 0;
			int d = 0;
			for (Id coordId : receiverPoints.keySet()) {
				double x1 = receiverPoints.get(coordId).getX();
				double y1 = receiverPoints.get(coordId).getY();
				double x2 = newCoord.getX();
				double y2 = newCoord.getY();
				double distance = (Math.sqrt((Math.pow((x1-x2),2)+Math.pow((y1-y2),2))));
				
				if(distance<50.0) {
					b = 1;
				}else if(distance<150.0) {
					c = 1;
				}else if(distance<250.0) {
					d = 1;
				}
			}
			if ((b==0)&&(c==0)&&(d==0)) {
				receiverPoints.put(new IdImpl("coordId"+counter+"_"+newCoord.getX()+"_"+newCoord.getY()), newCoord);
				counter++;
				System.out.println("receiverPointCounter: "+counter);
			} else if ((b==0)&&(c==1)) {
				double random = Math.random();
				if(random<0.02) {
					receiverPoints.put(new IdImpl("coordId"+counter+"_"+newCoord.getX()+"_"+newCoord.getY()), newCoord);
					counter++;
					System.out.println("receiverPointCounter: "+counter);
				}
			} else if ((b==0)&&(c==0)&&(d==1)) {
				double random = Math.random();
				if(random<0.005) {
					receiverPoints.put(new IdImpl("coordId"+counter+"_"+newCoord.getX()+"_"+newCoord.getY()), newCoord);
					counter++;
					System.out.println("receiverPointCounter: "+counter);
				}
			}
		}
	}
	
	public static void getRelevantLinkIds(Scenario scenario) {
		// for faster computation, for noise immission-calculation, not all network links must be considered.
		for(Id pointId : receiverPoints.keySet()) {
			double pointCoordX = receiverPoints.get(pointId).getX();
			double pointCoordY = receiverPoints.get(pointId).getY();
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
//				double yAbschnitt2Original = pointCoordY - (pointCoordX*vector2);
			
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
				
				// TODO: Den folgenden Wert von 500m als Grenzwert für die Berücksichtigung validiern und anpassen
				if(distance<=500.){
					relevantLinkIds.add(linkId);
					double minDistance = (GetActivityCoords.getStreetWidth(scenario, linkId))/3;
					if(distance<minDistance) {
						distance = minDistance;
					}
					relevantLinkIds2distance.put(linkId, distance);
				}
			}
			receiverPointId2relevantLinkIds.put(pointId, relevantLinkIds);
			receiverPointId2RelevantLinkIds2Distances.put(pointId,relevantLinkIds2distance);
//			log.info(receiverPointId2RelevantLinkIds2Distances);
		}
		getDistancesToPartsOfLink(scenario,receiverPointId2RelevantLinkIds2Distances);
	}
		
//		
//			
//			// Markiere 20 Punkte auf dem Link in gleichen Abständen
//				
//			List<Coord> coordinatesOfTheHelpfulNodes = new ArrayList<Coord>();
//			coordinatesOfTheHelpfulNodes.clear();
//			
////			int numberOfHelpfulNodes = 11;
//			int numberOfHelpfulNodes = 201;
//				
//			for(int i = 0 ; i < numberOfHelpfulNodes ; i++){
//				Coord fromNode = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord();
//				double fromNodeX = fromNode.getX();
//				double fromNodeY = fromNode.getY();
//				Coord toNode = scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord();
//				double toNodeX = toNode.getX();
//				double toNodeY = toNode.getY();
//				double diffX = toNodeX - fromNodeX;
//				double diffY = toNodeY - fromNodeY;
//					
//				Coord coord = new CoordImpl(fromNodeX + i*(diffX/(numberOfHelpfulNodes-1)) , fromNodeY + i*(diffY/(numberOfHelpfulNodes-1)));
//				coordinatesOfTheHelpfulNodes.add(coord);
//			}
//			linkId2CoordinatesOfTheHelpfulNodes.put(linkId,coordinatesOfTheHelpfulNodes);
//		}
//		
//		for(Id pointId : receiverPoints.keySet()) {
//			double coordX = receiverPoints.get(pointId).getX();
//			double coordY = receiverPoints.get(pointId).getY();
//			
//			List<Id> relevantLinkIds = new ArrayList<Id>();
//			relevantLinkIds.clear();
//			
//			for (Id linkId : scenario.getNetwork().getLinks().keySet()){
//				boolean linkIsRelevant = false;
//				//TODO: Hier besser mit einer do-while-Schleife um Zeit zu sparen
//				for (int i = 0 ; i < linkId2CoordinatesOfTheHelpfulNodes.get(linkId).size() ; i++){
//					Coord coordinates = linkId2CoordinatesOfTheHelpfulNodes.get(linkId).get(i);
//					double X = coordinates.getX();
//					double Y = coordinates.getY();
//					// TODO: Den folgenden Wert von 500m als Grenzwert für die Berücksichtigung validiern und anpassen
//					if((Math.sqrt((Math.pow(X-coordX , 2))+(Math.pow(Y-coordY , 2))))<=600.){ //TODO
//						linkIsRelevant = true;
//					}else{
//					}
//				}
//				if(linkIsRelevant==true){
//					relevantLinkIds.add(linkId);
//				}else{
//				}
//			}
//			receiverPointId2relevantLinkIds.put(pointId, relevantLinkIds);
//		}
//		linkId2CoordinatesOfTheHelpfulNodes.clear();
//		
//		// für jedes Koordinatenpaar ...
//		for(Id pointId : receiverPointId2relevantLinkIds.keySet()){
//			// ... zu jedem relevantenLink
////			List<Map<Id,Double>> linkIds2AvgDistances = new ArrayList<Map<Id,Double>>();
//			Map<Id, Double> linkId2Distances = new HashMap<Id, Double>();
//			for(Id linkId : receiverPointId2relevantLinkIds.get(pointId)){
////				//Mittelpunkt--ReceiverPoint
////				double fromX = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX();
////				double fromY = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY();
////				double toX = scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord().getX();
////				double toY = scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord().getY();
////						
////				double midX = fromX + ((toX-fromX)/2);
////				double midY = fromY + ((toY-fromY)/2);
////						
////				double distance = 0.;
////				distance = (Math.sqrt((Math.pow(receiverPoints.get(pointId).getX()-midX , 2))+(Math.pow(receiverPoints.get(pointId).getY()-midY , 2))));
////				linkId2Distances.put(linkId,distance);
//				
//				//shortest distance to the link
//				System.out.println("coordId: "+pointId);
//				System.out.println("X: "+(receiverPoints.get(pointId).getX()));
//				System.out.println("Y: "+(receiverPoints.get(pointId).getY()));
//				System.out.println("linkId: "+linkId);
//				double fromX = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getX();
//				System.out.println("fromX: "+fromX);
//				double fromY = scenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord().getY();
//				System.out.println("fromY: "+fromY);
//				double toX = scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord().getX();
//				System.out.println("toX: "+toX);
//				double toY = scenario.getNetwork().getLinks().get(linkId).getToNode().getCoord().getY();
//				System.out.println("toY: "+toY);
//				
//				double vectorX = (toX-fromX)/50;
//				double vectorY = (toY-fromY)/50;
//						
//				double distance = 0.;
//				double minDistance = Double.MAX_VALUE;
//				
//				
//				for(int i = 0 ; i < 51 ; i++) {
//					
//					double tmpValue = 0.;
//					double tmpX = fromX + (i*vectorX);
//					double tmpY = fromY + (i*vectorY);
//					tmpValue = (Math.sqrt((Math.pow(receiverPoints.get(pointId).getX()-tmpX , 2))+(Math.pow(receiverPoints.get(pointId).getY()-tmpY , 2))));
//					if(tmpValue < minDistance) {
//						minDistance = tmpValue;
//					}
//				}
//				distance = minDistance;
//				linkId2Distances.put(linkId,distance);						
//			}
//			receiverPointId2RelevantLinkIds2Distances.put(pointId,linkId2Distances);
//			System.out.println(receiverPointId2RelevantLinkIds2Distances);
//		}
//		getDistancesToPartsOfLink(scenario , receiverPointId2RelevantLinkIds2Distances);
//	}
	
	public static Map<Id, Map<Id, Map<Integer, Double>>> getDistancesToPartsOfLink (Scenario scenario , Map<Id, Map<Id,Double>> receiverpointId2RelevantlinkIds2Distances) {
		
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
				
				double xValue = receiverPoints.get(pointId).getX();
				double yValue = receiverPoints.get(pointId).getY();
				
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
	
	public static Id getNearestReceiverPoint (Coord coord ) {
		Id nearestReceiverPointId = null;
		
		double xCoord = coord.getX();
		double yCoord = coord.getY();
		double distance = Double.MAX_VALUE;
		
		for(Id receiverPointId : receiverPoints.keySet()) {
			double xValue = receiverPoints.get(receiverPointId).getX();
			double yValue = receiverPoints.get(receiverPointId).getY();
			
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

}
