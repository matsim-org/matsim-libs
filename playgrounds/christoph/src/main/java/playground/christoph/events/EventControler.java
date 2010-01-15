/* *********************************************************************** *
 * project: org.matsim.*
 * EventControler.java
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

package playground.christoph.events;

import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.utils.geometry.transformations.AtlantisToWGS84;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.core.utils.geometry.transformations.GK4toWGS84;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.analysis.wardrop.ActTimesCollector;
import playground.christoph.analysis.wardrop.Wardrop;
import playground.christoph.events.algorithms.FixedOrderQueueSimulationListener;
import playground.christoph.events.algorithms.LookupTableUpdater;
import playground.christoph.events.algorithms.ParallelActEndReplanner;
import playground.christoph.events.algorithms.ParallelInitialReplanner;
import playground.christoph.events.algorithms.ParallelLeaveLinkReplanner;
import playground.christoph.knowledge.KMLPersonWriter;
import playground.christoph.knowledge.container.MapKnowledgeDB;
import playground.christoph.knowledge.nodeselection.ParallelCreateKnownNodesMap;
import playground.christoph.knowledge.nodeselection.SelectNodes;
import playground.christoph.knowledge.nodeselection.SelectNodesCircular;
import playground.christoph.knowledge.nodeselection.SelectNodesDijkstra;
import playground.christoph.knowledge.nodeselection.SelectionReaderMatsim;
import playground.christoph.knowledge.nodeselection.SelectionWriter;
import playground.christoph.mobsim.ActEndReplanningModule;
import playground.christoph.mobsim.LeaveLinkReplanningModule;
import playground.christoph.mobsim.ReplanningManager;
import playground.christoph.mobsim.ReplanningQueueSimulation;
import playground.christoph.network.MyLinkFactoryImpl;
import playground.christoph.network.SubNetwork;
import playground.christoph.replanning.MyStrategyManagerConfigLoader;
import playground.christoph.router.CompassRoute;
import playground.christoph.router.DijkstraWrapper;
import playground.christoph.router.KnowledgePlansCalcRoute;
import playground.christoph.router.RandomCompassRoute;
import playground.christoph.router.RandomRoute;
import playground.christoph.router.TabuRoute;
import playground.christoph.router.costcalculators.KnowledgeTravelCostWrapper;
import playground.christoph.router.costcalculators.KnowledgeTravelTimeCalculator;
import playground.christoph.router.costcalculators.KnowledgeTravelTimeWrapper;
import playground.christoph.router.costcalculators.OnlyTimeDependentTravelCostCalculator;
import playground.christoph.router.util.DijkstraWrapperFactory;
import playground.christoph.router.util.SimpleRouterFactory;
import playground.christoph.scoring.OnlyTimeDependentScoringFunctionFactory;

/**
 * The Controler is responsible for complete simulation runs, including the
 * initialization of all required data, running the iterations and the
 * replanning, analyses, etc.
 *
 * @author Christoph Dobler
 */


/*
 * Example for the new entries in a config.xml file to read / write selected
 * nodes from / to files.
 * <module name="selection">
 * 		<param name="readSelection" value="true"/>
 * 		<param name="inputSelectionFile" value="mysimulations/berlin/selection.xml.gz" />
 * 		<param name="writeSelection" value="true"/>
 * 		<param name="outputSelectionFile" value="./output/berlin/selection.xml.gz" />
 * 		<param name="dtdFile" value="./src/playground/christoph/knowledge/nodeselection/Selection.dtd" />
 * </module>
 */
public class EventControler extends Controler {

	protected ReplanningQueueSimulation sim;

	protected ArrayList<PlanAlgorithm> replanners;
	protected ArrayList<SelectNodes> nodeSelectors;

	protected ActTimesCollector actTimesCollector;
	protected boolean calcWardrop = false;

	// for Batch Runs
	public double pNoReplanning = 1.0;
	public double pInitialReplanning = 0.0;
	public double pActEndReplanning = 0.0;
	public double pLeaveLinkReplanning = 0.0;

	protected int noReplanningCounter = 0;
	protected int initialReplanningCounter = 0;
	protected int actEndReplanningCounter = 0;
	protected int leaveLinkReplanningCounter = 0;

	protected int numReplanningThreads = 2;

	protected KnowledgeTravelTimeCalculator knowledgeTravelTime;

	protected ParallelInitialReplanner parallelInitialReplanner;
	protected ParallelActEndReplanner parallelActEndReplanner;
	protected ParallelLeaveLinkReplanner parallelLeaveLinkReplanner;
	protected LinkVehiclesCounter2 linkVehiclesCounter;
	protected LinkReplanningMap linkReplanningMap;
	protected LookupTableUpdater lookupTableUpdater;
	protected ReplanningManager replanningManager;

	private static final Logger log = Logger.getLogger(EventControler.class);

	/**
	 * Initializes a new instance of Controler with the given arguments.
	 *
	 * @param args
	 *            The arguments to initialize the controler with.
	 *            <code>args[0]</code> is expected to contain the path to a
	 *            configuration file, <code>args[1]</code>, if set, is expected
	 *            to contain the path to a local copy of the DTD file used in
	 *            the configuration file.
	 */
	public EventControler(String[] args) {
		super(args);

		setConstructorParameters();
	}

	// only for Batch Runs
	public EventControler(Config config) {
		super(config);

		setConstructorParameters();
	}

	private void setConstructorParameters()
	{
		// Use MyLinkImpl. They can carry some additional Information like their
		// TravelTime or VehicleCount.
		this.getNetwork().getFactory().setLinkFactory(new MyLinkFactoryImpl());

		// Use a Scoring Function, that only scores the travel times!
		this.setScoringFunctionFactory(new OnlyTimeDependentScoringFunctionFactory());

		// Use knowledge when Re-Routing
		OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(this.getTravelTimeCalculator());
		KnowledgeTravelCostWrapper travelCostWrapper = new KnowledgeTravelCostWrapper(travelCost);
		travelCostWrapper.checkNodeKnowledge(true);

		this.setTravelCostCalculator(travelCostWrapper);
//		this.setTravelCostCalculator(travelCost);
	}

	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalised Router.
	 */
	protected void initReplanningRouter() {
		replanners = new ArrayList<PlanAlgorithm>();

		KnowledgeTravelTimeCalculator travelTimeCalculator = new KnowledgeTravelTimeCalculator(sim.getQueueNetwork());
		TravelTimeDistanceCostCalculator travelCostCalculator = new TravelTimeDistanceCostCalculator(travelTimeCalculator, new CharyparNagelScoringConfigGroup());

		// AStarLandmarks
		// PreProcessLandmarks landmarks = new PreProcessLandmarks(new
		// FreespeedTravelTimeCost(new CharyparNagelScoringConfigGroup()));
		// landmarks.run(network);
		AStarLandmarksFactory factory = new AStarLandmarksFactory(network, new FreespeedTravelTimeCost(new CharyparNagelScoringConfigGroup()));
		replanners.add(new PlansCalcRoute(new PlansCalcRouteConfigGroup(), network, travelCostCalculator, travelTimeCalculator, factory));

		// BasicReplanners (Random, Tabu, Compass, ...)
		// each replanner can handle an arbitrary number of persons
		KnowledgePlansCalcRoute randomRouter = new KnowledgePlansCalcRoute(new PlansCalcRouteConfigGroup(), network, null, null, new SimpleRouterFactory(new RandomRoute(this.network)));
		replanners.add(randomRouter);

		KnowledgePlansCalcRoute tabuRouter = new KnowledgePlansCalcRoute(new PlansCalcRouteConfigGroup(), network, null, null, new SimpleRouterFactory(new TabuRoute(this.network)));
		replanners.add(tabuRouter);

		KnowledgePlansCalcRoute compassRouter = new KnowledgePlansCalcRoute(new PlansCalcRouteConfigGroup(), network, null, null, new SimpleRouterFactory(new CompassRoute(this.network)));
		replanners.add(compassRouter);

		KnowledgePlansCalcRoute randomCompassRouter = new KnowledgePlansCalcRoute(new PlansCalcRouteConfigGroup(), network, null, null, new SimpleRouterFactory(new RandomCompassRoute(this.network)));
		replanners.add(randomCompassRouter);

		// Dijkstra for Replanning
		// KnowledgeTravelTimeCalculator travelTime = new KnowledgeTravelTimeCalculator();
		// KnowledgeTravelCostCalculator travelCost = new KnowledgeTravelCostCalculator(travelTime);

		// Use a Wrapper - by doing this, already available MATSim
		// CostCalculators can be used
		// TravelTimeDistanceCostCalculator travelCost = new TravelTimeDistanceCostCalculator(travelTime);
		// KnowledgeTravelCostWrapper travelCostWrapper = new KnowledgeTravelCostWrapper(travelCost);

		// Use a Wrapper - by doing this, already available MATSim
		// CostCalculators can be used
		KnowledgeTravelTimeCalculator travelTime = new KnowledgeTravelTimeCalculator(sim.getQueueNetwork());
		KnowledgeTravelTimeWrapper travelTimeWrapper = new KnowledgeTravelTimeWrapper(travelTime);

		OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(travelTimeWrapper);
		KnowledgeTravelCostWrapper travelCostWrapper = new KnowledgeTravelCostWrapper(travelCost);

		travelTimeWrapper.checkNodeKnowledge(false);
		travelCostWrapper.checkNodeKnowledge(false);

		// Don't use Knowledge for CostCalculations
//		Dijkstra dijkstra = new MyDijkstra(network, travelCostWrapper, travelTimeWrapper);
//		Dijkstra dijkstra = new Dijkstra(network, travelCostWrapper, travelTimeWrapper);
//		DijkstraWrapper dijkstraWrapper = new DijkstraWrapper(dijkstra, travelCostWrapper, travelTimeWrapper, network);

/*
		// Don't use Wrappers with LookupTables KnowledgeTravelTimeCalculator
		travelTime = new KnowledgeTravelTimeCalculator();
		OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(travelTime);

		// Dijkstra
		dijkstra = new Dijkstra(network, travelCost, travelTime);
		// DijkstraWrapper dijkstraWrapper = new DijkstraWrapper(dijkstra, travelCost, travelTime);

		KnowledgeTravelCostWrapper travelCostWrapper = new KnowledgeTravelCostWrapper(travelCost);
		travelCostWrapper.checkNodeKnowledge(true);
		travelCostWrapper.useLookupTable(false); Dijkstra dijkstra = new Dijkstra(network, travelCostWrapper, travelTime);
		DijkstraWrapper dijkstraWrapper = new DijkstraWrapper(dijkstra, travelCostWrapper, travelTime);
*/
		KnowledgePlansCalcRoute dijkstraRouter = new KnowledgePlansCalcRoute(new PlansCalcRouteConfigGroup(), network,
				travelCostWrapper, travelTimeWrapper, new DijkstraWrapperFactory());

		replanners.add(dijkstraRouter);
	}

	public ArrayList<PlanAlgorithm> getReplanningRouters() {
		return replanners;
	}

	/*
	 * Hands over the ArrayList to the ParallelReplanner
	 */
	protected void initParallelReplanningModules()
	{
		this.parallelInitialReplanner = new ParallelInitialReplanner();
		this.parallelInitialReplanner.setNumberOfThreads(numReplanningThreads);
		this.parallelInitialReplanner.setReplannerArrayList(replanners);
		this.parallelInitialReplanner.createReplannerArray();
		this.parallelInitialReplanner.init();

		this.parallelActEndReplanner = new ParallelActEndReplanner();
		this.parallelActEndReplanner.setNumberOfThreads(numReplanningThreads);
		this.parallelActEndReplanner.setReplannerArrayList(replanners);
		this.parallelActEndReplanner.setReplannerArray(parallelInitialReplanner.getReplannerArray());
		this.parallelActEndReplanner.init();

		this.parallelLeaveLinkReplanner = new ParallelLeaveLinkReplanner(network);
		this.parallelLeaveLinkReplanner.setNumberOfThreads(numReplanningThreads);
		this.parallelLeaveLinkReplanner.setReplannerArrayList(replanners);
		this.parallelLeaveLinkReplanner.setReplannerArray(parallelInitialReplanner.getReplannerArray());
		this.parallelLeaveLinkReplanner.init();
	}

	/*
	 * Initializes the NodeSeletors that are used to create the Activity Spaces
	 * of the Persons of a Population.
	 */
	protected void initNodeSelectors() {
		nodeSelectors = new ArrayList<SelectNodes>();

		SelectNodesCircular snc = new SelectNodesCircular(this.network);
		snc.setDistance(5000);
		nodeSelectors.add(snc);

		SelectNodesDijkstra selectNodesDijkstra = new SelectNodesDijkstra(this.network);
		selectNodesDijkstra.setCostCalculator(new OnlyTimeDependentTravelCostCalculator(null));
		// selectNodesDijkstra.setCostCalculator(new OnlyDistanceDependentTravelCostCalculator(null));

		selectNodesDijkstra.setCostFactor(1.50);
		nodeSelectors.add(selectNodesDijkstra);
	}

	/*
	 * Creates the Handler and Listener Object so that
	 * they can be handed over to the Within Day
	 * Replanning Modules (TravelCostWrapper, etc).
	 *
	 * The full initialization of them is done later
	 * (we don't have all necessary Objects yet).
	 */
	protected void createHandlersAndListeners()
	{
		linkVehiclesCounter = new LinkVehiclesCounter2();
		linkReplanningMap = new LinkReplanningMap();
		lookupTableUpdater = new LookupTableUpdater();
		replanningManager = new ReplanningManager();
	}

	@Override
	protected void runMobSim()
	{
		sim = new ReplanningQueueSimulation(this.network, this.population, this.events);

		sim.useKnowledgeStorageHandler(true);

		createHandlersAndListeners();

		// fully initialize & add LinkVehiclesCounter
		linkVehiclesCounter.setQueueNetwork(sim.getQueueNetwork());

		/*
		 * Use a FixedOrderQueueSimulationListener to bundle the Listeners and
		 * ensure that they are started in the needed order.
		 */
		FixedOrderQueueSimulationListener foqsl = new FixedOrderQueueSimulationListener();

		foqsl.addQueueSimulationInitializedListener(linkVehiclesCounter);
		foqsl.addQueueSimulationAfterSimStepListener(linkVehiclesCounter);

//		foqsl.addQueueSimulationInitializedListener(lookupTableUpdater);
//		foqsl.addQueueSimulationAfterSimStepListener(lookupTableUpdater);

		foqsl.addQueueSimulationBeforeSimStepListener(replanningManager);

		sim.addQueueSimulationListeners(foqsl);

//		sim.addQueueSimulationListeners(lookupTableUpdater);
//		sim.addQueueSimulationListeners(linkVehiclesCounter);

		this.events.addHandler(linkVehiclesCounter);

		// fully initialize & add LinkReplanningMap
		linkReplanningMap.setQueueNetwork(sim.getQueueNetwork());
		this.events.addHandler(linkReplanningMap);

		// set QueueNetwork in the Traveltime Calculator
//		if (knowledgeTravelTime != null) knowledgeTravelTime.setQueueNetwork(sim.getQueueNetwork());

//		log.info("Remove not selected Plans");
//		clearPlans();

		// log.info("Read known Nodes Maps from a File");
		// readKnownNodesMap();

		log.info("Initialize Replanning Routers");
		initReplanningRouter();

		log.info("Initialize Parallel Replanning Modules");
		initParallelReplanningModules();

		log.info("Set Replanning flags");
		setReplanningFlags();

		log.info("Set Replanners for each Person");
		setReplanners();

		log.info("Initialize Node Selectors");
		initNodeSelectors();

		log.info("Set Node Selectors");
		setNodeSelectors();

		log.info("Initialize Knowledge Data Handler");
		initKnowledgeStorageHandler();

		log.info("Set Knowledge Data Handler");
		setKnowledgeStorageHandler();

		log.info("Set SubNetworks");
		setSubNetworks();

		/*
		 * Could be done before or after the creation of the activity rooms -
		 * depending on the intention of the simulation.
		 *
		 * If done before, the new created Route is the base for the activity
		 * rooms.
		 *
		 * If done afterwards, existing routes are the base for the activity
		 * rooms and the replanners have to act within the borders of the
		 * already defined rooms. The existing plans could for example be the
		 * results of a relaxed solution of a standard MATSim simulation.
		 */
		log.info("do initial Replanning");
		doInitialReplanning();

//		lookupTableUpdater.addLookupTable(LookupTable lookupTable);

		ActEndReplanningModule actEndReplanning = new ActEndReplanningModule(parallelActEndReplanner, sim);
		LeaveLinkReplanningModule leaveLinkReplanning = new LeaveLinkReplanningModule(parallelLeaveLinkReplanner, linkReplanningMap);

		replanningManager.setActEndReplanningModule(actEndReplanning);
		replanningManager.setLeaveLinkReplanningModule(leaveLinkReplanning);

//		replanningManager.doActEndReplanning(false);
//		replanningManager.doLeaveLinkReplanning(false);

		sim.run();
	}

	/*
	 * Add three boolean variables to each Person. They are used to indicate, if
	 * the plans of this person should be replanned each time if an activity
	 * ends, each time a link is left, before the simulation starts or never
	 * during an iteration.
	 *
	 * I don't like this way but, it is the best way I know at the moment... In
	 * my opinion these variables should be part of the PersonAgents within the
	 * QueueSimulation - but they can't be accessed from an EventHandler.
	 */
	protected void setReplanningFlags() {
		noReplanningCounter = 0;
		initialReplanningCounter = 0;
		actEndReplanningCounter = 0;
		leaveLinkReplanningCounter = 0;

		for (Person person : this.getPopulation().getPersons().values()) {
			// get Person's Custom Attributes
			Map<String, Object> customAttributes = person.getCustomAttributes();

			double probability = MatsimRandom.getRandom().nextDouble();

			// No Replanning
			if (probability <= pNoReplanning)
			{
				noReplanningCounter++;
				customAttributes.put("initialReplanning", new Boolean(false));
				customAttributes.put("leaveLinkReplanning", new Boolean(false));
				customAttributes.put("endActivityReplanning", new Boolean(false));
			}

			// Initial Replanning
			else if ((probability > pNoReplanning)
					&& (probability <= pNoReplanning + pInitialReplanning))
			{
				initialReplanningCounter++;
				customAttributes.put("initialReplanning", new Boolean(true));
				customAttributes.put("leaveLinkReplanning", new Boolean(false));
				customAttributes.put("endActivityReplanning", new Boolean(false));
			}

			// Act End Replanning
			else if ((probability > pNoReplanning + pInitialReplanning)
					&& (probability <= pNoReplanning + pInitialReplanning + pActEndReplanning))
			{
				actEndReplanningCounter++;
				customAttributes.put("initialReplanning", new Boolean(false));
				customAttributes.put("leaveLinkReplanning", new Boolean(false));
				customAttributes.put("endActivityReplanning", new Boolean(true));
			}

			// Leave Link Replanning
			else
			{
				leaveLinkReplanningCounter++;
				customAttributes.put("initialReplanning", new Boolean(false));
				customAttributes.put("leaveLinkReplanning", new Boolean(true));
				customAttributes.put("endActivityReplanning", new Boolean(false));
			}

			// (de)activate replanning
			if (actEndReplanningCounter == 0) replanningManager.doActEndReplanning(false);
			else replanningManager.doActEndReplanning(true);

			if (leaveLinkReplanningCounter == 0) replanningManager.doLeaveLinkReplanning(false);
			else replanningManager.doLeaveLinkReplanning(true);

		}

		log.info("No Replanning Probability: " + pNoReplanning);
		log.info("Initial Replanning Probability: " + pInitialReplanning);
		log.info("Act End Replanning Probability: " + pActEndReplanning);
		log.info("Leave Link Replanning Probability: " + pLeaveLinkReplanning);

		double numPersons = this.population.getPersons().size();
		log.info(noReplanningCounter + " persons don't replan their Plans ("+ noReplanningCounter / numPersons * 100.0 + "%)");
		log.info(initialReplanningCounter + " persons replan their plans initially (" + initialReplanningCounter / numPersons * 100.0 + "%)");
		log.info(actEndReplanningCounter + " persons replan their plans after an activity (" + actEndReplanningCounter / numPersons * 100.0 + "%)");
		log.info(leaveLinkReplanningCounter + " persons replan their plans at each node (" + leaveLinkReplanningCounter / numPersons * 100.0 + "%)");
		/*
		 * int counter = 0;
		 *
		 * Iterator<Person> PersonIterator =
		 * this.getPopulation().getPersons().values().iterator(); while
		 * (PersonIterator.hasNext()) { Person p = PersonIterator.next();
		 *
		 * // count persons - is decreased again, if a person is replanning
		 * noReplanningCounter++;
		 *
		 * counter++; if(counter < 1000000) { Map<String,Object>
		 * customAttributes = p.getCustomAttributes();
		 * customAttributes.put("initialReplanning", new Boolean(true));
		 * customAttributes.put("leaveLinkReplanning", new Boolean(true));
		 * customAttributes.put("endActivityReplanning", new Boolean(true));
		 *
		 * // (de)activate replanning
		 * MyQueueNetwork.doLeaveLinkReplanning(true);
		 * MyQueueNetwork.doActEndReplanning(true); } else { Map<String,Object>
		 * customAttributes = p.getCustomAttributes();
		 * customAttributes.put("initialReplanning", new Boolean(false));
		 * customAttributes.put("leaveLinkReplanning", new Boolean(false));
		 * customAttributes.put("endActivityReplanning", new Boolean(false));
		 *
		 * // deactivate replanning MyQueueNetwork.doLeaveLinkReplanning(false);
		 * MyQueueNetwork.doActEndReplanning(false); } }
		 */
	}

	/*
	 * Assigns a replanner to every Person of the population. Same problem as
	 * above: should be part of the PersonAgents, but only Persons are available
	 * in the replanning modules.
	 *
	 * At the moment: Replanning Modules are assigned hard coded. Later: Modules
	 * are assigned based on probabilities from config files.
	 */
	protected void setReplanners() {
		for (Person p : this.getPopulation().getPersons().values()) {
			Map<String, Object> customAttributes = p.getCustomAttributes();
			// customAttributes.put("Replanner", replanners.get(0)); // A*
			// customAttributes.put("Replanner", replanners.get(1)); // Random
			// customAttributes.put("Replanner", replanners.get(2)); // Tabu
			// customAttributes.put("Replanner", replanners.get(3)); // Compass
			// customAttributes.put("Replanner", replanners.get(4)); // RandomCompass
			customAttributes.put("Replanner", replanners.get(5)); // DijstraWrapper
			// customAttributes.put("Replanner", replanners.get(6)); // Dijstra
		}
	}

	/*
	 * Assigns nodeSelectors to every Person of the population, which are used
	 * to create an activity rooms for every Person. It is possible to assign
	 * more than one Selector to each Person. If non is selected the Person
	 * knows every Node of the network.
	 *
	 * At the moment: Selection Modules are assigned hard coded. Later: Modules
	 * are assigned based on probabilities from config files.
	 *
	 * If no NodeSelectors is added (the ArrayList is initialized but empty) the
	 * person knows the entire Network (KnowledgeTools.knowsLink(...) always
	 * returns true).
	 */
	protected void setNodeSelectors() {
		// Create NodeSelectorContainer
		for (Person p : this.getPopulation().getPersons().values()) {
			Map<String, Object> customAttributes = p.getCustomAttributes();

			ArrayList<SelectNodes> personNodeSelectors = new ArrayList<SelectNodes>();

			customAttributes.put("NodeSelectors", personNodeSelectors);
		}

		// Assign NodeSelectors
		int counter = 0;
		for (Person p : this.getPopulation().getPersons().values()) {
			counter++;
			Map<String, Object> customAttributes = p.getCustomAttributes();

			ArrayList<SelectNodes> personNodeSelectors = (ArrayList<SelectNodes>) customAttributes.get("NodeSelectors");

			// personNodeSelectors.add(nodeSelectors.get(0)); // Circular
			// NodeSelector
			personNodeSelectors.add(nodeSelectors.get(1)); // Dijkstra
															// NodeSelector

			// if (counter >= 1000) break;
		}
	}

	/*
	 * Read Maps of Nodes that each Agents "knows" from a file that is specified
	 * in config.xml.
	 */
	protected void readKnownNodesMap() {
		// reading Selection from file
		boolean readSelection = Boolean.valueOf(this.config.getModule("selection").getValue("readSelection"));
		if (readSelection) {
			String path = this.config.getModule("selection").getValue("inputSelectionFile");
			log.info("Path: " + path);

			// reading single File
			new SelectionReaderMatsim(this.network, this.population,((this.getScenario()).getKnowledges())).readFile(path);

			// reading multiple Files automatically
			// new SelectionReaderMatsim(this.network,
			// this.population).readMultiFile(path);

			log.info("Read input selection file!");
		}
	}

	/*
	 * Write Maps of Nodes that each Agents "knows" to a file that is specified
	 * in config.xml.
	 */
	protected void writeKownNodesMap() {
		// writing Selection to file
		boolean writeSelection = Boolean.valueOf(this.config.getModule("selection").getValue("writeSelection"));
		if (writeSelection) {
			String outPutFile = this.config.getModule("selection").getValue("outputSelectionFile");
			String dtdFile = "./src/playground/christoph/knowledge/nodeselection/Selection.dtd";

			// write single File
			new SelectionWriter(this.population, dtdFile, "1.0","dummy").writeFile(outPutFile);

			// write multiple Files automatically
			// new SelectionWriter(this.population, outPutFile, dtdFile, "1.0",
			// "dummy").write(10000);

			// new SelectionWriter(this.population,
			// getOutputFilename("selection.xml.gz"), "1.0", "dummy").write();

			log.info("Path: " + outPutFile);
		}
	}

	protected void initKnowledgeStorageHandler() {
		MapKnowledgeDB mapKnowledgeDB = new MapKnowledgeDB();
		mapKnowledgeDB.createTable();
		// mapKnowledgeDB.clearTable();
	}

	protected void setKnowledgeStorageHandler() {
		for (Person person : this.getPopulation().getPersons().values()) {
			Map<String, Object> customAttributes = person.getCustomAttributes();
			/*
			 * CellNetworkMapping cellNetworkMapping = new CellNetworkMapping(network); cellNetworkMapping.createMapping();
			 *
			 * NodeKnowledge nodeKnowledge = new CellKnowledge(cellNetworkMapping);
			 */
			customAttributes.put("NodeKnowledgeStorageType", MapKnowledgeDB.class.getName());

			MapKnowledgeDB mapKnowledgeDB = new MapKnowledgeDB();
			mapKnowledgeDB.setPerson(person);
			mapKnowledgeDB.setNetwork(network);

			customAttributes.put("NodeKnowledge", mapKnowledgeDB);
		}
	}

	protected void setSubNetworks() {
		for (Person person : this.getPopulation().getPersons().values()) {
			Map<String, Object> customAttributes = person.getCustomAttributes();

			customAttributes.put("SubNetwork", new SubNetwork(this.network));
		}

		/*
		 * SubNetworkCreator snc = new SubNetworkCreator(this.network);
		 * KnowledgeTools kt = new KnowledgeTools();
		 *
		 * int i = 0; for(PersonImpl person :
		 * this.getPopulation().getPersons().values()) { Map<String, Object>
		 * customAttributes = person.getCustomAttributes();
		 *
		 * NodeKnowledge nk = kt.getNodeKnowledge(person);
		 *
		 * ((MapKnowledgeDB)nk).readFromDB();
		 *
		 * SubNetwork subNetwork = snc.createSubNetwork(nk);
		 *
		 * ((MapKnowledgeDB)nk).clearLocalKnowledge();
		 *
		 * // log.info("Nodes: " + subNetwork.getNodes().size() + ", Links: " +
		 * subNetwork.getLinks().size());
		 *
		 * customAttributes.put("SubNetwork", subNetwork); i++; if (i % 1000 ==
		 * 0) log.info("Subnetworks: " + i); //
		 * customAttributes.put("SubNetwork", new SubNetwork(this.network)); }
		 */
	}

	/*
	 * Creates the Maps of Nodes that each Agents "knows".
	 */
	protected void createKnownNodes() {
		// create Known Nodes Maps on multiple Threads
		ParallelCreateKnownNodesMap.run(this.population, this.network, nodeSelectors, 4);

		// writePersonKML(this.population.getPerson("100139"));

		// non multi-core calculation
		// CreateKnownNodesMap.collectAllSelectedNodes(this.population,
		// this.network);

	} // setNodes()

	protected void doInitialReplanning() {
		ArrayList<Person> personsToReplan = new ArrayList<Person>();

		for (Person person : this.getPopulation().getPersons().values()) {
			boolean replanning = (Boolean) person.getCustomAttributes().get("initialReplanning");

			if (replanning) {
				personsToReplan.add(person);
			}
		}

		double time = 0.0;
		// Remove Knowledge after replanning to save memory.
		this.parallelInitialReplanner.setRemoveKnowledge(true);

		// Run Replanner.
		this.parallelInitialReplanner.run(personsToReplan, time);

		// Number of Routes that could not be created...
		log.info(RandomRoute.getErrorCounter() + " Routes could not be created by RandomRoute.");
		log.info(TabuRoute.getErrorCounter() + " Routes could not be created by TabuRoute.");
		log.info(CompassRoute.getErrorCounter() + " Routes could not be created by CompassRoute.");
		log.info(RandomCompassRoute.getErrorCounter() + " Routes could not be created by RandomCompassRoute.");
		log.info(DijkstraWrapper.getErrorCounter() + " Routes could not be created by DijkstraWrapper.");

		/*
		 * for (Person person : this.getPopulation().getPersons().values()) {
		 * boolean replanning =
		 * (Boolean)person.getCustomAttributes().get("initialReplanning");
		 *
		 * if (replanning) { KnowledgePlansCalcRoute replanner =
		 * (KnowledgePlansCalcRoute)replanners.get(1);
		 * replanner.setPerson(person); replanner.run(person.getSelectedPlan());
		 * } }
		 */
	} // doInitialReplanning

	// removes all plans, that are currently not selectedS
	protected void clearPlans() {
		for (Person person : this.getPopulation().getPersons().values()) {
			((PersonImpl) person).removeUnselectedPlans();
		}
	}

	protected void writePersonKML(PersonImpl person) {
		KMLPersonWriter test = new KMLPersonWriter(network, person);

		// set CoordinateTransformation
		String coordSys = this.config.global().getCoordinateSystem();
		if (coordSys.equalsIgnoreCase("GK4")) test.setCoordinateTransformation(new GK4toWGS84());
		if (coordSys.equalsIgnoreCase("Atlantis")) test.setCoordinateTransformation(new AtlantisToWGS84());
		if (coordSys.equalsIgnoreCase("CH1903_LV03")) test.setCoordinateTransformation(new CH1903LV03toWGS84());

		// waiting for an implementation...
		// new WGS84toCH1903LV03();

		String outputDirectory = this.config.controler().getOutputDirectory();
		test.setOutputDirectory(outputDirectory);

		test.writeFile();
	}

	@Override
	protected void setUp() {
		super.setUp();

		if (calcWardrop) {
			actTimesCollector = new ActTimesCollector();

			actTimesCollector.setNetwork(this.network);
			actTimesCollector.setPopulation(this.population);

			// actTimesCollector.setStartTime(startTime);
			// actTimesCollector.setEndTime(endTime);

			events.addHandler(actTimesCollector);
		}

	}

	/**
	 * @return A fully initialized StrategyManager for the plans replanning.
	 */
	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		MyStrategyManagerConfigLoader.load(this, this.config, manager);
		return manager;
	}

	@Override
	protected void shutdown(final boolean unexpected) {
		if (calcWardrop) {
			log.info("");
			Wardrop wardrop = new Wardrop(network, population);

			// wardrop.setActTimesCollector(this.actTimesCollector);
			/*
			 * // with length Correction wardrop.setUseLengthCorrection(true);
			 * wardrop
			 * .fillMatrixViaActTimesCollectorObject(this.actTimesCollector);
			 * wardrop.getResults(); log.info("");
			 */
			// without length Correction
			wardrop.setUseLengthCorrection(false);
			wardrop.fillMatrixViaActTimesCollectorObject(this.actTimesCollector);
			wardrop.getResults();
			log.info("");
		}

		super.shutdown(unexpected);
	}

	public void setConfig(Config config) {
		this.config.config();
	}

	/*
	 * =================================================================== main
	 * ===================================================================
	 */

	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final EventControler controler = new EventControler(args);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}

}
