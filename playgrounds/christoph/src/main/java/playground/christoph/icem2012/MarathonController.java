/* *********************************************************************** *
 * project: org.matsim.*
 * MarathonController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.icem2012;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.Module;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.PTTravelTimeFactory;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.TripRouterFactoryInternal;
import org.matsim.core.router.TripRouterFactoryImpl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.old.LegRouter;
import org.matsim.core.router.old.PlanRouterAdapter;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsFactory;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.withinday.controller.WithinDayController;
import org.matsim.withinday.mobsim.WithinDayQSimFactory;
import org.matsim.withinday.replanning.identifiers.ActivityPerformingIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.LegPerformingIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.filter.ProbabilityFilterFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayReplannerFactory;
import org.opengis.feature.simple.SimpleFeature;

import playground.christoph.evacuation.analysis.AgentsInEvacuationAreaCounter;
import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.api.core.v01.Coord3d;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.controler.WithindayMultimodalTripRouterFactory;
import playground.christoph.evacuation.core.utils.geometry.Coord3dImpl;
import playground.christoph.evacuation.mobsim.AgentsTracker;
import playground.christoph.evacuation.mobsim.VehiclesTracker;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.network.AddZCoordinatesToNetwork;
import playground.christoph.evacuation.trafficmonitoring.BikeTravelTimeFactory;
import playground.christoph.evacuation.trafficmonitoring.PTTravelTimeKTIEvacuationFactory;
import playground.christoph.evacuation.trafficmonitoring.SwissPTTravelTime;
import playground.christoph.evacuation.trafficmonitoring.WalkTravelTimeFactory;
import playground.christoph.evacuation.withinday.replanning.identifiers.AffectedAgentsFilter;
import playground.christoph.evacuation.withinday.replanning.identifiers.AffectedAgentsFilterFactory;
import playground.christoph.evacuation.withinday.replanning.identifiers.InformedAgentsFilter;
import playground.christoph.evacuation.withinday.replanning.identifiers.InformedAgentsFilterFactory;
import playground.christoph.evacuation.withinday.replanning.identifiers.InformedHouseholdsTracker;
import playground.christoph.evacuation.withinday.replanning.replanners.CurrentLegInitialReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.EndActivityAndEvacuateReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.ExtendCurrentActivityReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.utils.SHPFileUtil;
import playground.gregor.sim2d_v3.events.XYDataWriter;
import playground.gregor.sim2d_v3.helper.gisdebug.GisDebugger;
import playground.gregor.sim2d_v3.router.Walk2DLegRouter;
import playground.gregor.sim2d_v3.scenario.ScenarioLoader2DImpl;
import playground.gregor.sim2d_v3.simulation.HybridQ2DMobsimFactory;
import playground.gregor.sim2d_v3.simulation.Sim2DDepartureHandler;
import playground.gregor.sim2d_v3.simulation.Sim2DEngine;
import playground.gregor.sim2d_v3.simulation.floor.DefaultVelocityCalculator;
import playground.gregor.sim2d_v3.simulation.floor.VelocityCalculator;
import playground.meisterk.kti.config.KtiConfigGroup;

import com.vividsolutions.jts.geom.Geometry;

import contrib.multimodal.config.MultiModalConfigGroup;
import contrib.multimodal.router.MultimodalTripRouterFactory;

public final class MarathonController extends WithinDayController implements 
	MobsimInitializedListener, MobsimBeforeSimStepListener, MobsimAfterSimStepListener {

	private final static Logger log = Logger.getLogger(MarathonController.class);
	
	public static String basePath = "D:/Users/Christoph/workspace/matsim/mysimulations/";
//	public static String basePath = "/home/cdobler/workspace/matsim/mysimulations/";
	public static String dhm25File = basePath + "networks/GIS/nodes_3d_ivtch_dhm25.shp";
	public static String srtmFile = basePath + "networks/GIS/nodes_3d_srtm.shp";
	public static String affectedAreaFile = basePath + "icem2012/input/affectedArea.shp";
	
	/*
	 * innerBuffer ... minimum distance between affected area and an exit node
	 * outerBuffer ... maximum distance between affected area and an exit node
	 */
	private double innerBuffer = 1000.0;
	private double outerBuffer = 3000.0;
	
	private Geometry affectedArea;
	private CoordAnalyzer coordAnalyzer;
	private CoordAnalyzer bufferedCoordAnalyzer;
	private Set<Id> affectedNodes;
	private Set<Id> affectedLinks;
	private Set<Id> affectedFacilities;
	
	private AgentsTracker agentsTracker;
	private InformedHouseholdsTracker informedHouseholdsTracker;
	private VehiclesTracker vehiclesTracker;
	private DecisionDataProvider decisionDataProvider;
	private KtiConfigGroup ktiConfigGroup;
	
	/*
	 * Identifiers
	 */
	private DuringActivityIdentifier affectedActivityPerformingIdentifier;
	private DuringActivityIdentifier notAffectedActivityPerformingIdentifier;
	private DuringLegIdentifier legPerformingIdentifier;
	private DuringLegIdentifier duringLegRerouteIdentifier;
	
	/*
	 * ReplannerFactories
	 */
	private WithinDayDuringActivityReplannerFactory extendCurrentActivityReplannerFactory;
	private WithinDayDuringActivityReplannerFactory marathonEndActivityAndEvacuateReplannerFactory;
	private WithinDayDuringLegReplannerFactory currentLegInitialReplannerFactory;
	private WithinDayDuringLegReplannerFactory switchWalkModeReplannerFactory;
	private WithinDayDuringLegReplannerFactory duringLegRerouteReplannerFactory;

	/*
	 * Replanners that are used to adapt agent's plans for the first time. They can be disabled
	 * after all agents have been informed and have adapted their plans.
	 */
	private List<WithinDayReplannerFactory<?>> initialReplannerFactories;

	private TravelTime walkTravelTime;
	private TravelTime bikeTravelTime;
	private TravelTime rideTravelTime;
	private TravelTime carTravelTime;
	private TravelTime ptTravelTime;
	
	private AgentsInEvacuationAreaCounter agentsInEvacuationAreaCounter;
	
	public static void main(String[] args) {

		String configFile = args[0];
		Config c = ConfigUtils.loadConfig(configFile);

		Scenario sc = ScenarioUtils.createScenario(c);
		((PopulationFactoryImpl)sc.getPopulation().getFactory()).setRouteFactory("walk2d", new LinkNetworkRouteFactory());
		((PopulationFactoryImpl)sc.getPopulation().getFactory()).setRouteFactory(TransportMode.walk, new LinkNetworkRouteFactory());

        // TODO: Refactored out of core config
        // Please just create and add the config group instead.
        MultiModalConfigGroup multiModalConfigGroup1 = (MultiModalConfigGroup) c.getModule(MultiModalConfigGroup.GROUP_NAME);
        if (multiModalConfigGroup1 == null) {
            multiModalConfigGroup1 = new MultiModalConfigGroup();
            c.addModule(multiModalConfigGroup1);
        }
        if (multiModalConfigGroup1.isMultiModalSimulationEnabled()) {
            // TODO: Refactored out of core config
            // Please just create and add the config group instead.
            MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) c.getModule(MultiModalConfigGroup.GROUP_NAME);
            if (multiModalConfigGroup == null) {
                multiModalConfigGroup = new MultiModalConfigGroup();
                c.addModule(multiModalConfigGroup);
            }
            for (String transportMode : CollectionUtils.stringToArray(multiModalConfigGroup.getSimulatedModes())) {
				((PopulationFactoryImpl)sc.getPopulation().getFactory()).setRouteFactory(transportMode, new LinkNetworkRouteFactory());
			}	
		}
				
		ScenarioUtils.loadScenario(sc);

		Controler controller = new MarathonController(sc);
		controller.run();
	}

	public MarathonController(Scenario sc) {
		super(sc);
		setOverwriteFiles(true);
		
		/*
		 * Create the empty object. They are filled in the loadData() method.
		 */
		this.ktiConfigGroup = new KtiConfigGroup();
		
		/*
		 * Set evacuation parameter.
		 */
		EvacuationConfig.evacuationTime = 9.5 * 3600;
		EvacuationConfig.duringLegReroutingShare = 0.25;
		EvacuationConfig.panicShare = 0.0;
		EvacuationConfig.householdParticipationShare = 1.0;
		EvacuationConfig.informAgentsRayleighSigma = 600.0;
		
//		HybridQ2DMobsimFactory factory = new HybridQ2DMobsimFactory();
//		
//		// explicit set the mobsim factory
//		this.setMobsimFactory(factory);
		
		this.setNumberOfReplanningThreads(this.config.global().getNumberOfThreads());
		
		/*
		 * Initialize TravelTimeCollector and create a FactoryWrapper which will act as
		 * factory but returns always the same travel time object, which is possible since
		 * the TravelTimeCollector is not personalized.
		 */
		Set<String> analyzedModes = new HashSet<String>();
		analyzedModes.add(TransportMode.car);
		this.setModesAnalyzedByTravelTimeCollector(analyzedModes);
		
		/*
		 * Use advanced walk-, bike and pt travel time calculators
		 */
		Map<String, TravelTime> travelTimes = new HashMap<String, TravelTime>();
		travelTimes.put(TransportMode.car, this.carTravelTime);
		travelTimes.put(TransportMode.walk, this.walkTravelTime);
		travelTimes.put("walk2d", this.walkTravelTime);
		travelTimes.put(TransportMode.bike, this.bikeTravelTime);
		travelTimes.put(TransportMode.ride, this.rideTravelTime);
		travelTimes.put(TransportMode.pt, this.ptTravelTime);
		
		/*
		 * Create and initialize replanning manager and replanning maps.
		 */
		Map<String, TravelTime> linkReplanningTravelTime = this.createLinkReplanningMapTravelTime();
		super.createAndInitLinkReplanningMap(linkReplanningTravelTime);		
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
		
		/*
		 * Creating dummy households for some parts of the
		 * evacuation code.
		 */
		createHouseholds();
		
		/*
		 * Adding z-coordinates to the network
		 */
		AddZCoordinatesToNetwork zCoordinateAdder;
		zCoordinateAdder = new AddZCoordinatesToNetwork(this.scenarioData, dhm25File, srtmFile);
		zCoordinateAdder.addZCoordinatesToNetwork();
		zCoordinateAdder.checkSteepness();
		
		/*
		 * Fixing height coordinates of nodes that have been added and that
		 * are not included in the height shp files.
		 */
		for (Node node : scenarioData.getNetwork().getNodes().values()) {
			String idString = node.getId().toString(); 
			if (idString.contains("_shifted")) {
				idString = idString.replace("_shifted", "");
				Node node2 = scenarioData.getNetwork().getNodes().get(scenarioData.createId(idString));
				Coord3d coord2 = (Coord3d) node2.getCoord();
				
				Coord coord = node.getCoord();
				Coord3d coord3d = new Coord3dImpl(coord.getX(), coord.getY(), coord2.getZ());
				((NodeImpl) node).setCoord(coord3d);
			}
		}
		
		ScenarioLoader2DImpl loader = new ScenarioLoader2DImpl(this.scenarioData);
		loader.load2DScenario();
	}

	@Override
	protected void setUp() {
		super.setUp();
	
		/*
		 * Replace WithinDayQSimFactory with a CombiMobsimFactory
		 * which combines a WithinDayQSimFactory with a HybridQ2DMobsimFactory. 
		 */
		super.setMobsimFactory(new CombiMobsimFactory(new WithinDayQSimFactory(getWithinDayEngine())));
	}
	
	/*
	 * Combines a WithinDayQSimFactory and a HybridQ2DMobsimFactory
	 * to have Within-day Replanning and Sim2D in one simulation.
	 */
	private static class CombiMobsimFactory extends HybridQ2DMobsimFactory {
	
		private final WithinDayQSimFactory factory;
		private Sim2DEngine sim2DEngine = null;
		
		public CombiMobsimFactory(WithinDayQSimFactory factory) {
			this.factory = factory;
		}
		
		@Override
		public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		
			QSim qSim = factory.createMobsim(sc, eventsManager);
			
			Sim2DEngine e = new Sim2DEngine(qSim);
			this.sim2DEngine = e;
			qSim.addMobsimEngine(e);
			Sim2DDepartureHandler d = new Sim2DDepartureHandler(e);
			qSim.addDepartureHandler(d);
			
			return qSim;
		}
		
		@Override
		public Sim2DEngine getSim2DEngine() {
			return this.sim2DEngine;
		}
	}

	public PlanAlgorithm createRoutingAlgorithm(TravelDisutility travelCosts, TravelTime travelTimes) {
		
		// the contructor does not call createRoutingAlgorithm on the argument, so it is ok (td)
		PlanRouterAdapter plansCalcRoute = new PlanRouterAdapter( this );
		
		TravelTime travelTime = new WalkTravelTimeFactory(config.plansCalcRoute()).createTravelTime();
		LegRouter walk2DLegRouter = new Walk2DLegRouter(network, travelTime, (IntermodalLeastCostPathCalculator) plansCalcRoute.getLeastCostPathCalculator());
		plansCalcRoute.addLegHandler("walk2d", walk2DLegRouter);
		
		return plansCalcRoute;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		
		super.notifyStartup(event);
		
		XYDataWriter xyDataWriter = new XYDataWriter();
		this.getEvents().addHandler(xyDataWriter);
		this.addControlerListener(xyDataWriter);
		this.getMobsimListeners().add(xyDataWriter);
		
		readAffectedArea();
		
		identifyAffectedInfrastructure();
		
		createPreAndPostRunFacilities();
		
		createExitLinks();
		
		this.informedHouseholdsTracker = new InformedHouseholdsTracker(this.scenarioData.getHouseholds(), 
				this.scenarioData.getPopulation().getPersons().keySet(), this.getEvents(), EvacuationConfig.informAgentsRayleighSigma);
		this.getFixedOrderSimulationListener().addSimulationListener(informedHouseholdsTracker);
		
		this.agentsTracker = new AgentsTracker(this.scenarioData);
		this.getEvents().addHandler(agentsTracker);
		this.getFixedOrderSimulationListener().addSimulationListener(agentsTracker);
		
		// we do not use vehicles so far
//		this.vehiclesTracker = new VehiclesTracker(this.getEvents());
//		this.getEvents().addHandler(vehiclesTracker);
//		this.getFixedOrderSimulationListener().addSimulationListener(vehiclesTracker);
		
//		this.popAdmin = new PopulationAdministration(this.scenarioData);
//		this.popAdmin.selectPanicPeople(EvacuationConfig.panicShare);
//		this.popAdmin.selectParticipatingHouseholds(EvacuationConfig.householdParticipationShare);
//		this.addControlerListener(this.popAdmin);
		
		this.decisionDataProvider = new DecisionDataProvider();	
//		DecisionDataGrabber decisionDataGrabber = new DecisionDataGrabber(scenario, decisionDataProvider, coordAnalyzer, 
//				householdsTracker, householdObjectAttributes);	

		// Create the set of analyzed modes.
		Set<String> transportModes = new HashSet<String>();
		transportModes.add(TransportMode.bike);
		transportModes.add(TransportMode.car);
		transportModes.add(TransportMode.pt);
		transportModes.add(TransportMode.ride);
		transportModes.add(TransportMode.walk);
		transportModes.add("walk2d");
		
		// Create and add an AgentsInEvacuationAreaCounter
		double scaleFactor = 1 / this.config.getQSimConfigGroup().getFlowCapFactor();
		agentsInEvacuationAreaCounter = new AgentsInEvacuationAreaCounter(this.scenarioData, transportModes, coordAnalyzer.createInstance(), 
				decisionDataProvider, scaleFactor);

		this.addControlerListener(agentsInEvacuationAreaCounter);
		this.getFixedOrderSimulationListener().addSimulationListener(agentsInEvacuationAreaCounter);
		this.events.addHandler(agentsInEvacuationAreaCounter);
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
//		MobsimFactory factory = super.getMobsimFactory();
//		if (factory instanceof HybridQ2DMobsimFactory) {
//			log.info("Replacing VelocityCalculator with MarathonVelocityCalculator");
//			Sim2DEngine sim2DEngine = ((HybridQ2DMobsimFactory) factory).getSim2DEngine();
//			
//			VelocityCalculator velocityCalculator = new MarathonVelocityCalculator(
//					new DefaultVelocityCalculator(this.config.plansCalcRoute())); 
//			sim2DEngine.setVelocityCalculator(velocityCalculator);
//		}
//		
		this.initReplanners((QSim)e.getQueueSimulation());
	}
	
	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		if (EvacuationConfig.evacuationTime == e.getSimulationTime()) {
			this.updateTrackModes();
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
	
	/*
	 * Some parts of the evacuation code is based on household to make
	 * decisions on household level. To be able to use this code we create
	 * dummy households where each agent is part of a single person household.
	 */
	private void createHouseholds() {
		
		Households households = scenarioData.getHouseholds();
		HouseholdsFactory factory = households.getFactory();
		
		for (Person person : scenarioData.getPopulation().getPersons().values()) {
			Household household = factory.createHousehold(person.getId());
			household.getMemberIds().add(person.getId());
			households.getHouseholds().put(household.getId(), household);
		}
	}
	
	private void readAffectedArea() {
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		features.addAll(ShapeFileReader.getAllFeatures(affectedAreaFile));		
		SHPFileUtil util = new SHPFileUtil();
		this.affectedArea = util.mergeGeometries(features);
		
		this.coordAnalyzer = new CoordAnalyzer(this.affectedArea);
	}
	
	private void identifyAffectedInfrastructure() {
		affectedNodes = new HashSet<Id>();
		affectedLinks = new HashSet<Id>();
		affectedFacilities = new HashSet<Id>();
		
		for (Node node : scenarioData.getNetwork().getNodes().values()) {
			if (coordAnalyzer.isNodeAffected(node)) affectedNodes.add(node.getId());
		}
		for (Link link : scenarioData.getNetwork().getLinks().values()) {
			if (coordAnalyzer.isLinkAffected(link)) affectedLinks.add(link.getId());
		}
		for (Facility facility : scenarioData.getActivityFacilities().getFacilities().values()) {
			if (coordAnalyzer.isFacilityAffected(facility)) affectedFacilities.add(facility.getId());
		}
	}
	
	private void createPreAndPostRunFacilities() {
		
		Id startLinkId = scenarioData.createId(CreateMarathonPopulation.startLink);
		Id endLinkId = scenarioData.createId(CreateMarathonPopulation.endLink);
		
		Link startLink = scenarioData.getNetwork().getLinks().get(startLinkId);
		Link endLink = scenarioData.getNetwork().getLinks().get(endLinkId);
		
		Id preRunFacilityId = scenarioData.createId("preRunFacility");
		ActivityFacility preRunFacility = (scenarioData).getActivityFacilities().createAndAddFacility(preRunFacilityId, startLink.getCoord());
		((ActivityFacilityImpl) preRunFacility).setLinkId(startLinkId);
		ActivityOption activityOption = ((ActivityFacilityImpl) preRunFacility).createActivityOption("preRun");
		activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 0*3600, 24*3600));
		activityOption.setCapacity(Double.MAX_VALUE);
				
		Id postRunFacilityId = scenarioData.createId("postRunFacility");
		ActivityFacility postRunFacility = (scenarioData).getActivityFacilities().createAndAddFacility(postRunFacilityId, endLink.getCoord());
		((ActivityFacilityImpl) postRunFacility).setLinkId(startLinkId);
		activityOption = ((ActivityFacilityImpl) preRunFacility).createActivityOption("postRun");
		activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 0*3600, 24*3600));
		activityOption.setCapacity(Double.MAX_VALUE);
	}
	
	private void createExitLinks() {
		Geometry innerBuffer = affectedArea.buffer(this.innerBuffer);
		Geometry outerBuffer = affectedArea.buffer(this.outerBuffer);
		
		GisDebugger.setCRSString("EPSG: 21781");
		
		CoordAnalyzer innerAnalyzer = new CoordAnalyzer(innerBuffer);
		GisDebugger.addGeometry(innerBuffer, "inner Buffer");
		GisDebugger.dump(this.getControlerIO().getOutputFilename("affectedAreaInnerBuffer.shp"));
		
		CoordAnalyzer outerAnalyzer = new CoordAnalyzer(outerBuffer);
		GisDebugger.addGeometry(outerBuffer, "outer Buffer");
		GisDebugger.dump(this.getControlerIO().getOutputFilename("/affectedAreaOuterBuffer.shp"));
		
		this.bufferedCoordAnalyzer = new CoordAnalyzer(innerBuffer);
		
		Set<Node> exitNodes = new LinkedHashSet<Node>();
		for (Node node : scenarioData.getNetwork().getNodes().values()) {
			if (outerAnalyzer.isNodeAffected(node) && !innerAnalyzer.isNodeAffected(node)) {
				
				// if the from node of an in-link is inside the affected area, we ignore the node
				boolean ignoreNode = false;
				for (Link inLink : node.getInLinks().values()) {
					if (coordAnalyzer.isNodeAffected(inLink.getFromNode())) {
						ignoreNode = true;
						break;
					}
				}
				if (!ignoreNode) exitNodes.add(node);
			}
		}
		log.info("Found " + exitNodes.size() + " exit nodes.");
		
		/*
		 * Create first rescue coordinate. All exit nodes are connected to this node.
		 * Use a coordinate which should be outside the evacuation area but still is feasible.
		 * Otherwise the AStarLandmarks algorithm could be confused and loose some performance.
		 */
		Coord rescueNodeCoord = scenarioData.createCoord(683595.0, 244940.0); // somewhere in Lake Zurich
		Id rescueNodeId = scenarioData.createId("rescueNode");
		Node rescueNode = network.getFactory().createNode(rescueNodeId, rescueNodeCoord);
		network.addNode(rescueNode);

		Set<String> allowedTransportModes = new HashSet<String>();
		allowedTransportModes.add(TransportMode.bike);
		allowedTransportModes.add(TransportMode.car);
		allowedTransportModes.add(TransportMode.pt);
		allowedTransportModes.add(TransportMode.ride);
		allowedTransportModes.add(TransportMode.walk);
//		allowedTransportModes.add("walk2d");
		
		int counter = 0;
		for (Node node :exitNodes) {
			counter++;
			Id rescueLinkId = scenarioData.createId("rescueLink" + counter);
			Link rescueLink = network.getFactory().createLink(rescueLinkId, node, rescueNode);
			rescueLink.setNumberOfLanes(10);
			rescueLink.setLength(10);	// use short links for non-vehicular traffic
			rescueLink.setCapacity(1000000);
			rescueLink.setFreespeed(1000000);
			rescueLink.setAllowedModes(allowedTransportModes);
			network.addLink(rescueLink);
		}
		log.info("Created " + counter + " exit links.");
		
		/*
		 * Now we create a second rescue node that is connected only to the
		 * first rescue node. The link between them gets equipped with the
		 * rescue facility that is the destination of the evacuated persons.
		 */
		Coord rescueNodeCoord2 = scenarioData.createCoord(rescueNodeCoord.getX() + 1.0, 
				rescueNodeCoord.getY() + 1.0);
		Id rescueNodeId2 = scenarioData.createId("rescueNode2");
		Node rescueNode2 = network.getFactory().createNode(rescueNodeId2, rescueNodeCoord2);
		network.addNode(rescueNode2);
		
		Id rescueLinkId = scenarioData.createId("rescueLink");
		Link rescueLink = network.getFactory().createLink(rescueLinkId, rescueNode, rescueNode2);
		rescueLink.setNumberOfLanes(10);
		rescueLink.setLength(10);	// use short links for non-vehicular traffic
		rescueLink.setCapacity(1000000);
		rescueLink.setFreespeed(1000000);
		rescueLink.setAllowedModes(allowedTransportModes);
		network.addLink(rescueLink);
		
		/*
		 * Create and add the rescue facility and an activity option ("rescue")
		 */
		Id rescueFacilityId = scenarioData.createId("rescueFacility");
		ActivityFacility rescueFacility = (scenarioData).getActivityFacilities().createAndAddFacility(rescueFacilityId, rescueLink.getCoord());
		((ActivityFacilityImpl) rescueFacility).setLinkId(rescueLink.getId());
		
		ActivityOption activityOption = ((ActivityFacilityImpl) rescueFacility).createActivityOption("rescue");
		activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 0*3600, 24*3600));
		activityOption.setCapacity(Double.MAX_VALUE);
		
		/*
		 * Create and add rescue facilities to all exit-nodes in-links
		 */
		for (Node node :exitNodes) {
			for (Link inLink : node.getInLinks().values()) {
				rescueFacilityId = scenarioData.createId("rescueFacility" + inLink.getId().toString());
				rescueFacility = (scenarioData).getActivityFacilities().createAndAddFacility(rescueFacilityId, inLink.getCoord());
				((ActivityFacilityImpl) rescueFacility).setLinkId(rescueLink.getId());
				
				activityOption = ((ActivityFacilityImpl) rescueFacility).createActivityOption("rescue");
				activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 0*3600, 24*3600));
				activityOption.setCapacity(Double.MAX_VALUE);
				
				if (coordAnalyzer.isFacilityAffected(rescueFacility)) {
					log.warn("Rescue facility " + rescueFacility.getId().toString() + " is located in the affected area!");
				}
			}
		}
		
		/*
		 * Create and add mode switch facilities to all affected links
		 */
		for (Id affectedLinkId : this.affectedLinks) {
			
			Link link = this.scenarioData.getNetwork().getLinks().get(affectedLinkId);
			Id facilityId = scenarioData.createId("switchWalkModeFacility" + affectedLinkId.toString());
			ActivityFacility facility = (scenarioData).getActivityFacilities().createAndAddFacility(facilityId, link.getToNode().getCoord());
			((ActivityFacilityImpl) facility).setLinkId(link.getId());
			
			activityOption = ((ActivityFacilityImpl) facility).createActivityOption("switchWalkMode");
			activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 0*3600, 24*3600));
			activityOption.setCapacity(Double.MAX_VALUE);

		}
	}
	
	/*
	 * Allow walk and bike mode on track links after the evacuation has started.
	 */
	private void updateTrackModes() {
		for (String linkId : CreateMarathonPopulation.trackRelatedLinks) {
			Id id = scenarioData.createId(linkId);
			Link link = scenarioData.getNetwork().getLinks().get(id);
			Set<String> modes = new HashSet<String>(link.getAllowedModes());
			modes.add(TransportMode.walk);
			modes.add(TransportMode.bike);
			link.setAllowedModes(modes);
		}
	}
	
	
	private void initIdentifiers() {
		
		/*
		 * Initialize AgentFilters
		 */
		InformedAgentsFilterFactory initialReplanningFilterFactory = new InformedAgentsFilterFactory((this.informedHouseholdsTracker), 
				InformedAgentsFilter.FilterType.InitialReplanning);
		InformedAgentsFilterFactory notInitialReplanningFilterFactory = new InformedAgentsFilterFactory((this.informedHouseholdsTracker),
				InformedAgentsFilter.FilterType.NotInitialReplanning);
		
		// use affected area
//		AffectedAgentsFilterFactory affectedAgentsFilterFactory = new AffectedAgentsFilterFactory(this.scenarioData, this.agentsTracker,
//				this.vehiclesTracker, this.coordAnalyzer, AffectedAgentsFilter.FilterType.Affected);
//		AffectedAgentsFilterFactory notAffectedAgentsFilterFactory = new AffectedAgentsFilterFactory(this.scenarioData, this.agentsTracker,
//				this.vehiclesTracker, this.coordAnalyzer, AffectedAgentsFilter.FilterType.NotAffected);

		// use buffered affected area
		AffectedAgentsFilterFactory affectedAgentsFilterFactory = new AffectedAgentsFilterFactory(this.scenarioData, this.agentsTracker,
				this.vehiclesTracker, this.bufferedCoordAnalyzer, AffectedAgentsFilter.FilterType.Affected);
		AffectedAgentsFilterFactory notAffectedAgentsFilterFactory = new AffectedAgentsFilterFactory(this.scenarioData, this.agentsTracker,
				this.vehiclesTracker, this.bufferedCoordAnalyzer, AffectedAgentsFilter.FilterType.NotAffected);
		
		DuringActivityIdentifierFactory duringActivityFactory;
		DuringLegIdentifierFactory duringLegFactory;
		
		/*
		 * During Activity Identifiers
		 */
		duringActivityFactory = new ActivityPerformingIdentifierFactory(this.getActivityReplanningMap());
		duringActivityFactory.addAgentFilterFactory(affectedAgentsFilterFactory);
		duringActivityFactory.addAgentFilterFactory(initialReplanningFilterFactory);
		this.affectedActivityPerformingIdentifier = duringActivityFactory.createIdentifier();
		
		duringActivityFactory = new ActivityPerformingIdentifierFactory(this.getActivityReplanningMap());
		duringActivityFactory.addAgentFilterFactory(notAffectedAgentsFilterFactory);
		duringActivityFactory.addAgentFilterFactory(initialReplanningFilterFactory);
		this.notAffectedActivityPerformingIdentifier = duringActivityFactory.createIdentifier();
		
		/*
		 * During Leg Identifiers
		 */
		duringLegFactory = new LegPerformingIdentifierFactory(this.getLinkReplanningMap());
		duringLegFactory.addAgentFilterFactory(initialReplanningFilterFactory);
		this.legPerformingIdentifier = duringLegFactory.createIdentifier();
		
		// replan all transport modes
		duringLegFactory = new LeaveLinkIdentifierFactory(this.getLinkReplanningMap()); 
		duringLegFactory.addAgentFilterFactory(notInitialReplanningFilterFactory);
		this.duringLegRerouteIdentifier = duringLegFactory.createIdentifier();
		this.duringLegRerouteIdentifier.addAgentFilter(new ProbabilityFilterFactory(EvacuationConfig.duringLegReroutingShare).createAgentFilter());
		
		duringActivityFactory = null;
		duringLegFactory = null;
	}
	
	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalized Router.
	 */
	@Override
	protected void initReplanners(QSim sim) {
		
		initIdentifiers();
		
		Map<String, TravelTime> travelTimes = new HashMap<String, TravelTime>();
		travelTimes.put(TransportMode.walk, new WalkTravelTimeFactory(this.config.plansCalcRoute()).createTravelTime());
		travelTimes.put("walk2d", new WalkTravelTimeFactory(this.config.plansCalcRoute()).createTravelTime());
		travelTimes.put(TransportMode.bike, new BikeTravelTimeFactory(this.config.plansCalcRoute()).createTravelTime());
		travelTimes.put(TransportMode.pt, new PTTravelTimeKTIEvacuationFactory(this.scenarioData, 
					new PTTravelTimeFactory(this.config.plansCalcRoute(), getTravelTimeCollector(), new WalkTravelTimeFactory(this.config.plansCalcRoute()).createTravelTime()).createTravelTime()).createTravelTime());
		
//		// add time dependent penalties to travel costs within the affected area
		TravelDisutilityFactory costFactory = new OnlyTimeDependentTravelCostCalculatorFactory();
		TravelDisutilityFactory penaltyCostFactory = new PenaltyTravelCostFactory(costFactory, coordAnalyzer);

		LeastCostPathCalculatorFactory factory = new FastAStarLandmarksFactory(this.network, new FreespeedTravelTimeAndDisutility(this.config.planCalcScore()));
		
		TripRouterFactoryInternal delegate = new TripRouterFactoryImpl(this.scenarioData, penaltyCostFactory, this.getTravelTimeCollector(), factory, this.getTransitRouterFactory());
		WithindayMultimodalTripRouterFactory tripRouterFactory = new WithindayMultimodalTripRouterFactory(this, travelTimes, delegate);
		this.getWithinDayEngine().setTripRouterFactory(tripRouterFactory);
		
		/*
		 * During Activity Replanners
		 */
		EndActivityAndEvacuateReplannerFactory endActivityAndEvacuateReplannerFactory = new EndActivityAndEvacuateReplannerFactory(this.scenarioData, this.getWithinDayEngine(),
				(SwissPTTravelTime) this.ptTravelTime);
		this.marathonEndActivityAndEvacuateReplannerFactory = new MarathonEndActivityAndEvacuateReplannerFactory(this.scenarioData, this.getWithinDayEngine(), endActivityAndEvacuateReplannerFactory);
		this.marathonEndActivityAndEvacuateReplannerFactory.addIdentifier(this.affectedActivityPerformingIdentifier);
		this.getWithinDayEngine().addTimedDuringActivityReplannerFactory(this.marathonEndActivityAndEvacuateReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
		
		
		this.extendCurrentActivityReplannerFactory = new ExtendCurrentActivityReplannerFactory(this.scenarioData, this.getWithinDayEngine());
		this.extendCurrentActivityReplannerFactory.addIdentifier(this.notAffectedActivityPerformingIdentifier);
		this.getWithinDayEngine().addTimedDuringActivityReplannerFactory(this.extendCurrentActivityReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
		
		/*
		 * During Leg Replanners
		 */
		// use affected area
//		this.currentLegInitialReplannerFactory = new CurrentLegInitialReplannerFactory(this.scenarioData, this.getReplanningManager(), router, 1.0, coordAnalyzer);
//		this.currentLegInitialReplannerFactory.addIdentifier(this.legPerformingIdentifier);
//		this.getReplanningManager().addTimedDuringLegReplannerFactory(this.currentLegInitialReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
		
		// use buffered affected area
		this.currentLegInitialReplannerFactory = new CurrentLegInitialReplannerFactory(this.scenarioData, this.getWithinDayEngine(), this.bufferedCoordAnalyzer);
		this.currentLegInitialReplannerFactory.addIdentifier(this.legPerformingIdentifier);
		this.getWithinDayEngine().addTimedDuringLegReplannerFactory(this.currentLegInitialReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
		
		this.switchWalkModeReplannerFactory = new SwitchToWalk2DLegReplannerFactory(this.scenarioData, this.getWithinDayEngine(), this.coordAnalyzer);
		this.switchWalkModeReplannerFactory.addIdentifier(this.duringLegRerouteIdentifier);
		this.getWithinDayEngine().addTimedDuringLegReplannerFactory(this.switchWalkModeReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);

		this.duringLegRerouteReplannerFactory = new MarathonCurrentLegReplannerFactory(this.scenarioData, this.getWithinDayEngine());
		this.duringLegRerouteReplannerFactory.addIdentifier(this.duringLegRerouteIdentifier);
		this.getWithinDayEngine().addTimedDuringLegReplannerFactory(this.duringLegRerouteReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
		
		/*
		 * Collect Replanners that can be disabled after all agents have been informed.
		 */
		this.initialReplannerFactories = new ArrayList<WithinDayReplannerFactory<?>>();
		this.initialReplannerFactories.add(this.marathonEndActivityAndEvacuateReplannerFactory);
		this.initialReplannerFactories.add(this.extendCurrentActivityReplannerFactory);
		this.initialReplannerFactories.add(this.currentLegInitialReplannerFactory);
	}

	/*
	 * The LinkReplanningMap calculates the earliest link exit time for each agent.
	 * To do so, a MultiModalTravelTime object is required which calculates these
	 * times. We use a MultiModalTravelTimeWrapper with walk- and bike travel times
	 * and replace the car, ride and pt travel time calculators with free speed
	 * travel time calculators.
	 */

	private Map<String, TravelTime> createLinkReplanningMapTravelTime() {
		
		Map<String, TravelTime> factory = new HashMap<String, TravelTime>();
		factory.put(TransportMode.walk, new WalkTravelTimeFactory(this.config.plansCalcRoute()).createTravelTime());
		factory.put("walk2d", new WalkTravelTimeFactory(this.config.plansCalcRoute()).createTravelTime());
		factory.put(TransportMode.bike, new BikeTravelTimeFactory(this.config.plansCalcRoute()).createTravelTime());
	
		// replace modes
		factory.put(TransportMode.car, new FreeSpeedTravelTime());
		factory.put(TransportMode.ride, new FreeSpeedTravelTime());
		factory.put(TransportMode.pt, new FreeSpeedTravelTime());

		return factory;
	}

}
