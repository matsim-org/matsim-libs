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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;

public class SearchTrafficLength {

	public static void main(String[] args) {
		String eventsFile="C:/data/parkingSearch/psim/zurich/output/run20/output/ITERS/it.2.events.xml.gz";
		Coord center = ParkingHerbieControler.getCoordinatesLindenhofZH();
		double radius=2500;
		Network network = GeneralLib.readNetwork("c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/output_network.xml.gz");
		
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();

		TravelDistanceLength trafficOnRoadsCount = new TravelDistanceLength(center,network,radius);
		
		events.addHandler(trafficOnRoadsCount);
		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventsFile);
		
		trafficOnRoadsCount.printTravelDistances();
	}
	
	private static class TravelDistanceLength implements 
	Wait2LinkEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler{

		int numberOfBins=24*4;
		int binSize=24*3600/numberOfBins;
		private Coord center;
		private Network network;
		private double radius;
		double[] travelledDistance=new double[numberOfBins];

		public TravelDistanceLength(Coord center, Network network, double radius){
			this.center = center;
			this.network = network;
			this.radius = radius;
		}
		
		public void printTravelDistances(){
			System.out.println("binNumber\ttravelDistance[m]");
			for (int i=0;i<numberOfBins;i++){
				System.out.println(i + "\t" + travelledDistance[i]);
			}
		}
		
		@Override
		public void reset(int iteration) {
			
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Link link = network.getLinks().get(event.getLinkId());
			if (GeneralLib.getDistance(center, link) <radius){
				int index=(int) Math.floor(GeneralLib.projectTimeWithin24Hours(event.getTime())) /binSize;
				travelledDistance[index]+=link.getLength();
			}
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void handleEvent(VehicleEntersTrafficEvent event) {
			// TODO Auto-generated method stub
			
		}

	}

}

