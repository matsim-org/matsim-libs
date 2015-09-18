/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.wrashid.PSF.data.hubCoordinates.hubLinkMapper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;
import org.matsim.contrib.parking.lib.obj.Matrix;

import playground.wrashid.lib.obj.GenericResult;
import playground.wrashid.lib.tools.kml.BasicPointVisualizer;

public class GenerateTableHubLinkMapping {

	public final static Integer unMappedLinkHubNumber=0;
	
	public static void main(String[] args) {
		Matrix matrix = GeneralLib
				.readStringMatrix("C:/Users/Admin/Desktop/psl-temp/GIS_coordinates_of_managers.txt");
		// key: hub number, value: linkIds
		LinkedListValueHashMap<Integer, Id<Link>> hubLinkMapping = new LinkedListValueHashMap<>();
		BasicPointVisualizer basicPointVisualizer = new BasicPointVisualizer();
		

		Object[] genericResult = getCornerCoordinates(matrix).getResult();

		Coord bottomLeft = (Coord) genericResult[0];
		Coord topRight = (Coord) genericResult[1];

		Network network = GeneralLib.readNetwork("C:/Users/Admin/Desktop/psl-temp/network.xml.gz");//tss, working as Admin... ;-)

		System.out.println("network loaded...");
		
		for (Link link : network.getLinks().values()) {
			if (isInBox(bottomLeft, topRight, link)) {
				Integer hubNumber=getHubNumberForLink(link,matrix);
				
				if (hubNumber!=null){
					hubLinkMapping.putAndSetBackPointer(hubNumber, link.getId());
				} else {
					//hubLinkMapping.putAndSetBackPointer(unMappedLinkHubNumber, link.getId());
					//basicPointVisualizer.addPointCoordinate(link.getCoord(), link.getId().toString(), Color.BLUE);
					//System.out.println(link.getId().toString());
				}
			} else {
				//hubLinkMapping.putAndSetBackPointer(unMappedLinkHubNumber, link.getId());
			}
		}
		
		writeResultToConsole(hubLinkMapping);
		
		
		//basicPointVisualizer.write("C:/Users/Admin/Desktop/psl-temp/unmappedLinks.kml");

	}

	private static void writeResultToConsole(LinkedListValueHashMap<Integer, Id<Link>> hubLinkMapping) {
		System.out.println("hubNumber" + "\t" +"linkId");
		for (Integer hubNumber:hubLinkMapping.getKeySet()){
			LinkedList<Id<Link>> linkIds = hubLinkMapping.get(hubNumber);
			
			linkIds=eliminateDuplicates(linkIds);
			
			for (int i=0;i<linkIds.size();i++){
				System.out.println(hubNumber + "\t" +linkIds.get(i).toString());
			}
		}		
	}

	public static LinkedList<Id<Link>> eliminateDuplicates(LinkedList<Id<Link>> linkIds) {
		LinkedList<Id<Link>> resultIds=new LinkedList<>();
		HashMap<String,Integer> hm=new HashMap<String, Integer>();
		
		for (Id<Link> linkId:linkIds){
			hm.put(linkId.toString(), null);
		}
		
		for (String linkIdString:hm.keySet()){
			resultIds.add(Id.create(linkIdString, Link.class));
		}
		
		return resultIds;
	}


	private static Integer getHubNumberForLink(Link link, Matrix matrix) {
		Random rand=new Random();
		// as the values used for the distance were average values, the spread of the sample needs to be bigger than 1
		double sampleSpread=3;
		double maxDistance=sampleSpread*100;
		
		
		
		double closestHubDistance=Double.MAX_VALUE;
		Integer closestHubNumber=null;
		
		
		for (int i=0;i<matrix.getNumberOfRows();i++){
			Coord currentHubManagerCoord= new Coord(matrix.getDouble(i, 1), matrix.getDouble(i, 2));
			double distance=GeneralLib.getDistance(link.getCoord(), currentHubManagerCoord);
			
			if (distance<maxDistance && distance<closestHubDistance){
				closestHubDistance=distance;
				closestHubNumber=matrix.getInteger(i, 0);
			}
		
		}
		
		return closestHubNumber;
	}

	private static boolean isInBox(Coord bottomLeft, Coord topRight, Link link) {
		if (link.getCoord().getX() > bottomLeft.getX()
				&& link.getCoord().getY() > bottomLeft.getY()) {
			if (link.getCoord().getX() < topRight.getX()
					&& link.getCoord().getY() < topRight.getY()) {
				return true;
			}
		}
		return false;
	}

	private static GenericResult getCornerCoordinates(Matrix matrix) {
		GenericResult genericResult;

		Coord bottomLeft = new Coord(Double.MAX_VALUE, Double.MAX_VALUE);
		Coord topRight = new Coord(Double.MIN_VALUE, Double.MIN_VALUE);

		for (int i = 0; i < matrix.getNumberOfRows(); i++) {
			double x = matrix.getDouble(i, 1);
			double y = matrix.getDouble(i, 2);

			if (x < bottomLeft.getX()) {
				bottomLeft.setX(x);
			}

			if (y < bottomLeft.getY()) {
				bottomLeft.setY(y);
			}

			if (x > topRight.getX()) {
				topRight.setX(x);
			}

			if (y > topRight.getY()) {
				topRight.setY(y);
			}
		}

		genericResult = new GenericResult(bottomLeft, topRight);
		return genericResult;
	}

}
