/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.withindayFW.analysis.trb2012;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.lib.obj.Matrix;

import playground.wrashid.parkingChoice.infrastructure.api.PParking;

public class PriceChangeBetweenIterations {

	public static void main(String[] args) {
		String basePath="H:/data/experiments/TRBAug2012/runs/run99/output/ITERS/";
		int firstIteration=1;
		int outputInterval=1;
		int lastIteration=18;
		
		String fileName= basePath + "it.0/0.publicParkingPricePerHourInTheMorning.txt";
		DoubleValueHashMap<Id<PParking>> oldPrices = readPrices(fileName);
		
		System.out.print("iterationNumber\t");
		System.out.print("numberOfRisingStreetParkingPrices\t");
		System.out.print("numberOfFallingStreetParkingPrices\t");
		System.out.print("numberOfStableStreetParkingPrices\t");
		System.out.print("numberOfRisingGarageParkingPrices\t");
		System.out.print("numberOfFallingGarageParkingPrices\t");
		System.out.println("numberOfStableGarageParkingPrices");
		
		for (int i=firstIteration;i<=lastIteration;i+=outputInterval){
			DoubleValueHashMap<Id<PParking>> newPrices = readPrices(basePath + "it." + i + "/" + i + ".publicParkingPricePerHourInTheMorning.txt");
			
			
			
			int numberOfRisingStreetParkingPrices=0;
			int numberOfFallingStreetParkingPrices=0;
			int numberOfStableStreetParkingPrices=0;
			
			int numberOfRisingGarageParkingPrices=0;
			int numberOfFallingGarageParkingPrices=0;
			int numberOfStableGarageParkingPrices=0;
			
			for (Id<PParking> parkingId:oldPrices.keySet()){
				if (parkingId.toString().contains("stp")){
					if(newPrices.get(parkingId)>oldPrices.get(parkingId)){
						numberOfRisingStreetParkingPrices++;
					} else if (newPrices.get(parkingId)<oldPrices.get(parkingId)){
						numberOfFallingStreetParkingPrices++;
					} else {
						numberOfStableStreetParkingPrices++;
					}
				} else if (parkingId.toString().contains("gp")){
					if(newPrices.get(parkingId)>oldPrices.get(parkingId)){
						numberOfRisingGarageParkingPrices++;
					} else if (newPrices.get(parkingId)<oldPrices.get(parkingId)){
						numberOfFallingGarageParkingPrices++;
					} else {
						numberOfStableGarageParkingPrices++;
					}
				} else {
					//DebugLib.stopSystemAndReportInconsistency();
				}
			}
			
			System.out.print(i);
			System.out.print("\t" + numberOfRisingStreetParkingPrices);
			System.out.print("\t" + numberOfFallingStreetParkingPrices);
			System.out.print("\t" + numberOfStableStreetParkingPrices);
			
			System.out.print("\t" + numberOfRisingGarageParkingPrices);
			System.out.print("\t" + numberOfFallingGarageParkingPrices);
			System.out.println("\t" + numberOfStableGarageParkingPrices);
			oldPrices=newPrices;
		}
		
	}

	private static DoubleValueHashMap<Id<PParking>> readPrices(String fileName) {
		DoubleValueHashMap<Id<PParking>> parkingPrice=new DoubleValueHashMap<>();
		
		Matrix morningMatrix = GeneralLib.readStringMatrix(fileName,"\t");
		
		for (int i=1;i<morningMatrix.getNumberOfRows();i++){
			Id<PParking> id=Id.create(morningMatrix.getString(i, 0), PParking.class);
			double price=morningMatrix.getDouble(i, 1);
			parkingPrice.put(id, price);
		}
		return parkingPrice;
	}
	
}
