/* *********************************************************************** *
 * project: org.matsim.*
 * KtiSpeedTest.java
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

package playground.marcel;

import org.matsim.analysis.CalcLegTimes;
import org.matsim.config.Config;
import org.matsim.deqsim.EventsReaderDEQv1;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.events.algorithms.EventWriterTXT;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPlansReader;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.EventsToScore;
import org.matsim.trafficmonitoring.TravelTimeCalculator;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;

public class KtiSpeedTest {

	private static void readTxtEvents(final String filename, final NetworkLayer network, final Events events) {
		new MatsimEventsReader(events).readFile(filename);
		events.printEventsCount();
	}

	private static void readBinEvents(final String filename, final NetworkLayer network, final Events events) {
		final EventsReaderDEQv1 eventsReader = new EventsReaderDEQv1(events);
		eventsReader.readFile(filename);
		events.printEventsCount();
	}

	public static void calcRouteMTwithTimes(final String[] args) {

		System.out.println("RUN: calcRouteMTwithTimes");

		final Config config = Gbl.createConfig(args);
		final World world = Gbl.createWorld();

		System.out.println("  reading world... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		network = (NetworkLayer)world.createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading facilities... ");
		Facilities facilities = (Facilities)world.createLayer(Facilities.LAYER_TYPE, null);
		new MatsimFacilitiesReader(facilities).readFile(config.facilities().getInputFile());
		System.out.println("  done.");

		System.out.println("  setting up plans objects...");
		final Population population = new Population(Population.NO_STREAMING);
		System.out.println("  done.");

		System.out.println("  reading plans...");
		PopulationReader plansReader = new MatsimPlansReader(population);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		population.printPlansCount();
		System.out.println("  done.");

		System.out.println("  reading events, calculating travel times...");
		final Events events = new Events();
		final TravelTimeCalculator ttime = new TravelTimeCalculator(network, 15*60);
		events.addHandler(ttime);

//		events.addHandler(new VolumesAnalyzer(3600, 24*3600-1, network));
		events.addHandler(new EventsToScore(population, new CharyparNagelScoringFunctionFactory()));
		events.addHandler(new EventWriterTXT("testevents.txt"));
		events.addHandler(new CalcLegTimes(population));

		events.printEventHandlers();

		Gbl.startMeasurement();
		if (config.getParam("events", "inputFormat").equals("matsimDEQ1")) {
			readBinEvents(config.getParam("events", "inputFile"), network, events);
		} else {
			readTxtEvents(config.getParam("events", "inputFile"), network, events);
		}
		Gbl.printElapsedTime();
		System.out.println("  done.");

		System.out.println("  processing plans, calculating routes...");
//		PreProcessLandmarks preProcessRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
//		preProcessRoutingData.run(network);
//		final ReRouteLandmarks reroute = new ReRouteLandmarks(network, new TravelTimeDistanceCostCalculator(ttime), ttime, preProcessRoutingData);
////		final ReRoute reroute = new ReRoute(network, new TravelTimeDistanceCostCalculator(ttime), ttime);
//		reroute.init();
//		Gbl.startMeasurement();
//		int cnter = 0;
//		for (final Person person : population.getPersons().values()) {
//			for (final Plan plan : person.getPlans()) {
//				if (cnter % 10 == 0) reroute.handlePlan(plan);
//				cnter++;
//			}
//		}
//		reroute.finish();
//		Gbl.printElapsedTime();
		System.out.println("  done.");
//		System.out.println("# lookups: " + TravelTimeCalculator.reqCounter);
	}

	public static void main(final String[] args) {
		calcRouteMTwithTimes(args);
	}

}
