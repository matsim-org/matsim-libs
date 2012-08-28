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

package playground.wrashid.parkingSearch.withindayFW.analysis;

import java.util.LinkedList;

import org.matsim.contrib.parking.lib.GeneralLib;


public class ConsoleOutputAnalysis {

	public static void main(String[] args) {
		LinkedList<String> readFileRows = GeneralLib.readFileRows("H:/data/experiments/TRBAug2012/runs/run91/output/logfile.log");

		int i = 0;
		while (i < readFileRows.size()) {
			String line = readFileRows.get(i);
			// if
			// (line.contains("ParkingCostCalculatorZHPerStreetOptimizedPrice")
			// && line.contains("average garage parking")){
			// if (line.contains("ParkingCostOptimizerZH")){
			//if (line.contains("avg. score of the executed plan of each agent")){
			
			 if (line.contains("ParkingAnalysisHandlerZH")){
			// if (line.contains("HUPCControllerKTIzh") || line.contains("ParkingAnalysisHandlerZH")
			//		|| line.contains("ParkingAgentsTracker") || line.contains("ParkingCostOptimizerZH")) {

				System.out.println(line);
			}
			i++;
		}
	}

}
