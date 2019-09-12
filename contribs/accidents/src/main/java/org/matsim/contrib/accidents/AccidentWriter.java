/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.accidents;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.events.IterationEndsEvent;

/**
* @author ikaddoura
*/

class AccidentWriter {
	private static String convertSecondToHHMMSSString(int nSecondTime) {
	    return LocalTime.MIN.plusSeconds(nSecondTime).toString();
	}
	
	public void write(Scenario scenario, IterationEndsEvent event, Map<Id<Link>, AccidentLinkInfo> linkId2info, AnalysisEventHandler analzyer) {
		double timeBinSize = scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize();
				
		//File with Linkinfo for Tests
		File linkInfoFile = new File(scenario.getConfig().controler().getOutputDirectory() + "ITERS/it." + event.getIteration() + "/" + scenario.getConfig().controler().getRunId() + "." + event.getIteration() + ".linkInfo.csv");
		BufferedWriter linkInformation = null;
		try {
			linkInformation = new BufferedWriter (new FileWriter(linkInfoFile));
			linkInformation.write("Link_ID ;" + "roadTypeBVWP ;");
			for (double endTime = timeBinSize ; endTime <= scenario.getConfig().travelTimeCalculator().getMaxTime(); endTime = endTime + timeBinSize ) {
				linkInformation.write(convertSecondToHHMMSSString((int)(endTime-timeBinSize)));
				linkInformation.write(" - ");
				linkInformation.write(convertSecondToHHMMSSString((int)(endTime)));
				linkInformation.write("_demand ");
				linkInformation.write(";");
				}
			linkInformation.write("demandPerDay ;");
			linkInformation.newLine();
			for (AccidentLinkInfo info : linkId2info.values()) {
				double demandPerDay = 0.0;
				linkInformation.write(info.getLinkId().toString());
				linkInformation.write(";");
				linkInformation.write(String.valueOf(info.getRoadTypeBVWP()));
				linkInformation.write(";");
				for (double endTime = timeBinSize ; endTime <= scenario.getConfig().travelTimeCalculator().getMaxTime(); endTime = endTime + timeBinSize ) {
					double time = (endTime - timeBinSize/2.);
					int timeBinNr = (int) (time / timeBinSize);
					AccidentsConfigGroup accidentSettings = (AccidentsConfigGroup) scenario.getConfig().getModules().get(AccidentsConfigGroup.GROUP_NAME);
					double demand = accidentSettings.getSampleSize() * analzyer.getDemand(info.getLinkId(), timeBinNr);
					demandPerDay += demand;

					linkInformation.write(Double.toString(demand));
					linkInformation.write(";");
				}
				linkInformation.write(Double.toString(demandPerDay));	
				linkInformation.newLine();
			}
			linkInformation.close();
		} catch (IOException e3) {
			e3.printStackTrace();
		}
			
		File accidentCostsBVWPFile = new File(scenario.getConfig().controler().getOutputDirectory() + "ITERS/it." + event.getIteration() + "/" + scenario.getConfig().controler().getRunId() + "." + event.getIteration() + ".accidentCosts_BVWP.csv");
		BufferedWriter accidentCostsBVWP = null;
		try {
			accidentCostsBVWP = new BufferedWriter (new FileWriter(accidentCostsBVWPFile));
			//HEADER
			accidentCostsBVWP.write("Link ID ;");
			for (double endTime = timeBinSize ; endTime <= scenario.getConfig().travelTimeCalculator().getMaxTime(); endTime = endTime + timeBinSize ) {
				accidentCostsBVWP.write(convertSecondToHHMMSSString((int)(endTime-timeBinSize)));
				accidentCostsBVWP.write(" - ");
				accidentCostsBVWP.write(convertSecondToHHMMSSString((int)(endTime)));
				accidentCostsBVWP.write("_Costs [EUR] ;");
			}
			accidentCostsBVWP.write("Costs per Day [EUR] ;");
			accidentCostsBVWP.write("Costs per Year [EUR] ;");
			accidentCostsBVWP.newLine();
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		for (AccidentLinkInfo info : linkId2info.values()) {			
			double accidentCostsPerDay_BVWP = 0.0;
			double accidentCostsPerYear_BVWP = 0.0;
			
			try {
				if (info.getComputationMethod().toString().equals( AccidentsConfigGroup.AccidentsComputationMethod.BVWP.toString() )){
					accidentCostsBVWP.write(info.getLinkId().toString());
					accidentCostsBVWP.write(";");
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			for (double endTime = timeBinSize ; endTime <= scenario.getConfig().travelTimeCalculator().getMaxTime(); endTime = endTime + timeBinSize ) {
				
				double time = (endTime - timeBinSize/2.);
				int timeBinNr = (int) (time / timeBinSize);
				
				if (info.getComputationMethod().toString().equals( AccidentsConfigGroup.AccidentsComputationMethod.BVWP.toString() )){
					accidentCostsPerDay_BVWP += info.getTimeSpecificInfo().get(timeBinNr).getAccidentCosts();
					try {
						accidentCostsBVWP.write(Double.toString(info.getTimeSpecificInfo().get(timeBinNr).getAccidentCosts()));
						accidentCostsBVWP.write(";");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			accidentCostsPerYear_BVWP = accidentCostsPerDay_BVWP*365;
			try {
				if (info.getComputationMethod().toString().equals( AccidentsConfigGroup.AccidentsComputationMethod.BVWP.toString() )){
					accidentCostsBVWP.write(Double.toString(accidentCostsPerDay_BVWP));
					accidentCostsBVWP.write(";");
					accidentCostsBVWP.write(Double.toString(accidentCostsPerYear_BVWP));
					accidentCostsBVWP.write(";");
					accidentCostsBVWP.newLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			accidentCostsBVWP.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

