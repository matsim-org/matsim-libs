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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Module;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.PTTravelTimeFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.RideTravelTimeFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureWriter;
import org.matsim.core.mobsim.qsim.qnetsimengine.PassengerDepartureHandler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.TripRouterFactoryInternal;
import org.matsim.core.router.TripRouterFactoryImpl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.TravelTimeFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.functions.OnlyTravelDependentScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.households.Household;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.withinday.controller.WithinDayController;
import org.matsim.withinday.replanning.identifiers.ActivityPerformingIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.LegPerformingIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.filter.ProbabilityFilterFactory;
import org.matsim.withinday.replanning.identifiers.filter.TransportModeFilterFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilterFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayReplannerFactory;
import org.opengis.feature.simple.SimpleFeature;

import playground.christoph.evacuation.analysis.AgentsInEvacuationAreaActivityCounter;
import playground.christoph.evacuation.analysis.AgentsInEvacuationAreaCounter;
import playground.christoph.evacuation.analysis.AgentsReturnHomeCounter;
import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.analysis.EvacuationTimePicture;
import playground.christoph.evacuation.analysis.TravelTimesWriter;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.config.EvacuationConfigReader;
import playground.christoph.evacuation.mobsim.EvacuationQSimFactory;
import playground.christoph.evacuation.mobsim.HouseholdsTracker;
import playground.christoph.evacuation.mobsim.LegModeChecker;
import playground.christoph.evacuation.mobsim.OldPassengerDepartureHandler;
import playground.christoph.evacuation.mobsim.VehiclesTracker;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataGrabber;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisionmodel.DecisionModelRunner;
import playground.christoph.evacuation.pt.EvacuationTransitRouterFactory;
import playground.christoph.evacuation.pt.TransitRouterNetworkReaderMatsimV1;
import playground.christoph.evacuation.router.LeastCostPathCalculatorSelectorFactory;
import playground.christoph.evacuation.router.RandomCompassRouterFactory;
import playground.christoph.evacuation.router.util.AffectedAreaPenaltyCalculator;
import playground.christoph.evacuation.router.util.FuzzyTravelTimeEstimatorFactory;
import playground.christoph.evacuation.router.util.PenaltyTravelCostFactory;
import playground.christoph.evacuation.trafficmonitoring.BikeTravelTimeFactory;
import playground.christoph.evacuation.trafficmonitoring.PTTravelTimeEvacuationFactory;
import playground.christoph.evacuation.trafficmonitoring.PTTravelTimeKTIEvacuationFactory;
import playground.christoph.evacuation.trafficmonitoring.SwissPTTravelTime;
import playground.christoph.evacuation.trafficmonitoring.WalkTravelTimeFactory;
import playground.christoph.evacuation.vehicles.AssignVehiclesToPlans;
import playground.christoph.evacuation.vehicles.CreateVehiclesForHouseholds;
import playground.christoph.evacuation.vehicles.HouseholdVehicleAssignmentReader;
import playground.christoph.evacuation.withinday.replanning.identifiers.AgentsToDropOffIdentifier;
import playground.christoph.evacuation.withinday.replanning.identifiers.AgentsToDropOffIdentifierFactory;
import playground.christoph.evacuation.withinday.replanning.identifiers.AgentsToPickupIdentifier;
import playground.christoph.evacuation.withinday.replanning.identifiers.AgentsToPickupIdentifierFactory;
import playground.christoph.evacuation.withinday.replanning.identifiers.InformedAgentsFilter.FilterType;
import playground.christoph.evacuation.withinday.replanning.identifiers.InformedAgentsFilterFactory;
import playground.christoph.evacuation.withinday.replanning.identifiers.InformedHouseholdsTracker;
import playground.christoph.evacuation.withinday.replanning.identifiers.JoinedHouseholdsIdentifier;
import playground.christoph.evacuation.withinday.replanning.identifiers.JoinedHouseholdsIdentifierFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.CurrentActivityToMeetingPointReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.CurrentLegToMeetingPointReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.DropOffAgentReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.JoinedHouseholdsReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.PickupAgentReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.utils.ModeAvailabilityChecker;
import playground.christoph.evacuation.withinday.replanning.utils.SHPFileUtil;
import playground.christoph.evacuation.withinday.replanning.utils.SelectHouseholdMeetingPoint;
import playground.meisterk.kti.config.KtiConfigGroup;

import com.vividsolutions.jts.geom.Geometry;


public class EvacuationControler extends WithinDayController implements 
		MobsimAfterSimStepListener, IterationStartsListener, AfterMobsimListener {

//	public static final String referenceEventsFile = "../../matsim/mysimulations/census2000V2/output_10pct_evac_reference/ITERS/it.0/evac.1.0.events.xml.gz";
	public static final String referenceEventsFile = null;

	public static final String FILENAME_VEHICLES = "output_vehicles.xml.gz";
	
	protected boolean adaptOriginalPlans = true;

	/*
	 * Identifiers
	 */
	protected DuringActivityIdentifier joinedHouseholdsIdentifier;
	protected DuringActivityIdentifier activityPerformingIdentifier;
	protected DuringLegIdentifier legPerformingIdentifier;
	protected DuringLegIdentifier agentsToDropOffIdentifier;
	protected DuringLegIdentifier agentsToPickupIdentifier;
	protected DuringLegIdentifier duringLegRerouteIdentifier;
	
	/*
	 * ReplannerFactories
	 */
	protected WithinDayDuringActivityReplannerFactory currentActivityToMeetingPointReplannerFactory;
	protected WithinDayDuringActivityReplannerFactory joinedHouseholdsReplannerFactory;
	protected WithinDayDuringLegReplannerFactory currentLegToMeetingPointReplannerFactory;
	protected WithinDayDuringLegReplannerFactory dropOffAgentsReplannerFactory;
	protected WithinDayDuringLegReplannerFactory pickupAgentsReplannerFactory;
	protected WithinDayDuringLegReplannerFactory duringLegRerouteReplannerFactory;
	
	/*
	 * Replanners that are used to adapt agent's plans for the first time. They can be disabled
	 * after all agents have been informed and have adapted their plans.
	 */
	protected List<WithinDayReplannerFactory<?>> initialReplannerFactories;
		
	protected ObjectAttributes householdObjectAttributes;
//	protected HouseholdsUtils householdsUtils;
	protected HouseholdVehicleAssignmentReader householdVehicleAssignmentReader;
//	protected HouseholdVehiclesTracker householdVehiclesTracker;
	
	protected CreateVehiclesForHouseholds createVehiclesForHouseholds;
	protected AssignVehiclesToPlans assignVehiclesToPlans;
	
	protected JointDepartureOrganizer jointDepartureOrganizer;
	protected JointDepartureWriter jointDepartureWriter;
	protected InformedHouseholdsTracker informedHouseholdsTracker;
	protected SelectHouseholdMeetingPoint selectHouseholdMeetingPoint;
	protected ModeAvailabilityChecker modeAvailabilityChecker;
	protected DecisionDataProvider decisionDataProvider;
	protected DecisionDataGrabber decisionDataGrabber;
	protected DecisionModelRunner decisionModelRunner;
	protected AffectedAreaPenaltyCalculator penaltyCalculator;
	protected HouseholdsTracker householdsTracker;
	protected VehiclesTracker vehiclesTracker;
	protected CoordAnalyzer coordAnalyzer;
	protected Geometry affectedArea;

	protected TransitRouterNetwork routerNetwork;
	
	protected TravelTime walkTravelTime;
	protected TravelTime bikeTravelTime;
	protected TravelTime ptTravelTime;
	protected TravelTime rideTravelTime;
	
	protected KtiConfigGroup ktiConfigGroup;
	
	/*
	 * Analysis modules
	 */
	protected EvacuationTimePicture evacuationTimePicture;
	protected AgentsReturnHomeCounter agentsReturnHomeCounter;
	protected AgentsInEvacuationAreaCounter agentsInEvacuationAreaCounter;
	protected AgentsInEvacuationAreaActivityCounter agentsInEvacuationAreaActivityCounter;
	
	static final Logger log = Logger.getLogger(EvacuationControler.class);

	public EvacuationControler(String[] args) {
		super(args);
		
		/*
		 * Change the persons' plans. Doing so might simplify bug-tracking.
		 */
		if (adaptOriginalPlans) {
			ExperimentalBasicWithindayAgent.copySelectedPlan = false;
		}
		
		/*
		 * Set ride_passenger mode in PassengerDepartureHandler
		 */
		PassengerDepartureHandler.passengerMode = OldPassengerDepartureHandler.passengerTransportMode;
		
		/*
		 * Create the empty object. They are filled in the loadData() method.
		 */
		this.ktiConfigGroup = new KtiConfigGroup();
		
		new EvacuationConfigReader().readFile(args[1]);
		EvacuationConfig.printConfig();
		
		// Use a Scoring Function, that only scores the travel times!
		this.setScoringFunctionFactory(new OnlyTravelDependentScoringFunctionFactory());	

		/*
		 * get number of threads from config file
		 */
		this.setNumberOfReplanningThreads(this.config.global().getNumberOfThreads());
		
		/*
		 * Create and initialize replanning manager and replanning maps.
		 */
		Map<String, TravelTime> linkReplanningTravelTimes = this.createLinkReplanningMapTravelTimes();
		super.createAndInitLinkReplanningMap(linkReplanningTravelTimes);
	}

	@Override
	protected void loadData() {
		
		// load data in super class
		super.loadData();
	
		/*
		 * The KTIConfigGroup is loaded as generic Module. We replace this
		 * generic object with a KtiConfigGroup object and copy all its parameter.
		 */
		Module module = this.config.getModule(KtiConfigGroup.GROUP_NAME);
		this.config.removeModule(KtiConfigGroup.GROUP_NAME);
		this.config.addModule(KtiConfigGroup.GROUP_NAME, this.ktiConfigGroup);
		
		for (Entry<String, String> entry : module.getParams().entrySet()) {
			this.ktiConfigGroup.addParam(entry.getKey(), entry.getValue());
		}

		/*
		 * If enabled, set the evacuation transit router factory here.
		 */
		if (EvacuationConfig.useTransitRouter) {
			
			/*
			 * If useTransit is not enabled in the config, no transit schedule object
			 * is created. Since we need one, we enable transit, call getTransitSchedule
			 * (which triggers the creation in the scenario) and disable transit again.
			 */
			if (!config.scenario().isUseTransit()) {
				config.scenario().setUseTransit(true);
				scenarioData.getTransitSchedule();
				config.scenario().setUseTransit(false);
			}
			
			new TransitScheduleReader(scenarioData).readFile(config.transit().getTransitScheduleFile());
			routerNetwork = new TransitRouterNetwork();
			new TransitRouterNetworkReaderMatsimV1(scenarioData, routerNetwork).parse(EvacuationConfig.transitRouterFile);
		}
	}

	/*
	 * When the Controller Startup Event is created, the EventsManager
	 * has already been initialized. Therefore we can initialize now
	 * all Objects, that have to be registered at the EventsManager.
	 * 
	 * This code was previously called from a StartupListener. However, this
	 * is not possible anymore since the TripRouterFactory is not initialized
	 * when the StartupListener is called. cdobler, dec'12
	 */
	@Override
	public void setUp() {
		
		// initialze plan router
		super.setUp();
		
//		PlansLinkReferenceDumping planAlgo = new PlansLinkReferenceDumping();
//		planAlgo.run(population);
		
		/*
		 * Using a LegModeChecker to ensure that all agents' plans have valid mode chains.
		 */
		LegModeChecker legModeChecker = new LegModeChecker(this.getTripRouterFactory().instantiateAndConfigureTripRouter(), this.getNetwork());
		legModeChecker.setValidNonCarModes(new String[]{TransportMode.walk, TransportMode.bike, TransportMode.pt});
		legModeChecker.setToCarProbability(0.5);
		legModeChecker.run(this.scenarioData.getPopulation());
		legModeChecker.printStatistics();
		
		/*
		 * Prepare the scenario:
		 * 	- connect facilities to network
		 *  - adapt network capacities and speeds
		 * 	- add exit links to network
		 * 	- add pickup facilities
		 *  - add z Coordinates to network
		 */
		new PrepareEvacuationScenario().prepareScenario(this.scenarioData);
		
		/*
		 * Adapt walk- and bike speed according to car speed reduction.
		 */
		this.config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.walk, this.config.plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk) * EvacuationConfig.speedFactor);
		this.config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.bike, this.config.plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.bike) * EvacuationConfig.speedFactor);
		
		// load household object attributes
		this.householdObjectAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(householdObjectAttributes).parse(EvacuationConfig.householdObjectAttributesFile);
						
//		this.householdsUtils = new HouseholdsUtils(this.scenarioData, this.getEvents());
//		this.getEvents().addHandler(householdsUtils);
//		this.getFixedOrderSimulationListener().addSimulationListener(householdsUtils);
//		this.householdsUtils.printStatistics();
		
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		SHPFileUtil util = new SHPFileUtil();
		for (String file : EvacuationConfig.evacuationArea) {
			features.addAll(ShapeFileReader.getAllFeatures(file));		
		}
		affectedArea = util.mergeGeometries(features);
		log.info("Size of affected area: " + affectedArea.getArea());
		
		this.penaltyCalculator = new AffectedAreaPenaltyCalculator(this.getNetwork(), affectedArea, 
				EvacuationConfig.affectedAreaDistanceBuffer, EvacuationConfig.affectedAreaTimePenaltyFactor);
		
		this.coordAnalyzer = new CoordAnalyzer(affectedArea);	
		
		this.informedHouseholdsTracker = new InformedHouseholdsTracker(this.scenarioData.getHouseholds(), 
				this.scenarioData.getPopulation().getPersons().keySet(), this.getEvents(), EvacuationConfig.informAgentsRayleighSigma);
		this.getFixedOrderSimulationListener().addSimulationListener(informedHouseholdsTracker);
		
		this.householdsTracker = new HouseholdsTracker(this.scenarioData);
		this.getEvents().addHandler(householdsTracker);
		this.getFixedOrderSimulationListener().addSimulationListener(householdsTracker);
		
		this.jointDepartureOrganizer = new JointDepartureOrganizer();
		this.jointDepartureWriter = new JointDepartureWriter(this.jointDepartureOrganizer);
		this.addControlerListener(this.jointDepartureWriter);
		
		this.vehiclesTracker = new VehiclesTracker(jointDepartureOrganizer, this.jointDepartureWriter);
		this.getEvents().addHandler(vehiclesTracker);
		this.getFixedOrderSimulationListener().addSimulationListener(vehiclesTracker);
		
		this.decisionDataProvider = new DecisionDataProvider();
		this.decisionDataGrabber = new DecisionDataGrabber(this.scenarioData, this.decisionDataProvider, this.coordAnalyzer.createInstance(), 
				this.householdsTracker, this.householdObjectAttributes);
		this.getFixedOrderSimulationListener().addSimulationListener(this.decisionDataGrabber);
				
		this.decisionModelRunner = new DecisionModelRunner(this.scenarioData, this.decisionDataProvider);
		this.getFixedOrderSimulationListener().addSimulationListener(this.decisionModelRunner);
		this.addControlerListener(this.decisionModelRunner);
		
		/*
		 * Initialize TravelTimeCollector and create a FactoryWrapper which will act as
		 * factory but returns always the same travel time object, which is possible since
		 * the TravelTimeCollector is not personalized.
		 */
		Set<String> collectedModes = new HashSet<String>();
		collectedModes.add(TransportMode.car);
		super.createAndInitTravelTimeCollector(collectedModes);
		
		/*
		 * Use advanced walk-, bike and pt travel time calculators
		 */
		this.walkTravelTime = new WalkTravelTimeFactory(this.config.plansCalcRoute()).createTravelTime();
		this.bikeTravelTime = new BikeTravelTimeFactory(this.config.plansCalcRoute()).createTravelTime();
		
		TravelTimeFactory ptFactory;
		TravelTime nonEvacuationPTTravelTime = new PTTravelTimeFactory(this.config.plansCalcRoute(), this.getTravelTimeCollector(), walkTravelTime).createTravelTime();
		if (EvacuationConfig.useTransitRouter) {
			
			TransitRouterConfig transitRouterConfig = new TransitRouterConfig(config.planCalcScore(),
					config.plansCalcRoute(), config.transitRouter(), config.vspExperimental());
			EvacuationTransitRouterFactory evacuationTransitRouterFactory = 
					new EvacuationTransitRouterFactory(config, walkTravelTime, routerNetwork, transitRouterConfig);
			
			ptFactory = new PTTravelTimeEvacuationFactory(this.scenarioData, nonEvacuationPTTravelTime,
					evacuationTransitRouterFactory, this.informedHouseholdsTracker);
		} else {
			ptFactory = new PTTravelTimeKTIEvacuationFactory(this.scenarioData, nonEvacuationPTTravelTime);
			
			/*
			 * Update PT Travel time Matrices for evacuation routes (requires CoordAnalyzer)
			 */
			String stopsFile = this.getControlerIO().getOutputFilename("pt_evacuation_stops.shp");
			String travelTimesFile = this.getControlerIO().getOutputFilename("pt_evacuation_times.shp");
			((PTTravelTimeKTIEvacuationFactory) ptFactory).prepareMatrixForEvacuation(this.coordAnalyzer, stopsFile, travelTimesFile);

		}
		this.ptTravelTime = ptFactory.createTravelTime();
		
		
		// Use the TravelTimeCollector as ride travel time estimator. 
		this.rideTravelTime = new RideTravelTimeFactory(this.getTravelTimeCollector(), walkTravelTime).createTravelTime(); 

		Map<String, TravelTime> travelTimes = new HashMap<String, TravelTime>();
		travelTimes.put(TransportMode.walk, walkTravelTime);
		travelTimes.put(TransportMode.bike, bikeTravelTime);
		travelTimes.put(TransportMode.ride, rideTravelTime);
		travelTimes.put(TransportMode.pt, ptTravelTime);

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
		 * ModeAvailabilityChecker to check which vehicles are available for
		 * a household at a given facility.
		 */
		this.modeAvailabilityChecker = new ModeAvailabilityChecker(this.scenarioData, vehiclesTracker);
		
		/*
		 * Select a households meeting point respecting its members positions
		 * and their expected evacuation times.
		 */
		this.initHouseholdMeetingPointSelector();
		
		/*
		 * Write vehicles to file.
		 */
		new VehicleWriterV1(scenarioData.getVehicles()).writeFile(this.getControlerIO().getOutputFilename(FILENAME_VEHICLES));
			
		/*
		 * Assign vehicles to agent's plans.
		 */
		this.assignVehiclesToPlans = new AssignVehiclesToPlans(this.scenarioData, this.getTripRouterFactory().instantiateAndConfigureTripRouter());
		for (Household household : ((ScenarioImpl) scenarioData).getHouseholds().getHouseholds().values()) {
			this.assignVehiclesToPlans.run(household);
		}
		this.assignVehiclesToPlans.printStatistics();
		
		/*
		 * Use a MobsimFactory which creates vehicles according to available vehicles per
		 * household and adds the replanning Manager as mobsim engine.
		 */
		MobsimFactory mobsimFactory = new EvacuationQSimFactory(this.getWithinDayEngine(), this.jointDepartureOrganizer);
		this.setMobsimFactory(mobsimFactory);
		
		/*
		 * Create the set of analyzed modes.
		 */
		Set<String> analyzedModes = new HashSet<String>();
		analyzedModes.add(TransportMode.bike);
		analyzedModes.add(TransportMode.car);
		analyzedModes.add(TransportMode.pt);
		analyzedModes.add(TransportMode.ride);
		analyzedModes.add(TransportMode.walk);
		analyzedModes.add(OldPassengerDepartureHandler.passengerTransportMode);

		/*
		 * intialize analyse modules
		 */
		// Create txt and kmz files containing distribution of evacuation times. 
		if (EvacuationConfig.createEvacuationTimePicture) {
			evacuationTimePicture = new EvacuationTimePicture(scenarioData, coordAnalyzer.createInstance(), householdsTracker, vehiclesTracker);
			this.addControlerListener(evacuationTimePicture);
			this.getFixedOrderSimulationListener().addSimulationListener(evacuationTimePicture);
			this.events.addHandler(evacuationTimePicture);	
		}
		
		// Create and add an AgentsInEvacuationAreaCounter.
		if (EvacuationConfig.countAgentsInEvacuationArea) {
			double scaleFactor = 1 / this.config.getQSimConfigGroup().getFlowCapFactor();
			agentsInEvacuationAreaCounter = new AgentsInEvacuationAreaCounter(this.scenarioData, analyzedModes, coordAnalyzer.createInstance(), 
					this.decisionDataProvider, scaleFactor);
			this.addControlerListener(agentsInEvacuationAreaCounter);
			this.getFixedOrderSimulationListener().addSimulationListener(agentsInEvacuationAreaCounter);
			this.events.addHandler(agentsInEvacuationAreaCounter);
			
			agentsInEvacuationAreaActivityCounter = new AgentsInEvacuationAreaActivityCounter(this.scenarioData, coordAnalyzer.createInstance(), 
					this.decisionDataProvider, scaleFactor);
			this.addControlerListener(agentsInEvacuationAreaActivityCounter);
			this.getFixedOrderSimulationListener().addSimulationListener(agentsInEvacuationAreaActivityCounter);
			this.events.addHandler(agentsInEvacuationAreaActivityCounter);
			
			this.agentsReturnHomeCounter = new AgentsReturnHomeCounter(this.scenarioData, analyzedModes, coordAnalyzer.createInstance(), 
					this.decisionDataProvider, scaleFactor);
			this.addControlerListener(this.agentsReturnHomeCounter);
			this.getFixedOrderSimulationListener().addSimulationListener(agentsReturnHomeCounter);
			this.events.addHandler(agentsReturnHomeCounter);
		}
				
		// initialize the Identifiers here because some of them have to be registered as SimulationListeners
		this.initIdentifiers();
		
//		// debugging - initialize events comparator
//		if (referenceEventsFile != null) {
//			OnlineEventsComparator comparator = new OnlineEventsComparator(referenceEventsFile);
//			this.getEvents().addHandler(comparator);
//			this.addControlerListener(comparator);
//			comparator.run();			
//		}
	}
		
	/*
	 * Initialize tool to select household meeting points.
	 * We don't add any fuzzy values or additional disutility to travel time and costs
	 * since households select their meeting point just when the evacuation
	 * has started based on their experience from "normal" days.
	 */
	private void initHouseholdMeetingPointSelector() {
			
		Map<String, TravelTime> travelTimes = new HashMap<String, TravelTime>();
		travelTimes.put(TransportMode.walk, walkTravelTime);
		travelTimes.put(TransportMode.bike, bikeTravelTime);
		travelTimes.put(TransportMode.pt, ptTravelTime);
		travelTimes.put(TransportMode.ride, rideTravelTime);

		// set the TravelTimeCollector for car mode
		travelTimes.put(TransportMode.car, this.getTravelTimeCollector());
	
		this.selectHouseholdMeetingPoint = new SelectHouseholdMeetingPoint(this, travelTimes, 
				this.vehiclesTracker, this.coordAnalyzer.createInstance(), this.affectedArea, 
				this.modeAvailabilityChecker.createInstance(), this.informedHouseholdsTracker, this.decisionDataProvider,
				this.decisionModelRunner);
		this.getFixedOrderSimulationListener().addSimulationListener(this.selectHouseholdMeetingPoint);
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
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		if (this.informedHouseholdsTracker.allAgentsInformed()) {
			if (this.initialReplannerFactories != null) {
				
				for (WithinDayReplannerFactory<?> factory : this.initialReplannerFactories) {
					if (factory instanceof WithinDayDuringActivityReplannerFactory) {
						this.getWithinDayEngine().removeDuringActivityReplannerFactory((WithinDayDuringActivityReplannerFactory) factory);
					} else if(factory instanceof WithinDayDuringLegReplannerFactory) {
						this.getWithinDayEngine().removeDuringLegReplannerFactory((WithinDayDuringLegReplannerFactory) factory);
					} 
				}
				log.info("Disabled initial within-day replanners");
				
				this.initialReplannerFactories = null;
			}
		}
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		
		// write car travel times
		TravelTimesWriter travelTimesWriter = new TravelTimesWriter(this.getLinkTravelTimes(), this.getNetwork(), config.travelTimeCalculator());
		travelTimesWriter.collectTravelTimes();
		
		String absoluteTravelTimesFile = this.getControlerIO().getIterationFilename(0, TravelTimesWriter.travelTimesAbsoluteFile);
		String relativeTravelTimesFile = this.getControlerIO().getIterationFilename(0, TravelTimesWriter.travelTimesRelativeFile);
		travelTimesWriter.writeAbsoluteTravelTimes(absoluteTravelTimesFile);
		travelTimesWriter.writeRelativeTravelTimes(relativeTravelTimesFile);
		
		String absoluteSHPTravelTimesFile = this.getControlerIO().getIterationFilename(0, TravelTimesWriter.travelTimesAbsoluteSHPFile);
		String relativeSHPTravelTimesFile = this.getControlerIO().getIterationFilename(0, TravelTimesWriter.travelTimesRelativeSHPFile);
		travelTimesWriter.writeAbsoluteSHPTravelTimes(absoluteSHPTravelTimesFile, MGC.getCRS(config.global().getCoordinateSystem()), true);
		travelTimesWriter.writeRelativeSHPTravelTimes(relativeSHPTravelTimesFile, MGC.getCRS(config.global().getCoordinateSystem()), true);
		
//		householdsUtils.printStatistics();
//		householdsUtils.printClosingStatistics();
		
//		householdVehiclesTracker.printClosingStatistics();
	}

	protected void initIdentifiers() {
		
		/*
		 * Initialize AgentFilters
		 */
		AgentFilterFactory initialReplanningFilterFactory = new InformedAgentsFilterFactory(this.informedHouseholdsTracker, FilterType.InitialReplanning);
		AgentFilterFactory notInitialReplanningFilterFactory = new InformedAgentsFilterFactory(this.informedHouseholdsTracker, FilterType.NotInitialReplanning);
		
		Set<String> nonPTModes = new HashSet<String>();
		nonPTModes.add(TransportMode.car);
		nonPTModes.add(TransportMode.ride);
		nonPTModes.add(TransportMode.bike);
		nonPTModes.add(TransportMode.walk);
		AgentFilterFactory nonPTLegAgentsFilterFactory = new TransportModeFilterFactory(nonPTModes);
		this.getMobsimListeners().add((TransportModeFilterFactory) nonPTLegAgentsFilterFactory);
		
		DuringActivityIdentifierFactory duringActivityFactory;
		DuringLegIdentifierFactory duringLegFactory;
		
		/*
		 * During Activity Identifiers
		 */
		duringActivityFactory = new ActivityPerformingIdentifierFactory(this.getActivityReplanningMap());
		duringActivityFactory.addAgentFilterFactory(initialReplanningFilterFactory);
		this.activityPerformingIdentifier = duringActivityFactory.createIdentifier();
		
		duringActivityFactory = new JoinedHouseholdsIdentifierFactory(this.scenarioData, this.selectHouseholdMeetingPoint, 
				this.coordAnalyzer.createInstance(), this.vehiclesTracker, this.householdsTracker, this.informedHouseholdsTracker,
				this.modeAvailabilityChecker.createInstance(), this.decisionDataProvider);
		duringActivityFactory.addAgentFilterFactory(notInitialReplanningFilterFactory);
		this.joinedHouseholdsIdentifier = duringActivityFactory.createIdentifier();
		this.getFixedOrderSimulationListener().addSimulationListener((JoinedHouseholdsIdentifier) this.joinedHouseholdsIdentifier);

		
		/*
		 * During Leg Identifiers
		 */
		duringLegFactory = new LegPerformingIdentifierFactory(this.getLinkReplanningMap());
		duringLegFactory.addAgentFilterFactory(initialReplanningFilterFactory);
		this.legPerformingIdentifier = duringLegFactory.createIdentifier();

		duringLegFactory = new AgentsToDropOffIdentifierFactory(this.scenarioData, this.coordAnalyzer, this.vehiclesTracker); 
		duringLegFactory.addAgentFilterFactory(notInitialReplanningFilterFactory);
		this.agentsToDropOffIdentifier = duringLegFactory.createIdentifier();
		this.getEvents().addHandler((AgentsToDropOffIdentifier) this.agentsToDropOffIdentifier);
		this.getFixedOrderSimulationListener().addSimulationListener((AgentsToDropOffIdentifier) this.agentsToDropOffIdentifier);
		
		duringLegFactory = new AgentsToPickupIdentifierFactory(this.scenarioData, this.coordAnalyzer, this.vehiclesTracker, 
				this.walkTravelTime, this.informedHouseholdsTracker, this.decisionDataProvider); 
		duringLegFactory.addAgentFilterFactory(notInitialReplanningFilterFactory);
		this.agentsToPickupIdentifier = duringLegFactory.createIdentifier();
		this.getEvents().addHandler((AgentsToPickupIdentifier) this.agentsToPickupIdentifier);
		this.getFixedOrderSimulationListener().addSimulationListener((AgentsToPickupIdentifier) this.agentsToPickupIdentifier);
		
//		Set<String> duringLegRerouteTransportModes = new HashSet<String>();
//		duringLegRerouteTransportModes.add(TransportMode.car);
//		this.duringLegRerouteIdentifier = new LeaveLinkIdentifierFactory(this.getLinkReplanningMap(), duringLegRerouteTransportModes).createIdentifier();
		// replan all transport modes except PT
		duringLegFactory = new LeaveLinkIdentifierFactory(this.getLinkReplanningMap()); 
		duringLegFactory.addAgentFilterFactory(notInitialReplanningFilterFactory);
		duringLegFactory.addAgentFilterFactory(nonPTLegAgentsFilterFactory);
		duringLegFactory.addAgentFilterFactory(new ProbabilityFilterFactory(EvacuationConfig.duringLegReroutingShare));
		this.duringLegRerouteIdentifier = duringLegFactory.createIdentifier();
		
		duringActivityFactory = null;
		duringLegFactory = null;
	}
	
	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalized Router.
	 */
	protected void initReplanners(QSim sim) {
		
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) sim.getScenario().getPopulation().getFactory()).getModeRouteFactory();
		
		TravelTime carTravelTime = null;
		
		// if fuzzy travel times should be used
		if (EvacuationConfig.useFuzzyTravelTimes) {
			FuzzyTravelTimeEstimatorFactory fuzzyTravelTimeEstimatorFactory = new FuzzyTravelTimeEstimatorFactory(this.scenarioData, this.getTravelTimeCollector(), this.householdsTracker, this.vehiclesTracker);
			carTravelTime = fuzzyTravelTimeEstimatorFactory.createTravelTime();
		} else {
			carTravelTime = this.getTravelTimeCollector();
		}

		Map<String, TravelTime> travelTimes = new HashMap<String, TravelTime>();
		travelTimes.put(TransportMode.walk, walkTravelTime);
		travelTimes.put(TransportMode.bike, bikeTravelTime);
		travelTimes.put(TransportMode.pt, ptTravelTime);
		travelTimes.put(TransportMode.ride, rideTravelTime);
		// travelTimes.put(TransportMode.car, carTravelTime);

		
		// add time dependent penalties to travel costs within the affected area
		TravelDisutilityFactory costFactory = new OnlyTimeDependentTravelCostCalculatorFactory();
		TravelDisutilityFactory penaltyCostFactory = new PenaltyTravelCostFactory(costFactory, penaltyCalculator);
		
		LeastCostPathCalculatorFactory nonPanicFactory = new FastAStarLandmarksFactory(this.network, new FreespeedTravelTimeAndDisutility(this.config.planCalcScore()));
		LeastCostPathCalculatorFactory panicFactory = new RandomCompassRouterFactory(EvacuationConfig.tabuSearch, EvacuationConfig.compassProbability);
		LeastCostPathCalculatorFactory factory = new LeastCostPathCalculatorSelectorFactory(nonPanicFactory, panicFactory, this.decisionDataProvider);
		
		TripRouterFactoryInternal delegate = new TripRouterFactoryImpl(this.scenarioData, penaltyCostFactory, carTravelTime, factory, this.getTransitRouterFactory());
		WithindayMultimodalTripRouterFactory tripRouterFactory = new WithindayMultimodalTripRouterFactory(this, travelTimes, delegate);
		this.getWithinDayEngine().setTripRouterFactory(tripRouterFactory);
		
		/*
		 * During Activity Replanners
		 */
		this.currentActivityToMeetingPointReplannerFactory = new CurrentActivityToMeetingPointReplannerFactory(this.scenarioData, this.getWithinDayEngine(), this.decisionDataProvider, 
				this.modeAvailabilityChecker, (SwissPTTravelTime) ptTravelTime);
		this.currentActivityToMeetingPointReplannerFactory.addIdentifier(this.activityPerformingIdentifier);
		this.getWithinDayEngine().addTimedDuringActivityReplannerFactory(this.currentActivityToMeetingPointReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
		
		this.joinedHouseholdsReplannerFactory = new JoinedHouseholdsReplannerFactory(this.scenarioData, this.getWithinDayEngine(), decisionDataProvider,
				(JoinedHouseholdsIdentifier) joinedHouseholdsIdentifier, (SwissPTTravelTime) this.ptTravelTime);
		this.joinedHouseholdsReplannerFactory.addIdentifier(joinedHouseholdsIdentifier);
		this.getWithinDayEngine().addTimedDuringActivityReplannerFactory(this.joinedHouseholdsReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);

		/*
		 * During Leg Replanners
		 */
		this.currentLegToMeetingPointReplannerFactory = new CurrentLegToMeetingPointReplannerFactory(this.scenarioData, this.getWithinDayEngine(), decisionDataProvider);
		this.currentLegToMeetingPointReplannerFactory.addIdentifier(this.legPerformingIdentifier);
		this.getWithinDayEngine().addTimedDuringLegReplannerFactory(this.currentLegToMeetingPointReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);

		this.dropOffAgentsReplannerFactory = new DropOffAgentReplannerFactory(this.scenarioData, this.getWithinDayEngine());
		this.dropOffAgentsReplannerFactory.addIdentifier(this.agentsToDropOffIdentifier);
		this.getWithinDayEngine().addTimedDuringLegReplannerFactory(this.dropOffAgentsReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
		
		this.pickupAgentsReplannerFactory = new PickupAgentReplannerFactory(this.scenarioData, this.getWithinDayEngine());
		this.pickupAgentsReplannerFactory.addIdentifier(this.agentsToPickupIdentifier);
		this.getWithinDayEngine().addTimedDuringLegReplannerFactory(this.pickupAgentsReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
		
		this.duringLegRerouteReplannerFactory = new CurrentLegReplannerFactory(this.scenarioData, this.getWithinDayEngine());
		this.duringLegRerouteReplannerFactory.addIdentifier(this.duringLegRerouteIdentifier);
		this.getWithinDayEngine().addTimedDuringLegReplannerFactory(this.duringLegRerouteReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
		
		
		/*
		 * Collect Replanners that can be disabled after all agents have been informed.
		 */
		this.initialReplannerFactories = new ArrayList<WithinDayReplannerFactory<?>>();
		this.initialReplannerFactories.add(this.currentActivityToMeetingPointReplannerFactory);
		this.initialReplannerFactories.add(this.currentLegToMeetingPointReplannerFactory);
	}

	/*
	 * The LinkReplanningMap calculates the earliest link exit time for each agent.
	 * Replanning are allowed if an agent is not longer than this time traveling on
	 * a link.
	 * We use walk- and bike travel times based on person's and link's attributes and
	 * free speed travel time calculators for car, ride and pt travel times.
	 */
	private Map<String, TravelTime> createLinkReplanningMapTravelTimes() {
		
		// create a copy of the MultiModalTravelTimeWrapperFactory and set a FreeSpeedTravelTimeCalculator for car mode
		Map<String, TravelTime> travelTimes = new HashMap<String, TravelTime>();

		travelTimes.put(TransportMode.walk, this.walkTravelTime);
		travelTimes.put(TransportMode.bike, this.bikeTravelTime);
		travelTimes.put(TransportMode.car, new FreeSpeedTravelTime());
		travelTimes.put(TransportMode.ride, new FreeSpeedTravelTime());
		travelTimes.put(TransportMode.pt, new FreeSpeedTravelTime());

		return travelTimes;
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
