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
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;

public class SearchTrafficDeltaCountForGIS {

	public static void main(String[] args) {
		String eventsFileBaseCase="C:/data/parkingSearch/psim/zurich/output/run21/output/ITERS/it.2.events.xml.gz";
		String eventsFileWithSearch="C:/data/parkingSearch/psim/zurich/output/run20/output/ITERS/it.2.events.xml.gz";
		Coord center = ParkingHerbieControler.getCoordinatesLindenhofZH();
		double radius=2500;
		Network network = GeneralLib.readNetwork("c:/data/parkingSearch/psim/zurich/inputs/ktiRun24/output_network.xml.gz");
		
		TrafficCount trafficCountBaseCase = getTraffiCounts(eventsFileBaseCase, center, radius, network);
		TrafficCount trafficCountWithSearchTraffic = getTraffiCounts(eventsFileWithSearch, center, radius, network);
		
		printLinkCounts(trafficCountBaseCase, trafficCountWithSearchTraffic,network);
	}

	private static void printLinkCounts(TrafficCount trafficCountBaseCase,
			TrafficCount trafficCountWithSearchTraffic, Network network) {
		HashSet<Id> jointLinkSet=new HashSet<Id>();
		jointLinkSet.addAll(trafficCountBaseCase.getLinkCounts().getKeySet());
		jointLinkSet.addAll(trafficCountWithSearchTraffic.getLinkCounts().getKeySet());
		
		System.out.println("linkId\tx\ty\tcountsA\tcountsB\tdeltaCountsBMinusA");
		for (Id linkId:jointLinkSet){
			Coord coord = network.getLinks().get(linkId).getCoord();
			int delta=trafficCountWithSearchTraffic.getLinkCounts().get(linkId)-trafficCountBaseCase.getLinkCounts().get(linkId);
			System.out.println(linkId + "\t" + coord.getX()+ "\t" + coord.getY() + "\t" + trafficCountBaseCase.getLinkCounts().get(linkId)+ "\t" + trafficCountWithSearchTraffic.getLinkCounts().get(linkId) + "\t" + delta);
		}
	}

	private static TrafficCount getTraffiCounts(
			String eventsFileBaseCase, Coord center, double radius,
			Network network) {
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		TrafficCount trafficOnRoadsCount = new TrafficCount(center,network,radius);
		events.addHandler(trafficOnRoadsCount);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(eventsFileBaseCase);
		return trafficOnRoadsCount;
	}
	
	private static class TrafficCount implements 
	Wait2LinkEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler{

		private Coord center;
		private Network network;
		private double radius;
		private IntegerValueHashMap<Id> linkCounts;

		public TrafficCount(Coord center, Network network, double radius){
			this.center = center;
			this.network = network;
			this.radius = radius;
			setLinkCounts(new IntegerValueHashMap<Id>());
		}
		
		@Override
		public void reset(int iteration) {
			
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Link link = network.getLinks().get(event.getLinkId());
			if (GeneralLib.getDistance(center, link) <radius){
				getLinkCounts().increment(link.getId());
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

		public IntegerValueHashMap<Id> getLinkCounts() {
			return linkCounts;
		}

		private void setLinkCounts(IntegerValueHashMap<Id> counts) {
			this.linkCounts = counts;
		}

	}

}

