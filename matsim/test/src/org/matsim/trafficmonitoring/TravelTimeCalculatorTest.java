/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeCalculatorTest.java
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

package org.matsim.trafficmonitoring;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class TravelTimeCalculatorTest extends MatsimTestCase {

	private static final boolean generateNewCompareData = false;
	private static final boolean generateNewCompareDataPessimistic = false;
		// set generateNewComparedata to true to generate new comparison data
		// if new comparison data is generated, the test will fail with an Exception
		// to ensure this option isn't enabled for real tests!

	public final void testLinkTravelTimeArrayOptimistic() throws Exception {

		String className =  "/" + this.getClass().getSimpleName();
		String  inputDir = "test/input/" + (this.getClass().getCanonicalName().replace('.', '/')).replace(className, "")	+ "/TravelTimeCalculator/";
		String networkFile = inputDir + "link10_network.xml";
		String eventsFile = inputDir + "link10_events.txt";
		String compareFile = inputDir + "link10_ttimes.txt";

		int timeBinSize = 15*60;

		// setup global stuff
		Gbl.createConfig(new String[] {});

		// setup network
		NetworkLayer network = new NetworkLayer();

		// read network
		new MatsimNetworkReader(network).readFile(networkFile);
		// setup events
		Events events = new Events();
		MatsimEventsReader eventsReader = new MatsimEventsReader(events);

		// setup traveltime calculator
		TravelTimeAggregatorFactory factory = new TravelTimeAggregatorFactory();
		TravelTimeCalculator ttcalc = new TravelTimeCalculator(network, timeBinSize, 30*3600, factory);
		events.addHandler(ttcalc);

		// read events
		eventsReader.readFile(eventsFile);
		events.printEventsCount();

		// read comparison data
		BufferedReader infile = new BufferedReader(new FileReader(compareFile));
		String line;
		String[] compareData = new String[4*24];
		try {
			for (int i = 0; i < 4*24; i++) {
				line = infile.readLine();
				compareData[i] = line;
			}
		}
		catch (IOException e) {
			throw e;
		}
		finally {
			try {
				infile.close();
			} catch (IOException ignored) {}
		}

		// prepare comparison
		Link link10 = network.getLink("10");

		if (generateNewCompareData) {
			BufferedWriter outfile = null;
			try {
				outfile = new BufferedWriter(new FileWriter(compareFile));
				for (int i = 0; i < 4*24; i++) {
					double ttime = ttcalc.getLinkTravelTime(link10, i*timeBinSize);
					outfile.write(Double.toString(ttime) + "\n");
				}
			}
			catch (IOException e) {
				throw e;
			}
			finally {
				if (outfile != null) {
					try {
						outfile.close();
					} catch (IOException ignored) {}
				}
			}
			throw new Exception("A new file containg data for comparison was created. No comparison was made.");
		}

		// do comparison
		for (int i = 0; i < 4*24; i++) {
			double ttime = ttcalc.getLinkTravelTime(link10, i*timeBinSize);
			assertEquals(compareData[i], Double.toString(ttime));
		}
	}

	public final void testLinkTravelTimeHashMapOptimistic() throws Exception {

		String className =  "/" + this.getClass().getSimpleName();
		String  inputDir = "test/input/" + (this.getClass().getCanonicalName().replace('.', '/')).replace(className, "")	+ "/TravelTimeCalculator/";
		String networkFile = inputDir + "link10_network.xml";
		String eventsFile = inputDir + "link10_events.txt";
		String compareFile = inputDir + "link10_ttimes.txt";

		int timeBinSize = 15*60;

		// setup global stuff
		Gbl.createConfig(new String[] {});

		// setup network
		NetworkLayer network = new NetworkLayer();

		// read network
		new MatsimNetworkReader(network).readFile(networkFile);
		// setup events
		Events events = new Events();
		MatsimEventsReader eventsReader = new MatsimEventsReader(events);

		// setup traveltime calculator
		TravelTimeAggregatorFactory factory = new TravelTimeAggregatorFactory();
		factory.setTravelTimeRolePrototype(TravelTimeDataHashMap.class);
		TravelTimeCalculator ttcalc = new TravelTimeCalculator(network, timeBinSize, 30*3600, factory);
		events.addHandler(ttcalc);

		// read events
		eventsReader.readFile(eventsFile);
		events.printEventsCount();

		// read comparison data
		BufferedReader infile = new BufferedReader(new FileReader(compareFile));
		String line;
		String[] compareData = new String[4*24];
		try {
			for (int i = 0; i < 4*24; i++) {
				line = infile.readLine();
				compareData[i] = line;
			}
		}
		catch (IOException e) {
			throw e;
		}
		finally {
			try {
				infile.close();
			} catch (IOException ignored) {}
		}

		// prepare comparison
		Link link10 = network.getLink("10");

		if (generateNewCompareData) {
			BufferedWriter outfile = null;
			try {
				outfile = new BufferedWriter(new FileWriter(compareFile));
				for (int i = 0; i < 4*24; i++) {
					double ttime = ttcalc.getLinkTravelTime(link10, i*timeBinSize);
					outfile.write(Double.toString(ttime) + "\n");
				}
			}
			catch (IOException e) {
				throw e;
			}
			finally {
				if (outfile != null) {
					try {
						outfile.close();
					} catch (IOException ignored) {}
				}
			}
			throw new Exception("A new file containg data for comparison was created. No comparison was made.");
		}

		// do comparison
		for (int i = 0; i < 4*24; i++) {
			double ttime = ttcalc.getLinkTravelTime(link10, i*timeBinSize);
			assertEquals(compareData[i], Double.toString(ttime));
		}
	}
	
	public final void testLinkTravelTimeHashMapPessimistic() throws Exception {

		String className =  "/" + this.getClass().getSimpleName();
		String  inputDir = "test/input/" + (this.getClass().getCanonicalName().replace('.', '/')).replace(className, "")	+ "/TravelTimeCalculator/";
		String networkFile = inputDir + "link10_network.xml";
		String eventsFile = inputDir + "link10_events.txt";
		String compareFile = inputDir + "link10_ttimes_pessimistic.txt";

		int timeBinSize = 1*60;

		// setup global stuff
		Gbl.createConfig(new String[] {});

		// setup network
		NetworkLayer network = new NetworkLayer();

		// read network
		new MatsimNetworkReader(network).readFile(networkFile);
		// setup events
		Events events = new Events();
		MatsimEventsReader eventsReader = new MatsimEventsReader(events);

		// setup traveltime calculator
		TravelTimeAggregatorFactory factory = new TravelTimeAggregatorFactory();
		factory.setTravelTimeRolePrototype(TravelTimeDataHashMap.class);
		factory.setTravelTimeAggregatorPrototype(PessimisticTravelTimeAggregator.class);
		TravelTimeCalculator ttcalc = new TravelTimeCalculator(network, timeBinSize, 30*3600, factory);
		events.addHandler(ttcalc);

		// read events
		eventsReader.readFile(eventsFile);
		events.printEventsCount();

		// read comparison data
		BufferedReader infile = new BufferedReader(new FileReader(compareFile));
		String line;
		String[] compareData = new String[60*24];
		try {
			for (int i = 0; i < 60*24; i++) {
				line = infile.readLine();
				compareData[i] = line;
			}
		}
		catch (IOException e) {
			throw e;
		}
		finally {
			try {
				infile.close();
			} catch (IOException ignored) {}
		}

		// prepare comparison
		Link link10 = network.getLink("10");

		if (generateNewCompareDataPessimistic) {
			BufferedWriter outfile = null;
			try {
				outfile = new BufferedWriter(new FileWriter(compareFile));
				for (int i = 0; i < 60*24; i++) {
					double ttime = ttcalc.getLinkTravelTime(link10, i*timeBinSize);
					outfile.write(Double.toString(ttime) + "\n");
				}
			}
			catch (IOException e) {
				throw e;
			}
			finally {
				if (outfile != null) {
					try {
						outfile.close();
					} catch (IOException ignored) {}
				}
			}
			throw new Exception("A new file containg data for comparison was created. No comparison was made.");
		}

		// do comparison
		for (int i = 0; i < 60*24; i++) {
			double ttime = ttcalc.getLinkTravelTime(link10, i*timeBinSize);
			assertEquals(compareData[i], Double.toString(ttime));
		}
	}
	
}
