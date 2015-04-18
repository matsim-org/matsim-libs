/* *********************************************************************** *
 * project: org.matsim.*
 * MiniScenario.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.wrashid.msimoni.analyses.experiments;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.RunnableMobsim;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulation;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import playground.wrashid.msimoni.analyses.InFlowInfoAcuumulatorWithPt;
import playground.wrashid.msimoni.analyses.MainDensityAnalysisWithPtV2;
import playground.wrashid.msimoni.analyses.OutFlowInfoAccumulatorWithPt;

import java.util.*;

public class MiniScenarioMultiRun {

	private static final Logger log = Logger.getLogger(MiniScenarioMultiRun.class);

	public static void main(String[] args) {
		new MiniScenarioMultiRun();
	}

	public MiniScenarioMultiRun() {

		int runId = 0;
		int performEachXRun=1;

		for (int agentsPerHour = 50; agentsPerHour < 200; agentsPerHour += 50) {
			for (int binSizeInSeconds = 30; binSizeInSeconds < 300; binSizeInSeconds += 60) {
				for (int gapSpeed = 1; gapSpeed < 15; gapSpeed += 1) {
					if (runId % performEachXRun == 0) {
						String caption = "runId: " + runId
								+ ", agentsPerHour: " + agentsPerHour
								+ ", binSizeInSeconds:" + binSizeInSeconds
								+ ", gapSpeed: "
								+ gapSpeed;
						System.out.println(caption);
						Config config = ConfigUtils.createConfig();
						Scenario scenario = ScenarioUtils
								.createScenario(config);

						createNetwork(scenario);
						createPopulation(scenario, agentsPerHour);
						runSimulation(scenario, Integer.toString(gapSpeed), binSizeInSeconds, runId,caption);
					}
					runId++;

				}
			}
		}

	}

	public static void createNetwork(Scenario scenario) {

		NetworkFactory factory = scenario.getNetwork().getFactory();

		Node n0 = factory.createNode(Id.create("n0", Node.class),
				scenario.createCoord(0.0, 0.0));
		Node n1 = factory.createNode(Id.create("n1", Node.class),
				scenario.createCoord(200.0, 0.0));
		Node n2 = factory.createNode(Id.create("n2", Node.class),
				scenario.createCoord(200.0, 200.0));
		Node n3 = factory.createNode(Id.create("n3", Node.class),
				scenario.createCoord(0.0, 200.0));

		Link l0 = factory.createLink(Id.create("l0", Link.class), n0, n1);
		Link l1 = factory.createLink(Id.create("l1", Link.class), n1, n2);
		Link l2 = factory.createLink(Id.create("l2", Link.class), n2, n3);
		Link l3 = factory.createLink(Id.create("l3", Link.class), n3, n0);

		l0.setFreespeed(80.0 / 3.6);
		l1.setFreespeed(80.0 / 3.6);
		l2.setFreespeed(80.0 / 3.6);
		l3.setFreespeed(80.0 / 3.6);

		l0.setLength(200.0);
		l1.setLength(200.0);
		l2.setLength(200.0);
		l3.setLength(200.0);

		l0.setCapacity(2000.0);
		l1.setCapacity(2000.0);
		l2.setCapacity(2000.0);
		l3.setCapacity(2000.0);

		scenario.getNetwork().addNode(n0);
		scenario.getNetwork().addNode(n1);
		scenario.getNetwork().addNode(n2);
		scenario.getNetwork().addNode(n3);

		scenario.getNetwork().addLink(l0);
		scenario.getNetwork().addLink(l1);
		scenario.getNetwork().addLink(l2);
		scenario.getNetwork().addLink(l3);

		new NetworkWriter(scenario.getNetwork()).write("network.xml");
	}

	private static void createPopulation(Scenario scenario, int agentsPerHour) {

		PopulationFactory factory = scenario.getPopulation().getFactory();

		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		for (int i = 0; i < 1000; i++) {
			linkIds.add(Id.create("l0", Link.class));
			linkIds.add(Id.create("l1", Link.class));
			linkIds.add(Id.create("l2", Link.class));
			linkIds.add(Id.create("l3", Link.class));
		}
		NetworkRoute route = (NetworkRoute) new LinkNetworkRouteFactory()
				.createRoute(Id.create("l3", Link.class), Id.create("l0", Link.class));
		route.setLinkIds(Id.create("l3", Link.class), linkIds,
				Id.create("l0", Link.class));

		Random random = MatsimRandom.getLocalInstance();
		int p = 0;
		for (int hour = 0; hour < 24; hour++) {
			for (int pNum = 0; pNum < agentsPerHour; pNum++) {
				Person person = factory.createPerson(Id.create(String
						.valueOf(p++), Person.class));
				Plan plan = factory.createPlan();
				Activity from = factory.createActivityFromLinkId("home",
						Id.create("l3", Link.class));
				from.setEndTime(Math.round(3600 * (hour + random.nextDouble())));
				Leg leg = factory.createLeg(TransportMode.car);
				leg.setRoute(route);
				Activity to = factory.createActivityFromLinkId("home",
						Id.create("l3", Link.class));
				plan.addActivity(from);
				plan.addLeg(leg);
				plan.addActivity(to);
				person.addPlan(plan);
				scenario.getPopulation().addPerson(person);
			}
		}
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork())
				.write("population.xml");
		log.info("Created " + scenario.getPopulation().getPersons().size()
				+ " persons");
	}

	private static void runSimulation(Scenario scenario, String gapSpeed,
			int binSizeInSeconds, int runId, String caption) {

		EventsManager eventsManager = EventsUtils.createEventsManager();

		EventWriterXML eventsWriter = new EventWriterXML(
				Controler.FILENAME_EVENTS_XML);
		eventsManager.addHandler(eventsWriter);

		Map<Id<Link>, Link> links = new TreeMap<Id<Link>, Link>();
		links.put(Id.create("l0", Link.class), scenario.getNetwork().getLinks()
				.get(Id.create("l0", Link.class)));
		links.put(Id.create("l1", Link.class), scenario.getNetwork().getLinks()
				.get(Id.create("l1", Link.class)));
		links.put(Id.create("l2", Link.class), scenario.getNetwork().getLinks()
				.get(Id.create("l2", Link.class)));
		links.put(Id.create("l3", Link.class), scenario.getNetwork().getLinks()
				.get(Id.create("l3", Link.class)));

		// 5 minute bins
		InFlowInfoAcuumulatorWithPt inflowHandler = new InFlowInfoAcuumulatorWithPt(
				links, binSizeInSeconds);
		OutFlowInfoAccumulatorWithPt outflowHandler = new OutFlowInfoAccumulatorWithPt(
				links, binSizeInSeconds);
		eventsManager.addHandler(inflowHandler);
		eventsManager.addHandler(outflowHandler);

		int avgBinSize = 5; // calculate a value every 5 seconds
		InFlowInfoAcuumulatorWithPt avgInflowHandler = new InFlowInfoAcuumulatorWithPt(
				links, avgBinSize);
		OutFlowInfoAccumulatorWithPt avgOutflowHandler = new OutFlowInfoAccumulatorWithPt(
				links, avgBinSize);
		eventsManager.addHandler(avgInflowHandler);
		eventsManager.addHandler(avgOutflowHandler);

		eventsManager.resetHandlers(0);
		eventsWriter.init(Controler.FILENAME_EVENTS_XML);

		scenario.getConfig().setParam("JDEQSim", "endTime", "96:00:00");
		scenario.getConfig().setParam("JDEQSim", "gapTravelSpeed", gapSpeed); // instead
																				// of
																				// 15m/s
		// scenario.getConfig().setParam("JDEQSim", "squeezeTime", "10.0");//
		// instead of 1800.0
		// scenario.getConfig().setParam("JDEQSim", "minimumInFlowCapacity",
		// "100.0"); // instead of 1800.0
		// scenario.getConfig().setParam("JDEQSim", "storageCapacityFactor",
		// "5.0"); // instead of 1.0
		RunnableMobsim sim = new JDEQSimulation(scenario, eventsManager);
		sim.run();

		// QSimConfigGroup conf = new QSimConfigGroup();
		// conf.setStartTime(0.0);
		// conf.setEndTime(48*3600);
		// conf.setTrafficDynamics(QSimConfigGroup.TRAFF_DYN_W_HOLES);
		// scenario.getConfig().addQSimConfigGroup(conf);
		// Mobsim sim = new QSimFactory().createMobsim(scenario, eventsManager);
		// sim.run();

		eventsWriter.closeFile();

		// now analyze results
		HashMap<Id, int[]> linkInFlow = inflowHandler.getLinkInFlow();
		HashMap<Id, int[]> linkOutFlow = outflowHandler.getLinkOutFlow();

		log.info("Entries from the inflow handler:  " + linkInFlow.size());
		log.info("Entries from the outflow handler: " + linkOutFlow.size());

		HashMap<Id, int[]> deltaFlow = MainDensityAnalysisWithPtV2.deltaFlow(
				linkInFlow, linkOutFlow);
		HashMap<Id<Link>, double[]> density = MainDensityAnalysisWithPtV2
				.calculateDensity(deltaFlow, links);

		log.info("inflows-----------------------------------------------");
		MainDensityAnalysisWithPtV2.printFlow(linkInFlow, links);

		log.info("outflows-----------------------------------------------");
		MainDensityAnalysisWithPtV2.printFlow(linkOutFlow, links);

		log.info("density-----------------------------------------------");
		MainDensityAnalysisWithPtV2.printDensity(density, links);

		HashMap<Id, int[]> avgLinkInFlow = avgInflowHandler.getLinkInFlow();
		HashMap<Id, int[]> avgLinkOutFlow = avgOutflowHandler.getLinkOutFlow();

		HashMap<Id, int[]> avgDeltaFlow = MainDensityAnalysisWithPtV2
				.deltaFlow(avgLinkInFlow, avgLinkOutFlow);
		int valuesPerBin = binSizeInSeconds / avgBinSize;
		if (binSizeInSeconds % avgBinSize != 0)
			throw new RuntimeException(
					"binSize in seconds % binSize for averaging is != 0");
		HashMap<Id<Link>, double[]> avgDensity = MainDensityAnalysisWithPtV2
				.calculateAverageDensity(MainDensityAnalysisWithPtV2
						.calculateDensity(avgDeltaFlow, links), valuesPerBin);

		log.info("avg density-----------------------------------------------");
		MainDensityAnalysisWithPtV2.printDensity(avgDensity, links);

		for (Id linkId : links.keySet()) {
			double[] densityBins = avgDensity.get(linkId).clone();
			int[] flow = linkOutFlow.get(linkId);
			double[] outFlowBins = new double[flow.length];
			for (int i = 1; i < flow.length; i++)
				outFlowBins[i] = flow[i] - flow[i - 1];

			GeneralLib.generateXYScatterPlot("c:/tmp/runId-" + runId +"-link-" + linkId
					+ ".png", densityBins,
					outFlowBins, caption, "density", "outfow");
		}
	}
}
