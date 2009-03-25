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
import org.matsim.analysis.LegHistogram;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.events.Events;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.cppdeqsim.EventsReaderDEQv1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationReader;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;

public class KtiSpeedTest {

	private static void readTxtEvents(final String filename, final Events events) {
		new MatsimEventsReader(events).readFile(filename);
		events.printEventsCount();
	}

	private static void readBinEvents(final String filename, final Events events) {
		final EventsReaderDEQv1 eventsReader = new EventsReaderDEQv1(events);
		eventsReader.readFile(filename);
		events.printEventsCount();
	}

	public static void calcRouteMTwithTimes(final String[] args) {

		System.out.println("RUN: calcRouteMTwithTimes");

		final Config config = Gbl.createConfig(args);
		final World world = new World();

		System.out.println("  reading world... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(world);
		worldReader.readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading the network...");
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading facilities... ");
		Facilities facilities = (Facilities)world.createLayer(Facilities.LAYER_TYPE, null);
		new MatsimFacilitiesReader(facilities).readFile(config.facilities().getInputFile());
		System.out.println("  done.");

		System.out.println("  setting up plans objects...");
		final Population population = new PopulationImpl(PopulationImpl.NO_STREAMING);
		System.out.println("  done.");

		System.out.println("  reading plans...");
		PopulationReader plansReader = new MatsimPopulationReader(population, network);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		population.printPlansCount();
		System.out.println("  done.");

		System.out.println("  reading events, calculating travel times...");
		final Events events = new Events();
		final TravelTimeCalculator ttime = new TravelTimeCalculator(network, 15*60);

		events.addHandler(new DepartureDelayAverageCalculator(network, 900));
		events.addHandler(new CalcLegTimes(population));
		events.addHandler(new LegHistogram(300));
		events.addHandler(new EventsToScore(population, new CharyparNagelScoringFunctionFactory(config.charyparNagelScoring())));
		events.addHandler(ttime);
		events.addHandler(new VolumesAnalyzer(900, 30*3600, network));
		events.addHandler(new EventWriterTXT("testevents.txt"));

		events.printEventHandlers();

		Gbl.startMeasurement();
//		if (config.getParam("events", "inputFormat").equals("matsimDEQ1")) {
//			readBinEvents(config.getParam("events", "inputFile"), events);
//		} else {
			readTxtEvents(config.getParam("events", "inputFile"), events);
//		}
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
		calcRouteMTwithTimes(new String[] {"../mystudies/myconfig.xml"});
	}

}
