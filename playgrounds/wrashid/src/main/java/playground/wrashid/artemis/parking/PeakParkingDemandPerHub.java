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

package playground.wrashid.artemis.parking;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingIntervalInfo;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.artemis.hubs.LinkHubMapping;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingOccupancyBins;

public class PeakParkingDemandPerHub {

	public static void main(String[] args) {
		String networkPath="H:/data/experiments/ARTEMIS/output/run10/output_network.xml.gz";
		Network network= GeneralLib.readNetwork(networkPath);
		
		String linkHubMappingTable = "H:/data/experiments/ARTEMIS/zh/dumb charging/input/run1/linkHub_orig.mappingTable.txt";
		LinkHubMapping linkHubMapping = new LinkHubMapping(linkHubMappingTable);
		
		HashMap<Id, ParkingOccupancyBins> hubIdParkingOccupancies=new HashMap<Id, ParkingOccupancyBins>();

		String eventsFile="H:/data/experiments/ARTEMIS/output/run10/ITERS/it.50/50.events.txt.gz";
		EventsManager events = EventsUtils.createEventsManager();

		ParkingTimesPlugin parkingTimesPlugin = new ParkingTimesPlugin();
		
		events.addHandler(parkingTimesPlugin);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		
		reader.readFile(eventsFile);
		
		parkingTimesPlugin.closeLastAndFirstParkingIntervals();
		
		accumulateHubParkingOccupancyStatistics(linkHubMapping, hubIdParkingOccupancies, parkingTimesPlugin);
		
		printPeakOccupancyStatistics(hubIdParkingOccupancies, linkHubMapping);
	}

	private static void printPeakOccupancyStatistics(HashMap<Id, ParkingOccupancyBins> hubIdParkingOccupancies, LinkHubMapping linkHubMapping) {
		System.out.println("hubId\tpeakParkingOccupancy");
		
		List<Id<Link>> hubIds = playground.wrashid.lib.obj.Collections.getSortedKeySet(linkHubMapping.getHubs());
		
		for (Id<Link> hubId:hubIds){
			int peakOccupancy=0;
			if (hubIdParkingOccupancies.containsKey(hubId)){
				peakOccupancy=hubIdParkingOccupancies.get(hubId).getPeakOccupanyOfDay();
			}
			System.out.println(hubId.toString() +"\t"+peakOccupancy);
		}
	}

	private static void accumulateHubParkingOccupancyStatistics(LinkHubMapping linkHubMapping,
			HashMap<Id, ParkingOccupancyBins> hubIdParkingOccupancies, ParkingTimesPlugin parkingTimesPlugin) {
		for (Id personId: parkingTimesPlugin.getParkingTimeIntervals().getKeySet()){
			LinkedList<ParkingIntervalInfo> personParkingTimes = parkingTimesPlugin.getParkingTimeIntervals().get(personId);
			for (ParkingIntervalInfo parkingIntervalInfo : personParkingTimes) {
				initParkingOccupancyHashMap(linkHubMapping, hubIdParkingOccupancies, parkingIntervalInfo);
				
				hubIdParkingOccupancies.get(linkHubMapping.getHubIdForLinkId(parkingIntervalInfo.getLinkId())).inrementParkingOccupancy(parkingIntervalInfo.getArrivalTime(), parkingIntervalInfo.getDepartureTime());
			}
		}
	}

	private static void initParkingOccupancyHashMap(LinkHubMapping linkHubMapping,
			HashMap<Id, ParkingOccupancyBins> hubIdParkingOccupancies, ParkingIntervalInfo parkingIntervalInfo) {
		if (!hubIdParkingOccupancies.containsKey(linkHubMapping.getHubIdForLinkId(parkingIntervalInfo.getLinkId()))){
			hubIdParkingOccupancies.put(linkHubMapping.getHubIdForLinkId(parkingIntervalInfo.getLinkId()), new ParkingOccupancyBins());
		}
	}
	
	
	
}
