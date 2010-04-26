/* *********************************************************************** *
 * project: org.matsim.*
 * CreateDijkstraKnowledge4MultiFactors.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.christoph.knowledge.container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.utils.misc.Time;

import playground.christoph.knowledge.nodeselection.DijkstraForSelectNodes;
import playground.christoph.network.MyLinkFactoryImpl;
import playground.christoph.network.MyLinkImpl;
import playground.christoph.router.costcalculators.OnlyTimeDependentTravelCostCalculator;
import playground.christoph.router.util.KnowledgeTools;

/*
 * Creates Knowledge Entries for multiple CostFactors in a single Iteration,
 * what provides a major speed up. The Dijkstra Algorithm has to be run only once
 * per route instead of once per combination of route and costfactor.
 */
public class CreateDijkstraKnowledge4MultiFactors {

	private final static Logger log = Logger.getLogger(CreateDijkstraKnowledge4MultiFactors.class);

	private NetworkImpl network;
	private ActivityFacilitiesImpl facilities;
	private Population population;
	private Config config;
	private final ScenarioImpl scenario;

	private final int numOfThreads = 8;

	private final double[] dijkstraCostFactors = {1.0, 1.25, 1.5, 1.75, 2.0};
	private List<Double> costFactorsList;

	/*
	 * Dijkstra based Node Selector
	 * The selected Nodes depend on the used Cost Calculator
	 */
	// null as Argument: -> no TimeCalculator -> use FreeSpeedTravelTime
	private final TravelCost costCalculator = new OnlyTimeDependentTravelCostCalculator(null);

	/*
	 * How to call the Table in the Database. Additionally the used size
	 * factor will be added to the String.
	 */
	private static final String baseTableName = "BatchTable";

	private final String configFileName = "test/scenarios/berlin/config.xml";
	private final String dtdFileName = null;
	private final String networkFile = "test/scenarios/berlin/network.xml.gz";
	private final String populationFile = "test/scenarios/berlin/plans_hwh_1pct.xml.gz";
	private final String facilitiesFile = null;

//	private final String configFileName = "mysimulations/kt-zurich/config.xml";
//	private final String dtdFileName = null;
//	private final String networkFile = "mysimulations/kt-zurich/input/network.xml";
//	private final String populationFile = "mysimulations/kt-zurich/input/plans.xml";
//	private final String facilitiesFile = null;

//	private final String configFileName = "mysimulations/switzerland/config.xml";
//	private final String dtdFileName = null;
//	private final String networkFile = "mysimulations/switzerland/input/network.xml";
////	private final String populationFile = "mysimulations/switzerland/input/plans.xml.gz";
//	private final String populationFile = "mysimulations/switzerland/input/plans_split_1.xml.gz";
//	private final String facilitiesFile = "mysimulations/switzerland/input/facilities.xml.gz";

//	private final String configFileName = "mysimulations/kt-zurich-cut/config_10.xml";
//	private final String dtdFileName = null;
//	private final String networkFile = "mysimulations/kt-zurich-cut/mapped_network.xml.gz";
//	private final String populationFile = "mysimulations/kt-zurich-cut/plans_10.xml.gz";
//	private final String facilitiesFile = "mysimulations/kt-zurich-cut/facilities.zrhCutC.xml.gz";

	public static void main(String[] args)
	{
		new CreateDijkstraKnowledge4MultiFactors();
	}

	public CreateDijkstraKnowledge4MultiFactors()
	{
		this.scenario = new ScenarioImpl();
		loadNetwork();
		if (facilitiesFile != null) loadFacilities();
		loadPopulation();
		loadConfig();

		log.info("Network size: " + network.getNodes().size());
		log.info("Population size: " + population.getPersons().size());

		setKnowledgeStorageHandler();
		createCostFactorsList();
		initDatabaseTables();
		run();
	}

	private void loadNetwork()
	{
		network = scenario.getNetwork();

		/*
		 * Use MyLinkImpl. They can carry some additional Information like their
		 * TravelTime or TravelCost.
		 */
		network.getFactory().setLinkFactory(new MyLinkFactoryImpl());

		new MatsimNetworkReader(scenario).readFile(networkFile);

		/*
		 * Now calculate the TravelCosts for each link. By doing this the
		 * Dijkstra Algorithm does not have to do it again for each loop.
		 */
		for (Link link : network.getLinks().values())
		{
			double travelCosts = costCalculator.getLinkTravelCost(link, Time.UNDEFINED_TIME);
			((MyLinkImpl)link).setTravelCost(travelCosts);
		}

		log.info("Loading Network ... done");
	}

	private void loadFacilities()
	{
		facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(facilitiesFile);

		log.info("Loading Facilities ... done");
	}

	private void loadPopulation()
	{
		new MatsimPopulationReader(scenario).readFile(populationFile);
		this.population = this.scenario.getPopulation();
		log.info("Loading Population ... done");
	}

	private void loadConfig()
	{
		this.config = scenario.getConfig();
		this.config.checkConsistency();
		try {
			new MatsimConfigReader(this.config).readFile(this.configFileName, this.dtdFileName);
		} catch (IOException e) {
			log.error("Problem loading the configuration file from " + this.configFileName);
			throw new RuntimeException(e);
		}
		log.info("Loading Config ... done");
	}

	/*
	 * How to store the known Nodes of the Agents?
	 * Currently we store them in a Database.
	 */
	private void setKnowledgeStorageHandler()
	{
		for(Person person : population.getPersons().values())
		{
			Map<String, Object> customAttributes = person.getCustomAttributes();

			customAttributes.put("NodeKnowledgeStorageType", MapKnowledgeDB.class.getName());

			MapKnowledgeDB mapKnowledgeDB = new MapKnowledgeDB();
			mapKnowledgeDB.setPerson(person);
			mapKnowledgeDB.setNetwork(network);

			customAttributes.put("NodeKnowledge", mapKnowledgeDB);
		}
	}

	private void createCostFactorsList()
	{
		costFactorsList = new ArrayList<Double>();

		for (double d : dijkstraCostFactors) costFactorsList.add(d);

		// We have to sort the CostFactors!!!
		Collections.sort(costFactorsList);
	}

	/*
	 * Creates the Tables in the Database if they do not already exist.
	 * If they exist, clear their content.
	 */
	private void initDatabaseTables()
	{
		MapKnowledgeDB mapKnowledgeDB = new MapKnowledgeDB();

		for (double costFactor : costFactorsList)
		{
			// Set DB Name
			String tableName = baseTableName + String.valueOf(costFactor).replace(".", "_");

			mapKnowledgeDB.setTableName(tableName);
			mapKnowledgeDB.createTable();
			mapKnowledgeDB.clearTable();
		}
	}

	private void run()
	{
		Thread[] threads = new Thread[this.numOfThreads];
		CreateKnowledgeThread[] creationThreads = new CreateKnowledgeThread[numOfThreads];

		/*
		 * Create Cost Arrays only once initially. They don't change so all Threads can
		 * use them at the same time.
		 */
		log.info("Creating Cost Arrays ...");
		SpanningTreeProvider stp = new SpanningTreeProvider(this.network);
		stp.createCostArray();
		log.info("Creating Cost Arrays ... done");

		// setup threads
		for (int i = 0; i < numOfThreads; i++)
		{
			DijkstraForSelectNodes dijkstra = new DijkstraForSelectNodes(this.network);
			dijkstra.setCostCalculator(costCalculator);

			CreateKnowledgeThread createKnowledgeThread = new CreateKnowledgeThread(i, dijkstra, costFactorsList, stp.clone(), network);

			creationThreads[i] = createKnowledgeThread;

			Thread thread = new Thread(createKnowledgeThread, "Thread#" + i);
			threads[i] = thread;
		}

		// distribute workload between threads, as long as threads are not yet started, so we don't need synchronized data structures
		int i = 0;
		for (Person person : this.population.getPersons().values())
		{
			creationThreads[i % numOfThreads].handlePerson(person);
			i++;
		}

		// start the threads
		for (Thread thread : threads)
		{
			thread.start();
		}

		// wait for the threads to finish
		try {
			for (Thread thread : threads)
			{
				thread.join();
			}
		}
		catch (InterruptedException e)
		{
			Gbl.errorMsg(e);
		}
	}

	/**
	 * The thread class that really handles the persons.
	 */
	private static class CreateKnowledgeThread implements Runnable
	{
		public final int threadId;
		private final List<Double> costFactors;
		private final List<Person> persons;
		private final KnowledgeTools knowledgeTools;
		private final DijkstraForSelectNodes dijkstra;
		private final SpanningTreeProvider stp;
		private final Network network;

		public CreateKnowledgeThread(final int i, final DijkstraForSelectNodes dijkstra, final List<Double> costFactors, SpanningTreeProvider stp, final Network network)
		{
			this.threadId = i;
			this.network = network;

			// Sort the List - this is necessary for the Algorithm to work properly!
			Collections.sort(costFactors);
			this.costFactors = costFactors;
			this.stp = stp;

			this.knowledgeTools = new KnowledgeTools();
			this.dijkstra = dijkstra;
			this.persons = new LinkedList<Person>();
		}

		public void handlePerson(final Person person)
		{
			this.persons.add(person);
		}

		public void run()
		{
			int numRuns = 0;

			for (Person person : this.persons)
			{
				// get selected Plan
				Plan plan = person.getSelectedPlan();

				// get all Acts of the selected Plan
				List<ActivityImpl> acts = new ArrayList<ActivityImpl>();

				for (PlanElement pe : plan.getPlanElements())
				{
					if (pe instanceof ActivityImpl)
					{
						acts.add((ActivityImpl) pe);
					}
				}

				// create new Knowledges ArrayList
				List<Map<Id, Node>> knowledges = new ArrayList<Map<Id, Node>>();
				for (int i = 0; i < costFactors.size(); i++)
				{
					knowledges.add(new HashMap<Id, Node>());
				}

				for(int i = 1; i < acts.size(); i++)
				{
					Node startNode = this.network.getLinks().get(acts.get(i-1).getLinkId()).getToNode();
					Node endNode = this.network.getLinks().get(acts.get(i).getLinkId()).getFromNode();

					List<Map<Id, Node>> additionalKnowledges = multiRun(startNode, endNode);

					int j = 0;
					for (Map<Id, Node> map : additionalKnowledges)
					{
						knowledges.get(j).putAll(map);
						j++;
					}
				}

				// Write Knowledge to DataBase
				Map<String, Object> customAttributes = person.getCustomAttributes();

				MapKnowledgeDB mapKnowledgeDB = (MapKnowledgeDB) customAttributes.get("NodeKnowledge");

				int i = 0;
				for (Map<Id, Node> knowledge : knowledges)
				{
					// Set DB Name
					String tableName = baseTableName + String.valueOf(costFactors.get(i)).replace(".", "_");
					mapKnowledgeDB.setTableName(tableName);

					mapKnowledgeDB.setKnownNodes(knowledge);

					i++;
				}

				// Remove Knowledge
				knowledgeTools.removeKnowledge(person);

				numRuns++;
				if (numRuns % 500 == 0) log.info("created known Nodes for " + numRuns + " persons in thread " + threadId);

			}

			log.info("Thread " + threadId + " done.");

		}	// run


		private List<Map<Id, Node>> multiRun(Node startNode, Node endNode)
		{
			// Without any CostFactors we do nothing.
			if (costFactors.size() == 0) return null;

			List<Map<Id, Node>> knowledges = new ArrayList<Map<Id, Node>>();

			Map<Node, Double> totalNodes;

			// If we get the Dijkstra Data from a SpanningTreeProvider
			if (stp != null)
			{
				totalNodes = stp.getTotalSpanningTree(startNode, endNode);
			}
			else
			{
			// 	Calculate the Dijkstra only once.
				dijkstra.executeTotalNetwork(startNode, endNode);
				totalNodes = dijkstra.getTotalMinDistances();
			}

			// get the minimal costs to get from the start- to the endnode
			double minCostsStart = totalNodes.get(endNode);
			double minCostsEnd = totalNodes.get(startNode);

			if (minCostsStart / minCostsEnd < 0.95 || minCostsStart / minCostsEnd > 1.05)
			{
				log.warn("Different Costs in different Traveldirection (> 5% Difference found)!");
			}

			double minCosts;
			if (minCostsStart > minCostsEnd) minCosts = minCostsStart;
			else minCosts = minCostsEnd;


			// Now start the iterative Knowledge Creation
			Map<Id, Node> previousKnowledge = new HashMap<Id, Node>();

			for (double costFactor : costFactors)
			{
				Map<Id, Node> knowledge = new HashMap<Id, Node>();
				knowledge.putAll(previousKnowledge);

				Iterator<Entry<Node, Double>> iter = totalNodes.entrySet().iterator();
				while(iter.hasNext())
				{
					Entry<Node, Double> entry = iter.next();

					Node node = entry.getKey();
					double costs = entry.getValue();

					/*
					 * If the costs are smaller than the specified limit -> add Node.
					 */
					if (costs <= minCosts * costFactor)
					{
						knowledge.put(node.getId(), node);
						iter.remove();
					}
				}

				knowledges.add(knowledge);

				previousKnowledge.clear();
				previousKnowledge.putAll(knowledge);
			}

			totalNodes.clear();

			return knowledges;
		}

	}	// CreateKnowledgeThread
}
