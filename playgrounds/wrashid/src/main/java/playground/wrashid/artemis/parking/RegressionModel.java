package playground.wrashid.artemis.parking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import playground.wrashid.artemis.hubs.LinkHubMapping;

import java.util.List;

public class RegressionModel {

	private static LinkHubMapping linkHubMapping;
	
	public static void main(String[] args) {
		String linkHubMappingTable = "C:/eTmp/run10/linkHub_orig.mappingTable.txt";
		linkHubMapping=new LinkHubMapping(linkHubMappingTable);
		
		DoubleValueHashMap<Id> numberOfKilometersOfStreetsPerHub = getNumberOfKilometersStreetPerHub();
		
		DoubleValueHashMap<Id> numberOfStreetParkingsPerHub = getNumberOfStreetParkingsPerHub();
		
		DoubleValueHashMap<Id> numberOfGarageParkingsPerHub = getNumberOfGarageParkingsPerHub();
		
		
		printParkingSupplyStatistics(numberOfKilometersOfStreetsPerHub, numberOfStreetParkingsPerHub,
				numberOfGarageParkingsPerHub);
	}




	private static void printParkingSupplyStatistics(DoubleValueHashMap<Id> numberOfKilometersOfStreetsPerHub,
			DoubleValueHashMap<Id> numberOfStreetParkingsPerHub, DoubleValueHashMap<Id> numberOfGarageParkingsPerHub) {
		System.out.println("hubId\tnumberOfKilometersOfStreetsPerHub\tnumberOfStreetParkingsPerHub\tnumberOfGarageParkingsPerHub");
		
		List<Id<Link>> hubIds = playground.wrashid.lib.obj.Collections.getSortedKeySet(linkHubMapping.getHubs());

		for (Id<Link> hubId:hubIds){
			double nuberOfKilometersOfStreet=numberOfKilometersOfStreetsPerHub.keySet().contains(hubId)?numberOfKilometersOfStreetsPerHub.get(hubId):0;
			double numberOfStreetParkings=numberOfStreetParkingsPerHub.keySet().contains(hubId)?numberOfStreetParkingsPerHub.get(hubId):0;
			double numberOfGarageParkings=numberOfGarageParkingsPerHub.keySet().contains(hubId)?numberOfGarageParkingsPerHub.get(hubId):0;
			
			System.out.println(hubId + "\t" + nuberOfKilometersOfStreet + "\t" +  numberOfStreetParkings  + "\t" + numberOfGarageParkings);
		}
		
	}

	 
	
	
	private static DoubleValueHashMap<Id> getNumberOfStreetParkingsPerHub() {
		NetworkImpl network = getNetwork();
		String inputGarageFacilitiesPath="C:/data/My Dropbox/ETH/Projekte/Parkplätze/data zürich/facilities/output/streetpark_facilities.xml";
		
		return getNumberOfParkingsPerHub(network, inputGarageFacilitiesPath);
	}




	private static DoubleValueHashMap<Id> getNumberOfGarageParkingsPerHub() {
		NetworkImpl network = getNetwork();
		String inputGarageFacilitiesPath="C:/data/My Dropbox/ETH/Projekte/Parkplätze/data zürich/facilities/output/garage_facilities_ohne öffnungszeiten.xml";
		
		return getNumberOfParkingsPerHub(network, inputGarageFacilitiesPath);
	}




	private static DoubleValueHashMap<Id> getNumberOfParkingsPerHub(NetworkImpl network, String inputParkingFacilitiesPath) {
		ActivityFacilities garageParkingFacilities = GeneralLib.readActivityFacilities(inputParkingFacilitiesPath);
		
		DoubleValueHashMap<Id> numberOfParkingsPerHub = new DoubleValueHashMap<Id>();
		
		for (ActivityFacility parkingGarage:garageParkingFacilities.getFacilities().values()){
			numberOfParkingsPerHub.incrementBy(linkHubMapping.getHubIdForLinkId(NetworkUtils.getNearestLink(network, parkingGarage.getCoord()).getId()),parkingGarage.getActivityOptions().get("parking").getCapacity());
		}
		return numberOfParkingsPerHub;
	}

	private static NetworkImpl getNetwork() {
		String inputNetworkPath="C:/eTmp/run10/output_network.xml.gz";
		NetworkImpl network= (NetworkImpl) GeneralLib.readNetwork(inputNetworkPath);
		return network;
	}

	

	private static DoubleValueHashMap<Id> getNumberOfKilometersStreetPerHub() {
		NetworkImpl network = getNetwork();
		
		DoubleValueHashMap<Id> numberOfKilometersOfStreetsInHub = new DoubleValueHashMap<Id>();
		
		for (Link link:network.getLinks().values()){
			if (isNotHighwayOrCountryRoad(link)){
				numberOfKilometersOfStreetsInHub.incrementBy(linkHubMapping.getHubIdForLinkId(link.getId()),link.getLength()/1000);
			}
		}
		return numberOfKilometersOfStreetsInHub;
	}

	


	private static boolean isNotHighwayOrCountryRoad(Link link) {
		if (link.getFreespeed()<70.0*1000.0/3600){
			return true;
		}
		
		return false;
	}


	

}
