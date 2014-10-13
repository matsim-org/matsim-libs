/* *********************************************************************** *
 * project: org.matsim.*
 * EnergyConsumptionTable.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.wrashid.PSF2.vehicle.energyConsumption;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;
import org.matsim.vehicles.VehicleType;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;
import playground.wrashid.lib.obj.math.Polynomial;

public class EnergyConsumptionTable {

	// Id: vehicleClassId
	// Double: Free speed on link
	// Polynomial: polynomial, where the driven average speed can be given as input
	// in order to get the energy consumption as output
	TwoHashMapsConcatenated<Id<VehicleType>,Double, Polynomial> energyConsumptionRegressionModel=new TwoHashMapsConcatenated<>();
	
	public EnergyConsumptionTable(String filePathName){		
		Matrix stringMatrix = GeneralLib.readStringMatrix(filePathName);
		
		for (int i=1;i<stringMatrix.getNumberOfRows();i++){
			Id<VehicleType> vehicleClassId=Id.create(stringMatrix.getString(i, 0), VehicleType.class);
			double linkFreeSpeed=stringMatrix.getDouble(i, 1);
			
			int numberOfCoefficients=stringMatrix.getNumberOfColumnsInRow(i)-2;
			
			double[] coefficients=new double[numberOfCoefficients];
			
			for (int j=0;j<numberOfCoefficients;j++){
				coefficients[j]=stringMatrix.getDouble(i, stringMatrix.getNumberOfColumnsInRow(i)-j-1);
			}
			
			Polynomial polynomial=new Polynomial(coefficients);
			
			energyConsumptionRegressionModel.put(vehicleClassId, linkFreeSpeed, polynomial);
		}
		
	}
	
	public double getEnergyConsumptionInJoule(Id<VehicleType> vehicleClassId, double averageDrivenSpeedOnLinkInKmPerHour, double linkFreeSpeedInMetersPerSecond, double linkLengthInMeters){
		
		double linkLengthInKm=linkLengthInMeters/1000;
		double linkFreeSpeedInKmPerHour=linkFreeSpeedInMetersPerSecond/1000*3600;
		linkFreeSpeedInKmPerHour=mapFreeSpeedToClosestChoice(linkFreeSpeedInKmPerHour);
		
		double energyConsumptionInJoulePerKm= energyConsumptionRegressionModel.get(vehicleClassId, linkFreeSpeedInKmPerHour).evaluate(averageDrivenSpeedOnLinkInKmPerHour);
		
		return linkLengthInKm*energyConsumptionInJoulePerKm;
	}
	
	/**
	 * Only predefined free speed values are allowed (for which the values must be available in the data set)
	 * @param linkFreeSpeedInKmPerHour
	 * @return
	 */
	private double mapFreeSpeedToClosestChoice(double linkFreeSpeedInKmPerHour){
		double[] freeSpeedChoiceSetInKmPerHour= {30.0,50.0,60.0,90.0,120};
		
		double minimumSpeedDelta=Double.MAX_VALUE;
		int indexOfClosestFreeSpeed=-1;
		
		for (int i=0;i<freeSpeedChoiceSetInKmPerHour.length;i++){
			double currentSpeedDelta=freeSpeedChoiceSetInKmPerHour[i]-linkFreeSpeedInKmPerHour;
			if (currentSpeedDelta<minimumSpeedDelta){
				minimumSpeedDelta=currentSpeedDelta;
				indexOfClosestFreeSpeed=i;
			}
		}
		
		return freeSpeedChoiceSetInKmPerHour[indexOfClosestFreeSpeed];
	}
	
}
