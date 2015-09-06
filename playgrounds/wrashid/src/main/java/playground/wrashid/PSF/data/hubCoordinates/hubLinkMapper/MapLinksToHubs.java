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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.HashMapInverter;
import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;
import org.matsim.contrib.parking.lib.obj.Matrix;


// TODO: I could it make faster by introducing hierarchical mapping 
// OR: just let it run for a very long time...
public class MapLinksToHubs {
	public static void main(String[] args) {
		// key: linkId, value: hub number
		HashMap<Id,Integer> linkHubMapping=new HashMap<Id,Integer>();
		// key: hub number, value: linkIds
		LinkedListValueHashMap<Integer,LinkedList<Id>> hubLinkMapping=new LinkedListValueHashMap<Integer,LinkedList<Id>>();
		Matrix matrix=GeneralLib.readStringMatrix("A:/data/ewz daten/GIS_coordinates_of_managers.txt");
		Network network=GeneralLib.readNetwork("A:/data/matsim/input/runRW1003/network-osm-ch.xml.gz");
		
		double averageHubSubManagers=6.381134321058625;
		
		// this is a N^2/expensive operation: browse through all network links for each "HubSubManagers"
	
		int numberOfLinksProcessedStatistics=0;
		for (Link link:network.getLinks().values()){
			for (int i=0;i<matrix.getNumberOfRows();i++){
				Coord coordinateOfCurrentHubSubManager= new Coord(matrix.getDouble(i, 1), matrix.getDouble(i, 2));
				
				if (GeneralLib.getDistance(link.getCoord(), coordinateOfCurrentHubSubManager)<averageHubSubManagers){
					linkHubMapping.put(link.getId(), matrix.convertDoubleToInteger(i, 0));
				}
			}
			numberOfLinksProcessedStatistics++;
			if (numberOfLinksProcessedStatistics % 1000==0){
				System.out.println("number of links processed:" + numberOfLinksProcessedStatistics);
			}
		}
		
		hubLinkMapping=initHubLinkMapping(linkHubMapping);
		
		printAllLinkHubMappingsToConsole(hubLinkMapping);
	}
	
	private static LinkedListValueHashMap<Integer,LinkedList<Id>> initHubLinkMapping(HashMap<Id,Integer> linkHubMapping){
		return new HashMapInverter(linkHubMapping).getLinkedListValueHashMap();
	}
	
	private static int getNumberOfHubs(Matrix matrix){
		int numberOfHubs=Integer.MIN_VALUE;
		for (int i=0;i<matrix.getNumberOfRows();i++){
			int currentHubNumber=matrix.convertDoubleToInteger(i, 0);
			if (currentHubNumber>numberOfHubs){
				numberOfHubs=currentHubNumber;
			}
		}
		return numberOfHubs;
	}

	private static void printAllLinkHubMappingsToConsole(LinkedListValueHashMap<Integer,LinkedList<Id>> hubLinkMapping) {
		for (int i=0;i<hubLinkMapping.getNumberOfEntriesInLongestList();i++){
			// assumption, that ids of hubs start with 1!!!
			for (int j=1;i<=hubLinkMapping.size();j++){
				if (i<hubLinkMapping.get(j).size()){
					System.out.print(hubLinkMapping.get(j).get(i));
				} else {
					System.out.print(-1.0);
				}
				
				if (j!=hubLinkMapping.size()){
					System.out.print("\t");
				}
				
			}
			System.out.println("\n");
		}
	}
}
