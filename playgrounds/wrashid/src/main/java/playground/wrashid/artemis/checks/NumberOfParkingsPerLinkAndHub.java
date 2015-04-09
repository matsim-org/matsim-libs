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

package playground.wrashid.artemis.checks;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.contrib.parking.lib.obj.Matrix;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;

import java.util.LinkedList;

public class NumberOfParkingsPerLinkAndHub {

	public static void main(String[] args) {
		Matrix stringMatrix = GeneralLib.readStringMatrix("H:/data/experiments/ARTEMIS/nov2011/inputs/linkHubMappings/linkHub.mappingTable.txt", "\t");
		NetworkImpl network = (NetworkImpl) GeneralLib.readNetwork("H:/data/experiments/TRBAug2011/runs/ktiRun45/output/output_network.xml.gz");
		String parkingBasePath =	"H:/data/experiments/TRBAug2011/parkings/flat/";
		
		LinkedList<PParking> privateParkingCityZH=getParkingCollection(parkingBasePath + "privateParkings_v1_kti.xml");
		LinkedList<PParking> streetParkings=getParkingCollection(parkingBasePath + "streetParkings.xml");
		LinkedList<PParking> garageParkings=getParkingCollection(parkingBasePath + "garageParkings.xml");
		LinkedList<PParking> publicParkingsOutsideZH=getParkingCollection(parkingBasePath + "publicParkingsOutsideZHCity_v0_dilZh30km_10pct.xml");
		
		stringMatrix.putString(0, 2, "privatParkings");
		stringMatrix.putString(0, 3, "streetParkings");
		stringMatrix.putString(0, 4, "garageParkings");
		stringMatrix.putString(0, 5, "publicParkingsOutsideZHCity");
		
		IntegerValueHashMap<Id> numberOfPrivateParkingsAttachedToLinks = getNumberOfParkingsPerLink(network, privateParkingCityZH);
		IntegerValueHashMap<Id> numberOfStreetParkingsAttachedToLinks = getNumberOfParkingsPerLink(network, streetParkings);
		IntegerValueHashMap<Id> numberOfGarageParkingsAttachedToLinks = getNumberOfParkingsPerLink(network, garageParkings);
		IntegerValueHashMap<Id> numberOfPublicParkingsOutsideCityAttachedToLinks = getNumberOfParkingsPerLink(network, publicParkingsOutsideZH);
		
		
		for (int i=1;i<stringMatrix.getNumberOfRows();i++){
			Id<Link> linkId=Id.create(stringMatrix.getString(i, 1), Link.class);
			
			stringMatrix.putString(i, 2, Integer.toString(numberOfPrivateParkingsAttachedToLinks.get(linkId)));
			stringMatrix.putString(i, 3, Integer.toString(numberOfStreetParkingsAttachedToLinks.get(linkId)));
			stringMatrix.putString(i, 4, Integer.toString(numberOfGarageParkingsAttachedToLinks.get(linkId)));
			stringMatrix.putString(i, 5, Integer.toString(numberOfPublicParkingsOutsideCityAttachedToLinks.get(linkId)));
			
		}
		
		stringMatrix.writeMatrix("H:/data/experiments/ARTEMIS/nov2011/analysis/numberOfParkingsPerLinkAndHub.txt");
		
		
		
	}

	private static IntegerValueHashMap<Id> getNumberOfParkingsPerLink(NetworkImpl network,
			LinkedList<PParking> privateParkingCityZH) {
		IntegerValueHashMap<Id> numberOfParkingsAttachedToLinks=new IntegerValueHashMap<Id>();
		
		for (PParking parking:privateParkingCityZH){
			Id closestLinkId = NetworkUtils.getNearestLink(network, parking.getCoord()).getId();
			numberOfParkingsAttachedToLinks.incrementBy(closestLinkId, (int) Math.round(parking.getCapacity()));
		}
		return numberOfParkingsAttachedToLinks;
	}
	
	private static LinkedList<PParking> getParkingCollection(String path){
		LinkedList<PParking> parkingCollection=new LinkedList<PParking>();
		ParkingHerbieControler.readParkings(1.0, path, parkingCollection);
		return parkingCollection;
	}
	
}
