package playground.christoph.knowledge.container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsImpl;
import org.matsim.core.facilities.ActivityFacilities;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.events.LinkVehiclesCounter;
import playground.christoph.events.algorithms.ParallelInitialReplanner;
import playground.christoph.events.algorithms.ParallelReplanner;
import playground.christoph.knowledge.nodeselection.ParallelCreateKnownNodesMap;
import playground.christoph.knowledge.nodeselection.SelectNodes;
import playground.christoph.knowledge.nodeselection.SelectNodesCircular;
import playground.christoph.knowledge.nodeselection.SelectNodesDijkstra;
import playground.christoph.mobsim.MyQueueNetwork;
import playground.christoph.mobsim.ReplanningQueueSimulation;
import playground.christoph.router.CompassRoute;
import playground.christoph.router.DijkstraWrapper;
import playground.christoph.router.KnowledgePlansCalcRoute;
import playground.christoph.router.RandomCompassRoute;
import playground.christoph.router.RandomRoute;
import playground.christoph.router.TabuRoute;
import playground.christoph.router.costcalculators.KnowledgeTravelCostCalculator;
import playground.christoph.router.costcalculators.KnowledgeTravelTimeCalculator;
import playground.christoph.router.costcalculators.OnlyTimeDependentTravelCostCalculator;

public class CreateKnowledgeForDatabase {

	private final static Logger log = Logger.getLogger(CreateKnowledgeForDatabase.class);
	
	private NetworkLayer network;
	private PopulationImpl population;
	private ActivityFacilities facilities;
	private Config config;
	private ArrayList<PlanAlgorithm> replanners;
	private ArrayList<SelectNodes> nodeSelectors;
	private double dijkstraCostFactor = 1.0;
	//private double[] dijkstraCostFactors = {1.0, 1.5, 2.0, 2.5, 3.0};
	private double[] dijkstraCostFactors = {1.25};
	private int parallelThreads = 2;
	private ReplanningQueueSimulation sim;
	private EventsImpl events;
	private LinkVehiclesCounter linkVehiclesCounter;
	
	private final String dtdFileName = null;
	
	private final String separator = System.getProperty("file.separator");

/*
	// Default
	private String configFileName = "test/scenarios/berlin/config.xml";
	private String networkFile = "test/scenarios/berlin/network.xml";
	private String populationFile = "test/scenarios/berlin/plans_hwh_1pct.xml";
	private String facilitiesFile = null;
*/

	private String configFileName = "mysimulations/kt-zurich/config.xml";
	private String networkFile = "mysimulations/kt-zurich/input/network.xml";
	private String populationFile = "mysimulations/kt-zurich/input/plans.xml.gz";
	private String facilitiesFile = "mysimulations/kt-zurich/input/facilities.xml.gz";
	
/*	
 	private String configFileName = "mysimulations/kt-zurich-cut/config_10.xml";
	private String networkFile = "mysimulations/kt-zurich-cut/network.xml";
	private String populationFile = "mysimulations/kt-zurich-cut/plans_10.xml";
	private String facilitiesFile = "mysimulations/kt-zurich-cut/facilities.zrhCutC.xml";
*/	
/*	
	private String configFileName = "G:/mysimulations/kt-zurich/config.xml";
	private String networkFile = "G:/mysimulations/kt-zurich/input/network.xml";
	private String populationFile = "G:/mysimulations/kt-zurich/input/plans.xml";
	private String facilitiesFile = null;
*/
	
	private final String baseTableName = "BatchTable";
	
	public static void main(String[] args)
	{	
	//	MapKnowledgeDB.setTableName("BatchTable");
		new CreateKnowledgeForDatabase();
	}
	
	public CreateKnowledgeForDatabase()
	{	
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
		
		loadConfig();
		
		// Initialize a Replanning Queue Simulation 
//		initSim();
		
//		initLinkVehiclesCounter();
				
		// initialize and assign the Node Selectors
		initNodeSelectors();
		log.info("Initialized NodeSelctors");
		setNodeSelectors();
		log.info("Assigned NodeSelectors");
			
		// initialize and set the Replanning Router
		initReplanningRouter();
		log.info("Intialized Replanning Routers");
		setReplanningRouter();
		log.info("Assigned Replanning Routers");

/*		
		// initialize and assign the Knowledge Storage Handlers
		initKnowledgeStorageHandler();
		log.info("Initialize Knowledge Storage Handler");
		setKnowledgeStorageHandler();
		log.info("Set Knowledge Storage Handler");
*/	
		
		
		for (double factor : dijkstraCostFactors)
		{
			for (SelectNodes selectNodes : nodeSelectors)
			{
				if (selectNodes instanceof SelectNodesDijkstra)
				{
					((SelectNodesDijkstra)selectNodes).setCostFactor(factor);
				}
			}

			MapKnowledgeDB.setTableName(this.baseTableName + String.valueOf(factor).replace(".", "_"));
			
			// initialize and assign the Knowledge Storage Handlers
			initKnowledgeStorageHandler();
			log.info("Initialize Knowledge Storage Handler");
			setKnowledgeStorageHandler();
			log.info("Set Knowledge Storage Handler");
			
			// create known Nodes for each Person
			createKnownNodes();
			log.info("Created Known Nodes");
			
		}		
		
		/*
		 *  do initial Replanning
		 *  if this Step is needed depends on the used Node Selectors
		 */
/*
		initParallelReplanningModules();
		log.info("Initialized Parallel Replanning Modules");
		doInitialReplanning();
		log.info("Done Initial Replanning");
*/		

/*		// create known Nodes for each Person
		createKnownNodes();
		log.info("Created Known Nodes");
*/
	}
	
	private void initSim()
	{
		events = new EventsImpl();
		sim = new ReplanningQueueSimulation(network, population, events);
	}
	
	private void initLinkVehiclesCounter()
	{
		linkVehiclesCounter = new LinkVehiclesCounter();
		linkVehiclesCounter.setQueueNetwork(sim.getQueueNetwork());
		this.events.addHandler(linkVehiclesCounter);
		sim.getMyQueueNetwork().setLinkVehiclesCounter(linkVehiclesCounter);
		
		sim.addQueueSimulationListeners(linkVehiclesCounter);
	}
	
	/*
	 * New Routers for the Replanning are used instead of using the controler's. 
	 * By doing this every person can use a personalised Router.
	 */
	private void initReplanningRouter()
	{
		replanners = new ArrayList<PlanAlgorithm>();
			
		// Dijkstra for Replanning
		KnowledgeTravelTimeCalculator travelTime = new KnowledgeTravelTimeCalculator();
		KnowledgeTravelCostCalculator travelCost = new KnowledgeTravelCostCalculator(travelTime);
		
		// Use a Wrapper - by doing this, already available MATSim CostCalculators can be used
//		TravelTimeDistanceCostCalculator travelCost = new TravelTimeDistanceCostCalculator(travelTime);
//		KnowledgeTravelCostWrapper travelCostWrapper = new KnowledgeTravelCostWrapper(travelCost);

		// Use a Wrapper - by doing this, already available MATSim CostCalculators can be used
		//OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(travelTime);
		//KnowledgeTravelCostWrapper travelCostWrapper = new KnowledgeTravelCostWrapper(travelCost);
		
		// Use the Wrapper with the same CostCalculator as the MobSim uses
		//KnowledgeTravelCostWrapper travelCostWrapper = new KnowledgeTravelCostWrapper(this.getTravelCostCalculator());
		
//		Dijkstra dijkstra = new Dijkstra(network, travelCostWrapper, travelTime);
//		DijkstraWrapper dijkstraWrapper = new DijkstraWrapper(dijkstra, travelCostWrapper, travelTime);
		Dijkstra dijkstra = new Dijkstra(network, travelCost, travelTime);
		DijkstraWrapper dijkstraWrapper = new DijkstraWrapper(dijkstra, travelCost, travelTime, network);
		KnowledgePlansCalcRoute dijkstraRouter = new KnowledgePlansCalcRoute(network, dijkstraWrapper, dijkstraWrapper);
		
		dijkstraRouter.setMyQueueNetwork(new MyQueueNetwork(network));
		//dijkstraRouter.setMyQueueNetwork(sim.getMyQueueNetwork());
		replanners.add(dijkstraRouter);
	}
	
	/*
	 * Assigns a replanner to every Person of the population.
	 * Same problem as above: should be part of the PersonAgents, but only
	 * Persons are available in the replanning modules.
	 * 
	 * At the moment: Replanning Modules are assigned hard coded.
	 * Later: Modules are assigned based on probabilities from config files. 
	 */
	private void setReplanningRouter()
	{
		for (Person person : population.getPersons().values())
		{		
			Map<String,Object> customAttributes = person.getCustomAttributes();
			customAttributes.put("Replanner", replanners.get(0));	// DijstraWrapper
		}
	}
	
	/*
	 * Hands over the ArrayList to the ParallelReplanner
	 */
	private void initParallelReplanningModules()
	{
		ParallelReplanner.init(replanners);
		ParallelReplanner.setNumberOfThreads(parallelThreads);	
	}
	
	/*
	 *  Create the "cheapest" Routes on an empty Network.
	 *  Doing this should make the persons current routes irrelevant...
	 *  Necessary for example, if a Circular Node Selector is used.
	 */
	private void doInitialReplanning()
	{
		List<PersonImpl> personsToReplan = new ArrayList<PersonImpl>();
		
		personsToReplan.addAll(population.getPersons().values());
		
		double time = 0.0;
		
		// Run Replanner
		ParallelInitialReplanner.run(personsToReplan, time);

		// Number of Routes that could not be created...
		log.info(RandomRoute.getErrorCounter() + " Routes could not be created by RandomRoute.");
		log.info(TabuRoute.getErrorCounter() + " Routes could not be created by TabuRoute.");
		log.info(CompassRoute.getErrorCounter() + " Routes could not be created by CompassRoute.");
		log.info(RandomCompassRoute.getErrorCounter() + " Routes could not be created by RandomCompassRoute.");
		log.info(DijkstraWrapper.getErrorCounter() + " Routes could not be created by DijkstraWrapper.");
	} //doInitialReplanning
	
	
	/*
	 * Initializes the NodeSeletors that are used to create the Activity Spaces of the
	 * Persons of a Population.
	 */
	private void initNodeSelectors()
	{
		nodeSelectors = new ArrayList<SelectNodes>();
		
		nodeSelectors.add(new SelectNodesCircular(this.network));
		
		SelectNodesDijkstra selectNodesDijkstra = new SelectNodesDijkstra(this.network);
		// null as Argument: -> no TimeCalculator -> use FreeSpeedTravelTime
		selectNodesDijkstra.setCostCalculator(new OnlyTimeDependentTravelCostCalculator(null));
		selectNodesDijkstra.setCostFactor(dijkstraCostFactor);
		nodeSelectors.add(selectNodesDijkstra);
	}
	
	/*
	 * Assigns nodeSelectors to every Person of the population, which are
	 * used to create an activity rooms for every Person. It is possible to
	 * assign more than one Selector to each Person.
	 * If non is selected the Person knows every Node of the network.
	 *
	 * At the moment: Selection Modules are assigned hard coded.
	 * Later: Modules are assigned based on probabilities from config files.
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
	
	private void initKnowledgeStorageHandler()
	{
		MapKnowledgeDB mapKnowledgeDB = new MapKnowledgeDB();
		mapKnowledgeDB.createTable();
		mapKnowledgeDB.clearTable();
	}
	
	private void setKnowledgeStorageHandler()
	{
		for(Person person : population.getPersons().values())
		{
			Map<String, Object> customAttributes = person.getCustomAttributes();
			
			customAttributes.put("NodeKnowledgeStorageType", MapKnowledgeDB.class.getName());
		}
	}
	
	/*
	 * Creates the Maps of Nodes that each Agents "knows".
	 */
	private void createKnownNodes()
	{
		// create Known Nodes Maps on multiple Threads
		ParallelCreateKnownNodesMap.run(this.population, this.network, nodeSelectors, parallelThreads);
	}
	
	private void loadNetwork()
	{
		network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFile);
		log.info("Loading Network ... done");
	}
	
	private void loadFacilities()
	{
		facilities = new ActivityFacilitiesImpl();
		new MatsimFacilitiesReader(facilities).readFile(facilitiesFile);
		
		Gbl.getWorld().setFacilityLayer((ActivityFacilitiesImpl)facilities);
		
		log.info("Loading Facilities ... done");
	}
	
	private void loadPopulation()
	{
		population = new PopulationImpl();
		new MatsimPopulationReader(population, network).readFile(populationFile);
		log.info("Loading Population ... done");
	}
	
	private void loadConfig()
	{
		this.config = new Config();
		this.config.addCoreModules();
		this.config.checkConsistency();
		try {
			new MatsimConfigReader(this.config).readFile(this.configFileName, this.dtdFileName);
		} catch (IOException e) {
			log.error("Problem loading the configuration file from " + this.configFileName);
			throw new RuntimeException(e);
		}
		Gbl.setConfig(config);
		log.info("Loading Config ... done");
	}

}
