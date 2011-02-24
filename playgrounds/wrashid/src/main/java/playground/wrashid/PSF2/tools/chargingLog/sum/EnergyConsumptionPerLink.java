/* *********************************************************************** *
 * project: org.matsim.*
 * EnergyConsumptionPerLink.java
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

package playground.wrashid.PSF2.tools.chargingLog.sum;


import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.DoubleValueHashMap;
import playground.wrashid.lib.obj.StringMatrix;

public class EnergyConsumptionPerLink {

	public static void main(String[] args) {
		String chargingLogFileNamePath="H:/data/experiments/ARTEMIS/zh/dumb charging/output/run2/ITERS/it.0/0.chargingLog.txt";
		DoubleValueHashMap<String> energyConsumptionPerLink = readChargingLog(chargingLogFileNamePath);
		
		System.out.println("linkId\tenergyConsumption");
		energyConsumptionPerLink.printToConsole();
		
		System.out.println("link with highest energyConsumption: " + energyConsumptionPerLink.getKeyForMaxValue());
		
	}

	public static DoubleValueHashMap<String> readChargingLog(String chargingLogFileNamePath) {
		StringMatrix matrix=GeneralLib.readStringMatrix(chargingLogFileNamePath);
		DoubleValueHashMap<String> energyConsumptionPerLink=new DoubleValueHashMap<String>();
		
		
		// starting with index 1 (ignoring first line)
		for (int i=1;i<matrix.getNumberOfRows();i++){
			String linkId=matrix.getString(i, 0);
			Double startSOC=matrix.getDouble(i, 4);
			Double endSOC=matrix.getDouble(i, 5);
			double energyCharged=endSOC-startSOC;
			
			DebugLib.assertTrue(energyCharged>0, "startSOC:" + startSOC + " - endSOC:" + endSOC);
			
			energyConsumptionPerLink.incrementBy(linkId, energyCharged);			
		}
		return energyConsumptionPerLink;
	}
	
}
