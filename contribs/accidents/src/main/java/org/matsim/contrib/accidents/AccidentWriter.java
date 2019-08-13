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
import org.matsim.contrib.accidents.data.AccidentLinkInfo;
import org.matsim.contrib.accidents.data.LinkAccidentsComputationMethod;
import org.matsim.contrib.accidents.handlers.AnalysisEventHandler;
import org.matsim.core.controler.events.IterationEndsEvent;

/**
* @author ikaddoura
*/

public class AccidentWriter {	
	private static String convertSecondToHHMMSSString(int nSecondTime) {
	    return LocalTime.MIN.plusSeconds(nSecondTime).toString();
	}
	
	public void write(Scenario scenario, IterationEndsEvent event, Map<Id<Link>, AccidentLinkInfo> linkId2info, AnalysisEventHandler analzyer) {
		double timeBinSize = scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize();
		double numberOfTimeBinsPerDay = (24 * 3600) / timeBinSize;
		double actualNumberOfTimeBins = scenario.getConfig().travelTimeCalculator().getMaxTime() / timeBinSize;
		
		double differenceOfTimeBins = actualNumberOfTimeBins - numberOfTimeBinsPerDay;
		
		//File with Linkinfo for Tests
		File linkInfoFile = new File(scenario.getConfig().controler().getOutputDirectory() + "ITERS/it." + event.getIteration() + "/" + scenario.getConfig().controler().getRunId() + "." + event.getIteration() + ".linkInfo.csv");
		BufferedWriter linkInformation = null;
		try {
			linkInformation = new BufferedWriter (new FileWriter(linkInfoFile));
			linkInformation.write("Link_ID ;" + "planequal_planfree_tunnel ;"+ "landUseType ;"+ "numberOfLanes ;"+ "roadTypeBVWP ;" + "speedLimit ;" + "roadWidth ;" + "numberOfSideRoads ;" + "parkinType ;" + "areaType ;");
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
				linkInformation.write(String.valueOf(info.getPlanequal_planfree_tunnel()));
				linkInformation.write(";");
				linkInformation.write(String.valueOf(info.getLandUseType()));
				linkInformation.write(";");
				linkInformation.write(String.valueOf(info.getNumberOfLanes()));
				linkInformation.write(";");
				linkInformation.write(String.valueOf(info.getRoadTypeBVWP()));
				linkInformation.write(";");
				linkInformation.write(String.valueOf(info.getSpeedLimit()));
				linkInformation.write(";");
				linkInformation.write(String.valueOf(info.getRoadWidth()));
				linkInformation.write(";");
				linkInformation.write(String.valueOf(info.getNumberSideRoads()));
				linkInformation.write(";");
				linkInformation.write(String.valueOf(info.getParkingType()));
				linkInformation.write(";");
				linkInformation.write(String.valueOf(info.getAreaType()));
				linkInformation.write(";");
					for (double endTime = timeBinSize ; endTime <= scenario.getConfig().travelTimeCalculator().getMaxTime(); endTime = endTime + timeBinSize ) {
					double time = (endTime - timeBinSize/2.);
					int timeBinNr = (int) (time / timeBinSize);
					AccidentsConfigGroup accidentSettings = (AccidentsConfigGroup) scenario.getConfig().getModules().get(AccidentsConfigGroup.GROUP_NAME);
					double demand = accidentSettings.getSampleSize() * analzyer.getDemand(info.getLinkId(), timeBinNr);
					if (timeBinNr >= (int) differenceOfTimeBins){
						demandPerDay += demand;
					}
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
	
				
		//2 FIles, one for BVWP and one for Denmarkmodel
		//Can be changed later
		
		File accidentCostsDenmarkFile = new File(scenario.getConfig().controler().getOutputDirectory() + "ITERS/it." + event.getIteration() + "/" + scenario.getConfig().controler().getRunId() + "." + event.getIteration() + ".accidentCosts_DenmarkModel.csv");
		BufferedWriter accidentCostsDenmark = null;
		try {
			accidentCostsDenmark = new BufferedWriter (new FileWriter(accidentCostsDenmarkFile));
			//HEADER
			accidentCostsDenmark.write("Link ID ;");
			for (double endTime = timeBinSize ; endTime <= scenario.getConfig().travelTimeCalculator().getMaxTime(); endTime = endTime + timeBinSize ) {
				accidentCostsDenmark.write(convertSecondToHHMMSSString((int)(endTime-timeBinSize)));
				accidentCostsDenmark.write(" - ");
				accidentCostsDenmark.write(convertSecondToHHMMSSString((int)(endTime)));
				accidentCostsDenmark.write("_Costs [EUR] ;");
				accidentCostsDenmark.write(convertSecondToHHMMSSString((int)(endTime-timeBinSize)));
				accidentCostsDenmark.write(" - ");
				accidentCostsDenmark.write(convertSecondToHHMMSSString((int)(endTime)));
				accidentCostsDenmark.write("_Frequency ;");
			}
			accidentCostsDenmark.write("Costs per Day [EUR] ;");
			accidentCostsDenmark.write("Frequency per Day ;");
			accidentCostsDenmark.write("Costs per Year [EUR] ;");
			accidentCostsDenmark.write("Frequency per Year ;");
			accidentCostsDenmark.newLine();
			
		} catch (IOException e1) {
			e1.printStackTrace();
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
				accidentCostsBVWP.write(convertSecondToHHMMSSString((int)(endTime-timeBinSize)));
				accidentCostsBVWP.write(" - ");
				accidentCostsBVWP.write(convertSecondToHHMMSSString((int)(endTime)));
				accidentCostsBVWP.write("_Frequency ;");
			}
			accidentCostsBVWP.write("Costs per Day [EUR] ;");
			accidentCostsBVWP.write("Frequency per Day ;");
			accidentCostsBVWP.write("Costs per Year [EUR] ;");
			accidentCostsBVWP.write("Frequency per Year ;");
			accidentCostsBVWP.newLine();
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		for (AccidentLinkInfo info : linkId2info.values()) {			
			double accidentCostsPerDay_Denmark = 0.0;
			double accidentFrequencyPerDay_Denmark = 0.0;
			double accidentCostsPerYear_Denmark = 0.0;
			double accidentFrequencyPerYear_Denmark = 0.0;
			
			double accidentCostsPerDay_BVWP = 0.0;
			double accidentFrequencyPerDay_BVWP = 0.0;
			double accidentCostsPerYear_BVWP = 0.0;
			double accidentFrequencyPerYear_BVWP = 0.0;
			
			try {
				if (info.getComputationMethod().toString().equals(LinkAccidentsComputationMethod.DenmarkModel.toString())){
				accidentCostsDenmark.write(info.getLinkId().toString());
				accidentCostsDenmark.write(";");
				} else if (info.getComputationMethod().toString().equals(LinkAccidentsComputationMethod.BVWP.toString())){
				accidentCostsBVWP.write(info.getLinkId().toString());
				accidentCostsBVWP.write(";");
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			for (double endTime = timeBinSize ; endTime <= scenario.getConfig().travelTimeCalculator().getMaxTime(); endTime = endTime + timeBinSize ) {
				
				double time = (endTime - timeBinSize/2.);
				int timeBinNr = (int) (time / timeBinSize);
				
				if (info.getComputationMethod().toString().equals(LinkAccidentsComputationMethod.DenmarkModel.toString())){
					if (timeBinNr >= (int) differenceOfTimeBins){ // We need daily numbers and not 30h intervals for easily transformation to Year
						accidentCostsPerDay_Denmark += info.getTimeSpecificInfo().get(timeBinNr).getAccidentCosts();
						accidentFrequencyPerDay_Denmark += info.getTimeSpecificInfo().get(timeBinNr).getAccidentFrequency();
					}
					try {	
						accidentCostsDenmark.write(Double.toString(info.getTimeSpecificInfo().get(timeBinNr).getAccidentCosts()));
						accidentCostsDenmark.write(";");
						accidentCostsDenmark.write(Double.toString(info.getTimeSpecificInfo().get(timeBinNr).getAccidentFrequency()));
						accidentCostsDenmark.write(";");
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else if (info.getComputationMethod().toString().equals(LinkAccidentsComputationMethod.BVWP.toString())){
					if (timeBinNr >= (int) differenceOfTimeBins){ // We need daily numbers and not 30h intervals for easily transformation to Year
						accidentCostsPerDay_BVWP += info.getTimeSpecificInfo().get(timeBinNr).getAccidentCosts();
						accidentFrequencyPerDay_BVWP += info.getTimeSpecificInfo().get(timeBinNr).getAccidentFrequency();
					}
					try {
						accidentCostsBVWP.write(Double.toString(info.getTimeSpecificInfo().get(timeBinNr).getAccidentCosts()));
						accidentCostsBVWP.write(";");
						accidentCostsBVWP.write(Double.toString(info.getTimeSpecificInfo().get(timeBinNr).getAccidentFrequency()));
						accidentCostsBVWP.write(";");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			accidentCostsPerYear_BVWP = accidentCostsPerDay_BVWP*365;
			accidentCostsPerYear_Denmark = accidentCostsPerDay_Denmark*365;
			accidentFrequencyPerYear_BVWP = accidentFrequencyPerDay_BVWP*365;
			accidentFrequencyPerYear_Denmark = accidentFrequencyPerDay_Denmark*365;
			try {
				if (info.getComputationMethod().toString().equals(LinkAccidentsComputationMethod.DenmarkModel.toString())){
					accidentCostsDenmark.write(Double.toString(accidentCostsPerDay_Denmark));
					accidentCostsDenmark.write(";");
					accidentCostsDenmark.write(Double.toString(accidentFrequencyPerDay_Denmark));
					accidentCostsDenmark.write(";");
					accidentCostsDenmark.write(Double.toString(accidentCostsPerYear_Denmark));
					accidentCostsDenmark.write(";");
					accidentCostsDenmark.write(Double.toString(accidentFrequencyPerYear_Denmark));
					accidentCostsDenmark.write(";");
					accidentCostsDenmark.newLine();
				}else if (info.getComputationMethod().toString().equals(LinkAccidentsComputationMethod.BVWP.toString())){	
					accidentCostsBVWP.write(Double.toString(accidentCostsPerDay_BVWP));
					accidentCostsBVWP.write(";");
					accidentCostsBVWP.write(Double.toString(accidentFrequencyPerDay_BVWP));
					accidentCostsBVWP.write(";");
					accidentCostsBVWP.write(Double.toString(accidentCostsPerYear_BVWP));
					accidentCostsBVWP.write(";");
					accidentCostsBVWP.write(Double.toString(accidentFrequencyPerYear_BVWP));
					accidentCostsBVWP.write(";");
					accidentCostsBVWP.newLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			accidentCostsDenmark.close();
			accidentCostsBVWP.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

