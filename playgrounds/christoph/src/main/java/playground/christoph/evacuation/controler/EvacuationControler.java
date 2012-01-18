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
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.parallelEventsHandler.SimStepParallelEventsManagerImpl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
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
import org.matsim.core.router.util.PersonalizableTravelTimeFactory;
import org.matsim.core.scoring.OnlyTimeDependentScoringFunctionFactory;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.ptproject.qsim.multimodalsimengine.router.util.MultiModalTravelTimeWrapperFactory;
import org.matsim.withinday.controller.WithinDayController;
import org.matsim.withinday.replanning.identifiers.ActivityPerformingIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.LegPerformingIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.modules.ReplanningModule;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;

import playground.christoph.evacuation.analysis.AgentsInEvacuationAreaCounter;
import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.analysis.EvacuationTimePicture;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.EvacuationQSimFactory;
import playground.christoph.evacuation.mobsim.PassengerEventsCreator;
import playground.christoph.evacuation.network.AddExitLinksToNetwork;
import playground.christoph.evacuation.router.util.FuzzyTravelTimeDataCollector;
import playground.christoph.evacuation.router.util.FuzzyTravelTimeEstimatorFactory;
import playground.christoph.evacuation.vehicles.HouseholdVehicleAssignmentReader;
import playground.christoph.evacuation.vehicles.HouseholdVehiclesTracker;
import playground.christoph.evacuation.withinday.replanning.identifiers.JoinedHouseholdsIdentifier;
import playground.christoph.evacuation.withinday.replanning.identifiers.JoinedHouseholdsIdentifierFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.CurrentActivityToMeetingPointReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.CurrentLegToMeetingPointReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.JoinedHouseholdsReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.utils.HouseholdsUtils;
import playground.christoph.evacuation.withinday.replanning.utils.ModeAvailabilityChecker;
import playground.christoph.evacuation.withinday.replanning.utils.SHPFileUtil;
import playground.christoph.evacuation.withinday.replanning.utils.SelectHouseholdMeetingPoint;

import com.vividsolutions.jts.geom.Geometry;

public class EvacuationControler extends WithinDayController implements SimulationInitializedListener, 
		StartupListener, AfterMobsimListener {

	protected boolean adaptOriginalPlans = false;
//	protected String[] evacuationAreaSHPFiles = new String[]{"../../matsim/mysimulations/census2000V2/input_1pct/shp/Zone1.shp"};
	protected String[] evacuationAreaSHPFiles = new String[]{"../../matsim/mysimulations/census2000V2/input_1pct/shp/KKW_Buffer10km.shp"};
	protected double maxCarAvailableDistance = 250.0;
	
	protected String[] householdVehicleFiles = new String[]{
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_AG.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_AI.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_AR.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_BE.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_BL.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_BS.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_FR.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_GE.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_GL.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_GR.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_JU.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_LU.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_NE.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_NW.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_OW.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_SG.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_SH.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_SO.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_SZ.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_TG.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_TI.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_UR.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_VD.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_VS.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_ZG.txt",
			"../../matsim/mysimulations/census2000V2/input_1pct/Fahrzeugtypen_Kanton_ZH.txt"};


//	protected String dhm25File = "../../matsim/mysimulations/networks/GIS/nodes_3d_dhm25.shp";
//	protected String srtmFile = "../../matsim/mysimulations/networks/GIS/nodes_3d_srtm.shp";
	
	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	protected int numReplanningThreads = 6;

	/*
	 * Identifiers
	 */
	protected DuringActivityIdentifier joinedHouseholdsIdentifier;
	protected DuringActivityIdentifier activityPerformingIdentifier;
	protected DuringLegIdentifier legPerformingIdentifier;
//	protected InitialIdentifier initialIdentifier;
//	protected DuringActivityIdentifier duringSecureActivityIdentifier;
//	protected DuringActivityIdentifier duringInsecureActivityIdentifier;
//	protected DuringLegIdentifier duringSecureLegIdentifier;
//	protected DuringLegIdentifier duringInsecureLegIdentifier;
//	protected DuringLegIdentifier currentInsecureLegIdentifier;
	
	/*
	 * Replanners
	 */
	protected WithinDayDuringActivityReplanner currentActivityToMeetingPointReplanner;
	protected WithinDayDuringActivityReplanner joinedHouseholdsReplanner;
	protected WithinDayDuringLegReplanner currentLegToMeetingPointReplanner;
//	protected WithinDayInitialReplanner initialReplanner;
//	protected WithinDayDuringActivityReplanner duringSecureActivityReplanner;
//	protected WithinDayDuringActivityReplanner duringInsecureActivityReplanner;
//	protected WithinDayDuringLegReplanner duringSecureLegReplanner;
//	protected WithinDayDuringLegReplanner duringInsecureLegReplanner;
//	protected WithinDayDuringLegReplanner currentInsecureLegReplanner;
	
//	protected AddZCoordinatesToNetwork zCoordinateAdder;
	protected HouseholdsUtils householdsUtils;
	protected HouseholdVehiclesTracker householdVehiclesTracker;
	protected HouseholdVehicleAssignmentReader householdVehicleAssignmentReader;
	protected SelectHouseholdMeetingPoint selectHouseholdMeetingPoint;
	protected ModeAvailabilityChecker modeAvailabilityChecker;
	protected PassengerEventsCreator passengerEventsCreator;
	protected CoordAnalyzer coordAnalyzer;
	protected Geometry affectedArea;

	/*
	 * Analysis modules
	 */
	protected boolean analyzeEvacuation = false;
	protected EvacuationTimePicture evacuationTimePicture;
	protected AgentsInEvacuationAreaCounter agentsInEvacuationAreaCounter;
	
	static final Logger log = Logger.getLogger(EvacuationControler.class);

	public EvacuationControler(String[] args) {
		super(args);

		setConstructorParameters();
		
		// register this as a Controller and Simulation Listener
		super.getFixedOrderSimulationListener().addSimulationListener(this);
		super.addControlerListener(this);
	}

	private void setConstructorParameters() {

		// Use a Scoring Function, that only scores the travel times!
		this.setScoringFunctionFactory(new OnlyTimeDependentScoringFunctionFactory());
	}

	/*
	 * When the Controller Startup Event is created, the EventsManager
	 * has already been initialized. Therefore we can initialize now
	 * all Objects, that have to be registered at the EventsManager.
	 */
	@Override
	public void notifyStartup(StartupEvent event) {
		
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
		
		
//		/*
//		 * Adding z-coordinates to the network
//		 */
//		zCoordinateAdder = new AddZCoordinatesToNetwork(this.scenarioData, dhm25File, srtmFile);
//		zCoordinateAdder.addZCoordinatesToNetwork();
//		zCoordinateAdder.checkSteepness();

//		/*
//		 * Use advanced walk- and bike travel time calculators
//		 */
//		PersonalizableTravelTimeFactory walkTravelTimeFactory = new WalkTravelTimeFactory(this.config.plansCalcRoute());
//		PersonalizableTravelTimeFactory bikeTravelTimeFactory = new BikeTravelTimeFactory(this.config.plansCalcRoute());
//		this.getMultiModalTravelTimeWrapperFactory().setPersonalizableTravelTimeFactory(TransportMode.walk, walkTravelTimeFactory);
//		this.getMultiModalTravelTimeWrapperFactory().setPersonalizableTravelTimeFactory(TransportMode.bike, bikeTravelTimeFactory);
		
		Set<String> analyzedModes = new HashSet<String>();
		analyzedModes.add(TransportMode.car);
		super.createAndInitTravelTimeCollector(analyzedModes);
		
		super.createAndInitReplanningManager(numReplanningThreads);
		super.createAndInitActivityReplanningMap();
		super.createAndInitLinkReplanningMap();
		
		this.modeAvailabilityChecker = new ModeAvailabilityChecker(this.scenarioData, this.maxCarAvailableDistance);
		this.getEvents().addHandler(modeAvailabilityChecker);
		this.getFixedOrderSimulationListener().addSimulationListener(modeAvailabilityChecker);
		
		this.householdsUtils = new HouseholdsUtils(this.scenarioData, this.getEvents());
		this.getEvents().addHandler(householdsUtils);
		this.getFixedOrderSimulationListener().addSimulationListener(householdsUtils);
		this.householdsUtils.printStatistics();
		
		Set<Feature> features = new HashSet<Feature>();
		SHPFileUtil util = new SHPFileUtil();
		for (String file : this.evacuationAreaSHPFiles) {
			features.addAll(util.readFile(file));		
		}
		affectedArea = util.mergeGeomgetries(features);
		
		this.coordAnalyzer = new CoordAnalyzer(affectedArea);
		
		this.selectHouseholdMeetingPoint = new SelectHouseholdMeetingPoint(this.scenarioData, this.getEvents(), householdsUtils, coordAnalyzer);
		this.getFixedOrderSimulationListener().addSimulationListener(this.selectHouseholdMeetingPoint);
		
		this.passengerEventsCreator = new PassengerEventsCreator(this.events);
		
		this.getEvents().addHandler(passengerEventsCreator);
		this.getFixedOrderSimulationListener().addSimulationListener(passengerEventsCreator);
		
		/*
		 * Read household-vehicles-assignment files.
		 */
		this.householdVehicleAssignmentReader = new HouseholdVehicleAssignmentReader(this.scenarioData);
		for (String file : this.householdVehicleFiles) this.householdVehicleAssignmentReader.parseFile(file);
		this.householdVehicleAssignmentReader.createVehiclesForCrossboarderHouseholds();
		
		this.householdVehiclesTracker = new HouseholdVehiclesTracker(this.scenarioData, householdVehicleAssignmentReader.getAssignedVehicles());
		this.getEvents().addHandler(householdVehiclesTracker);
				
		/*
		 * Use a MobsimFactory which creates vehicles according to available vehicles per
		 * household.
		 */
		MobsimFactory mobsimFactory = new EvacuationQSimFactory();
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
		transportModes.add(PassengerEventsCreator.passengerTransportMode);
		
		if (analyzeEvacuation) {
			/*
			 * Create and add an AgentsInEvacuationAreaCounter.
			 */
			double scaleFactor = 1 / this.config.getQSimConfigGroup().getFlowCapFactor();
			agentsInEvacuationAreaCounter = new AgentsInEvacuationAreaCounter(this.scenarioData, transportModes, coordAnalyzer, scaleFactor);
			this.addControlerListener(agentsInEvacuationAreaCounter);
			this.getFixedOrderSimulationListener().addSimulationListener(agentsInEvacuationAreaCounter);
			this.events.addHandler(agentsInEvacuationAreaCounter);
			
			evacuationTimePicture = new EvacuationTimePicture(scenarioData, transportModes, coordAnalyzer);
			this.addControlerListener(evacuationTimePicture);
			this.getFixedOrderSimulationListener().addSimulationListener(evacuationTimePicture);
			this.events.addHandler(evacuationTimePicture);			
		}
		
		// initialize the Identifiers here because some of them have to be registered as SimulationListeners
		this.initIdentifiers();
	}
	
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		
		/*
		 * Need to do this since this does not only create events, but also behaves 
		 * like a Mobsim Engine by ending (passenger) legs.  kai, dec'11 
		 */
		((QSim)e.getQueueSimulation()).addMobsimEngine(this.passengerEventsCreator);
		
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
		
		((QSim)e.getQueueSimulation()).addDepartureHandler(passengerEventsCreator);
	}
		
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		householdsUtils.printStatistics();
		householdsUtils.printClosingStatistics();
		
		householdVehiclesTracker.printClosingStatistics();
	}

	protected void initIdentifiers() {
		
		/*
		 * During Activity Identifiers
		 */
		this.activityPerformingIdentifier = new ActivityPerformingIdentifierFactory(this.getActivityReplanningMap()).createIdentifier();
		
		this.joinedHouseholdsIdentifier = new JoinedHouseholdsIdentifierFactory(this.householdsUtils, 
				this.selectHouseholdMeetingPoint, this.modeAvailabilityChecker, this.passengerEventsCreator).createIdentifier();
		this.getEvents().addHandler((JoinedHouseholdsIdentifier) this.joinedHouseholdsIdentifier);
		this.getFixedOrderSimulationListener().addSimulationListener((JoinedHouseholdsIdentifier) this.joinedHouseholdsIdentifier);
		
		/*
		 * During Leg Identifiers
		 */
		this.legPerformingIdentifier = new LegPerformingIdentifierFactory(this.getLinkReplanningMap()).createIdentifier();
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
		 * Intial Replanners
		 */
//		this.initialIdentifier = new InitialIdentifierImpl(this.sim);
//		this.selector.addIdentifier(initialIdentifier, pInitialReplanning);
//		this.initialReplanner = new InitialReplanner(ReplanningIdGenerator.getNextId(), this.scenarioData);
//		this.initialReplanner.setReplanner(router);
//		this.initialReplanner.addAgentsToReplanIdentifier(this.initialIdentifier);
//		this.replanningManager.addIntialReplanner(this.initialReplanner);

		/*
		 * During Activity Replanners
		 */
		this.currentActivityToMeetingPointReplanner = new CurrentActivityToMeetingPointReplannerFactory(this.scenarioData, router, 1.0, householdsUtils, modeAvailabilityChecker).createReplanner();
		this.currentActivityToMeetingPointReplanner.addAgentsToReplanIdentifier(this.activityPerformingIdentifier);
		this.getReplanningManager().addTimedDuringActivityReplanner(this.currentActivityToMeetingPointReplanner, EvacuationConfig.evacuationTime, EvacuationConfig.evacuationTime + 1);
		
		this.joinedHouseholdsReplanner = new JoinedHouseholdsReplannerFactory(this.scenarioData, router, 1.0, householdsUtils, (JoinedHouseholdsIdentifier) joinedHouseholdsIdentifier).createReplanner();
		this.joinedHouseholdsReplanner.addAgentsToReplanIdentifier(joinedHouseholdsIdentifier);
		this.getReplanningManager().addTimedDuringActivityReplanner(this.joinedHouseholdsReplanner, EvacuationConfig.evacuationTime + 1, Double.MAX_VALUE);
		
//		this.duringSecureActivityIdentifier = new SecureActivityPerformingIdentifierFactory(this.getActivityReplanningMap(), EvacuationConfig.centerCoord, EvacuationConfig.innerRadius).createIdentifier();
//		this.selector.addIdentifier(this.duringSecureActivityIdentifier, this.pDuringActivityReplanning);
//		this.duringSecureActivityReplanner = new ExtendCurrentActivityReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
//		this.duringSecureActivityReplanner.addAgentsToReplanIdentifier(this.duringSecureActivityIdentifier);
//		this.duringSecureActivityReplanner.setReplanningProbability(pReplanning);
//		this.getReplanningManager().addDuringActivityReplanner(this.duringSecureActivityReplanner);

//		this.duringInsecureActivityIdentifier = new InsecureActivityPerformingIdentifierFactory(this.getActivityReplanningMap(), EvacuationConfig.centerCoord, EvacuationConfig.innerRadius).createIdentifier();
//		this.selector.addIdentifier(this.duringInsecureActivityIdentifier, this.pDuringActivityReplanning);
//		this.duringInsecureActivityReplanner = new EndActivityAndEvacuateReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
//		this.duringInsecureActivityReplanner.addAgentsToReplanIdentifier(this.duringInsecureActivityIdentifier);
//		this.duringInsecureActivityReplanner.setReplanningProbability(pReplanning);
//		this.getReplanningManager().addDuringActivityReplanner(this.duringInsecureActivityReplanner);

		/*
		 * During Leg Replanners
		 */
		this.currentLegToMeetingPointReplanner = new CurrentLegToMeetingPointReplannerFactory(this.scenarioData, router, 1.0, householdsUtils).createReplanner();
		this.currentLegToMeetingPointReplanner.addAgentsToReplanIdentifier(this.legPerformingIdentifier);
		this.getReplanningManager().addTimedDuringLegReplanner(this.currentLegToMeetingPointReplanner, EvacuationConfig.evacuationTime, EvacuationConfig.evacuationTime + 1);
		
//		this.duringSecureLegIdentifier = new SecureLegPerformingIdentifierFactory(this.getLinkReplanningMap(), network, EvacuationConfig.centerCoord, EvacuationConfig.innerRadius).createIdentifier();
//		this.selector.addIdentifier(this.duringSecureLegIdentifier, this.pDuringLegReplanning);
//		this.duringSecureLegReplanner = new CurrentLegToSecureFacilityReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
//		this.duringSecureLegReplanner.addAgentsToReplanIdentifier(this.duringSecureLegIdentifier);
//		this.duringSecureLegReplanner.setReplanningProbability(pReplanning);
//		this.getReplanningManager().addDuringLegReplanner(this.duringSecureLegReplanner);

//		this.duringInsecureLegIdentifier = new InsecureLegPerformingIdentifierFactory(this.getLinkReplanningMap(), network, EvacuationConfig.centerCoord, EvacuationConfig.innerRadius).createIdentifier();
//		this.selector.addIdentifier(this.duringInsecureLegIdentifier, this.pDuringLegReplanning);
//		this.duringInsecureLegReplanner = new CurrentLegToRescueFacilityReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
//		this.duringInsecureLegReplanner.addAgentsToReplanIdentifier(this.duringInsecureLegIdentifier);
//		this.duringInsecureLegReplanner.setReplanningProbability(pReplanning);
//		this.getReplanningManager().addDuringLegReplanner(this.duringInsecureLegReplanner);

//		this.currentInsecureLegIdentifier = new LeaveLinkIdentifierFactory(this.getLinkReplanningMap()).createIdentifier();
//		this.selector.addIdentifier(this.currentInsecureLegIdentifier, this.pDuringLegReplanning);
//		this.currentInsecureLegReplanner = new CurrentLegReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
//		this.currentInsecureLegReplanner.addAgentsToReplanIdentifier(this.currentInsecureLegIdentifier);
//		this.currentInsecureLegReplanner.setReplanningProbability(pReplanning);
//		this.getReplanningManager().addDuringLegReplanner(this.currentInsecureLegReplanner);
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
