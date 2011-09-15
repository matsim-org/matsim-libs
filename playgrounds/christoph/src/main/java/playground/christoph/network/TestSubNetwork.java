/* *********************************************************************** *
 * project: org.matsim.*
 * TestSubNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.network;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculator;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.NetsimNetwork;

import playground.christoph.knowledge.container.MapKnowledgeDB;
import playground.christoph.knowledge.container.NodeKnowledge;
import playground.christoph.knowledge.container.dbtools.DBConnectionTool;
import playground.christoph.network.util.SubNetworkCreator;
import playground.christoph.network.util.SubNetworkTools;
import playground.christoph.router.costcalculators.KnowledgeTravelCostWrapper;
import playground.christoph.router.costcalculators.KnowledgeTravelTimeCalculator;
import playground.christoph.router.costcalculators.KnowledgeTravelTimeWrapper;
import playground.christoph.router.util.KnowledgeTools;

public class TestSubNetwork {
	
	private final static Logger log = Logger.getLogger(TestSubNetwork.class);

	private final ScenarioImpl scenario;
	private Config config;

	private KnowledgeTravelTimeCalculator travelTime;
	private KnowledgeTravelTimeWrapper travelTimeWrapper;
	private OnlyTimeDependentTravelCostCalculator travelCost;
	private KnowledgeTravelCostWrapper travelCostWrapper;

	private PlansCalcRoute dijkstraRouter;
	private NetsimNetwork qNetwork;

	private final String configFileName = "../matsim/mysimulations/kt-zurich/config.xml";
	private final String dtdFileName = null;
	private final String networkFile = "../matsim/mysimulations/kt-zurich/input/network.xml";
	private final String populationFile = "../matsim/mysimulations/kt-zurich/input/plans.xml";

	public static void main(String[] args) {
		new TestSubNetwork();
	}

	public TestSubNetwork() {
		DBConnectionTool dbct = new DBConnectionTool();
		dbct.connect();

		loadConfig();
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(this.config);
		loadNetwork();
		loadPopulation();

		log.info("Network size: " + this.scenario.getNetwork().getLinks().size());
		log.info("Population size: " + this.scenario.getPopulation().getPersons().size());

		setKnowledgeStorageHandler();

		initReplanner();

		createSubNetworks();
	}

	private void loadNetwork() {
		NetworkImpl network = this.scenario.getNetwork();
		network.getFactory().setLinkFactory(new MyLinkFactoryImpl());

		new MatsimNetworkReader(this.scenario).readFile(networkFile);

		log.info("Loading Network ... done");
	}

	private void loadPopulation() {
		new MatsimPopulationReader(this.scenario).readFile(populationFile);
		log.info("Loading Population ... done");
	}

	private void setKnowledgeStorageHandler() {
		for(Person person : this.scenario.getPopulation().getPersons().values())
		{
			Map<String, Object> customAttributes = person.getCustomAttributes();

			customAttributes.put("NodeKnowledgeStorageType", MapKnowledgeDB.class.getName());

			MapKnowledgeDB mapKnowledgeDB = new MapKnowledgeDB();
			mapKnowledgeDB.setPerson(person);
			mapKnowledgeDB.setNetwork(this.scenario.getNetwork());

			customAttributes.put("NodeKnowledge", mapKnowledgeDB);
		}
	}

	private void initReplanner() {
		QSim sim = new QSim(this.scenario, (EventsUtils.createEventsManager()));
		qNetwork = sim.getNetsimNetwork();

		travelTime = new KnowledgeTravelTimeCalculator(qNetwork);
		travelTimeWrapper = new KnowledgeTravelTimeWrapper(travelTime);

		travelCost = new OnlyTimeDependentTravelCostCalculator(travelTimeWrapper);
		travelCostWrapper = new KnowledgeTravelCostWrapper(travelCost);

		travelTimeWrapper.checkNodeKnowledge(false);
		travelCostWrapper.checkNodeKnowledge(false);

		dijkstraRouter = new PlansCalcRoute(new PlansCalcRouteConfigGroup(), this.scenario.getNetwork(),
				travelCostWrapper, travelTimeWrapper, new DijkstraFactory(), ((PopulationFactoryImpl) this.scenario.getPopulation().getFactory()).getModeRouteFactory());
	}

	private void createSubNetworks() {
		SubNetworkCreator snc = new SubNetworkCreator(this.scenario.getNetwork());
		SubNetworkTools snt = new SubNetworkTools();
		KnowledgeTools kt = new KnowledgeTools();

		int i = 0;
		for(Person person : this.scenario.getPopulation().getPersons().values()) {
			Map<String, Object> customAttributes = person.getCustomAttributes();

			NodeKnowledge nk = kt.getNodeKnowledge(person);

			((MapKnowledgeDB)nk).readFromDB();

			SubNetwork subNetwork = snc.createSubNetwork(nk);

			customAttributes.put("SubNetwork", subNetwork);

			if(!checkNodeKnowledge(subNetwork, nk)) log.error("Not all Nodes included!");

			replanRoute(person);

//			log.info("Nodes: " + subNetwork.getNodes().size() + ", Links: " + subNetwork.getLinks().size());

			// clear memory
			((MapKnowledgeDB)nk).clearLocalKnowledge();
			snt.resetSubNetwork(person);

			i++;
			if (i % 1000 == 0) log.info("Subnetworks: " + i);
//			customAttributes.put("SubNetwork", new SubNetwork(this.network));
		}
	}

	private boolean checkNodeKnowledge(SubNetwork subNetwork, NodeKnowledge nodeKnowledge) {
		
		for (Node node : nodeKnowledge.getKnownNodes().values()) {
			if (!subNetwork.getNodes().containsKey(node.getId())) return false;
		}

		for(Link link : subNetwork.getLinks().values()) {
			if(!nodeKnowledge.getKnownNodes().containsKey(link.getToNode().getId())) return false;
			if(!nodeKnowledge.getKnownNodes().containsKey(link.getFromNode().getId())) return false;
		}

		return true;
	}

	private void replanRoute(Person person) {
		dijkstraRouter.run(person.getSelectedPlan());
	}

	private void loadConfig() {
		this.config = new Config();
		this.config.addCoreModules();
		this.config.checkConsistency();
		new MatsimConfigReader(this.config).readFile(this.configFileName, this.dtdFileName);
		log.info("Loading Config ... done");
	}
}
