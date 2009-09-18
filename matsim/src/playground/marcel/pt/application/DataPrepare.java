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

package playground.marcel.pt.application;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.TransitScheduleWriterV1;
import org.matsim.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vis.otfvis.opengl.OnTheFlyQueueSimQuad;
import org.xml.sax.SAXException;

import playground.marcel.pt.config.TransitConfigGroup;
import playground.marcel.pt.router.PlansCalcTransitRoute;
import playground.marcel.pt.router.TransitRouter;
import playground.marcel.pt.utils.CreatePseudoNetwork;
import playground.marcel.pt.utils.MergeNetworks;
import playground.mohit.converter.Visum2TransitSchedule;
import playground.mohit.converter.VisumNetwork;
import playground.mohit.converter.VisumNetworkReader;

public class DataPrepare {

	private static final Logger log = Logger.getLogger(DataPrepare.class);

	// INPUT FILES
	private final static String VISUM_FILE = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/input/oev_modell.net";
	private final static String NETWORK_FILE = "/Volumes/Data/VSP/svn/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch-osm.xml";
	private final static String INPUT_PLANS_FILE = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/plans.census2000ivtch1pct.dilZh30km.sample.xml.gz";

	// INTERMEDIARY FILES
	private final static String TRANSIT_NETWORK_FILE = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/network.oevModellZH.xml";
	private final static String TRANSIT_SCHEDULE_WITHOUT_NETWORK_FILE = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/transitSchedule.OevModellZH.xml";

	// OUTPUT FILES
	private final static String TRANSIT_SCHEDULE_WITH_NETWORK_FILE = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/transitSchedule.networkOevModellZH.xml";
	private final static String VEHICLE_FILE = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/vehicles.oevModellZH.xml";
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
			new Visum2TransitSchedule(vNetwork, this.scenario.getTransitSchedule(), this.scenario.getVehicles()).convert();
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
		new NetworkWriter(network, TRANSIT_NETWORK_FILE).write();
		try {
			new TransitScheduleWriter(this.scenario.getTransitSchedule()).writeFile(TRANSIT_SCHEDULE_WITH_NETWORK_FILE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void mergeNetworks() {
		NetworkLayer transitNetwork = new NetworkLayer();
		NetworkLayer streetNetwork = new NetworkLayer();
		try {
			new MatsimNetworkReader(transitNetwork).parse(TRANSIT_NETWORK_FILE);
			new MatsimNetworkReader(streetNetwork).parse(NETWORK_FILE);
			MergeNetworks.merge(streetNetwork, "", transitNetwork, "", this.scenario.getNetwork());
			new NetworkWriter(this.scenario.getNetwork(), MULTIMODAL_NETWORK_FILE).write();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void routePopulation() {
		PopulationImpl pop = this.scenario.getPopulation();
		try {
			new MatsimPopulationReader(this.scenario).parse(INPUT_PLANS_FILE);
			pop.printPlansCount();
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
		new PopulationWriter(pop).write(ROUTED_PLANS_FILE);
	}

	protected void visualizeRouterNetwork() {
		TransitRouter router = new TransitRouter(this.scenario.getTransitSchedule());
		Network routerNet = router.getTransitRouterNetwork();

		log.info("create vis network");
		ScenarioImpl visScenario = new ScenarioImpl();
		NetworkLayer visNet = visScenario.getNetwork();

		for (Node node : routerNet.getNodes().values()) {
			visNet.createNode(node.getId(), node.getCoord());
		}
		for (Link link : routerNet.getLinks().values()) {
			visNet.createLink(link.getId(), visNet.getNodes().get(link.getFromNode().getId()), visNet.getNodes().get(link.getToNode().getId()),
					link.getLength(), link.getFreespeed(Time.UNDEFINED_TIME), link.getCapacity(Time.UNDEFINED_TIME), link.getNumberOfLanes(Time.UNDEFINED_TIME));
		}

		log.info("write routerNet.xml");
		new NetworkWriter(visNet, "visNet.xml").write();

		log.info("start visualizer");
		EventsImpl events = new EventsImpl();
		OnTheFlyQueueSimQuad client = new OnTheFlyQueueSimQuad(visScenario, events);
		client.run();
	}

	public static void main(final String[] args) {
		DataPrepare app = new DataPrepare();
		app.prepareConfig();
		app.convertSchedule();
		app.createNetworkFromSchedule();
		app.mergeNetworks();
		app.routePopulation();
//		app.visualizeRouterNetwork();

		log.info("done.");
	}

}
