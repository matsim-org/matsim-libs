/* *********************************************************************** *
 * project: org.matsim.*
 * CreateKnowledgeForDatabase.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Time;

import playground.christoph.knowledge.nodeselection.ParallelCreateKnownNodesMap;
import playground.christoph.knowledge.nodeselection.SelectNodes;
import playground.christoph.knowledge.nodeselection.SelectNodesCircular;
import playground.christoph.knowledge.nodeselection.SelectNodesDijkstra;
import playground.christoph.network.MyLinkFactoryImpl;
import playground.christoph.network.MyLinkImpl;

/* 
 * The following Parameters have to be set:
 * - the scenario that should be used
 * - the number of parallel threads
 * - configuration of the Node Selectors (Cost Calculators, etc.)
 * - used Node Selectors per Agent
 * - the default Table Name in the Database
 */
public class CreateKnowledgeForDatabase {

	private final static Logger log = Logger.getLogger(CreateKnowledgeForDatabase.class);

	private final ScenarioImpl scenario;
	private NetworkImpl network;
	private Population population;
	private ActivityFacilitiesImpl facilities;
	private ArrayList<SelectNodes> nodeSelectors;

	private final double dijkstraCostFactor = 1.0;
	private final double[] dijkstraCostFactors = {1.05, 1.1, 1.15, 1.2, 1.3, 1.35, 1.4, 1.45};
//	private double[] dijkstraCostFactors = {1.0, 1.25, 1.5, 1.75, 2.0, 2.25, 2.5, 2.75, 3.0};
//	private double[] dijkstraCostFactors = {1.0};
	private TravelCost costCalculator;

	private final int parallelThreads = 8;

	private final String separator = System.getProperty("file.separator");

	// Default
	// Berlin Scenario - contained in MATSim
	private String configFileName = "test/scenarios/berlin/config.xml";
	private String networkFile = "test/scenarios/berlin/network.xml.gz";
	private String populationFile = "test/scenarios/berlin/plans_hwh_1pct.xml.gz";
	private final String facilitiesFile = null;

/*
	private String configFileName = "mysimulations/kt-zurich/config.xml";
	private String networkFile = "mysimulations/kt-zurich/input/network.xml";
	private String populationFile = "mysimulations/kt-zurich/input/plans.xml.gz";
	private String facilitiesFile = "mysimulations/kt-zurich/input/facilities.xml.gz";
*/

/*
 	private String configFileName = "mysimulations/kt-zurich-cut/config_10.xml";
	private String networkFile = "mysimulations/kt-zurich-cut/network.xml.gz";
	private String populationFile = "mysimulations/kt-zurich-cut/plans_10.xml";
	private String facilitiesFile = "mysimulations/kt-zurich-cut/facilities.zrhCutC.xml.gz";
*/

/*
	private String configFileName = "G:/mysimulations/kt-zurich/config.xml";
	private String networkFile = "G:/mysimulations/kt-zurich/input/network.xml";
	private String populationFile = "G:/mysimulations/kt-zurich/input/plans.xml";
	private String facilitiesFile = null;
*/

	/*
	 * How to call the Table in the Database. Additionally the used size
	 * factor will be added to the String.
	 */
	private static final String baseTableName = "BatchTable";

	public static void main(String[] args)
	{
		new CreateKnowledgeForDatabase();
	}

	public CreateKnowledgeForDatabase()
	{
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		configFileName = configFileName.replace("/", separator);
		networkFile = networkFile.replace("/", separator);
		populationFile = populationFile.replace("/", separator);

		loadNetwork();
		log.info("Network size (links): " + network.getLinks().size());
		log.info("Network size (nodes): " + network.getNodes().size());

		if (facilitiesFile != null)
		{
			loadFacilities();
			log.info("Facilities size: " + facilities.getFacilities().size());
		}

		loadPopulation();
		log.info("Population size: " + population.getPersons().size());

		// initialize and assign the Node Selectors
		initNodeSelectors();
		log.info("Initialized NodeSelctors");
		setNodeSelectors();
		log.info("Assigned NodeSelectors");

		// create TravelCost LookupTable using MyLinkImpls
		createLookupTable();

		for (double factor : dijkstraCostFactors)
		{
			for (SelectNodes selectNodes : nodeSelectors)
			{
				if (selectNodes instanceof SelectNodesDijkstra)
				{
					((SelectNodesDijkstra)selectNodes).setCostFactor(factor);
				}
			}

			// initialize and assign the Knowledge Storage Handlers
			initDatabaseTable(factor);
			log.info("Initialize Knowledge Storage Handler");
			setKnowledgeStorageHandler(factor);
			log.info("Set Knowledge Storage Handler");

			// create known Nodes for each Person
			createKnownNodes();
			log.info("Created Known Nodes");
		}
	}

	/*
	 * Initializes the NodeSeletors that are used to create the Activity Spaces of the
	 * Persons of a Population.
	 *
	 * Depending on the used Node Selector different Nodes for the known Areas
	 * are selected.
	 */
	private void initNodeSelectors()
	{
		nodeSelectors = new ArrayList<SelectNodes>();

		/*
		 * Circular Node Selector
		 */
		nodeSelectors.add(new SelectNodesCircular(this.network));

		/*
		 * Dijkstra based Node Selector
		 * The selected Nodes depend on the used Cost Calculator
		 */
		// null as Argument: -> no TimeCalculator -> use FreeSpeedTravelTime
		costCalculator = new OnlyTimeDependentTravelCostCalculator(null);

		SelectNodesDijkstra selectNodesDijkstra = new SelectNodesDijkstra(this.network);
		selectNodesDijkstra.setCostCalculator(costCalculator);
		selectNodesDijkstra.setCostFactor(dijkstraCostFactor);
		nodeSelectors.add(selectNodesDijkstra);
	}

	/*
	 * Assigns nodeSelectors to every Person of the population, which are
	 * used to create an activity rooms for every Person. It is possible to
	 * assign more than one Selector to each Person.
	 * If non is selected the Person knows every Node of the network.
	 *
	 * If no NodeSelectors is added (the ArrayList is initialized but empty)
	 * the person knows the entire Network (KnowledgeTools.knowsLink(...)
	 * always returns true).
	 */
	private void setNodeSelectors()
	{
		// Create NodeSelectorContainer
		for (Person person : population.getPersons().values())
		{
			Map<String,Object> customAttributes = person.getCustomAttributes();

			List<SelectNodes> personNodeSelectors = new ArrayList<SelectNodes>();

			// personNodeSelectors.add(nodeSelectors.get(0));	// Circular NodeSelector
			personNodeSelectors.add(nodeSelectors.get(1));	// Dijkstra NodeSelector

			customAttributes.put("NodeSelectors", personNodeSelectors);
		}
	}

	/*
	 * Creates the Table in the Database if it does not already exist.
	 * If it exists, clear its content.
	 */
	private void initDatabaseTable(double costFactor)
	{
		MapKnowledgeDB mapKnowledgeDB = new MapKnowledgeDB();

		// Set DB Name
		String tableName = baseTableName + String.valueOf(costFactor).replace(".", "_");

		mapKnowledgeDB.setTableName(tableName);
		mapKnowledgeDB.createTable();
		mapKnowledgeDB.clearTable();
	}

	/*
	 * How to store the known Nodes of the Agents?
	 * Currently we store them in a Database.
	 */
	private void setKnowledgeStorageHandler(double factor)
	{
		for(Person person : population.getPersons().values())
		{
			Map<String, Object> customAttributes = person.getCustomAttributes();

			customAttributes.put("NodeKnowledgeStorageType", MapKnowledgeDB.class.getName());

			// Create MapKnowledgeDB and set the name of the Table in the Database.
			MapKnowledgeDB mapKnowledgeDB = new MapKnowledgeDB();
			mapKnowledgeDB.setPerson(person);
			mapKnowledgeDB.setNetwork(network);
			mapKnowledgeDB.setTableName(baseTableName + String.valueOf(factor).replace(".", "_"));

			customAttributes.put("NodeKnowledge", mapKnowledgeDB);
		}
	}

	/*
	 * Creates the Maps of Nodes that each Agents "knows" parallel on multiple Threads.
	 */
	private void createKnownNodes()
	{
		ParallelCreateKnownNodesMap.run(this.population, this.network, nodeSelectors, parallelThreads);
	}

	/*
	 * We use MyLinks when creating the Network.
	 * They can store additional Information like TravelTimes and
	 * TravelCosts so they can act like LookupTables.
	 *
	 * We can do this as long as all Agents use the same Cost- and
	 * TimeCalculators.
	 */
	private void loadNetwork()
	{
		network = scenario.getNetwork();
		network.getFactory().setLinkFactory(new MyLinkFactoryImpl());

		new MatsimNetworkReader(scenario).readFile(networkFile);
		log.info("Loading Network ... done");
	}

	/*
	 * If we use MyLinks to use them as LookTables for
	 * the TravelCosts, we have to set the TravelCosts.
	 */
	private void createLookupTable()
	{
		for (Link link : network.getLinks().values())
		{
			((MyLinkImpl) link).setTravelCost(costCalculator.getLinkGeneralizedTravelCost(link, Time.UNDEFINED_TIME));
		}
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

}
