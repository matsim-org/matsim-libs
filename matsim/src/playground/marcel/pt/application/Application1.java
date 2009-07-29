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
import org.matsim.core.api.experimental.ScenarioImpl;
import org.matsim.core.api.experimental.network.Link;
import org.matsim.core.api.experimental.network.Network;
import org.matsim.core.api.experimental.network.Node;
import org.matsim.core.api.experimental.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.events.Events;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.TransitScheduleWriterV1;
import org.matsim.transitSchedule.api.TransitScheduleReader;
import org.matsim.vis.otfvis.opengl.OnTheFlyQueueSimQuad;
import org.xml.sax.SAXException;

import playground.marcel.pt.config.TransitConfigGroup;
import playground.marcel.pt.router.TransitRouter;
import playground.marcel.pt.routerintegration.PlansCalcTransitRoute;
import playground.mohit.converter.Visum2TransitSchedule;
import playground.mohit.converter.VisumNetwork;
import playground.mohit.converter.VisumNetworkReader;

public class Application1 {

	private static final Logger log = Logger.getLogger(Application1.class);

	private final static String DILUTED_PT_1PCT_PLANS_FILENAME = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/plans.census2000ivtch1pct.dilZh30km.pt.xml.gz";
	private final static String DILUTED_PT_ROUTED_1PCT_PLANS_FILENAME = "/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/plans.census2000ivtch1pct.dilZh30km.pt-routedOevModell.xml.gz";

	private final ScenarioImpl scenario;
	private final Config config;

	public Application1() {
		this.scenario = new ScenarioImpl();
		this.config = this.scenario.getConfig();
	}

	protected void prepareConfig() {
		this.config.scenario().setUseTransit(true);
	}

	protected void convertSchedule() {
		final VisumNetwork vNetwork = new VisumNetwork();
		try {
			log.info("reading visum network.");
//			new VisumNetworkReader(vNetwork).read("/Volumes/Data/VSP/coding/eclipse35/thesis-data/networks/yalcin/ptzh_orig.net");
			new VisumNetworkReader(vNetwork).read("/Volumes/Data/VSP/coding/eclipse35/thesis-data/application/input/oev_modell.net");
			log.info("converting visum data to TransitSchedule.");
			new Visum2TransitSchedule(vNetwork, this.scenario.getTransitSchedule()).convert();
			log.info("writing TransitSchedule to file.");
			new TransitScheduleWriterV1(this.scenario.getTransitSchedule()).write("../thesis-data/application/transitschedule.oevModell.xml");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void readSchedule() {
		log.info("reading TransitSchedule from file.");
		try {
			new TransitScheduleReader(this.scenario).readFile("../thesis-data/application/transitSchedule.oevModell.xml");
//			new TransitScheduleReader(this.scenario).readFile("../thesis-data/application/zuerichSchedule.xml");
//			new TransitScheduleReader(this.scenario).readFile("../shared-svn/studies/schweiz-ivtch/pt-experimental/TransitSim/transitSchedule.xml");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}


	protected void routePopulation() {
		PopulationImpl pop = this.scenario.getPopulation();
		try {
			new MatsimPopulationReader(this.scenario).parse(DILUTED_PT_1PCT_PLANS_FILENAME);
			pop.printPlansCount();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

//		new CreatePseudoNetwork(this.scenario.getTransitSchedule(), this.scenario.getNetwork()).run();

		DijkstraFactory dijkstraFactory = new DijkstraFactory();
		FreespeedTravelTimeCost timeCostCalculator = new FreespeedTravelTimeCost(this.scenario.getConfig().charyparNagelScoring());
		TransitConfigGroup transitConfig = new TransitConfigGroup();
		PlansCalcTransitRoute router = new PlansCalcTransitRoute(this.scenario.getConfig().plansCalcRoute(),
				this.scenario.getNetwork(), timeCostCalculator, timeCostCalculator, dijkstraFactory,
				this.scenario.getTransitSchedule(), transitConfig);
		log.info("start pt-router");
		router.run(pop);
		log.info("write routed plans out.");
		new PopulationWriter(pop).write(DILUTED_PT_ROUTED_1PCT_PLANS_FILENAME);
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
//		OTFVis.main(new String[] {"visNet.xml"});
		Events events = new Events();
		OnTheFlyQueueSimQuad client = new OnTheFlyQueueSimQuad(visScenario, events);
		client.run();
	}

	public static void main(final String[] args) {
		Application1 app = new Application1();
		app.prepareConfig();
//		app.convertSchedule();
		app.readSchedule(); // either convert, or read, but not both!
		app.routePopulation();
//		app.visualizeRouterNetwork();

		log.info("done.");
	}

}
