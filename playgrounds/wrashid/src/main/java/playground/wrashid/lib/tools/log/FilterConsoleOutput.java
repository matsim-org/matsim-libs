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

package playground.wrashid.lib.tools.log;

import java.util.LinkedList;

import org.matsim.contrib.parking.lib.GeneralLib;


public class FilterConsoleOutput {

	public static void main(String[] args) {
		//LinkedList<String> readFileRows = GeneralLib.readFileRows("H:/data/experiments/TRBAug2012/runs/run1/log outputs/ivtch-1.log");
		LinkedList<String> readFileRows = GeneralLib.readFileRows("H:/data/experiments/TRBAug2012/runs/run1/output/ITERS/it.0/0.parkingOccupancy.txt");
		
		
		
		int i=0;
		while (i<readFileRows.size()){
			String line = readFileRows.get(i);
			//if (line.contains("ParkingCostCalculatorZHPerStreetOptimizedPrice") && line.contains("average garage parking")){
			//if (line.contains("LegHistogramListener:83 number of car legs")){
			if (line.contains("stp")){
			System.out.println(line);
			}
			i++;
		}
	}
	
}
