/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import com.vividsolutions.jts.geom.Geometry;
import org.apache.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.analysis.christoph.TravelTimesWriter;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.core.mobsim.qsim.qnetsimengine.MissedJointDepartureWriter;
import org.matsim.core.mobsim.qsim.qnetsimengine.PassengerQNetsimEngine;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.withinday.controller.WithinDayControlerListener;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.identifiers.ActivityPerformingIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.LegPerformingIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.filter.ActivityStartingFilterFactory;
import org.matsim.withinday.replanning.identifiers.filter.EarliestLinkExitTimeFilterFactory;
import org.matsim.withinday.replanning.identifiers.filter.ProbabilityFilterFactory;
import org.matsim.withinday.replanning.identifiers.filter.TransportModeFilterFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.*;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayReplannerFactory;
import org.matsim.withinday.trafficmonitoring.LinkEnteredProvider;
import org.opengis.feature.simple.SimpleFeature;
import playground.christoph.analysis.PassengerVolumesAnalyzer;
import playground.christoph.evacuation.analysis.*;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.*;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataGrabber;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataWriter;
import playground.christoph.evacuation.mobsim.decisionmodel.DecisionModelRunner;
import playground.christoph.evacuation.pt.EvacuationTransitRouterFactory;
import playground.christoph.evacuation.pt.TransitRouterNetworkReaderMatsimV1;
import playground.christoph.evacuation.router.LeastCostPathCalculatorSelectorFactory;
import playground.christoph.evacuation.router.RandomCompassRouterFactory;
import playground.christoph.evacuation.router.util.AffectedAreaPenaltyCalculator;
import playground.christoph.evacuation.router.util.FuzzyTravelTimeEstimatorFactory;
import playground.christoph.evacuation.router.util.PenaltyTravelCostFactory;
import playground.christoph.evacuation.trafficmonitoring.EvacuationPTTravelTime;
import playground.christoph.evacuation.trafficmonitoring.PTTravelTimeEvacuationCalculator;
import playground.christoph.evacuation.withinday.replanning.identifiers.*;
import playground.christoph.evacuation.withinday.replanning.identifiers.filters.AffectedAgentsFilter;
import playground.christoph.evacuation.withinday.replanning.identifiers.filters.AffectedAgentsFilterFactory;
import playground.christoph.evacuation.withinday.replanning.identifiers.filters.InformedAgentsFilter;
import playground.christoph.evacuation.withinday.replanning.identifiers.filters.InformedAgentsFilterFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.*;
import playground.christoph.evacuation.withinday.replanning.utils.ModeAvailabilityChecker;
import playground.christoph.evacuation.withinday.replanning.utils.SHPFileUtil;
import playground.christoph.evacuation.withinday.replanning.utils.SelectHouseholdMeetingPoint;

import javax.inject.Provider;
import java.util.*;
import java.util.Map.Entry;

public class EvacuationControlerListener implements StartupListener {

	private static final Logger log = Logger.getLogger(EvacuationControlerListener.class);
	
	private final WithinDayControlerListener withinDayControlerListener;
	private final Provider<Map<String, TravelTime>> multiModalControlerListener;
	
	/*
	 * Data collectors and providers
	 */
	private ReplanningTracker replanningTracker;
	private JointDepartureOrganizer jointDepartureOrganizer;
	private JointDepartureCoordinator jointDepartureCoordinator;
	private MissedJointDepartureWriter missedJointDepartureWriter;
	private VehiclesTracker vehiclesTracker;
	private HouseholdsTracker householdsTracker;
	private InformedHouseholdsTracker informedHouseholdsTracker;
	private DecisionDataGrabber decisionDataGrabber;
	private DecisionDataWriter decisionDataWriter;
	private DecisionModelRunner decisionModelRunner;
	private LinkEnteredProvider linkEnteredProvider;
	private HouseholdDepartureManager householdDepartureManager;
	
	/*
	 * Geography related stuff
	 */
	private CoordAnalyzer coordAnalyzer;
	private AffectedAreaPenaltyCalculator penaltyCalculator;
	private Geometry affectedArea;
	
	private ModeAvailabilityChecker modeAvailabilityChecker;
	private SelectHouseholdMeetingPoint selectHouseholdMeetingPoint;
	
	/*
	 * Analysis modules
	 */
	private EvacuationTimePicture evacuationTimePicture;
	private AgentsReturnHomeCounter agentsReturnHomeCounter;
	private AgentsInEvacuationAreaCounter agentsInEvacuationAreaCounter;
	private AgentsInEvacuationAreaActivityCounter agentsInEvacuationAreaActivityCounter;
	private DetailedAgentsTracker detailedAgentsTracker;
	private LinkVolumesWriter linkVolumesWriter;
	private TravelTimesWriter travelTimesWriter;
	
	/*
	 * Identifiers
	 */
	private DuringActivityAgentSelector joinedHouseholdsIdentifier;
	private DuringActivityAgentSelector activityPerformingIdentifier;
	private DuringLegAgentSelector legPerformingIdentifier;
	private DuringLegAgentSelector agentsToDropOffIdentifier;
	private DuringLegAgentSelector agentsToPickupIdentifier;
	private DuringLegAgentSelector duringLegRerouteIdentifier;
	
	/*
	 * ReplannerFactories
	 */
	private WithinDayDuringActivityReplannerFactory currentActivityToMeetingPointReplannerFactory;
	private WithinDayDuringActivityReplannerFactory joinedHouseholdsReplannerFactory;
	private WithinDayDuringLegReplannerFactory currentLegToMeetingPointReplannerFactory;
	private WithinDayDuringLegReplannerFactory dropOffAgentsReplannerFactory;
	private WithinDayDuringLegReplannerFactory pickupAgentsReplannerFactory;
	private WithinDayDuringLegReplannerFactory duringLegRerouteReplannerFactory;
	
	private InitialReplanningRemover initialReplanningRemover;
	
	/*
	 * WithinDayTripRouter Stuff
	 */
	private Map<String, TravelTime> withinDayTravelTimes;
	private EvacuationTransitRouterFactory evacuationTransitRouterFactory;
	private Provider<TripRouter> withinDayTripRouterFactory;
	private LeastCostPathCalculatorFactory withinDayLeastCostPathCalculatorFactory;
	private TravelDisutilityFactory withinDayTravelDisutilityFactory;
	
	/*
	 * Data
	 */
	
	private final FixedOrderControlerListener fixedOrderControlerListener = new FixedOrderControlerListener();
	
	public EvacuationControlerListener(WithinDayControlerListener withinDayControlerListener, 
			Provider<Map<String, TravelTime>> multiModalControlerListener) {
		this.withinDayControlerListener = withinDayControlerListener;
		this.multiModalControlerListener = multiModalControlerListener;
	}
	
	@Override
	public void notifyStartup(final StartupEvent event) {
		
		// register FixedOrderControlerListener
		event.getControler().addControlerListener(this.fixedOrderControlerListener);
		
		this.initGeographyStuff(event.getControler().getScenario());
		
		this.initDataGrabbersAndProviders(event.getControler());
		
		this.initAnalysisStuff(event.getControler());
		
		this.initReplanningStuff(event.getControler());
		
		/*
		 * Use a MobsimFactory which creates vehicles according to available vehicles per
		 * household and adds the replanning Manager as mobsim engine.
		 */
		final Scenario scenario = event.getControler().getScenario();
		final EvacuationQSimFactory mobsimFactory = new EvacuationQSimFactory(this.withinDayControlerListener.getWithinDayEngine(),
				scenario.getHouseholds().getHouseholdAttributes(), this.jointDepartureOrganizer,
				this.multiModalControlerListener.get());
		event.getControler().addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new com.google.inject.Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return mobsimFactory.createMobsim(scenario, event.getControler().getEvents());
					}
				});
			}
		});
		event.getControler().addControlerListener(mobsimFactory);	// only to write some files for debugging
	}

	private void initGeographyStuff(Scenario scenario) {
		
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		SHPFileUtil util = new SHPFileUtil();
		for (String file : EvacuationConfig.evacuationArea) {
			features.addAll(ShapeFileReader.getAllFeatures(file));		
		}
		this.affectedArea = util.mergeGeometries(features);
		log.info("Size of affected area: " + affectedArea.getArea());
		
		this.penaltyCalculator = new AffectedAreaPenaltyCalculator(scenario.getNetwork(), affectedArea, 
				EvacuationConfig.affectedAreaDistanceBuffer, EvacuationConfig.affectedAreaTimePenaltyFactor);
		
		this.coordAnalyzer = new CoordAnalyzer(affectedArea);
	}
	
	private void initDataGrabbersAndProviders(Controler controler) {
		
		Scenario scenario = controler.getScenario();
		
		this.jointDepartureOrganizer = new JointDepartureOrganizer();
		this.jointDepartureCoordinator = new JointDepartureCoordinator(this.withinDayControlerListener.getMobsimDataProvider());
		this.missedJointDepartureWriter = new MissedJointDepartureWriter(this.jointDepartureOrganizer);
		this.fixedOrderControlerListener.addControlerListener(this.missedJointDepartureWriter);

        this.informedHouseholdsTracker = new InformedHouseholdsTracker(controler.getScenario().getPopulation(),
				((ScenarioImpl) controler.getScenario()).getHouseholds());
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(informedHouseholdsTracker);
		controler.getEvents().addHandler(this.informedHouseholdsTracker);
		
		this.replanningTracker = new ReplanningTracker(this.informedHouseholdsTracker);
		controler.getEvents().addHandler(this.replanningTracker);
		
		this.householdsTracker = new HouseholdsTracker(scenario);
		controler.getEvents().addHandler(this.householdsTracker);
		this.fixedOrderControlerListener.addControlerListener(this.householdsTracker);
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(householdsTracker);
		
		this.decisionDataGrabber = new DecisionDataGrabber(scenario, this.coordAnalyzer.createInstance(), 
				this.householdsTracker, ((ScenarioImpl) scenario).getHouseholds().getHouseholdAttributes());
		
		this.decisionModelRunner = new DecisionModelRunner(scenario, this.decisionDataGrabber, this.informedHouseholdsTracker);
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(this.decisionModelRunner);
		this.fixedOrderControlerListener.addControlerListener(this.decisionModelRunner);
		
		this.decisionDataWriter = new DecisionDataWriter(this.decisionModelRunner.getDecisionDataProvider());
		this.fixedOrderControlerListener.addControlerListener(this.decisionDataWriter);
				
		this.vehiclesTracker = new VehiclesTracker(this.withinDayControlerListener.getMobsimDataProvider());
		controler.getEvents().addHandler(vehiclesTracker);

		this.modeAvailabilityChecker = new ModeAvailabilityChecker(scenario, this.withinDayControlerListener.getMobsimDataProvider());
				
		this.linkEnteredProvider = new LinkEnteredProvider();
		controler.getEvents().addHandler(this.linkEnteredProvider);
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(this.linkEnteredProvider);
		
		// workaround
		scenario.getConfig().transit().setUseTransit(false);
		
		// init within-day travel times and trip router factory
		this.initWithinDayTravelTimes(controler);
		this.initWithinDayTripRouterFactory(controler);
		
		// evacuationTransitRouterFactory is not initialized yet
		this.selectHouseholdMeetingPoint = new SelectHouseholdMeetingPoint(scenario, this.withinDayTravelTimes, 
				this.coordAnalyzer.createInstance(), this.affectedArea, this.informedHouseholdsTracker, 
				this.decisionModelRunner, this.withinDayControlerListener.getMobsimDataProvider(), this.evacuationTransitRouterFactory);
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(this.selectHouseholdMeetingPoint);
		this.fixedOrderControlerListener.addControlerListener(this.selectHouseholdMeetingPoint);
		
		// Has to be registered as simulation listener after the informed households tracker since it uses results from there! 
		this.householdDepartureManager = new HouseholdDepartureManager(scenario, this.coordAnalyzer.createInstance(), 
				this.householdsTracker, this.informedHouseholdsTracker, this.decisionModelRunner.getDecisionDataProvider());
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(this.householdDepartureManager);
	}
	
	private void initAnalysisStuff(Controler controler) {
		
		Scenario scenario = controler.getScenario();
		
		/*
		 * Create the set of analyzed modes.
		 */
		Set<String> analyzedModes = new HashSet<String>();
		analyzedModes.add(TransportMode.bike);
		analyzedModes.add(TransportMode.car);
		analyzedModes.add(TransportMode.pt);
		analyzedModes.add(TransportMode.ride);
		analyzedModes.add(TransportMode.walk);
		analyzedModes.add(PassengerQNetsimEngine.PASSENGER_TRANSPORT_MODE);

		// Create txt and kmz files containing distribution of evacuation times. 
		if (EvacuationConfig.createEvacuationTimePicture) {
			evacuationTimePicture = new EvacuationTimePicture(scenario, this.coordAnalyzer.createInstance(), this.householdsTracker, 
					this.withinDayControlerListener.getMobsimDataProvider());
			controler.addControlerListener(this.evacuationTimePicture);
			this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(this.evacuationTimePicture);
			controler.getEvents().addHandler(this.evacuationTimePicture);	
		}
		
		// Create and add an AgentsInEvacuationAreaCounter.
		if (EvacuationConfig.countAgentsInEvacuationArea) {
			double scaleFactor = 1 / scenario.getConfig().qsim().getFlowCapFactor();
			
			agentsInEvacuationAreaCounter = new AgentsInEvacuationAreaCounter(scenario, analyzedModes, this.coordAnalyzer.createInstance(), 
					this.decisionModelRunner.getDecisionDataProvider(), scaleFactor);
			this.fixedOrderControlerListener.addControlerListener(this.agentsInEvacuationAreaCounter);
			this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(this.agentsInEvacuationAreaCounter);
			controler.getEvents().addHandler(this.agentsInEvacuationAreaCounter);
			
			agentsInEvacuationAreaActivityCounter = new AgentsInEvacuationAreaActivityCounter(scenario, this.coordAnalyzer.createInstance(), 
					this.decisionModelRunner.getDecisionDataProvider(), scaleFactor);
			this.fixedOrderControlerListener.addControlerListener(this.agentsInEvacuationAreaActivityCounter);
			this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(this.agentsInEvacuationAreaActivityCounter);
			controler.getEvents().addHandler(this.agentsInEvacuationAreaActivityCounter);
			
			this.agentsReturnHomeCounter = new AgentsReturnHomeCounter(scenario, analyzedModes, this.coordAnalyzer.createInstance(), 
					this.decisionModelRunner.getDecisionDataProvider(), scaleFactor);
			this.fixedOrderControlerListener.addControlerListener(this.agentsReturnHomeCounter);
			this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(this.agentsReturnHomeCounter);
			controler.getEvents().addHandler(this.agentsReturnHomeCounter);
		}
		
		this.detailedAgentsTracker = new DetailedAgentsTracker(scenario, this.householdsTracker, 
				this.decisionModelRunner.getDecisionDataProvider(), this.coordAnalyzer);
		this.fixedOrderControlerListener.addControlerListener(this.detailedAgentsTracker);
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(this.detailedAgentsTracker);
		controler.getEvents().addHandler(this.detailedAgentsTracker);
		
		int timeSlice = 900;
		int maxTime = 36 * 3600;
		VolumesAnalyzer volumesAnalyzer = new PassengerVolumesAnalyzer(timeSlice, maxTime, scenario.getNetwork());
		controler.getEvents().addHandler(volumesAnalyzer);
		double scaleFactor = 1 / scenario.getConfig().qsim().getFlowCapFactor();
		this.linkVolumesWriter = new LinkVolumesWriter(volumesAnalyzer, scenario.getNetwork(), timeSlice, maxTime, scaleFactor, true);
		this.linkVolumesWriter.setCrsString(scenario.getConfig().global().getCoordinateSystem());
		this.fixedOrderControlerListener.addControlerListener(this.linkVolumesWriter);
		
		this.travelTimesWriter = new TravelTimesWriter(true, true);
		this.travelTimesWriter.setCrsString(scenario.getConfig().global().getCoordinateSystem());
		this.fixedOrderControlerListener.addControlerListener(this.travelTimesWriter);
	}
	
	private void initWithinDayTravelTimes(Controler controler) {
		
		Scenario scenario = controler.getScenario();
		
		this.withinDayTravelTimes = new HashMap<String, TravelTime>();
		this.withinDayTravelTimes.putAll(this.multiModalControlerListener.get());
		this.withinDayTravelTimes.put(TransportMode.car, this.withinDayControlerListener.getTravelTimeCollector());
		
		/*
		 * If fuzzy travel times should be used, wrap each TravelTime into a
		 * FuzzyTravelTime object.
		 */
		if (EvacuationConfig.useFuzzyTravelTimes) {
			Map<String, TravelTime> fuzziedTravelTimes = new HashMap<String, TravelTime>();
			for (Entry<String, TravelTime> entry : this.withinDayTravelTimes.entrySet()) {
				String mode = entry.getKey();
				TravelTime travelTime = entry.getValue();
				
				// so far no fuzzy travel time support for pt...
				if (TransportMode.pt.equals(mode)) {
					fuzziedTravelTimes.put(mode, travelTime);
				} else {
					FuzzyTravelTimeEstimatorFactory fuzzyTravelTimeEstimatorFactory = new FuzzyTravelTimeEstimatorFactory(scenario, 
							travelTime, this.householdsTracker, this.withinDayControlerListener.getMobsimDataProvider());
					TravelTime fuzziedTravelTime = fuzzyTravelTimeEstimatorFactory.get();
					fuzziedTravelTimes.put(mode, fuzziedTravelTime);
				}
			}
			this.withinDayTravelTimes = fuzziedTravelTimes;
		}
	}
	
	private void initReplanningStuff(Controler controler) {
		
		this.initialReplanningRemover = new InitialReplanningRemover(this.withinDayControlerListener.getWithinDayEngine(), 
				this.replanningTracker);
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(this.initialReplanningRemover);
		
//		this.initWithinDayTripRouterFactory(controler);
		this.initIdentifiers(controler);
		this.initReplanners(controler);
	}

	private void initWithinDayTripRouterFactory(Controler controler) {
		
		Config config = controler.getConfig();
		Scenario scenario = controler.getScenario();
		
		/*
		 * Add time dependent penalties to travel costs within the affected area.
		 */
		TravelDisutilityFactory costFactory = new OnlyTimeDependentTravelDisutilityFactory();
		this.withinDayTravelDisutilityFactory = new PenaltyTravelCostFactory(costFactory, penaltyCalculator);
		
		LeastCostPathCalculatorFactory nonPanicFactory = new FastAStarLandmarksFactory(scenario.getNetwork(), 
				new FreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore()));
		LeastCostPathCalculatorFactory panicFactory = new RandomCompassRouterFactory(EvacuationConfig.tabuSearch, 
				EvacuationConfig.compassProbability);
		this.withinDayLeastCostPathCalculatorFactory = new LeastCostPathCalculatorSelectorFactory(nonPanicFactory, panicFactory,
				this.decisionModelRunner.getDecisionDataProvider());
		
//		new TransitScheduleReader(scenario).readFile(config.transit().getTransitScheduleFile());
		TransitRouterNetwork routerNetwork = new TransitRouterNetwork();
		new TransitRouterNetworkReaderMatsimV1(scenario, routerNetwork).parse(EvacuationConfig.transitRouterFile);
		
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(config.planCalcScore(),
				config.plansCalcRoute(), config.transitRouter(), config.vspExperimental());
//		this.evacuationTransitRouterFactory = new EvacuationTransitRouterFactory(config, 
//				this.withinDayTravelTimes.get(TransportMode.walk), routerNetwork, transitRouterConfig);		
		this.evacuationTransitRouterFactory = new EvacuationTransitRouterFactory(config, 
				this.withinDayTravelTimes.get(TransportMode.walk), scenario.getTransitSchedule(), routerNetwork, transitRouterConfig);
		
		// TODO: EvacuationTransitRouterFactory is not a TransitRouterFactory so far!
		this.withinDayTripRouterFactory = new EvacuationTripRouterFactory(scenario, this.withinDayTravelTimes, 
				this.withinDayTravelDisutilityFactory, this.withinDayLeastCostPathCalculatorFactory, evacuationTransitRouterFactory);
	}
	
	private void initIdentifiers(Controler controler) {
		
		Scenario scenario = controler.getScenario();
		MobsimDataProvider mobsimDataProvider = this.withinDayControlerListener.getMobsimDataProvider();
		/*
		 * Initialize AgentFilters
		 */
		InformedAgentsFilterFactory initialReplanningFilterFactory = new InformedAgentsFilterFactory(this.informedHouseholdsTracker, 
				this.replanningTracker, InformedAgentsFilter.FilterType.InitialReplanning);
		InformedAgentsFilterFactory notInitialReplanningFilterFactory = new InformedAgentsFilterFactory(this.informedHouseholdsTracker, 
				this.replanningTracker, InformedAgentsFilter.FilterType.NotInitialReplanning);
		
		AffectedAgentsFilterFactory affectedAgentsFilterFactory = new AffectedAgentsFilterFactory(scenario, this.householdsTracker, 
				mobsimDataProvider, coordAnalyzer, AffectedAgentsFilter.FilterType.NotAffected);
		TransportModeFilterFactory carOnlyTransportModeFilterFactory = new TransportModeFilterFactory(
				CollectionUtils.stringToSet(TransportMode.car), mobsimDataProvider);
		TransportModeFilterFactory walkOnlyTransportModeFilterFactory = new TransportModeFilterFactory(
				CollectionUtils.stringToSet(TransportMode.walk), mobsimDataProvider);
		
		EarliestLinkExitTimeFilterFactory earliestLinkExitTimeFilterFactory = new EarliestLinkExitTimeFilterFactory(
				this.withinDayControlerListener.getEarliestLinkExitTimeProvider());
		ActivityStartingFilterFactory activityStartingFilterFactory = new ActivityStartingFilterFactory(mobsimDataProvider);
		
		Set<String> nonPTModes = new HashSet<String>();
		nonPTModes.add(TransportMode.car);
		nonPTModes.add(TransportMode.ride);
		nonPTModes.add(TransportMode.bike);
		nonPTModes.add(TransportMode.walk);
		AgentFilterFactory nonPTLegAgentsFilterFactory = new TransportModeFilterFactory(nonPTModes, mobsimDataProvider);
		
		DuringActivityIdentifierFactory duringActivityFactory;
		DuringLegIdentifierFactory duringLegFactory;
		
		/*
		 * During Activity Identifiers
		 */
		duringActivityFactory = new ActivityPerformingIdentifierFactory(this.withinDayControlerListener.getActivityReplanningMap(), 
				this.withinDayControlerListener.getMobsimDataProvider());
		duringActivityFactory.addAgentFilterFactory(initialReplanningFilterFactory);
		this.activityPerformingIdentifier = duringActivityFactory.createIdentifier();
		
		duringActivityFactory = new JoinedHouseholdsIdentifierFactory(scenario, this.selectHouseholdMeetingPoint, 
				this.modeAvailabilityChecker.createInstance(), this.jointDepartureOrganizer, mobsimDataProvider, 
				this.householdDepartureManager);
		duringActivityFactory.addAgentFilterFactory(notInitialReplanningFilterFactory);
		this.joinedHouseholdsIdentifier = duringActivityFactory.createIdentifier();
		
		/*
		 * During Leg Identifiers
		 */
		duringLegFactory = new LegPerformingIdentifierFactory(this.withinDayControlerListener.getLinkReplanningMap(), mobsimDataProvider);
		duringLegFactory.addAgentFilterFactory(initialReplanningFilterFactory);
		this.legPerformingIdentifier = duringLegFactory.createIdentifier();

		duringLegFactory = new AgentsToDropOffIdentifierFactory(mobsimDataProvider, this.linkEnteredProvider, this.jointDepartureOrganizer, 
				this.jointDepartureCoordinator, affectedAgentsFilterFactory, carOnlyTransportModeFilterFactory, notInitialReplanningFilterFactory, 
				earliestLinkExitTimeFilterFactory);
		this.agentsToDropOffIdentifier = duringLegFactory.createIdentifier();
		this.jointDepartureCoordinator.setAgentsToDropOffIdentifier((AgentsToDropOffIdentifier) this.agentsToDropOffIdentifier);
		
		duringLegFactory = new AgentsToPickupIdentifierFactory(scenario, this.coordAnalyzer, this.vehiclesTracker,
				mobsimDataProvider, this.withinDayControlerListener.getEarliestLinkExitTimeProvider(), this.informedHouseholdsTracker, 
				this.decisionModelRunner.getDecisionDataProvider(), this.jointDepartureOrganizer, this.jointDepartureCoordinator,
				affectedAgentsFilterFactory, walkOnlyTransportModeFilterFactory, notInitialReplanningFilterFactory, activityStartingFilterFactory); 
		duringLegFactory.addAgentFilterFactory(notInitialReplanningFilterFactory);
		this.agentsToPickupIdentifier = duringLegFactory.createIdentifier();
		this.jointDepartureCoordinator.setAgentsToPickupIdentifier((AgentsToPickupIdentifier) this.agentsToPickupIdentifier);
		
//		Set<String> duringLegRerouteTransportModes = new HashSet<String>();
//		duringLegRerouteTransportModes.add(TransportMode.car);
//		this.duringLegRerouteIdentifier = new LeaveLinkIdentifierFactory(this.getLinkReplanningMap(), duringLegRerouteTransportModes).createIdentifier();
		// replan all transport modes except PT
		duringLegFactory = new LeaveLinkIdentifierFactory(this.withinDayControlerListener.getLinkReplanningMap(), mobsimDataProvider); 
		duringLegFactory.addAgentFilterFactory(notInitialReplanningFilterFactory);
		duringLegFactory.addAgentFilterFactory(nonPTLegAgentsFilterFactory);
		duringLegFactory.addAgentFilterFactory(new ProbabilityFilterFactory(EvacuationConfig.duringLegReroutingShare));
		this.duringLegRerouteIdentifier = duringLegFactory.createIdentifier();
		
		this.initialReplanningRemover.addIdentifier(this.joinedHouseholdsIdentifier);
		this.initialReplanningRemover.addIdentifier(this.activityPerformingIdentifier);
		this.initialReplanningRemover.addIdentifier(this.legPerformingIdentifier);
		this.initialReplanningRemover.addIdentifier(this.agentsToDropOffIdentifier);
		this.initialReplanningRemover.addIdentifier(this.agentsToPickupIdentifier);
		this.initialReplanningRemover.addIdentifier(this.duringLegRerouteIdentifier);
	}
	
	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalized Router.
	 */
	private void initReplanners(Controler controler) {
		
		Scenario scenario = controler.getScenario();
		WithinDayEngine withinDayEngine = this.withinDayControlerListener.getWithinDayEngine();
		DecisionDataProvider decisionDataProvider = this.decisionModelRunner.getDecisionDataProvider();
		Provider<TripRouter> tripRouterFactory = this.withinDayTripRouterFactory;
		
		TravelDisutility travelDisutility = this.withinDayTravelDisutilityFactory.createTravelDisutility(
				this.withinDayTravelTimes.get(TransportMode.car), scenario.getConfig().planCalcScore()); 
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, this.withinDayTravelTimes.get(TransportMode.car));
		
		EvacuationPTTravelTime ptTravelTime = (EvacuationPTTravelTime) this.withinDayTravelTimes.get(TransportMode.pt);
		PTTravelTimeEvacuationCalculator ptTravelTimeEvacuation = new PTTravelTimeEvacuationCalculator(this.evacuationTransitRouterFactory,
				ptTravelTime, this.informedHouseholdsTracker);
		
		/*
		 * During Activity Replanners
		 */
		this.currentActivityToMeetingPointReplannerFactory = new CurrentActivityToMeetingPointReplannerFactory(scenario, 
				withinDayEngine, decisionDataProvider, this.modeAvailabilityChecker, 
				ptTravelTimeEvacuation, tripRouterFactory, routingContext);
		this.currentActivityToMeetingPointReplannerFactory.addIdentifier(this.activityPerformingIdentifier);
		withinDayEngine.addTimedDuringActivityReplannerFactory(this.currentActivityToMeetingPointReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
		
		this.joinedHouseholdsReplannerFactory = new JoinedHouseholdsReplannerFactory(scenario, withinDayEngine, decisionDataProvider,
				(JoinedHouseholdsIdentifier) joinedHouseholdsIdentifier, ptTravelTimeEvacuation, tripRouterFactory, routingContext);
		this.joinedHouseholdsReplannerFactory.addIdentifier(joinedHouseholdsIdentifier);
		withinDayEngine.addTimedDuringActivityReplannerFactory(this.joinedHouseholdsReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);

		/*
		 * During Leg Replanners
		 */
		this.currentLegToMeetingPointReplannerFactory = new CurrentLegToMeetingPointReplannerFactory(scenario, withinDayEngine,
				decisionDataProvider, tripRouterFactory, routingContext);
		this.currentLegToMeetingPointReplannerFactory.addIdentifier(this.legPerformingIdentifier);
		withinDayEngine.addTimedDuringLegReplannerFactory(this.currentLegToMeetingPointReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);

		this.dropOffAgentsReplannerFactory = new DropOffAgentReplannerFactory(scenario, withinDayEngine, tripRouterFactory, routingContext,
				(AgentsToDropOffIdentifier) this.agentsToDropOffIdentifier);
		this.dropOffAgentsReplannerFactory.addIdentifier(this.agentsToDropOffIdentifier);
		withinDayEngine.addTimedDuringLegReplannerFactory(this.dropOffAgentsReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
		
		this.pickupAgentsReplannerFactory = new PickupAgentReplannerFactory(scenario, withinDayEngine, (AgentsToPickupIdentifier) this.agentsToPickupIdentifier);
		this.pickupAgentsReplannerFactory.addIdentifier(this.agentsToPickupIdentifier);
		withinDayEngine.addTimedDuringLegReplannerFactory(this.pickupAgentsReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
		
		this.duringLegRerouteReplannerFactory = new CurrentLegReplannerFactory(scenario, withinDayEngine, tripRouterFactory);
		this.duringLegRerouteReplannerFactory.addIdentifier(this.duringLegRerouteIdentifier);
		withinDayEngine.addTimedDuringLegReplannerFactory(this.duringLegRerouteReplannerFactory, EvacuationConfig.evacuationTime, Double.MAX_VALUE);
		
		/*
		 * Collect Replanners that can be disabled after all agents have been informed.
		 */
		this.initialReplanningRemover.addInitialReplannerFactory(this.currentActivityToMeetingPointReplannerFactory);
		this.initialReplanningRemover.addInitialReplannerFactory(this.currentLegToMeetingPointReplannerFactory);
	}
	/*
	 * This is a workaround... find a better solution...
	 */
	private static class InitialReplanningRemover implements MobsimAfterSimStepListener {

		/*
		 * Replanners that are used to adapt agent's plans for the first time. They can be disabled
		 * after all agents have been informed and have adapted their plans.
		 */
		private final List<WithinDayReplannerFactory<?>> initialReplannerFactories = new ArrayList<WithinDayReplannerFactory<?>>();
		private final List<AgentSelector> identifiers = new ArrayList<AgentSelector>();
		
		private final WithinDayEngine withinDayEngine; 
		private final ReplanningTracker replanningTracker;
		
		private boolean disabled = false;
			
		public InitialReplanningRemover(WithinDayEngine withinDayEngine, ReplanningTracker replanningTracker) {
			this.withinDayEngine = withinDayEngine;
			this.replanningTracker = replanningTracker;
		}
		
		public void addIdentifier(AgentSelector identifier) {
			this.identifiers.add(identifier);
		}
		
		public void addInitialReplannerFactory(WithinDayReplannerFactory<?> factory) {
			this.initialReplannerFactories.add(factory);
		}
		
		@Override
		public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
			if (!disabled && this.replanningTracker.allAgentsInitiallyReplanned()) {
				for (WithinDayReplannerFactory<?> factory : this.initialReplannerFactories) {
					if (factory instanceof WithinDayDuringActivityReplannerFactory) {
						this.withinDayEngine.removeDuringActivityReplannerFactory((WithinDayDuringActivityReplannerFactory) factory);
					} else if(factory instanceof WithinDayDuringLegReplannerFactory) {
						this.withinDayEngine.removeDuringLegReplannerFactory((WithinDayDuringLegReplannerFactory) factory);
					} 
				}
				log.info("Disabled " + this.initialReplannerFactories.size() + " initial within-day replanners.");

				int removedAgentFilters = 0;
				for (AgentSelector identifier : this.identifiers) {
					List<AgentFilter> filtersToRemove = new ArrayList<AgentFilter>();
					for (AgentFilter agentFilter : identifier.getAgentFilters()) {
						if (agentFilter instanceof InformedAgentsFilter) filtersToRemove.add(agentFilter);
					}
					for (AgentFilter agentFilter : filtersToRemove) {
						identifier.removeAgentFilter(agentFilter);
						removedAgentFilters++;
					}
				}
				log.info("Disabled " + removedAgentFilters + " initial within-day filters.");
				
				this.disabled = true;
			}
		}
		
	}
}
