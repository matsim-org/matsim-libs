/* *********************************************************************** *
 * project: org.matsim.*
 * Heureka2008.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.meisterk.heureka2008;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PlansAlgorithm;
import org.matsim.utils.misc.Time;

import playground.meisterk.MyRuns;
import playground.meisterk.heureka2008.PlansAnalyseTimes.Activities;

public class Heureka2008 {

	public static final int TIME_BIN_SIZE = 300;
	
	public static void analyseInitialTimes() {

		// initialize scenario with events from a given events file
		// - network
		final NetworkLayer network = MyRuns.initWorldNetwork();
		// - population
		PlansAlgorithm pa = new PlansAnalyseTimes(TIME_BIN_SIZE);
		ArrayList<PlansAlgorithm> plansAlgos = new ArrayList<PlansAlgorithm>();
		plansAlgos.add(pa);

		final Plans matsimAgentPopulation = MyRuns.initMatsimAgentPopulation(Plans.USE_STREAMING, plansAlgos);
		int[][] numDeps = ((PlansAnalyseTimes) pa).getNumDeps();
		Heureka2008.writeAnArray(numDeps, "output/deptimes.txt");
		int[][] numArrs = ((PlansAnalyseTimes) pa).getNumArrs();
		Heureka2008.writeAnArray(numArrs, "output/arrtimes.txt");
		int[][] numTraveling = ((PlansAnalyseTimes) pa).getNumTraveling();
		Heureka2008.writeAnArray(numTraveling, "output/traveling.txt");

//		// write to standard out
//		System.out.println("departures: ");
//		System.out.println("");
//		for (int aa=0; aa < numDeps.length; aa++) {
//
//			for (int tt=0; tt < numDeps[aa].length; tt++) {
//
//				System.out.print(numDeps[aa][tt] + " ");
//
//			}
//			System.out.println();
//
//		}
//
//		System.out.println();
//
//		System.out.println("arrivals: ");
//		System.out.println("");
//		for (int aa=0; aa < numArrs.length; aa++) {
//
//			for (int tt=0; tt < numArrs[aa].length; tt++) {
//
//				System.out.print(numArrs[aa][tt] + " ");
//
//			}
//			System.out.println();
//
//		}
//
//		System.out.println();
//
//		System.out.println("traveling: ");
//		System.out.println("");
//		for (int aa=0; aa < numTraveling.length; aa++) {
//
//			for (int tt=0; tt < numTraveling[aa].length; tt++) {
//
//				System.out.print(numTraveling[aa][tt] + " ");
//
//			}
//			System.out.println();
//
//		}
//
//		System.out.println();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Gbl.createConfig(args);
		Heureka2008.analyseInitialTimes();

	}

	private static void writeAnArray(int[][] anArray, String filename) {
		
		File outFile = null;
		BufferedWriter out = null;

		outFile = new File(filename);

		try {
			out = new BufferedWriter(new FileWriter(outFile));

			boolean timesAvailable = true;
			int timeIndex = 0;

			out.write("#");
			for (int ii=0; ii < Activities.values().length; ii++) {
				out.write(Activities.values()[ii] + "\t");
			}
			out.newLine();
			
			while (timesAvailable) {

				timesAvailable = false;
				
				out.write(Time.strFromSec(timeIndex * TIME_BIN_SIZE, ':') + "\t");
				for (int aa=0; aa < anArray.length; aa++) {

//					if (numDeps[aa][timeIndex] != null) {
					if (timeIndex < anArray[aa].length) {
						out.write(Integer.toString(anArray[aa][timeIndex]));
						timesAvailable = true;
					} else {
						out.write("0");
					}
					out.write("\t");
				}
				out.newLine();
				timeIndex++;
			}
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
}
