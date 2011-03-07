package playground.wrashid.artemis.parking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.DoubleValueHashMap;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.lib.obj.StringMatrix;

public class RegressionModel {

	public static void main(String[] args) {
		DoubleValueHashMap<Id> numberOfKilometersOfStreetsPerHub = getNumberOfKilometersStreetPerHub();
		
		DoubleValueHashMap<Id> numberOfStreetParkingsPerHub = getNumberOfStreetParkingsPerHub();
		
		DoubleValueHashMap<Id> numberOfGarageParkingsPerHub = getNumberOfGarageParkingsPerHub();
		
		
		System.out.println("hubId\tnumberOfKilometersOfStreetsPerHub\tnumberOfStreetParkingsPerHub\tnumberOfGarageParkingsPerHub");
		for (Id hubId:numberOfKilometersOfStreetsPerHub.keySet()){
			System.out.println(hubId + "\t" + numberOfKilometersOfStreetsPerHub.get(hubId) + "\t" +  numberOfStreetParkingsPerHub.get(hubId)  + "\t" + numberOfGarageParkingsPerHub.get(hubId));
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
		ActivityFacilitiesImpl garageParkingFacilities = GeneralLib.readActivityFacilities(inputParkingFacilitiesPath);
		
		DoubleValueHashMap<Id> numberOfParkingsPerHub = new DoubleValueHashMap<Id>();
		
		for (ActivityFacility parkingGarage:garageParkingFacilities.getFacilities().values()){
			numberOfParkingsPerHub.incrementBy(LinkHubMapping.getHubIdForLinkId(network.getNearestLink(parkingGarage.getCoord()).getId()),parkingGarage.getActivityOptions().get("parking").getCapacity());
		}
		return numberOfParkingsPerHub;
	}

	private static NetworkImpl getNetwork() {
		String inputNetworkPath="C:/eTmp/run10/output_network.xml.gz";
		NetworkImpl network= GeneralLib.readNetwork(inputNetworkPath);
		return network;
	}

	

	private static DoubleValueHashMap<Id> getNumberOfKilometersStreetPerHub() {
		NetworkImpl network = getNetwork();
		
		DoubleValueHashMap<Id> numberOfKilometersOfStreetsInHub = new DoubleValueHashMap<Id>();
		
		for (Link link:network.getLinks().values()){
			if (isNotHighwayOrCountryRoad(link)){
				numberOfKilometersOfStreetsInHub.incrementBy(LinkHubMapping.getHubIdForLinkId(link.getId()),link.getLength()/1000);
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


	private static class LinkHubMapping {
		static StringMatrix matrix;
		static LinkedListValueHashMap<Id, Id> hubIdLinkIdMapping;
		static {
			String linkHubMappingTable = "C:/eTmp/run10/linkHub_orig.mappingTable.txt";
			matrix = GeneralLib.readStringMatrix(linkHubMappingTable);

			hubIdLinkIdMapping = new LinkedListValueHashMap<Id, Id>();

			for (int i = 1; i < matrix.getNumberOfRows(); i++) {
				String hubId = matrix.getString(i, 0);
				String linkId = matrix.getString(i, 1);
				hubIdLinkIdMapping.putAndSetBackPointer(new IdImpl(hubId), new IdImpl(linkId));
			}
		}

		public static Id getHubIdForLinkId(Id linkId) {
			return hubIdLinkIdMapping.getKey(linkId);
		}

		public static Id getLinkIdForHubId(Id hubId) {
			return hubIdLinkIdMapping.getValue(hubId);

		}

	}

}
