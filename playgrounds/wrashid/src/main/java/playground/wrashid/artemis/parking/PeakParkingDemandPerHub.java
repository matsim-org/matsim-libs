package playground.wrashid.artemis.parking;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.NetworkImpl;

import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingIntervalInfo;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.artemis.hubs.LinkHubMapping;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingOccupancyBins;

public class PeakParkingDemandPerHub {

	public static void main(String[] args) {
		String networkPath="H:/data/experiments/ARTEMIS/output/run10/output_network.xml.gz";
		NetworkImpl network= GeneralLib.readNetwork(networkPath);
		
		String linkHubMappingTable = "H:/data/experiments/ARTEMIS/zh/dumb charging/input/run1/linkHub_orig.mappingTable.txt";
		LinkHubMapping linkHubMapping = new LinkHubMapping(linkHubMappingTable);
		
		HashMap<Id, ParkingOccupancyBins> hubIdParkingOccupancies=new HashMap<Id, ParkingOccupancyBins>();

		String eventsFile="H:/data/experiments/ARTEMIS/output/run10/ITERS/it.50/50.events.txt.gz";
		EventsManagerImpl events = new EventsManagerImpl();

		ParkingTimesPlugin parkingTimesPlugin = new ParkingTimesPlugin();
		
		events.addHandler(parkingTimesPlugin);
		
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		
		reader.readFile(eventsFile);
		
		parkingTimesPlugin.closeLastAndFirstParkingIntervals();
		
		accumulateHubParkingOccupancyStatistics(linkHubMapping, hubIdParkingOccupancies, parkingTimesPlugin);
		
		System.out.println("hubId\tpeakParkingOccupancy");
		GeneralLib.printHashmapToConsole(hubIdParkingOccupancies);
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
