/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.controler;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.parallelEventsHandler.SimStepParallelEventsManagerImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.PersonalizableTravelTimeFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.OnlyTimeDependentScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTimeCalculator;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.households.Household;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.ptproject.qsim.multimodalsimengine.router.util.MultiModalTravelTime;
import org.matsim.ptproject.qsim.multimodalsimengine.router.util.MultiModalTravelTimeWrapperFactory;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.withinday.controller.WithinDayController;
import org.matsim.withinday.replanning.identifiers.ActivityPerformingIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.LegPerformingIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.modules.ReplanningModule;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;

import playground.christoph.evacuation.analysis.AgentsInEvacuationAreaCounter;
import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.analysis.EvacuationTimePicture;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.config.EvacuationConfigReader;
import playground.christoph.evacuation.mobsim.AgentsTracker;
import playground.christoph.evacuation.mobsim.EvacuationQSimFactory;
import playground.christoph.evacuation.mobsim.HouseholdsTracker;
import playground.christoph.evacuation.mobsim.LegModeChecker;
import playground.christoph.evacuation.mobsim.PassengerDepartureHandler;
import playground.christoph.evacuation.mobsim.VehiclesTracker;
import playground.christoph.evacuation.network.AddExitLinksToNetwork;
import playground.christoph.evacuation.network.AddZCoordinatesToNetwork;
import playground.christoph.evacuation.router.util.FuzzyTravelTimeDataCollector;
import playground.christoph.evacuation.router.util.FuzzyTravelTimeEstimatorFactory;
import playground.christoph.evacuation.trafficmonitoring.BikeTravelTimeFactory;
import playground.christoph.evacuation.trafficmonitoring.WalkTravelTimeFactory;
import playground.christoph.evacuation.vehicles.AssignVehiclesToPlans;
import playground.christoph.evacuation.vehicles.CreateVehiclesForHouseholds;
import playground.christoph.evacuation.vehicles.HouseholdVehicleAssignmentReader;
import playground.christoph.evacuation.withinday.replanning.identifiers.AgentsToPickupIdentifier;
import playground.christoph.evacuation.withinday.replanning.identifiers.AgentsToPickupIdentifierFactory;
import playground.christoph.evacuation.withinday.replanning.identifiers.JoinedHouseholdsIdentifier;
import playground.christoph.evacuation.withinday.replanning.identifiers.JoinedHouseholdsIdentifierFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.CurrentActivityToMeetingPointReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.CurrentLegToMeetingPointReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.JoinedHouseholdsReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.PickupAgentReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.utils.HouseholdsUtils;
import playground.christoph.evacuation.withinday.replanning.utils.ModeAvailabilityChecker;
import playground.christoph.evacuation.withinday.replanning.utils.SHPFileUtil;
import playground.christoph.evacuation.withinday.replanning.utils.SelectHouseholdMeetingPoint;

import com.vividsolutions.jts.geom.Geometry;

public class EvacuationControler extends WithinDayController implements SimulationInitializedListener, 
		IterationStartsListener, StartupListener, AfterMobsimListener {

	public static final String FILENAME_VEHICLES = "output_vehicles.xml.gz";
	
	protected boolean adaptOriginalPlans = false;

	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	protected int numReplanningThreads;

	/*
	 * Identifiers
	 */
	protected DuringActivityIdentifier joinedHouseholdsIdentifier;
	protected DuringActivityIdentifier activityPerformingIdentifier;
	protected DuringLegIdentifier legPerformingIdentifier;
	protected DuringLegIdentifier agentsToPickupIdentifier;
	protected DuringLegIdentifier duringLegRerouteIdentifier;
	
	/*
	 * Replanners
	 */
	protected WithinDayDuringActivityReplanner currentActivityToMeetingPointReplanner;
	protected WithinDayDuringActivityReplanner joinedHouseholdsReplanner;
	protected WithinDayDuringLegReplanner currentLegToMeetingPointReplanner;
	protected WithinDayDuringLegReplanner pickupAgentsReplanner;
	protected WithinDayDuringLegReplanner duringLegRerouteReplanner;
	
	protected double duringLegRerouteShare = 0.10;
	
	protected AddZCoordinatesToNetwork zCoordinateAdder;
	protected HouseholdsUtils householdsUtils;
	protected HouseholdVehicleAssignmentReader householdVehicleAssignmentReader;
//	protected HouseholdVehiclesTracker householdVehiclesTracker;
	
	protected CreateVehiclesForHouseholds createVehiclesForHouseholds;
	protected AssignVehiclesToPlans assignVehiclesToPlans;
	
	protected SelectHouseholdMeetingPoint selectHouseholdMeetingPoint;
	protected ModeAvailabilityChecker modeAvailabilityChecker;
	protected PassengerDepartureHandler passengerDepartureHandler;
	protected HouseholdsTracker househouldsTracker;
	protected VehiclesTracker vehiclesTracker;
	protected CoordAnalyzer coordAnalyzer;
	protected Geometry affectedArea;

	protected PersonalizableTravelTimeFactory walkTravelTimeFactory;
	protected PersonalizableTravelTimeFactory bikeTravelTimeFactory;
	
	/*
	 * Analysis modules
	 */
	protected EvacuationTimePicture evacuationTimePicture;
	protected AgentsInEvacuationAreaCounter agentsInEvacuationAreaCounter;
	
	static final Logger log = Logger.getLogger(EvacuationControler.class);

	public EvacuationControler(String[] args) {
		super(args);
		
		new EvacuationConfigReader().readFile(args[1]);
		
		// Use a Scoring Function, that only scores the travel times!
		this.setScoringFunctionFactory(new OnlyTimeDependentScoringFunctionFactory());
		
		// register this as a Controller and Simulation Listener
		super.getFixedOrderSimulationListener().addSimulationListener(this);
		super.addControlerListener(this);
	}

	/*
	 * When the Controller Startup Event is created, the EventsManager
	 * has already been initialized. Therefore we can initialize now
	 * all Objects, that have to be registered at the EventsManager.
	 */
	@Override
	public void notifyStartup(StartupEvent event) {
		
		/*
		 * get number of threads from config file
		 */
		this.numReplanningThreads = this.config.global().getNumberOfThreads();
		
//		PlansLinkReferenceDumping planAlgo = new PlansLinkReferenceDumping();
//		planAlgo.run(population);
		
		/*
		 * Using a LegModeChecker to ensure that all agents' plans have valid mode chains.
		 */
		LegModeChecker legModeChecker = new LegModeChecker(this.createRoutingAlgorithm());
		legModeChecker.setValidNonCarModes(new String[]{TransportMode.walk, TransportMode.bike, TransportMode.pt});
		legModeChecker.setToCarProbability(0.5);
		legModeChecker.run(this.scenarioData.getPopulation());
		legModeChecker.printStatistics();
		
		/*
		 * If a SimStepParallelEventsManagerImpl is used, we ensure that it it
		 * processed as very first SimulationListener. Doing so ensures that all
		 * events of a time step have been processed before the other
		 * SimulationAfterSimStepListeners are informed. 
		 */
		if (this.getEvents() instanceof SimStepParallelEventsManagerImpl) {
			this.getQueueSimulationListener().remove(this.getEvents());
			this.getFixedOrderSimulationListener().addSimulationListener((SimStepParallelEventsManagerImpl) this.getEvents());
		}
		
		new WorldConnectLocations(this.config).connectFacilitiesWithLinks(getFacilities(), (NetworkImpl) getNetwork());

		// Add Rescue Links to Network
		new AddExitLinksToNetwork(this.scenarioData).createExitLinks();

		// Add secure Facilities to secure Links.
//		new AddSecureFacilitiesToNetwork(this.scenarioData).createSecureFacilities();
		
		// Add pickup facilities to Links.
		addPickupFacilities();
		
		/*
		 * Adding z-coordinates to the network
		 */
		zCoordinateAdder = new AddZCoordinatesToNetwork(this.scenarioData, EvacuationConfig.dhm25File, EvacuationConfig.srtmFile);
		zCoordinateAdder.addZCoordinatesToNetwork();
		zCoordinateAdder.checkSteepness();

		/*
		 * Use advanced walk- and bike travel time calculators
		 */
		this.walkTravelTimeFactory = new WalkTravelTimeFactory(this.config.plansCalcRoute());
		this.bikeTravelTimeFactory = new BikeTravelTimeFactory(this.config.plansCalcRoute());
		this.getMultiModalTravelTimeWrapperFactory().setPersonalizableTravelTimeFactory(TransportMode.walk, walkTravelTimeFactory);
		this.getMultiModalTravelTimeWrapperFactory().setPersonalizableTravelTimeFactory(TransportMode.bike, bikeTravelTimeFactory);
		
		Set<String> analyzedModes = new HashSet<String>();
		analyzedModes.add(TransportMode.car);
		super.createAndInitTravelTimeCollector(analyzedModes);
		
		super.createAndInitReplanningManager(numReplanningThreads);
		super.createAndInitActivityReplanningMap();
		MultiModalTravelTime linkReplanningTravelTime = this.createLinkReplanningMapTravelTime();
		super.createAndInitLinkReplanningMap(linkReplanningTravelTime);
		
		this.householdsUtils = new HouseholdsUtils(this.scenarioData, this.getEvents());
		this.getEvents().addHandler(householdsUtils);
		this.getFixedOrderSimulationListener().addSimulationListener(householdsUtils);
		this.householdsUtils.printStatistics();
		
		Set<Feature> features = new HashSet<Feature>();
		SHPFileUtil util = new SHPFileUtil();
		for (String file : EvacuationConfig.evacuationArea) {
			features.addAll(util.readFile(file));		
		}
		affectedArea = util.mergeGeomgetries(features);
		log.info("Size of affected area: " + affectedArea.getArea());
		
		this.coordAnalyzer = new CoordAnalyzer(affectedArea);
		
		this.selectHouseholdMeetingPoint = new SelectHouseholdMeetingPoint(this.scenarioData, this.getEvents(), householdsUtils, coordAnalyzer);
		this.getFixedOrderSimulationListener().addSimulationListener(this.selectHouseholdMeetingPoint);
		
		this.househouldsTracker = new HouseholdsTracker();
		this.getEvents().addHandler(househouldsTracker);
		this.getFixedOrderSimulationListener().addSimulationListener(househouldsTracker);
		
		this.vehiclesTracker = new VehiclesTracker(this.getEvents());
		this.getEvents().addHandler(vehiclesTracker);
		this.getFixedOrderSimulationListener().addSimulationListener(vehiclesTracker);
		
		this.passengerDepartureHandler = new PassengerDepartureHandler(this.getEvents(), vehiclesTracker);
		
		this.modeAvailabilityChecker = new ModeAvailabilityChecker(this.scenarioData, vehiclesTracker);
		
		/*
		 * Read household-vehicles-assignment files.
		 */
		this.householdVehicleAssignmentReader = new HouseholdVehicleAssignmentReader(this.scenarioData);
		for (String file : EvacuationConfig.vehicleFleet) this.householdVehicleAssignmentReader.parseFile(file);
		this.householdVehicleAssignmentReader.createVehiclesForCrossboarderHouseholds();
		
//		this.householdVehiclesTracker = new HouseholdVehiclesTracker(this.scenarioData, householdVehicleAssignmentReader.getAssignedVehicles());
//		this.getEvents().addHandler(householdVehiclesTracker);
		
		/*
		 * Create vehicles for households and add them to the scenario.
		 * When useVehicles is set to true, the scenario creates a Vehicles container if necessary.
		 */
		this.config.scenario().setUseVehicles(true);
		createVehiclesForHouseholds = new CreateVehiclesForHouseholds(this.scenarioData, this.householdVehicleAssignmentReader.getAssignedVehicles());
		createVehiclesForHouseholds.run();
		
		/*
		 * Write vehicles to file.
		 */
		new VehicleWriterV1(scenarioData.getVehicles()).writeFile(this.getControlerIO().getOutputFilename(FILENAME_VEHICLES));
			
		/*
		 * Assign vehicles to agent's plans.
		 */
		this.assignVehiclesToPlans = new AssignVehiclesToPlans(this.scenarioData, this.createRoutingAlgorithm());
		for (Household household : ((ScenarioImpl) scenarioData).getHouseholds().getHouseholds().values()) {
			this.assignVehiclesToPlans.run(household);
		}
		this.assignVehiclesToPlans.printStatistics();
		
		/*
		 * Use a MobsimFactory which creates vehicles according to available vehicles per
		 * household.
		 */
		MobsimFactory mobsimFactory = new EvacuationQSimFactory(this.getMultiModalTravelTimeWrapperFactory());
		this.setMobsimFactory(mobsimFactory);
		
		/*
		 * Create the set of analyzed modes.
		 */
		Set<String> transportModes = new HashSet<String>();
		transportModes.add(TransportMode.bike);
		transportModes.add(TransportMode.car);
		transportModes.add(TransportMode.pt);
		transportModes.add(TransportMode.ride);
		transportModes.add(TransportMode.walk);
		transportModes.add(PassengerDepartureHandler.passengerTransportMode);

		/*
		 * intialize analyse modules
		 */
		// Create kmz file containing distribution of evacuation times. 
		if (EvacuationConfig.createEvacuationTimePicture) {
			evacuationTimePicture = new EvacuationTimePicture(scenarioData, coordAnalyzer, househouldsTracker, vehiclesTracker);
			this.addControlerListener(evacuationTimePicture);
			this.getFixedOrderSimulationListener().addSimulationListener(evacuationTimePicture);
			this.events.addHandler(evacuationTimePicture);	
		}
		
		 // Create and add an AgentsInEvacuationAreaCounter.
		if (EvacuationConfig.countAgentsInEvacuationArea) {
			double scaleFactor = 1 / this.config.getQSimConfigGroup().getFlowCapFactor();
			agentsInEvacuationAreaCounter = new AgentsInEvacuationAreaCounter(this.scenarioData, transportModes, coordAnalyzer, scaleFactor);
			this.addControlerListener(agentsInEvacuationAreaCounter);
			this.getFixedOrderSimulationListener().addSimulationListener(agentsInEvacuationAreaCounter);
			this.events.addHandler(agentsInEvacuationAreaCounter);	
		}
		
		// initialize the Identifiers here because some of them have to be registered as SimulationListeners
		this.initIdentifiers();
	}
	
	/*
	 * PersonPrepareForSim is run before the first iteration is started.
	 * There, some routes might be recalculated and their vehicleIds set to null.
	 * As a result, we have to reassign the vehicles to the agents.
	 */
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (event.getIteration() == this.config.controler().getFirstIteration()) {
			this.assignVehiclesToPlans.reassignVehicles();
		}
	}
	
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		
		/*
		 * Need to do this since this does not only create events, but also behaves 
		 * like a Mobsim Engine by ending (passenger) legs.  kai, dec'11 
		 */
		((QSim)e.getQueueSimulation()).addMobsimEngine(this.vehiclesTracker);
			
		this.initReplanners((QSim)e.getQueueSimulation());
		
		/*
		 * We replace the selected plan of each agent with the executed plan which
		 * is adapted by the within day replanning modules.
		 * So far, this is necessary because some modules, like e.g. EventsToScore,
		 * use person.getSelectedPlan(). However, when using within-day replanning
		 * the selected plan might be different than the executed plan which
		 * in turn will result in code that crashes...
		 */
		if (adaptOriginalPlans) {
			for (MobsimAgent agent : ((QSim)e.getQueueSimulation()).getAgents()) {
				if (agent instanceof ExperimentalBasicWithindayAgent) {
					Plan executedPlan = ((ExperimentalBasicWithindayAgent) agent).getSelectedPlan();
					PersonImpl person = (PersonImpl)((ExperimentalBasicWithindayAgent) agent).getPerson();
					person.removePlan(person.getSelectedPlan());
					person.addPlan(executedPlan);
					person.setSelectedPlan(executedPlan);
				}
			}
		}
		
		((QSim)e.getQueueSimulation()).addDepartureHandler(passengerDepartureHandler);	
	}
		
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		householdsUtils.printStatistics();
		householdsUtils.printClosingStatistics();
		
//		householdVehiclesTracker.printClosingStatistics();
	}

	protected void initIdentifiers() {
		
		/*
		 * During Activity Identifiers
		 */
		this.activityPerformingIdentifier = new ActivityPerformingIdentifierFactory(this.getActivityReplanningMap()).createIdentifier();
		
		this.joinedHouseholdsIdentifier = new JoinedHouseholdsIdentifierFactory(this.scenarioData.getVehicles(), this.householdsUtils, 
				this.selectHouseholdMeetingPoint, this.modeAvailabilityChecker, this.vehiclesTracker).createIdentifier();
		this.getEvents().addHandler((JoinedHouseholdsIdentifier) this.joinedHouseholdsIdentifier);
		this.getFixedOrderSimulationListener().addSimulationListener((JoinedHouseholdsIdentifier) this.joinedHouseholdsIdentifier);
		
		/*
		 * During Leg Identifiers
		 */
		this.legPerformingIdentifier = new LegPerformingIdentifierFactory(this.getLinkReplanningMap()).createIdentifier();
		
		this.agentsToPickupIdentifier = new AgentsToPickupIdentifierFactory(this.scenarioData, this.coordAnalyzer, this.vehiclesTracker, walkTravelTimeFactory).createIdentifier();
		this.getEvents().addHandler((AgentsToPickupIdentifier) this.agentsToPickupIdentifier);
		this.getFixedOrderSimulationListener().addSimulationListener((AgentsToPickupIdentifier) this.agentsToPickupIdentifier);
		
		Set<String> duringLegRerouteTransportModes = new HashSet<String>();
		duringLegRerouteTransportModes.add(TransportMode.car);
		this.duringLegRerouteIdentifier = new LeaveLinkIdentifierFactory(this.getLinkReplanningMap(), duringLegRerouteTransportModes).createIdentifier();
	}
	
	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalized Router.
	 */
	protected void initReplanners(QSim sim) {
		
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) sim.getScenario().getPopulation().getFactory()).getModeRouteFactory();
			
		// use fuzzyTravelTimes
		FuzzyTravelTimeDataCollector fuzzyTravelTimeDataCollector = new FuzzyTravelTimeDataCollector(this.scenarioData);
		this.getEvents().addHandler(fuzzyTravelTimeDataCollector);
		
		FuzzyTravelTimeEstimatorFactory fuzzyTravelTimeEstimatorFactory = new FuzzyTravelTimeEstimatorFactory(this.getTravelTimeCollectorFactory(), fuzzyTravelTimeDataCollector);
		
		// create a copy of the MultiModalTravelTimeWrapperFactory and set the TravelTimeCollector for car mode
		MultiModalTravelTimeWrapperFactory timeFactory = new MultiModalTravelTimeWrapperFactory();
		for (Entry<String, PersonalizableTravelTimeFactory> entry : this.getMultiModalTravelTimeWrapperFactory().getPersonalizableTravelTimeFactories().entrySet()) {
			timeFactory.setPersonalizableTravelTimeFactory(entry.getKey(), entry.getValue());
		}
		timeFactory.setPersonalizableTravelTimeFactory(TransportMode.car, fuzzyTravelTimeEstimatorFactory);
		
		TravelCostCalculatorFactory costFactory = new OnlyTimeDependentTravelCostCalculatorFactory();
		
		LeastCostPathCalculatorFactory factory = new FastAStarLandmarksFactory(this.network, new FreespeedTravelTimeCost(this.config.planCalcScore()));
		AbstractMultithreadedModule router = new ReplanningModule(config, network, costFactory, timeFactory, factory, routeFactory);

		/*
		 * During Activity Replanners
		 */
		this.currentActivityToMeetingPointReplanner = new CurrentActivityToMeetingPointReplannerFactory(this.scenarioData, router, 1.0, householdsUtils, modeAvailabilityChecker).createReplanner();
		this.currentActivityToMeetingPointReplanner.addAgentsToReplanIdentifier(this.activityPerformingIdentifier);
		this.getReplanningManager().addTimedDuringActivityReplanner(this.currentActivityToMeetingPointReplanner, EvacuationConfig.evacuationTime, EvacuationConfig.evacuationTime + 1);
		
		this.joinedHouseholdsReplanner = new JoinedHouseholdsReplannerFactory(this.scenarioData, router, 1.0, householdsUtils, (JoinedHouseholdsIdentifier) joinedHouseholdsIdentifier).createReplanner();
		this.joinedHouseholdsReplanner.addAgentsToReplanIdentifier(joinedHouseholdsIdentifier);
		this.getReplanningManager().addTimedDuringActivityReplanner(this.joinedHouseholdsReplanner, EvacuationConfig.evacuationTime + 1, Double.MAX_VALUE);

		/*
		 * During Leg Replanners
		 */
		this.currentLegToMeetingPointReplanner = new CurrentLegToMeetingPointReplannerFactory(this.scenarioData, router, 1.0, householdsUtils).createReplanner();
		this.currentLegToMeetingPointReplanner.addAgentsToReplanIdentifier(this.legPerformingIdentifier);
		this.getReplanningManager().addTimedDuringLegReplanner(this.currentLegToMeetingPointReplanner, EvacuationConfig.evacuationTime, EvacuationConfig.evacuationTime + 1);
				
		this.pickupAgentsReplanner = new PickupAgentReplannerFactory(this.scenarioData, router, 1.0).createReplanner();
		this.pickupAgentsReplanner.addAgentsToReplanIdentifier(this.agentsToPickupIdentifier);
		this.getReplanningManager().addTimedDuringLegReplanner(this.pickupAgentsReplanner, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
		
		this.duringLegRerouteReplanner = new CurrentLegReplannerFactory(this.scenarioData, router, duringLegRerouteShare).createReplanner();
		this.duringLegRerouteReplanner.addAgentsToReplanIdentifier(this.duringLegRerouteIdentifier);
		this.getReplanningManager().addTimedDuringLegReplanner(this.duringLegRerouteReplanner, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
	}

	private void addPickupFacilities() {
		for (Link link : scenarioData.getNetwork().getLinks().values()) {
			/*
			 * Create and add the pickup facility and add activity option ("pickup")
			 */
			String idString = link.getId().toString() + "_pickup";
			ActivityFacility secureFacility = scenarioData.getActivityFacilities().createFacility(scenarioData.createId(idString), link.getCoord());
			((ActivityFacilityImpl)secureFacility).setLinkId(((LinkImpl)link).getId());
			
			ActivityOption activityOption = ((ActivityFacilityImpl)secureFacility).createActivityOption("pickup");
			activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 0*3600, 24*3600));
			activityOption.setCapacity(Double.MAX_VALUE);
		}
	}
	
	/*
	 * The LinkReplanningMap calculates the earliest link exit time for each agent.
	 * To do so, a MultiModalTravelTime object is required which calculates these
	 * times. We use a MultiModalTravelTimeWrapper with walk- and bike travel times
	 * and replace the car, ride and pt travel time calculators with free speed
	 * travel time calculators.
	 */
	private MultiModalTravelTime createLinkReplanningMapTravelTime() {
		
		// create a copy of the MultiModalTravelTimeWrapperFactory and set a FreeSpeedTravelTimeCalculator for car mode
		MultiModalTravelTimeWrapperFactory timeFactory = new MultiModalTravelTimeWrapperFactory();
		for (Entry<String, PersonalizableTravelTimeFactory> entry : this.getMultiModalTravelTimeWrapperFactory().getPersonalizableTravelTimeFactories().entrySet()) {
			timeFactory.setPersonalizableTravelTimeFactory(entry.getKey(), entry.getValue());
		}

		// replace modes
		timeFactory.setPersonalizableTravelTimeFactory(TransportMode.car, new FreeSpeedTravelTimeFactory());
		timeFactory.setPersonalizableTravelTimeFactory(TransportMode.ride, new FreeSpeedTravelTimeFactory());
		timeFactory.setPersonalizableTravelTimeFactory(TransportMode.pt, new FreeSpeedTravelTimeFactory());

		// return travel time object
		return timeFactory.createTravelTime();
	}
	
	private static class FreeSpeedTravelTimeFactory implements PersonalizableTravelTimeFactory {

		@Override
		public PersonalizableTravelTime createTravelTime() {
			return new FreeSpeedTravelTimeCalculator();
		}
	}
	
	/*
	 * ===================================================================
	 * main
	 * ===================================================================
	 */
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new EvacuationControler(args);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}
	
}
