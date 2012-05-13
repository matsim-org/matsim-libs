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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Module;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.MultiModalTravelTime;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.MultiModalTravelTimeWrapperFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.PTTravelTimeFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.RideTravelTimeFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.TravelTimeFactoryWrapper;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.PersonalizableTravelTimeFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.OnlyTimeDependentScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTimeCalculator;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.households.Household;
import org.matsim.matrices.Matrix;
//import org.matsim.utils.eventsfilecomparison.OnlineEventsComparator;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.withinday.controller.WithinDayController;
import org.matsim.withinday.replanning.identifiers.ActivityPerformingIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.LegPerformingIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import org.matsim.withinday.replanning.modules.ReplanningModule;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayReplannerFactory;

import playground.balmermi.world.Layer;
import playground.christoph.evacuation.analysis.AgentsInEvacuationAreaCounter;
import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.analysis.EvacuationTimePicture;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.config.EvacuationConfigReader;
import playground.christoph.evacuation.mobsim.EvacuationQSimFactory;
import playground.christoph.evacuation.mobsim.HouseholdsTracker;
import playground.christoph.evacuation.mobsim.LegModeChecker;
import playground.christoph.evacuation.mobsim.PassengerDepartureHandler;
import playground.christoph.evacuation.mobsim.VehiclesTracker;
import playground.christoph.evacuation.network.AddExitLinksToNetwork;
import playground.christoph.evacuation.network.AddZCoordinatesToNetwork;
import playground.christoph.evacuation.router.util.AffectedAreaPenaltyCalculator;
import playground.christoph.evacuation.router.util.FuzzyTravelTimeEstimatorFactory;
import playground.christoph.evacuation.router.util.PenaltyTravelCostFactory;
import playground.christoph.evacuation.trafficmonitoring.BikeTravelTimeFactory;
import playground.christoph.evacuation.trafficmonitoring.PTTravelTimeKTIFactory;
import playground.christoph.evacuation.trafficmonitoring.WalkTravelTimeFactory;
import playground.christoph.evacuation.vehicles.AssignVehiclesToPlans;
import playground.christoph.evacuation.vehicles.CreateVehiclesForHouseholds;
import playground.christoph.evacuation.vehicles.HouseholdVehicleAssignmentReader;
import playground.christoph.evacuation.withinday.replanning.identifiers.AgentsToPickupIdentifier;
import playground.christoph.evacuation.withinday.replanning.identifiers.AgentsToPickupIdentifierFactory;
import playground.christoph.evacuation.withinday.replanning.identifiers.InformedAgentsFilter.FilterType;
import playground.christoph.evacuation.withinday.replanning.identifiers.InformedAgentsFilterFactory;
import playground.christoph.evacuation.withinday.replanning.identifiers.InformedHouseholdsTracker;
import playground.christoph.evacuation.withinday.replanning.identifiers.JoinedHouseholdsIdentifier;
import playground.christoph.evacuation.withinday.replanning.identifiers.JoinedHouseholdsIdentifierFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.CurrentActivityToMeetingPointReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.CurrentLegToMeetingPointReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.JoinedHouseholdsReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.PickupAgentReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.utils.ModeAvailabilityChecker;
import playground.christoph.evacuation.withinday.replanning.utils.SHPFileUtil;
import playground.christoph.evacuation.withinday.replanning.utils.SelectHouseholdMeetingPoint;
import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;
import playground.meisterk.kti.router.SwissHaltestelle;
import playground.meisterk.kti.router.SwissHaltestellen;

import com.vividsolutions.jts.geom.Geometry;

public class EvacuationControler extends WithinDayController implements MobsimInitializedListener, 
		MobsimAfterSimStepListener, IterationStartsListener, StartupListener, AfterMobsimListener {

//	public static final String referenceEventsFile = "../../matsim/mysimulations/census2000V2/output_10pct_evac_reference/ITERS/it.0/evac.1.0.events.xml.gz";
	public static final String referenceEventsFile = null;

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
	 * ReplannerFactories
	 */
	protected WithinDayDuringActivityReplannerFactory currentActivityToMeetingPointReplannerFactory;
	protected WithinDayDuringActivityReplannerFactory joinedHouseholdsReplannerFactory;
	protected WithinDayDuringLegReplannerFactory currentLegToMeetingPointReplannerFactory;
	protected WithinDayDuringLegReplannerFactory pickupAgentsReplannerFactory;
	protected WithinDayDuringLegReplannerFactory duringLegRerouteReplannerFactory;
	
	/*
	 * Replanners that are used to adapt agent's plans for the first time. They can be disabled
	 * after all agents have been informed and have adapted their plans.
	 */
	protected List<WithinDayReplannerFactory<?>> initialReplannerFactories;
	
	protected double duringLegRerouteShare = 0.10;
	
	protected AddZCoordinatesToNetwork zCoordinateAdder;
	protected ObjectAttributes householdObjectAttributes;
//	protected HouseholdsUtils householdsUtils;
	protected HouseholdVehicleAssignmentReader householdVehicleAssignmentReader;
//	protected HouseholdVehiclesTracker householdVehiclesTracker;
	
	protected CreateVehiclesForHouseholds createVehiclesForHouseholds;
	protected AssignVehiclesToPlans assignVehiclesToPlans;
	
	protected InformedHouseholdsTracker informedHouseholdsTracker;
	protected SelectHouseholdMeetingPoint selectHouseholdMeetingPoint;
	protected ModeAvailabilityChecker modeAvailabilityChecker;
	protected PassengerDepartureHandler passengerDepartureHandler;
	protected AffectedAreaPenaltyCalculator penaltyCalculator;
	protected HouseholdsTracker householdsTracker;
	protected VehiclesTracker vehiclesTracker;
	protected CoordAnalyzer coordAnalyzer;
	protected Geometry affectedArea;

	protected PersonalizableTravelTimeFactory walkTravelTimeFactory;
	protected PersonalizableTravelTimeFactory bikeTravelTimeFactory;
	protected PersonalizableTravelTimeFactory ptTravelTimeFactory;
	protected PersonalizableTravelTimeFactory travelTimeCollectorWrapperFactory;
	
	protected KtiConfigGroup ktiConfigGroup;
	
	/*
	 * Analysis modules
	 */
	protected EvacuationTimePicture evacuationTimePicture;
	protected AgentsInEvacuationAreaCounter agentsInEvacuationAreaCounter;
	
	static final Logger log = Logger.getLogger(EvacuationControler.class);

	public EvacuationControler(String[] args) {
		super(args);
		
		/*
		 * Create the empty object. They are filled in the loadData() method.
		 */
		this.ktiConfigGroup = new KtiConfigGroup();
		
		new EvacuationConfigReader().readFile(args[1]);
		
		// Use a Scoring Function, that only scores the travel times!
		this.setScoringFunctionFactory(new OnlyTimeDependentScoringFunctionFactory());
		
		// register this as a Controller and Simulation Listener
		super.getFixedOrderSimulationListener().addSimulationListener(this);
		super.addControlerListener(this);
	}

	@Override
	protected void loadData() {
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
		
		// connect facilities to links
		new WorldConnectLocations(this.config).connectFacilitiesWithLinks(getFacilities(), (NetworkImpl) getNetwork());

		// Add Rescue Links to Network
		new AddExitLinksToNetwork(this.scenarioData).createExitLinks();

		// Add secure Facilities to secure Links.
//		new AddSecureFacilitiesToNetwork(this.scenarioData).createSecureFacilities();
		
		// Add pickup facilities to Links.
		addPickupFacilities();
		
		// load household object attributes
		this.householdObjectAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(householdObjectAttributes).parse(EvacuationConfig.householdObjectAttributesFile);
		
		/*
		 * Adding z-coordinates to the network
		 */
		zCoordinateAdder = new AddZCoordinatesToNetwork(this.scenarioData, EvacuationConfig.dhm25File, EvacuationConfig.srtmFile);
		zCoordinateAdder.addZCoordinatesToNetwork();
		zCoordinateAdder.checkSteepness();

		/*
		 * Initialize TravelTimeCollector and create a FactoryWrapper which will act as
		 * factory but returns always the same travel time object, which is possible since
		 * the TravelTimeCollector is not personalized.
		 */
		Set<String> analyzedModes = new HashSet<String>();
		analyzedModes.add(TransportMode.car);
		super.createAndInitTravelTimeCollector(analyzedModes);
		travelTimeCollectorWrapperFactory = new TravelTimeFactoryWrapper(this.getTravelTimeCollector());
		
		/*
		 * Use advanced walk-, bike and pt travel time calculators
		 */
		this.walkTravelTimeFactory = new WalkTravelTimeFactory(this.config.plansCalcRoute());
		this.bikeTravelTimeFactory = new BikeTravelTimeFactory(this.config.plansCalcRoute());
		this.ptTravelTimeFactory = new PTTravelTimeKTIFactory(this.scenarioData, 
				new PTTravelTimeFactory(this.config.plansCalcRoute(), travelTimeCollectorWrapperFactory, walkTravelTimeFactory));
		this.getMultiModalTravelTimeWrapperFactory().setPersonalizableTravelTimeFactory(TransportMode.walk, walkTravelTimeFactory);
		this.getMultiModalTravelTimeWrapperFactory().setPersonalizableTravelTimeFactory(TransportMode.bike, bikeTravelTimeFactory);
		this.getMultiModalTravelTimeWrapperFactory().setPersonalizableTravelTimeFactory(TransportMode.pt, ptTravelTimeFactory);
		
		/*
		 * Use the TravelTimeCollector as ride travel time estimator
		 */
//		this.getMultiModalTravelTimeWrapperFactory().setPersonalizableTravelTimeFactory(TransportMode.pt, 
//				new PTTravelTimeFactory(this.config.plansCalcRoute(), travelTimeCollectorWrapperFactory, walkTravelTimeFactory));
		this.getMultiModalTravelTimeWrapperFactory().setPersonalizableTravelTimeFactory(TransportMode.ride, 
				new RideTravelTimeFactory(travelTimeCollectorWrapperFactory, walkTravelTimeFactory));
		
//		this.householdsUtils = new HouseholdsUtils(this.scenarioData, this.getEvents());
//		this.getEvents().addHandler(householdsUtils);
//		this.getFixedOrderSimulationListener().addSimulationListener(householdsUtils);
//		this.householdsUtils.printStatistics();
		
		Set<Feature> features = new HashSet<Feature>();
		SHPFileUtil util = new SHPFileUtil();
		for (String file : EvacuationConfig.evacuationArea) {
			features.addAll(util.readFile(file));		
		}
		affectedArea = util.mergeGeomgetries(features);
		log.info("Size of affected area: " + affectedArea.getArea());
		
		this.penaltyCalculator = new AffectedAreaPenaltyCalculator(this.getNetwork(), affectedArea, 
				EvacuationConfig.affectedAreaDistanceBuffer, EvacuationConfig.affectedAreaTimePenaltyFactor);
		
		this.coordAnalyzer = new CoordAnalyzer(affectedArea);
		
		this.informedHouseholdsTracker = new InformedHouseholdsTracker(this.scenarioData.getHouseholds(), 
				this.scenarioData.getPopulation().getPersons().keySet(), this.getEvents());
		this.getFixedOrderSimulationListener().addSimulationListener(informedHouseholdsTracker);
		
		this.householdsTracker = new HouseholdsTracker(this.householdObjectAttributes);
		this.getEvents().addHandler(householdsTracker);
		this.getFixedOrderSimulationListener().addSimulationListener(householdsTracker);
		
		this.vehiclesTracker = new VehiclesTracker(this.getEvents());
		this.getEvents().addHandler(vehiclesTracker);
		this.getFixedOrderSimulationListener().addSimulationListener(vehiclesTracker);
		
		this.passengerDepartureHandler = new PassengerDepartureHandler(this.getEvents(), vehiclesTracker);
		
		/*
		 * Update PT Travel time Matrices (requires CoordAnalyzer)
		 */
		initKTIPTRouter();
		
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
		this.assignVehiclesToPlans = new AssignVehiclesToPlans(this.scenarioData, this.createRoutingAlgorithm());
		for (Household household : ((ScenarioImpl) scenarioData).getHouseholds().getHouseholds().values()) {
			this.assignVehiclesToPlans.run(household);
		}
		this.assignVehiclesToPlans.printStatistics();
		
		/*
		 * Use a MobsimFactory which creates vehicles according to available vehicles per
		 * household and adds the replanning Manager as mobsim engine.
		 */
		MobsimFactory mobsimFactory = new EvacuationQSimFactory(this.passengerDepartureHandler, this.getReplanningManager());
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
			agentsInEvacuationAreaCounter = new AgentsInEvacuationAreaCounter(this.scenarioData, transportModes, coordAnalyzer.createInstance(), scaleFactor);
			this.addControlerListener(agentsInEvacuationAreaCounter);
			this.getFixedOrderSimulationListener().addSimulationListener(agentsInEvacuationAreaCounter);
			this.events.addHandler(agentsInEvacuationAreaCounter);	
		}
		
		/*
		 * Create and initialize replanning manager and replanning maps.
		 */
		super.initReplanningManager(numReplanningThreads);
		super.getReplanningManager().setEventsManager(this.getEvents());	// set events manager to create replanning events
		super.createAndInitActivityReplanningMap();
		MultiModalTravelTime linkReplanningTravelTime = this.createLinkReplanningMapTravelTime();
		super.createAndInitLinkReplanningMap(linkReplanningTravelTime);
				
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
	 * Update PT Travel time Matrices
	 */
	private void initKTIPTRouter() {
		PlansCalcRouteKtiInfo pcrKTIi = ((PTTravelTimeKTIFactory) this.ptTravelTimeFactory).getPlansCalcRouteKtiInfo();
		/*
		 * Identify affected stops and zones
		 */
		SwissHaltestellen swissHaltestellen = pcrKTIi.getHaltestellen();
		Layer municipalities = pcrKTIi.getLocalWorld().getLayer("municipality");
		Set<Id> affectedMunicipalities = new LinkedHashSet<Id>();
		for (SwissHaltestelle swissHaltestelle : swissHaltestellen.getHaltestellenMap().values()) {
			Coord coord = swissHaltestelle.getCoord();
			boolean isAffected = this.coordAnalyzer.isCoordAffected(coord);
			
			if (isAffected) {
				for (BasicLocation location : municipalities.getNearestLocations(coord)) {
					affectedMunicipalities.add(location.getId());
				}
			}
		}
		log.info("total municipalities: " + municipalities.getLocations().size());
		log.info("affected municipalities: " + affectedMunicipalities.size());
		
		/*
		 * Create exit/rescue entry in the municipalities and haltestellen data structures
		 */
		log.info("creating additional entries in the PT travel time matrix...");
		Id rescueFacilityId = scenarioData.createId("rescueFacility");
		Id rescueLinkId = scenarioData.getActivityFacilities().getFacilities().get(rescueFacilityId).getLinkId();
		Link rescueLink = scenarioData.getNetwork().getLinks().get(rescueLinkId);
		municipalities.getLocations().put(rescueLinkId, rescueLink);
		swissHaltestellen.addHaltestelle(scenarioData.createId("rescueHaltestelle"), 
				EvacuationConfig.getRescueCoord().getX(), EvacuationConfig.getRescueCoord().getY());
		
		/*
		 * Create entries in the travel time matrix
		 * 
		 * If the municipality is affected, calculate the minimal travel time
		 * to the next non-affected municipality. Otherwise use a travel time of 1.0.
		 */
		Matrix ptMatrix = pcrKTIi.getPtTravelTimes();
		for (Id fromId : municipalities.getLocations().keySet()) {
			if (affectedMunicipalities.contains(fromId)) {
				
				double minTT = Double.MAX_VALUE;
				for (Id id : municipalities.getLocations().keySet()) {
					if (affectedMunicipalities.contains(id)) continue;
					else if (id.equals(rescueLinkId)) continue;
					double tt = ptMatrix.getEntry(fromId, id).getValue();
					if (tt < minTT) minTT = tt;
				}
				ptMatrix.createEntry(fromId, rescueLinkId, minTT);
			} else {
				ptMatrix.createEntry(fromId, rescueLinkId, 1.0);
			}
		}
		log.info("done.");
	}
	
	/*
	 * Initialize tool to select household meeting points.
	 * We don't add any fuzzy values or additional disutility to travel time and costs
	 * since households select their meeting point just when the evacuation
	 * has started based on their experience from "normal" days.
	 */
	private void initHouseholdMeetingPointSelector() {
				
		// create a copy of the MultiModalTravelTimeWrapperFactory...
		MultiModalTravelTimeWrapperFactory timeFactory = new MultiModalTravelTimeWrapperFactory();
		for (Entry<String, PersonalizableTravelTimeFactory> entry : this.getMultiModalTravelTimeWrapperFactory().getPersonalizableTravelTimeFactories().entrySet()) {
			timeFactory.setPersonalizableTravelTimeFactory(entry.getKey(), entry.getValue());			
		}
		// ... and set the TravelTimeCollector for car mode
		timeFactory.setPersonalizableTravelTimeFactory(TransportMode.car, this.travelTimeCollectorWrapperFactory);
		
		this.selectHouseholdMeetingPoint = new SelectHouseholdMeetingPoint(this.scenarioData, timeFactory, 
				this.householdsTracker, this.vehiclesTracker, this.coordAnalyzer.createInstance(), this.affectedArea, 
				this.modeAvailabilityChecker.createInstance(), this.informedHouseholdsTracker);
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
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		
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
	}
	
	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		if (this.informedHouseholdsTracker.allAgentsInformed()) {
			if (this.initialReplannerFactories != null) {
				
				for (WithinDayReplannerFactory<?> factory : this.initialReplannerFactories) {
					if (factory instanceof WithinDayDuringActivityReplannerFactory) {
						this.getReplanningManager().removeDuringActivityReplannerFactory((WithinDayDuringActivityReplannerFactory) factory);
					} else if(factory instanceof WithinDayDuringLegReplannerFactory) {
						this.getReplanningManager().removeDuringLegReplannerFactory((WithinDayDuringLegReplannerFactory) factory);
					} 
				}
				log.info("Disabled initial within-day replanners");
				
				this.initialReplannerFactories = null;
			}
		}
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
//		householdsUtils.printStatistics();
//		householdsUtils.printClosingStatistics();
		
//		householdVehiclesTracker.printClosingStatistics();
	}

	protected void initIdentifiers() {
		
		/*
		 * Initialize AgentFilters
		 */
		InformedAgentsFilterFactory initialReplanningFilterFactory = new InformedAgentsFilterFactory((this.informedHouseholdsTracker), FilterType.InitialReplanning);
		InformedAgentsFilterFactory notInitialReplanningFilterFactory = new InformedAgentsFilterFactory((this.informedHouseholdsTracker), FilterType.NotInitialReplanning);
		
		
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
				this.modeAvailabilityChecker.createInstance());
		duringActivityFactory.addAgentFilterFactory(notInitialReplanningFilterFactory);
		this.joinedHouseholdsIdentifier = duringActivityFactory.createIdentifier();
		this.getFixedOrderSimulationListener().addSimulationListener((JoinedHouseholdsIdentifier) this.joinedHouseholdsIdentifier);

		
		/*
		 * During Leg Identifiers
		 */
		duringLegFactory = new LegPerformingIdentifierFactory(this.getLinkReplanningMap());
		duringLegFactory.addAgentFilterFactory(initialReplanningFilterFactory);
		this.legPerformingIdentifier = duringLegFactory.createIdentifier();
		
		duringLegFactory = new AgentsToPickupIdentifierFactory(this.scenarioData, this.coordAnalyzer, this.vehiclesTracker, walkTravelTimeFactory); 
		duringLegFactory.addAgentFilterFactory(notInitialReplanningFilterFactory);
		this.agentsToPickupIdentifier = duringLegFactory.createIdentifier();
		this.getEvents().addHandler((AgentsToPickupIdentifier) this.agentsToPickupIdentifier);
		this.getFixedOrderSimulationListener().addSimulationListener((AgentsToPickupIdentifier) this.agentsToPickupIdentifier);
		
//		Set<String> duringLegRerouteTransportModes = new HashSet<String>();
//		duringLegRerouteTransportModes.add(TransportMode.car);
//		this.duringLegRerouteIdentifier = new LeaveLinkIdentifierFactory(this.getLinkReplanningMap(), duringLegRerouteTransportModes).createIdentifier();
		// replan all transport modes
		duringLegFactory = new LeaveLinkIdentifierFactory(this.getLinkReplanningMap()); 
		duringLegFactory.addAgentFilterFactory(notInitialReplanningFilterFactory);
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
		
		// use fuzzyTravelTimes
		FuzzyTravelTimeEstimatorFactory fuzzyTravelTimeEstimatorFactory = new FuzzyTravelTimeEstimatorFactory(this.scenarioData, this.travelTimeCollectorWrapperFactory, this.householdsTracker, this.vehiclesTracker);
			
		// create a copy of the MultiModalTravelTimeWrapperFactory...
		MultiModalTravelTimeWrapperFactory timeFactory = new MultiModalTravelTimeWrapperFactory();
		for (Entry<String, PersonalizableTravelTimeFactory> entry : this.getMultiModalTravelTimeWrapperFactory().getPersonalizableTravelTimeFactories().entrySet()) {
			timeFactory.setPersonalizableTravelTimeFactory(entry.getKey(), entry.getValue());			
		}
		// ... and set the TravelTimeCollector for car mode
		timeFactory.setPersonalizableTravelTimeFactory(TransportMode.car, fuzzyTravelTimeEstimatorFactory);

		// add time dependent penalties to travel costs within the affected area
		TravelDisutilityFactory costFactory = new OnlyTimeDependentTravelCostCalculatorFactory();
		TravelDisutilityFactory penaltyCostFactory = new PenaltyTravelCostFactory(costFactory, penaltyCalculator);
		
		LeastCostPathCalculatorFactory factory = new FastAStarLandmarksFactory(this.network, new FreespeedTravelTimeAndDisutility(this.config.planCalcScore()));
		AbstractMultithreadedModule router = new ReplanningModule(config, network, penaltyCostFactory, timeFactory, factory, routeFactory);
		
		/*
		 * During Activity Replanners
		 */
		this.currentActivityToMeetingPointReplannerFactory = new CurrentActivityToMeetingPointReplannerFactory(this.scenarioData, this.getReplanningManager(), router, 1.0, this.householdsTracker, 
				this.modeAvailabilityChecker, (PTTravelTimeKTIFactory) this.ptTravelTimeFactory);
		this.currentActivityToMeetingPointReplannerFactory.addIdentifier(this.activityPerformingIdentifier);
		this.getReplanningManager().addTimedDuringActivityReplannerFactory(this.currentActivityToMeetingPointReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
		
		this.joinedHouseholdsReplannerFactory = new JoinedHouseholdsReplannerFactory(this.scenarioData, this.getReplanningManager(), router, 1.0, householdsTracker,
				(JoinedHouseholdsIdentifier) joinedHouseholdsIdentifier, (PTTravelTimeKTIFactory) this.ptTravelTimeFactory);
		this.joinedHouseholdsReplannerFactory.addIdentifier(joinedHouseholdsIdentifier);
		this.getReplanningManager().addTimedDuringActivityReplannerFactory(this.joinedHouseholdsReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);

		/*
		 * During Leg Replanners
		 */
		this.currentLegToMeetingPointReplannerFactory = new CurrentLegToMeetingPointReplannerFactory(this.scenarioData, this.getReplanningManager(), router, 1.0, householdsTracker);
		this.currentLegToMeetingPointReplannerFactory.addIdentifier(this.legPerformingIdentifier);
		this.getReplanningManager().addTimedDuringLegReplannerFactory(this.currentLegToMeetingPointReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
				
		this.pickupAgentsReplannerFactory = new PickupAgentReplannerFactory(this.scenarioData, this.getReplanningManager(), router, 1.0);
		this.pickupAgentsReplannerFactory.addIdentifier(this.agentsToPickupIdentifier);
		this.getReplanningManager().addTimedDuringLegReplannerFactory(this.pickupAgentsReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
		
		this.duringLegRerouteReplannerFactory = new CurrentLegReplannerFactory(this.scenarioData, this.getReplanningManager(), router, duringLegRerouteShare);
		this.duringLegRerouteReplannerFactory.addIdentifier(this.duringLegRerouteIdentifier);
//		this.getReplanningManager().addTimedDuringLegReplanner(this.duringLegRerouteReplanner, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
		
		
		/*
		 * Collect Replanners that can be disabled after all agents have been informed.
		 */
		this.initialReplannerFactories = new ArrayList<WithinDayReplannerFactory<?>>();
		this.initialReplannerFactories.add(this.currentActivityToMeetingPointReplannerFactory);
		this.initialReplannerFactories.add(this.currentLegToMeetingPointReplannerFactory);
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
