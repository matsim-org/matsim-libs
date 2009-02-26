/* *********************************************************************** *
 * project: org.matsim.*
 * MyRuns.java
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

package playground.meisterk;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.PersonAlgorithm;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.ActUtilityParameters;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationReader;
import org.matsim.population.algorithms.PersonAnalyseTimesByActivityType;
import org.matsim.population.algorithms.PersonAnalyseTimesByActivityType.Activities;
import org.matsim.utils.misc.Time;

public class MyRuns {

	public final static String CONFIG_MODULE = "planCalcScore";
	public final static String CONFIG_WAITING = "waiting";
	public final static String CONFIG_LATE_ARRIVAL = "lateArrival";
	public final static String CONFIG_EARLY_DEPARTURE = "earlyDeparture";
	public final static String CONFIG_TRAVELING = "traveling";
	public final static String CONFIG_PERFORMING = "performing";
	public final static String CONFIG_LEARNINGRATE = "learningRate";
	public final static String CONFIG_DISTANCE_COST = "distanceCost";

	public static final int TIME_BIN_SIZE = 300;

	protected static final TreeMap<String, ActUtilityParameters> utilParams = new TreeMap<String, ActUtilityParameters>();
	protected static double marginalUtilityOfWaiting = Double.NaN;
	protected static double marginalUtilityOfLateArrival = Double.NaN;
	protected static double marginalUtilityOfEarlyDeparture = Double.NaN;
	protected static double marginalUtilityOfTraveling = Double.NaN;
	protected static double marginalUtilityOfPerforming = Double.NaN;
	protected static double distanceCost = Double.NaN;
	protected static double abortedPlanScore = Double.NaN;
	protected static double learningRate = Double.NaN;

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public static void run() throws Exception {

//		MyRuns.writeGUESSFile();
//		MyRuns.conversionSpeedTest();
//		MyRuns.convertPlansV0ToPlansV4();
//		MyRuns.produceSTRC2007KML();

		System.out.println();

	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) throws Exception {

		Gbl.createConfig(args);
//		Gbl.createWorld();
//		Gbl.createFacilities();

//		run();

		String emptyString = "car";

		String[] bla = emptyString.split(",");
		System.out.println(bla.length);

	}

	public static NetworkLayer initWorldNetwork() {

		NetworkLayer network = null;

		System.out.println("  creating network layer... ");
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		System.out.println("  done");

		System.out.println("  reading network xml file... ");
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		System.out.println("  done.");

		return network;
	}

	// Gbl.getConfig().plans().getInputFile()
	public static Population initMatsimAgentPopulation(final String inputFilename, final boolean isStreaming, final ArrayList<PersonAlgorithm> algos) {

		Population population = null;

		System.out.println("  reading plans xml file... ");
		population = new PopulationImpl(isStreaming);

		if (isStreaming) {
			// add plans algos for streaming
			if (algos != null) {
				for (PersonAlgorithm algo : algos) {
					population.addAlgorithm(algo);
				}
			}
		}
		PopulationReader plansReader = new MatsimPopulationReader(population);
		plansReader.readFile(inputFilename);
		population.printPlansCount();
		System.out.println("  done.");

		return population;
	}

	public static void readEvents(final Events events, final NetworkLayer network) {

		// load test events
		long startTime, endTime;

		System.out.println("  reading events file and (probably) running events algos");
		startTime = System.currentTimeMillis();
		new MatsimEventsReader(events).readFile(Gbl.getConfig().events().getInputFile());
		endTime = System.currentTimeMillis();
		System.out.println("  done.");
		System.out.println("  reading events from file and processing them took " + (endTime - startTime) + " ms.");
		System.out.flush();

	}

	/**
	 * Used this routine for MeisterEtAl_Heureka_2008 paper,
	 * plot of number of deps, arrs by activity type to visualize
	 * the time distribution from microcensus.
	 */
	public static void analyseInitialTimes() {

		// initialize scenario with events from a given events file
		// - network
		final NetworkLayer network = MyRuns.initWorldNetwork();
		// - population
		PersonAlgorithm pa = new PersonAnalyseTimesByActivityType(TIME_BIN_SIZE);
		ArrayList<PersonAlgorithm> plansAlgos = new ArrayList<PersonAlgorithm>();
		plansAlgos.add(pa);

		Population matsimAgentPopulation = new PopulationImpl(PopulationImpl.USE_STREAMING);
		PopulationReader plansReader = new MatsimPopulationReader(matsimAgentPopulation);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		matsimAgentPopulation.printPlansCount();
		int[][] numDeps = ((PersonAnalyseTimesByActivityType) pa).getNumDeps();
		MyRuns.writeAnArray(numDeps, "output/deptimes.txt");
		int[][] numArrs = ((PersonAnalyseTimesByActivityType) pa).getNumArrs();
		MyRuns.writeAnArray(numArrs, "output/arrtimes.txt");
		int[][] numTraveling = ((PersonAnalyseTimesByActivityType) pa).getNumTraveling();
		MyRuns.writeAnArray(numTraveling, "output/traveling.txt");

	}

	private static void writeAnArray(final int[][] anArray, final String filename) {

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

				out.write(Time.writeTime(timeIndex * TIME_BIN_SIZE) + "\t");
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
			e.printStackTrace();
		}
	}
}
