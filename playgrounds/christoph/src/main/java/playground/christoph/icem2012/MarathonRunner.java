/* *********************************************************************** *
 * project: org.matsim.*
 * MarathonRunner.java
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

import com.vividsolutions.jts.geom.Geometry;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.router.MultimodalTripRouterFactory;
import org.matsim.contrib.multimodal.router.util.BikeTravelTimeFactory;
import org.matsim.contrib.multimodal.router.util.WalkTravelTimeFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.*;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsFactory;
import org.matsim.withinday.controller.WithinDayControlerListener;
import org.matsim.withinday.controller.WithinDayModule;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.identifiers.ActivityPerformingIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.LegPerformingIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.filter.ProbabilityFilterFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityAgentSelector;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayReplannerFactory;
import org.opengis.feature.simple.SimpleFeature;
import playground.christoph.evacuation.analysis.AgentsInEvacuationAreaCounter;
import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.api.core.v01.Coord3d;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.core.utils.geometry.Coord3dImpl;
import playground.christoph.evacuation.mobsim.AgentsTracker;
import playground.christoph.evacuation.mobsim.InformedHouseholdsTracker;
import playground.christoph.evacuation.mobsim.ReplanningTracker;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.network.AddZCoordinatesToNetwork;
import playground.christoph.evacuation.trafficmonitoring.SwissPTTravelTimeCalculator;
import playground.christoph.evacuation.withinday.replanning.identifiers.filters.AffectedAgentsFilter;
import playground.christoph.evacuation.withinday.replanning.identifiers.filters.AffectedAgentsFilterFactory;
import playground.christoph.evacuation.withinday.replanning.identifiers.filters.InformedAgentsFilter;
import playground.christoph.evacuation.withinday.replanning.identifiers.filters.InformedAgentsFilterFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.old.CurrentLegInitialReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.old.EndActivityAndEvacuateReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.old.ExtendCurrentActivityReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.utils.SHPFileUtil;
import playground.gregor.sim2d_v4.simulation.Sim2DEngine;
import playground.gregor.sim2denvironment.GisDebugger;
import playground.meisterk.kti.config.KtiConfigGroup;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.*;
import java.util.Map.Entry;

public final class MarathonRunner implements StartupListener,
	MobsimInitializedListener, MobsimBeforeSimStepListener, MobsimAfterSimStepListener {

	private final static Logger log = Logger.getLogger(MarathonRunner.class);
	
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
	private Set<Id<Node>> affectedNodes;
	private Set<Id<Link>> affectedLinks;
	private Set<Id<ActivityFacility>> affectedFacilities;
	
	private AgentsTracker agentsTracker;
	private ReplanningTracker replanningTracker;
	private InformedHouseholdsTracker informedHouseholdsTracker;
	private DecisionDataProvider decisionDataProvider;
	private KtiConfigGroup ktiConfigGroup;
	
	/*
	 * Identifiers
	 */
	private DuringActivityAgentSelector affectedActivityPerformingIdentifier;
	private DuringActivityAgentSelector notAffectedActivityPerformingIdentifier;
	private DuringLegAgentSelector legPerformingIdentifier;
	private DuringLegAgentSelector duringLegRerouteIdentifier;
	
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
	
	private Scenario scenario;
	private WithinDayControlerListener withinDayControlerListener;
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

		Controler controler = new Controler(sc);
		controler.setOverwriteFiles(true);
		controler.addOverridingModule(new WithinDayModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindToProvider(Mobsim.class, CombiMobsimFactory.class);
			}
		});

		MarathonRunner controlerListener = new MarathonRunner(controler);
		controler.addControlerListener(controlerListener);
		
		controler.run();
	}

	public MarathonRunner(Controler controler) {
		
		this.scenario = controler.getScenario();
		
		this.withinDayControlerListener = new WithinDayControlerListener();
				
		init();
	}
	
	protected void init() {
		
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
		
		this.withinDayControlerListener.setNumberOfReplanningThreads(this.scenario.getConfig().global().getNumberOfThreads());
		
		/*
		 * Initialize TravelTimeCollector and create a FactoryWrapper which will act as
		 * factory but returns always the same travel time object, which is possible since
		 * the TravelTimeCollector is not personalized.
		 */
		Set<String> analyzedModes = new HashSet<String>();
		analyzedModes.add(TransportMode.car);
		this.withinDayControlerListener.setModesAnalyzedByTravelTimeCollector(analyzedModes);
		
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
		this.withinDayControlerListener.addMultiModalTravelTimes(linkReplanningTravelTime);
		
		/*
		 * Create the empty object. They are filled in the loadData() method.
		 */
		this.ktiConfigGroup = new KtiConfigGroup();
		
		/*
		 * The KTIConfigGroup is loaded as generic Module. We replace this
		 * generic object with a KtiConfigGroup object and copy all its parameter.
		 */
		ConfigGroup module = this.scenario.getConfig().getModule(KtiConfigGroup.GROUP_NAME);
		this.scenario.getConfig().removeModule(KtiConfigGroup.GROUP_NAME);
		this.scenario.getConfig().addModule(this.ktiConfigGroup);
		
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
		zCoordinateAdder = new AddZCoordinatesToNetwork(this.scenario, dhm25File, srtmFile);
		zCoordinateAdder.addZCoordinatesToNetwork();
		zCoordinateAdder.checkSteepness();
		
		/*
		 * Fixing height coordinates of nodes that have been added and that
		 * are not included in the height shp files.
		 */
		for (Node node : this.scenario.getNetwork().getNodes().values()) {
			String idString = node.getId().toString(); 
			if (idString.contains("_shifted")) {
				idString = idString.replace("_shifted", "");
				Node node2 = this.scenario.getNetwork().getNodes().get(Id.create(idString, Node.class));
				Coord3d coord2 = (Coord3d) node2.getCoord();
				
				Coord coord = node.getCoord();
				Coord3d coord3d = new Coord3dImpl(coord.getX(), coord.getY(), coord2.getZ());
				((NodeImpl) node).setCoord(coord3d);
			}
		}
		
//		ScenarioLoader2DImpl loader = new ScenarioLoader2DImpl(this.scenario);
//		loader.load2DScenario();
	}
	
	/*
	 * Combines a WithinDayQSimFactory and a HybridQ2DMobsimFactory
	 * to have Within-day Replanning and Sim2D in one simulation.
	 */
	private static class CombiMobsimFactory implements Provider<Mobsim> {
	
		private Sim2DEngine sim2DEngine = null;
		private Scenario sc;
		private EventsManager eventsManager;
		private WithinDayEngine withinDayEngine;

		@Inject
		CombiMobsimFactory(Scenario sc, EventsManager eventsManager, WithinDayEngine withinDayEngine) {
			this.sc = sc;
			this.eventsManager = eventsManager;
			this.withinDayEngine = withinDayEngine;
		}

		@Override
		public Mobsim get() {

			QSim mobsim = QSimUtils.createDefaultQSim(sc, eventsManager);
			log.info("Adding WithinDayEngine to Mobsim.");
			mobsim.addMobsimEngine(withinDayEngine);
			
			Sim2DEngine e = new Sim2DEngine(mobsim);
			this.sim2DEngine = e;
			mobsim.addMobsimEngine(e);
//			Sim2DDepartureHandler d = new Sim2DDepartureHandler(e);
//			qSim.addDepartureHandler(d);
			
			return mobsim;
		}
	}

//	public PlanAlgorithm createRoutingAlgorithm(TravelDisutility travelCosts, TravelTime travelTimes) {
//		
//		// the contructor does not call createRoutingAlgorithm on the argument, so it is ok (td)
//		PlanRouterAdapter plansCalcRoute = new PlanRouterAdapter( this );
//		
//		TravelTime travelTime = new WalkTravelTimeFactory(this.scenario.getConfig().plansCalcRoute()).createTravelTime();
//		LegRouter walk2DLegRouter = new Walk2DLegRouter(this.scenario.getNetwork(), travelTime, (IntermodalLeastCostPathCalculator) plansCalcRoute.getLeastCostPathCalculator());
//		plansCalcRoute.addLegHandler("walk2d", walk2DLegRouter);
//		
//		return plansCalcRoute;
//	}

	@Override
	public void notifyStartup(StartupEvent event) {

		
		this.withinDayControlerListener.notifyStartup(event);

//		XYDataWriter xyDataWriter = new XYDataWriter();
//		event.getControler().getEvents().addHandler(xyDataWriter);
//		event.getControler().addControlerListener(xyDataWriter);
//		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(xyDataWriter);
		
		readAffectedArea();
		
		identifyAffectedInfrastructure();
		
		createPreAndPostRunFacilities();
		
		createExitLinks(event.getControler());
		
		this.informedHouseholdsTracker = new InformedHouseholdsTracker(this.scenario.getPopulation(), ((ScenarioImpl) this.scenario).getHouseholds());
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(informedHouseholdsTracker);
		event.getControler().getEvents().addHandler(this.informedHouseholdsTracker);
		
		this.agentsTracker = new AgentsTracker(this.scenario);
		event.getControler().getEvents().addHandler(agentsTracker);
		event.getControler().addControlerListener(agentsTracker);
		
		this.replanningTracker = new ReplanningTracker(this.informedHouseholdsTracker);
		event.getControler().getEvents().addHandler(this.replanningTracker);
		
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
		double scaleFactor = 1 / this.scenario.getConfig().qsim().getFlowCapFactor();
		agentsInEvacuationAreaCounter = new AgentsInEvacuationAreaCounter(this.scenario, transportModes, coordAnalyzer.createInstance(), 
				decisionDataProvider, scaleFactor);

		event.getControler().addControlerListener(agentsInEvacuationAreaCounter);
		event.getControler().getEvents().addHandler(agentsInEvacuationAreaCounter);
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(agentsInEvacuationAreaCounter);
		
		this.initReplanners();
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
						this.withinDayControlerListener.getWithinDayEngine().removeDuringActivityReplannerFactory((WithinDayDuringActivityReplannerFactory) factory);
					} else if(factory instanceof WithinDayDuringLegReplannerFactory) {
						this.withinDayControlerListener.getWithinDayEngine().removeDuringLegReplannerFactory((WithinDayDuringLegReplannerFactory) factory);
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
		
		Households households = ((ScenarioImpl) this.scenario).getHouseholds();
		HouseholdsFactory factory = households.getFactory();
		
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			Household household = factory.createHousehold(Id.create(person.getId().toString(), Household.class));
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
		affectedNodes = new HashSet<>();
		affectedLinks = new HashSet<>();
		affectedFacilities = new HashSet<>();
		
		for (Node node : this.scenario.getNetwork().getNodes().values()) {
			if (coordAnalyzer.isNodeAffected(node)) affectedNodes.add(node.getId());
		}
		for (Link link : this.scenario.getNetwork().getLinks().values()) {
			if (coordAnalyzer.isLinkAffected(link)) affectedLinks.add(link.getId());
		}
		for (ActivityFacility facility : ((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities().values()) {
			if (coordAnalyzer.isFacilityAffected(facility)) affectedFacilities.add(facility.getId());
		}
	}
	
	private void createPreAndPostRunFacilities() {
		
		ActivityFacilities facilities = this.scenario.getActivityFacilities();
		
		Id<Link> startLinkId = Id.create(CreateMarathonPopulation.startLink, Link.class);
		Id<Link> endLinkId = Id.create(CreateMarathonPopulation.endLink, Link.class);
		
		Link startLink = this.scenario.getNetwork().getLinks().get(startLinkId);
		Link endLink = this.scenario.getNetwork().getLinks().get(endLinkId);
		
		Id<ActivityFacility> preRunFacilityId = Id.create("preRunFacility", ActivityFacility.class);
		ActivityFacility preRunFacility = facilities.getFactory().createActivityFacility(preRunFacilityId, startLink.getCoord());
		facilities.addActivityFacility(preRunFacility);
		((ActivityFacilityImpl) preRunFacility).setLinkId(startLinkId);
		ActivityOption activityOption = ((ActivityFacilityImpl) preRunFacility).createActivityOption("preRun");
		activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 0*3600, 24*3600));
		activityOption.setCapacity(Double.MAX_VALUE);
				
		Id<ActivityFacility> postRunFacilityId = Id.create("postRunFacility", ActivityFacility.class);
		ActivityFacility postRunFacility = facilities.getFactory().createActivityFacility(postRunFacilityId, endLink.getCoord());
		facilities.addActivityFacility(postRunFacility);
		((ActivityFacilityImpl) postRunFacility).setLinkId(startLinkId);
		activityOption = ((ActivityFacilityImpl) preRunFacility).createActivityOption("postRun");
		activityOption.addOpeningTime(new OpeningTimeImpl(OpeningTime.DayType.wk, 0*3600, 24*3600));
		activityOption.setCapacity(Double.MAX_VALUE);
	}
	
	private void createExitLinks(Controler controler) {
		Geometry innerBuffer = affectedArea.buffer(this.innerBuffer);
		Geometry outerBuffer = affectedArea.buffer(this.outerBuffer);
		
		GisDebugger.setCRSString("EPSG: 21781");
		
		CoordAnalyzer innerAnalyzer = new CoordAnalyzer(innerBuffer);
		GisDebugger.addGeometry(innerBuffer, "inner Buffer");
		GisDebugger.dump(controler.getControlerIO().getOutputFilename("affectedAreaInnerBuffer.shp"));
		
		CoordAnalyzer outerAnalyzer = new CoordAnalyzer(outerBuffer);
		GisDebugger.addGeometry(outerBuffer, "outer Buffer");
		GisDebugger.dump(controler.getControlerIO().getOutputFilename("/affectedAreaOuterBuffer.shp"));
		
		this.bufferedCoordAnalyzer = new CoordAnalyzer(innerBuffer);
		
		Set<Node> exitNodes = new LinkedHashSet<Node>();
		for (Node node : this.scenario.getNetwork().getNodes().values()) {
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
		Coord rescueNodeCoord = this.scenario.createCoord(683595.0, 244940.0); // somewhere in Lake Zurich
		Id<Node> rescueNodeId = Id.create("rescueNode", Node.class);
		Node rescueNode = this.scenario.getNetwork().getFactory().createNode(rescueNodeId, rescueNodeCoord);
		this.scenario.getNetwork().addNode(rescueNode);

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
			Id<Link> rescueLinkId = Id.create("rescueLink" + counter, Link.class);
			Link rescueLink = this.scenario.getNetwork().getFactory().createLink(rescueLinkId, node, rescueNode);
			rescueLink.setNumberOfLanes(10);
			rescueLink.setLength(10);	// use short links for non-vehicular traffic
			rescueLink.setCapacity(1000000);
			rescueLink.setFreespeed(1000000);
			rescueLink.setAllowedModes(allowedTransportModes);
			this.scenario.getNetwork().addLink(rescueLink);
		}
		log.info("Created " + counter + " exit links.");
		
		/*
		 * Now we create a second rescue node that is connected only to the
		 * first rescue node. The link between them gets equipped with the
		 * rescue facility that is the destination of the evacuated persons.
		 */
		Coord rescueNodeCoord2 = this.scenario.createCoord(rescueNodeCoord.getX() + 1.0, 
				rescueNodeCoord.getY() + 1.0);
		Id<Node> rescueNodeId2 = Id.create("rescueNode2", Node.class);
		Node rescueNode2 = this.scenario.getNetwork().getFactory().createNode(rescueNodeId2, rescueNodeCoord2);
		this.scenario.getNetwork().addNode(rescueNode2);
		
		Id<Link> rescueLinkId = Id.create("rescueLink", Link.class);
		Link rescueLink = this.scenario.getNetwork().getFactory().createLink(rescueLinkId, rescueNode, rescueNode2);
		rescueLink.setNumberOfLanes(10);
		rescueLink.setLength(10);	// use short links for non-vehicular traffic
		rescueLink.setCapacity(1000000);
		rescueLink.setFreespeed(1000000);
		rescueLink.setAllowedModes(allowedTransportModes);
		this.scenario.getNetwork().addLink(rescueLink);
		
		ActivityFacilities facilities = this.scenario.getActivityFacilities();
		/*
		 * Create and add the rescue facility and an activity option ("rescue")
		 */
		Id<ActivityFacility> rescueFacilityId = Id.create("rescueFacility", ActivityFacility.class);
		ActivityFacility rescueFacility = facilities.getFactory().createActivityFacility(rescueFacilityId, rescueLink.getCoord());
		facilities.addActivityFacility(rescueFacility);
		((ActivityFacilityImpl) rescueFacility).setLinkId(rescueLink.getId());
		
		ActivityOption activityOption = ((ActivityFacilityImpl) rescueFacility).createActivityOption("rescue");
		activityOption.addOpeningTime(new OpeningTimeImpl(0*3600, 24*3600));
		activityOption.setCapacity(Double.MAX_VALUE);
		
		/*
		 * Create and add rescue facilities to all exit-nodes in-links
		 */
		for (Node node :exitNodes) {
			for (Link inLink : node.getInLinks().values()) {
				rescueFacilityId = Id.create("rescueFacility" + inLink.getId().toString(), ActivityFacility.class);
				rescueFacility = facilities.getFactory().createActivityFacility(rescueFacilityId, inLink.getCoord());
				facilities.addActivityFacility(rescueFacility);
				((ActivityFacilityImpl) rescueFacility).setLinkId(rescueLink.getId());
				
				activityOption = ((ActivityFacilityImpl) rescueFacility).createActivityOption("rescue");
				activityOption.addOpeningTime(new OpeningTimeImpl(0*3600, 24*3600));
				activityOption.setCapacity(Double.MAX_VALUE);
				
				if (coordAnalyzer.isFacilityAffected(rescueFacility)) {
					log.warn("Rescue facility " + rescueFacility.getId().toString() + " is located in the affected area!");
				}
			}
		}
		
		/*
		 * Create and add mode switch facilities to all affected links
		 */
		for (Id<Link> affectedLinkId : this.affectedLinks) {
			
			Link link = this.scenario.getNetwork().getLinks().get(affectedLinkId);
			Id<ActivityFacility> facilityId = Id.create("switchWalkModeFacility" + affectedLinkId.toString(), ActivityFacility.class);
			ActivityFacility facility = facilities.getFactory().createActivityFacility(facilityId, link.getToNode().getCoord());
			facilities.addActivityFacility(facility);
			((ActivityFacilityImpl) facility).setLinkId(link.getId());
			
			activityOption = ((ActivityFacilityImpl) facility).createActivityOption("switchWalkMode");
			activityOption.addOpeningTime(new OpeningTimeImpl(0*3600, 24*3600));
			activityOption.setCapacity(Double.MAX_VALUE);

		}
	}
	
	/*
	 * Allow walk and bike mode on track links after the evacuation has started.
	 */
	private void updateTrackModes() {
		for (String linkId : CreateMarathonPopulation.trackRelatedLinks) {
			Id<Link> id = Id.create(linkId, Link.class);
			Link link = this.scenario.getNetwork().getLinks().get(id);
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
		InformedAgentsFilterFactory initialReplanningFilterFactory = new InformedAgentsFilterFactory(this.informedHouseholdsTracker, 
				this.replanningTracker, InformedAgentsFilter.FilterType.InitialReplanning);
		InformedAgentsFilterFactory notInitialReplanningFilterFactory = new InformedAgentsFilterFactory(this.informedHouseholdsTracker,
				this.replanningTracker, InformedAgentsFilter.FilterType.NotInitialReplanning);
		
		// use affected area
//		AffectedAgentsFilterFactory affectedAgentsFilterFactory = new AffectedAgentsFilterFactory(this.scenarioData, this.agentsTracker,
//				this.vehiclesTracker, this.coordAnalyzer, AffectedAgentsFilter.FilterType.Affected);
//		AffectedAgentsFilterFactory notAffectedAgentsFilterFactory = new AffectedAgentsFilterFactory(this.scenarioData, this.agentsTracker,
//				this.vehiclesTracker, this.coordAnalyzer, AffectedAgentsFilter.FilterType.NotAffected);

		// use buffered affected area
		AffectedAgentsFilterFactory affectedAgentsFilterFactory = new AffectedAgentsFilterFactory(this.scenario, this.agentsTracker,
				this.withinDayControlerListener.getMobsimDataProvider(), this.bufferedCoordAnalyzer, AffectedAgentsFilter.FilterType.Affected);
		AffectedAgentsFilterFactory notAffectedAgentsFilterFactory = new AffectedAgentsFilterFactory(this.scenario, this.agentsTracker,
				this.withinDayControlerListener.getMobsimDataProvider(), this.bufferedCoordAnalyzer, AffectedAgentsFilter.FilterType.NotAffected);
		
		DuringActivityIdentifierFactory duringActivityFactory;
		DuringLegIdentifierFactory duringLegFactory;
		
		/*
		 * During Activity Identifiers
		 */
		duringActivityFactory = new ActivityPerformingIdentifierFactory(this.withinDayControlerListener.getActivityReplanningMap(),
				this.withinDayControlerListener.getMobsimDataProvider());
		duringActivityFactory.addAgentFilterFactory(affectedAgentsFilterFactory);
		duringActivityFactory.addAgentFilterFactory(initialReplanningFilterFactory);
		this.affectedActivityPerformingIdentifier = duringActivityFactory.createIdentifier();
		
		duringActivityFactory = new ActivityPerformingIdentifierFactory(this.withinDayControlerListener.getActivityReplanningMap(),
				this.withinDayControlerListener.getMobsimDataProvider());
		duringActivityFactory.addAgentFilterFactory(notAffectedAgentsFilterFactory);
		duringActivityFactory.addAgentFilterFactory(initialReplanningFilterFactory);
		this.notAffectedActivityPerformingIdentifier = duringActivityFactory.createIdentifier();
		
		/*
		 * During Leg Identifiers
		 */
		duringLegFactory = new LegPerformingIdentifierFactory(this.withinDayControlerListener.getLinkReplanningMap(),
				this.withinDayControlerListener.getMobsimDataProvider());
		duringLegFactory.addAgentFilterFactory(initialReplanningFilterFactory);
		this.legPerformingIdentifier = duringLegFactory.createIdentifier();
		
		// replan all transport modes
		duringLegFactory = new LeaveLinkIdentifierFactory(this.withinDayControlerListener.getLinkReplanningMap(),
				this.withinDayControlerListener.getMobsimDataProvider()); 
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
	protected void initReplanners() {
		
		initIdentifiers();
		
		Map<String, TravelTime> travelTimes = new HashMap<String, TravelTime>();
		travelTimes.put(TransportMode.walk, new WalkTravelTimeFactory(this.scenario.getConfig().plansCalcRoute()).createTravelTime());
		travelTimes.put("walk2d", new WalkTravelTimeFactory(this.scenario.getConfig().plansCalcRoute()).createTravelTime());
		travelTimes.put(TransportMode.bike, new BikeTravelTimeFactory(this.scenario.getConfig().plansCalcRoute()).createTravelTime());
//		travelTimes.put(TransportMode.pt, new PTTravelTimeKTIEvacuationFactory(this.scenario, 
//					new PTTravelTimeFactory(this.scenario.getConfig().plansCalcRoute(), this.withinDayControlerListener.getTravelTimeCollector(), 
//							new WalkTravelTimeFactory(this.scenario.getConfig().plansCalcRoute()).createTravelTime()).createTravelTime()).createTravelTime());
		
//		// add time dependent penalties to travel costs within the affected area
		TravelDisutilityFactory costFactory = new OnlyTimeDependentTravelDisutilityFactory();
		TravelDisutilityFactory penaltyCostFactory = new PenaltyTravelCostFactory(costFactory, coordAnalyzer);

		TripRouterFactory tripRouterFactory = new MultimodalTripRouterFactory(this.scenario, travelTimes, penaltyCostFactory);
		this.withinDayControlerListener.setWithinDayTripRouterFactory(tripRouterFactory);

		TravelDisutility travelDisutility = penaltyCostFactory.createTravelDisutility(this.withinDayControlerListener.getTravelTimeCollector(), 
				this.scenario.getConfig().planCalcScore()); 
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, this.withinDayControlerListener.getTravelTimeCollector());
		
		/*
		 * During Activity Replanners
		 */
		EndActivityAndEvacuateReplannerFactory endActivityAndEvacuateReplannerFactory = new EndActivityAndEvacuateReplannerFactory(this.scenario, this.withinDayControlerListener.getWithinDayEngine(),
				(SwissPTTravelTimeCalculator) this.ptTravelTime, this.withinDayControlerListener.getWithinDayTripRouterFactory(), routingContext);
		this.marathonEndActivityAndEvacuateReplannerFactory = new MarathonEndActivityAndEvacuateReplannerFactory(this.scenario, this.withinDayControlerListener.getWithinDayEngine(), endActivityAndEvacuateReplannerFactory);
		this.marathonEndActivityAndEvacuateReplannerFactory.addIdentifier(this.affectedActivityPerformingIdentifier);
		this.withinDayControlerListener.getWithinDayEngine().addTimedDuringActivityReplannerFactory(this.marathonEndActivityAndEvacuateReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
				
		this.extendCurrentActivityReplannerFactory = new ExtendCurrentActivityReplannerFactory(this.scenario, this.withinDayControlerListener.getWithinDayEngine());
		this.extendCurrentActivityReplannerFactory.addIdentifier(this.notAffectedActivityPerformingIdentifier);
		this.withinDayControlerListener.getWithinDayEngine().addTimedDuringActivityReplannerFactory(this.extendCurrentActivityReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
		
		/*
		 * During Leg Replanners
		 */
		// use affected area
//		this.currentLegInitialReplannerFactory = new CurrentLegInitialReplannerFactory(this.scenarioData, this.getReplanningManager(), router, 1.0, coordAnalyzer);
//		this.currentLegInitialReplannerFactory.addIdentifier(this.legPerformingIdentifier);
//		this.getReplanningManager().addTimedDuringLegReplannerFactory(this.currentLegInitialReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
		
		// use buffered affected area
		this.currentLegInitialReplannerFactory = new CurrentLegInitialReplannerFactory(this.scenario, 
				this.withinDayControlerListener.getWithinDayEngine(), this.bufferedCoordAnalyzer, this.withinDayControlerListener.getWithinDayTripRouterFactory(), routingContext);
		this.currentLegInitialReplannerFactory.addIdentifier(this.legPerformingIdentifier);
		this.withinDayControlerListener.getWithinDayEngine().addTimedDuringLegReplannerFactory(this.currentLegInitialReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
		
		this.switchWalkModeReplannerFactory = new SwitchToWalk2DLegReplannerFactory(this.scenario, this.withinDayControlerListener.getWithinDayEngine(), this.coordAnalyzer);
		this.switchWalkModeReplannerFactory.addIdentifier(this.duringLegRerouteIdentifier);
		this.withinDayControlerListener.getWithinDayEngine().addTimedDuringLegReplannerFactory(this.switchWalkModeReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);

		this.duringLegRerouteReplannerFactory = new MarathonCurrentLegReplannerFactory(this.scenario, 
				this.withinDayControlerListener.getWithinDayEngine(), this.withinDayControlerListener.getWithinDayTripRouterFactory(), routingContext);
		this.duringLegRerouteReplannerFactory.addIdentifier(this.duringLegRerouteIdentifier);
		this.withinDayControlerListener.getWithinDayEngine().addTimedDuringLegReplannerFactory(this.duringLegRerouteReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
		
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
		factory.put(TransportMode.walk, new WalkTravelTimeFactory(this.scenario.getConfig().plansCalcRoute()).createTravelTime());
		factory.put("walk2d", new WalkTravelTimeFactory(this.scenario.getConfig().plansCalcRoute()).createTravelTime());
		factory.put(TransportMode.bike, new BikeTravelTimeFactory(this.scenario.getConfig().plansCalcRoute()).createTravelTime());
	
		// replace modes
		factory.put(TransportMode.car, new FreeSpeedTravelTime());
		factory.put(TransportMode.ride, new FreeSpeedTravelTime());
		factory.put(TransportMode.pt, new FreeSpeedTravelTime());

		return factory;
	}

}
