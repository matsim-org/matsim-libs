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
package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import java.util.HashSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;

public class SearchTraffic {

	
	
	public static void main(String[] args) {
		String eventsFile="C:/data/parkingSearch/psim/zurich/output/run21/output/";
		Coord center = ParkingHerbieControler.getCoordinatesLindenhofZH();
		double radius=2500;
		Network network = GeneralLib.readNetwork("c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/output_network.xml.gz");
		
		EventsManager events = EventsUtils.createEventsManager();

		TrafficOnRoadsCount trafficOnRoadsCount = new TrafficOnRoadsCount();
		
		events.addHandler(trafficOnRoadsCount);
		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventsFile);
	}
	
	private static class TrafficOnRoad{
		
		int binSize=60*24;
		int numberOfBins=binSize;
		HashSet<Id>[] bins=new HashSet[numberOfBins];
		
		TrafficOnRoad(){
			for (int i=0;i<numberOfBins;i++){
				bins[i]=new HashSet<Id>();
			}
		}
		
		public void registerAgentOnRoad(Id personId, double startTime, double endTime){
			int startIndex=(int) Math.round(GeneralLib.projectTimeWithin24Hours(startTime)) /binSize;
			int endIndex=(int) Math.round(GeneralLib.projectTimeWithin24Hours(endTime))/binSize;
			
			if (startIndex==endIndex){
				bins[startIndex].add(personId);
			} else if (startIndex<endIndex){
				for (int i=startIndex;i<=endIndex;i++){
					bins[i].add(personId);
				}
			}else if (startIndex>endIndex){
				for (int i=startIndex;i<numberOfBins;i++){
					bins[i].add(personId);
				}
				for (int i=0;i<=endIndex;i++){
					bins[i].add(personId);
				}
			}
			
		}
		
	}
	
	private static class TrafficOnRoadsCount implements 
	Wait2LinkEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler{

		DoubleValueHashMap<Id> linkEnterTime=new DoubleValueHashMap<Id>();

		public TrafficOnRoadsCount(){
		}
		
		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
//			if (event.getDriverId().equals(filterEventsForAgentId)){
//				System.out.println(event.toString());
//			}
		}


		@Override
		public void handleEvent(LinkEnterEvent event) {
			linkEnterTime.put(event.getDriverId(), event.getTime());
		}

		@Override
		public void handleEvent(Wait2LinkEvent event) {
			linkEnterTime.put(event.getPersonId(), event.getTime());
		}

	}

}

