/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mrieser.pt.application;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.router.PlansCalcTransitRoute;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.transitSchedule.TransitScheduleWriterV1;
import org.matsim.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vis.otfvis.OTFVisQSim;
import org.matsim.visum.VisumNetwork;
import org.matsim.visum.VisumNetworkReader;
import org.xml.sax.SAXException;

import playground.mrieser.pt.converter.Visum2TransitSchedule;
import playground.mrieser.pt.utils.MergeNetworks;

public class DataPrepare {

	private static final Logger log = Logger.getLogger(DataPrepare.class);

	// INPUT FILES
	private final static String VISUM_FILE = "/data/vis/bln/berlin.net";
	private final static String NETWORK_FILE = "/Volumes/Data/VSP/svn/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch-osm.xml";
	private final static String INPUT_PLANS_FILE = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/plans.census2000ivtch1pct.dilZh30km.sample.xml.gz";

	// INTERMEDIARY FILES
	private final static String TRANSIT_NETWORK_FILE = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/network.oevModellZH.xml";
	private final static String TRANSIT_SCHEDULE_WITHOUT_NETWORK_FILE = "/data/vis/bln/transitSchedule.xml";

	// OUTPUT FILES
	private final static String TRANSIT_SCHEDULE_WITH_NETWORK_FILE = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/transitSchedule.networkOevModellZH.xml";
	private final static String VEHICLE_FILE = "/data/vis/bln/vehicles.xml";
	private final static String MULTIMODAL_NETWORK_FILE = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/network.multimodal.xml";
	private final static String ROUTED_PLANS_FILE = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/plans.census2000ivtch1pct.dilZh30km.routedOevModell.xml.gz";


	private final ScenarioImpl scenario;
	private final Config config;

	public DataPrepare() {
		this.scenario = new ScenarioImpl();
		this.config = this.scenario.getConfig();
	}

	protected void prepareConfig() {
		this.config.scenario().setUseTransit(true);
		this.config.scenario().setUseVehicles(true);
	}

	protected void convertSchedule() {
		final VisumNetwork vNetwork = new VisumNetwork();
		try {
			log.info("reading visum network.");
			new VisumNetworkReader(vNetwork).read(VISUM_FILE);
			log.info("converting visum data to TransitSchedule.");
			Visum2TransitSchedule converter = new Visum2TransitSchedule(vNetwork, this.scenario.getTransitSchedule(), this.scenario.getVehicles());

			// configure how transport modes must be converted
			// the ones for Berlin
			converter.registerTransportMode("B", "bus");
			converter.registerTransportMode("F", TransportMode.walk);
			converter.registerTransportMode("K", "bus");
			converter.registerTransportMode("L", "other");
			converter.registerTransportMode("P", TransportMode.car);
			converter.registerTransportMode("R", TransportMode.bike);
			converter.registerTransportMode("S", "train");
			converter.registerTransportMode("T", "tram");
			converter.registerTransportMode("U", "train");
			converter.registerTransportMode("V", "other");
			converter.registerTransportMode("W", "bus");
			converter.registerTransportMode("Z", "train");

			// the ones for Zurich
//			converter.registerTransportMode("B", TransportMode.bus); // BUS
//			converter.registerTransportMode("F", TransportMode.walk); // BUSS
//			converter.registerTransportMode("I", TransportMode.car); // IV-PW
//			converter.registerTransportMode("R", TransportMode.train); // REGIONALVERKEHR
//			converter.registerTransportMode("S", TransportMode.other); // SCHIFF
//			converter.registerTransportMode("T", TransportMode.tram); // TRAM
//			converter.registerTransportMode("Y", TransportMode.train); // BERGBAHN
//			converter.registerTransportMode("Z", TransportMode.train); // FERNVERKEHR

			converter.convert();
			log.info("writing TransitSchedule to file.");
			new TransitScheduleWriterV1(this.scenario.getTransitSchedule()).write(TRANSIT_SCHEDULE_WITHOUT_NETWORK_FILE);
			log.info("writing vehicles to file.");
			new VehicleWriterV1(this.scenario.getVehicles()).writeFile(VEHICLE_FILE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void createNetworkFromSchedule() {
		NetworkLayer network = new NetworkLayer();
		new CreatePseudoNetwork(this.scenario.getTransitSchedule(), network, "tr_").createNetwork();
		new NetworkWriter(network).write(TRANSIT_NETWORK_FILE);
		try {
			new TransitScheduleWriter(this.scenario.getTransitSchedule()).writeFile(TRANSIT_SCHEDULE_WITH_NETWORK_FILE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void mergeNetworks() {
		ScenarioImpl transitScenario = new ScenarioImpl();
		NetworkLayer transitNetwork = transitScenario.getNetwork();
		ScenarioImpl streetScenario = new ScenarioImpl();
		NetworkLayer streetNetwork = streetScenario.getNetwork();
		try {
			new MatsimNetworkReader(transitScenario).parse(TRANSIT_NETWORK_FILE);
			new MatsimNetworkReader(streetScenario).parse(NETWORK_FILE);
			MergeNetworks.merge(streetNetwork, "", transitNetwork, "", this.scenario.getNetwork());
			new NetworkWriter(this.scenario.getNetwork()).write(MULTIMODAL_NETWORK_FILE);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void routePopulation() {
		Population pop = this.scenario.getPopulation();
		try {
			new MatsimPopulationReader(this.scenario).parse(INPUT_PLANS_FILE);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		DijkstraFactory dijkstraFactory = new DijkstraFactory();
		FreespeedTravelTimeCost timeCostCalculator = new FreespeedTravelTimeCost(this.scenario.getConfig().charyparNagelScoring());
		TransitConfigGroup transitConfig = new TransitConfigGroup();
		PlansCalcTransitRoute router = new PlansCalcTransitRoute(this.scenario.getConfig().plansCalcRoute(),
				this.scenario.getNetwork(), timeCostCalculator, timeCostCalculator, dijkstraFactory,
				this.scenario.getTransitSchedule(), transitConfig);
		log.info("start pt-router");
		router.run(pop);
		log.info("write routed plans out.");
		new PopulationWriter(pop, this.scenario.getNetwork()).write(ROUTED_PLANS_FILE);
	}

	protected void visualizeRouterNetwork() {
		TransitRouter router = new TransitRouter(this.scenario.getTransitSchedule());
		Network routerNet = router.getTransitRouterNetwork();

		log.info("create vis network");
		ScenarioImpl visScenario = new ScenarioImpl();
		Network visNet = visScenario.getNetwork();

		for (Node node : routerNet.getNodes().values()) {
			visNet.getFactory().createNode(node.getId(), node.getCoord());
			visNet.addNode(node);
		}
		for (Link link : routerNet.getLinks().values()) {
			Link l = visNet.getFactory().createLink(link.getId(), link.getFromNode().getId(), link.getToNode().getId());
			l.setLength(link.getLength());
			l.setFreespeed(link.getFreespeed());
			l.setCapacity(link.getCapacity());
			l.setNumberOfLanes(link.getNumberOfLanes());
		}

		log.info("write routerNet.xml");
		new NetworkWriter(visNet).write("visNet.xml");

		log.info("start visualizer");
		EventsManagerImpl events = new EventsManagerImpl();
		OTFVisQSim client = new OTFVisQSim(visScenario, events);
		client.run();
	}

	public static void main(final String[] args) {
		DataPrepare app = new DataPrepare();
		app.prepareConfig();
		app.convertSchedule();
//		app.createNetworkFromSchedule();
//		app.mergeNetworks();
//		app.routePopulation();
//		app.visualizeRouterNetwork();

		log.info("done.");
	}

}
