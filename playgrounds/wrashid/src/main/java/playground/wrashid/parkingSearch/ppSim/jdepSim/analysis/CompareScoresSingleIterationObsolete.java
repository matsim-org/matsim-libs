/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.ppSim.jdepSim.analysis;

import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.Matrix;
import org.matsim.contrib.parking.lib.obj.list.Lists;

import playground.wrashid.lib.obj.TwoHashMapsConcatenated;
import playground.wrashid.parkingSearch.ppSim.jdepSim.analysis.StrategyScoresAnalysis.StrategyScoreLog;

public class CompareScoresSingleIterationObsolete {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TwoHashMapsConcatenated<String, Integer, ArrayList<String>> parkingEventsA = StrategyScoresAnalysis.getParkingEvents(
				"H:/data/experiments/parkingSearchOct2013/runs/run104/output/", 399);
		TwoHashMapsConcatenated<String, Integer, ArrayList<String>> parkingEventsB = StrategyScoresAnalysis.getParkingEvents(
				"H:/data/experiments/parkingSearchOct2013/runs/run105/output/", 399);

		int columnIndex = 9;
		ArrayList<Double> values=new ArrayList<Double>();
		for (String personId : parkingEventsA.getKeySet1()) {
			for (int legIndex : parkingEventsA.getKeySet2(personId)) {
				ArrayList<String> rowA = parkingEventsA.get(personId, legIndex);
				ArrayList<String> rowB = parkingEventsB.get(personId, legIndex);
				
				double parkingSearchDurationA = Double.parseDouble(parkingEventsA.get(personId, legIndex).get(columnIndex));
				double parkingSearchDurationB = Double.parseDouble(parkingEventsB.get(personId, legIndex).get(columnIndex));
				double relDiffernce = 0;
				if (parkingSearchDurationA != parkingSearchDurationB) {
					if (parkingSearchDurationB == 0) {
						relDiffernce = 1.0;
					} else {
						relDiffernce = (parkingSearchDurationA - parkingSearchDurationB) / parkingSearchDurationB;
					}
				}

				if (!(parkingEventsA.get(personId, legIndex).get(6).contains("private") || parkingEventsB.get(personId, legIndex)
						.get(6).contains("private"))) {
					System.out.println(relDiffernce*100);
					values.add(relDiffernce*100);
				} else {
					// System.out.println();
				}
			}
		}
		
		DebugLib.stopSystemAndReportInconsistency("obsolete class");
		GeneralLib.generateHistogram("c:/tmp2/abc.png",Lists.getArray(values),4,"","","");
		
	}

}
