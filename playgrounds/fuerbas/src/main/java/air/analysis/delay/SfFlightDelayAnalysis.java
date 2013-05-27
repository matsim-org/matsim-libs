/* *********************************************************************** *
 * project: org.matsim.*
 * SfAirScheduleBuilder
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

package air.analysis.delay;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * When using SfAirController flight delays can be analyzed using this class. Range of delays may be determined by
 * MIN_MAX_DELAY, any delays larger (or smaller for negative values) will be accumulated. DELAY_OUTPUT_INTERVAL can be
 * used to define an interval within which delay will be accumulated.
 * 
 * @author fuerbas
 * @author dgrether
 * 
 */
public class SfFlightDelayAnalysis {

	private static final Logger log = Logger.getLogger(SfFlightDelayAnalysis.class);

	private static final int DELAY_OUTPUT_INTERVAL = 5; // set interval for delay accumulation

	// private static String actualTimes = "Z:\\WinHome\\munich_output\\ITERS\\it.0\\0.statistic.csv";
	// private static String scheduledTimes=
	// "Z:\\WinHome\\shared-svn\\studies\\countries\\de\\flight\\sf_oag_flight_model\\munich\\flight_model_muc_all_flights\\oag_flights.txt";
	// private static String delayOutput = "Z:\\WinHome\\munich_output\\delay.csv";
	// private static String delayOutputAcc = "Z:\\WinHome\\munich_output\\delay_acc.csv";
	// private static String delaySingleFlight = "";

//	private static String scheduledTimes = "/media/data/work/repos/shared-svn/studies/countries/eu/flight/sf_oag_flight_model/oag_flights.txt";
//	private static String actualTimes = "/media/data/work/matsim/matsimOutput/run1801/ITERS/it.0/1801.0.statistic.csv";
//	private static String delayOutput = "/media/data/work/matsim/matsimOutput/run1801/ITERS/it.0/1801.0.delay.csv";
//	// private static String delayOutputAcc = "/media/data/work/matsim/matsimOutput/run1801/ITERS/it.0/0.delay_acc.csv";
//	private static String delaySingleFlight = "/media/data/work/matsim/matsimOutput/run1801/ITERS/it.0/1801.0.delay_by_flight.csv";

//	private static String scheduledTimes = "/media/data/work/repos/shared-svn/studies/countries/de/flight/sf_oag_flight_model/oag_flights.txt";
//	private static String actualTimes = "/media/data/work/matsim/matsimOutput/run1802/ITERS/it.0/1802.0.statistic.csv";
//	private static String delayOutput = "/media/data/work/matsim/matsimOutput/run1802/ITERS/it.0/1802.0.delay.csv";
//	private static String delaySingleFlight = "/media/data/work/matsim/matsimOutput/run1802/ITERS/it.0/1802.0.delay_by_flight.csv";

	private static String scheduledTimes = "/media/data/work/repos/shared-svn/studies/countries/eu/flight/dg_oag_flight_model_2_runways_3600vph/oag_flights.txt";
	private static String actualTimes = "/media/data/work/matsim/matsimOutput/flight_model_eu/ITERS/it.0/0.statistic.csv";
	private static String delayOutput = "/media/data/work/matsim/matsimOutput/flight_model_eu/ITERS/it.0/0.delay.csv";
	private static String delaySingleFlight = "/media/data/work/matsim/matsimOutput/flight_model_eu/ITERS/it.0/0.delay_by_flight.csv";

	
	private Map<String, Double> readActualArrivals(String filename) throws IOException {
		Map<String, Double> map = new HashMap<String, Double>();
		BufferedReader brActual = new BufferedReader(new FileReader(new File(filename)));
		int lines = 0;
		while (brActual.ready()) {
			String line = brActual.readLine();
			String[] entries = line.split("\t");
			String flightNumber = entries[0];
			if (lines > 0) {
				Double arrival = Double.parseDouble(entries[1]) / 60;
				map.put(flightNumber, arrival);
			}
			lines++;
		}
		brActual.close();
		return map;
	}

	public void analyzeDelays(String scheduledTimes, String actualTimes, String delayOutput, String delaySingleFlight) throws Exception {
		SortedMap<Integer, Integer> delay = new TreeMap<Integer, Integer>();
		Map<String, Double> actualArrival = new HashMap<String, Double>();
		Map<String, Double> scheduledArrival = new HashMap<String, Double>();

		BufferedReader brScheduled = new BufferedReader(new FileReader(new File(scheduledTimes)));
		BufferedWriter bwDelaySingleFlights = new BufferedWriter(new FileWriter(new File(
				delaySingleFlight)));
		actualArrival = this.readActualArrivals(actualTimes);

		while (brScheduled.ready()) {
			String line = brScheduled.readLine();
			String[] entries = line.split("\t");
			String flightNumber = entries[2];

			if (actualArrival.containsKey(flightNumber)) {
				Double arrival = Double.parseDouble(entries[3]) + Double.parseDouble(entries[4]);
				scheduledArrival.put(flightNumber, arrival / 60);
				Integer flightDelay = (int) Math.round(actualArrival.get(flightNumber)
						- scheduledArrival.get(flightNumber));

				bwDelaySingleFlights.write(flightNumber + "\t" + actualArrival.get(flightNumber) + "\t"
						+ flightDelay);
				bwDelaySingleFlights.newLine();

				if (!delay.containsKey(flightDelay)) {
					delay.put(flightDelay, 0);
				}
				int soFar = delay.get(flightDelay);
				soFar++;
				delay.put(flightDelay, soFar);
			}
			else {
				log.warn("No actual arrival for scheduled flight nr " + flightNumber);
			}
		}

		brScheduled.close();
		bwDelaySingleFlights.close();

		this.writeDelays(delay, delayOutput);
		log.info("done!");
	}

	private void writeDelays(SortedMap<Integer, Integer> delay, String filename) throws IOException {
		BufferedWriter bwDelay = new BufferedWriter(new FileWriter(new File(filename)));
		bwDelay.write("Delay in minutes \t Number of Delays");
		bwDelay.newLine();

		for (int i = delay.firstKey() - 2; i < delay.lastKey() + 2; i ++) {
			Integer numberFlights = delay.get(i);
			if (numberFlights == null) {
				numberFlights = 0;
			}
			bwDelay.write(Integer.toString(i) +"\t" + numberFlights);
			bwDelay.newLine();
		}

		bwDelay.flush();
		bwDelay.close();
	}

	public static void main(String[] args) throws Exception {

		SfFlightDelayAnalysis ana = new SfFlightDelayAnalysis();
			ana.analyzeDelays(scheduledTimes, actualTimes, delayOutput, delaySingleFlight);
	}
}
