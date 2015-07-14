/* *********************************************************************** *
 * project: org.matsim.*
 * SelectHouseholdMeetingPoint.java
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

package playground.christoph.evacuation.withinday.replanning.utils;

import com.google.inject.Provider;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.vehicles.Vehicle;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.controler.EvacuationTripRouterFactory;
import playground.christoph.evacuation.mobsim.InformedHouseholdsTracker;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisiondata.HouseholdDecisionData;
import playground.christoph.evacuation.mobsim.decisionmodel.DecisionModelRunner;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.Participating;
import playground.christoph.evacuation.network.AddExitLinksToNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Decides where a household will meet after the evacuation order has been given.
 * This could be either at home or at another location, if the home location is
 * not treated to be secure. However, households might meet at their insecure home
 * location and then evacuate as a unit.
 * 
 * By default, all households meet at home and the select another location, if
 * their home location is not secure.
 * 
 * @author cdobler
 */
public class SelectHouseholdMeetingPoint implements MobsimBeforeSimStepListener,
	BeforeMobsimListener, AfterMobsimListener {

	private static final Logger log = Logger.getLogger(SelectHouseholdMeetingPoint.class);

	/*
	 * For debugging:
	 * If enabled, the routes (and their travel times) used to decide where to meet
	 * are written to files.
	 */
	public static boolean writeRoutesToFiles = true; 
	
	private final Scenario scenario;
	private final Map<String, TravelTime> travelTimes;
	private final CoordAnalyzer coordAnalyzer;
	private final Geometry affectedArea;
	private final ModeAvailabilityChecker modeAvailabilityChecker;
	private final InformedHouseholdsTracker informedHouseholdsTracker;
	private final int numOfThreads;
	private final DecisionDataProvider decisionDataProvider;
	private final MobsimDataProvider mobsimDataProvider;
	private final Provider<TransitRouter> transitRouterFactory;
	
	private TravelDisutilityFactory disutilityFactory;
	private TripRouterFactory toHomeFacilityRouterFactory;
	private TripRouterFactory fromHomeFacilityRouterFactory;
	
	private Thread[] threads;
	private Runnable[] runnables;
	
	private CyclicBarrier startBarrier;
	private CyclicBarrier endBarrier;
	private AtomicBoolean allMeetingsPointsSelected;
	
	/*
	 * Only for some statistics
	 */
	private int meetAtHome = 0;
	private int meetAtRescue = 0;
	private int meetSecure = 0;
	private int meetInsecure = 0;

	/*
	 * Only for debugging
	 */
	private String subScenarioNetworkFile;
	private String toHomePlansFile;
	private String directEvacuationPlansFile;
	private Network evacuationSubNetwork;
	
	public SelectHouseholdMeetingPoint(Scenario scenario, Map<String,TravelTime> travelTimes,
			CoordAnalyzer coordAnalyzer, Geometry affectedArea, InformedHouseholdsTracker informedHouseholdsTracker, 
			DecisionModelRunner decisionModelRunner, MobsimDataProvider mobsimDataProvider,
									   Provider<TransitRouter> transitRouterFactory) {
		this.scenario = scenario;
		this.travelTimes = travelTimes;
		this.coordAnalyzer = coordAnalyzer;
		this.affectedArea = affectedArea;
		this.informedHouseholdsTracker = informedHouseholdsTracker;
		this.mobsimDataProvider = mobsimDataProvider;
		this.transitRouterFactory = transitRouterFactory;
		
		this.numOfThreads = this.scenario.getConfig().global().getNumberOfThreads();
		this.allMeetingsPointsSelected = new AtomicBoolean(false);
		this.decisionDataProvider = decisionModelRunner.getDecisionDataProvider();
		this.modeAvailabilityChecker = new ModeAvailabilityChecker(this.scenario, this.mobsimDataProvider);
		
		init();
	}
	
	private void init() {
		
		this.disutilityFactory = new OnlyTimeDependentTravelDisutilityFactory();
		TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults();
		LeastCostPathCalculatorFactory leastCostPathCalculatorFactory = builder.createDefaultLeastCostPathCalculatorFactory(this.scenario);
//		TransitRouterFactory transitRouterFactory = null;
//		if (this.scenario.getConfig().transit().isUseTransit()) transitRouterFactory = builder.createDefaultTransitRouter(this.scenario);
		
		this.toHomeFacilityRouterFactory = new EvacuationTripRouterFactory(this.scenario, this.travelTimes, 
				this.disutilityFactory, leastCostPathCalculatorFactory, this.transitRouterFactory);
		
		/*
		 * A AStarLandmarks router might become confused by the exit links in the evacuation sub-network.
		 * Therefore use a plain Dijkstra.
		 */
		leastCostPathCalculatorFactory = new FastDijkstraFactory();
		
		Scenario fromHomeScenario = new CreateEvacuationAreaSubScenario(this.scenario, this.coordAnalyzer, this.affectedArea, 
				this.travelTimes.keySet()).createSubScenario();
		Map<String, TravelTime> fromHomeTravelTimes = new HashMap<String, TravelTime>();
		for (Entry<String, TravelTime> entry : this.travelTimes.entrySet()) {
			fromHomeTravelTimes.put(entry.getKey(), new TravelTimeWrapper(entry.getValue(), true));
		}
		
		this.fromHomeFacilityRouterFactory = new EvacuationTripRouterFactory(fromHomeScenario, fromHomeTravelTimes, 
				this.disutilityFactory, leastCostPathCalculatorFactory, this.transitRouterFactory);
		
		// for debugging
		this.evacuationSubNetwork = fromHomeScenario.getNetwork();
	}
	
	/*
	 * So far, households will directly meet at a rescue facility or at their home facility. 
	 * For the later case, they have to select a next meeting point, when all household 
	 * members have arrived at their current meeting point. At the moment, this next meeting 
	 * point is hard coded as a rescue facility.
	 * 
	 * So far, there is only a single rescue facility. Instead, multiple *real* rescue 
	 * facilities could be defined. 
	 */
	public Id selectNextMeetingPoint(Id householdId) {
		
		// if the household evacuates, select rescue facility as meeting point
		HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(householdId);
		
		boolean householdParticipates;
		Participating participating = hdd.getParticipating();
		if (participating == Participating.TRUE) householdParticipates = true;
		else if (participating == Participating.FALSE) householdParticipates = false;
		else throw new RuntimeException("Households participation state is undefined: " + householdId.toString());
		
		if (householdParticipates) {
			Id<ActivityFacility> rescueMeetingPointId = Id.create("rescueFacility", ActivityFacility.class);
			hdd.setMeetingPointFacilityId(rescueMeetingPointId);
			return rescueMeetingPointId;
		}
		// otherwise meet and stay at home
		else return hdd.getHomeFacilityId();
	}
	
	/*
	 * If the evacuation starts in the current time step, start defining the
	 * household meeting points.
	 */
	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		
		/*
		 * If this value is set to true, nothing else is left to do.
		 */
		if (this.allMeetingsPointsSelected.get()) return;
		
		double time = e.getSimulationTime();
		
		if (time >= EvacuationConfig.evacuationTime) {
			try {
				// If no household was informed in the current time step, we don't have to trigger the runners.
				if (this.informedHouseholdsTracker.getHouseholdsInformedInLastTimeStep().isEmpty()) {
//						log.info("No households informed in the current timestep.");
					return;
				}
				
				// set current Time
				for (Runnable runnable : this.runnables) {
					((SelectHouseholdMeetingPointRunner) runnable).setTime(time);
				}
				
				// assign households to threads
				int roundRobin = 0;				
				
				for (Id householdId : this.informedHouseholdsTracker.getHouseholdsInformedInLastTimeStep()) {
					((SelectHouseholdMeetingPointRunner) runnables[roundRobin % this.numOfThreads]).addHouseholdToCheck(householdId);
					roundRobin++;
				}
				
				/*
				 * Reach the start barrier to trigger the threads.
				 */
				this.startBarrier.await();
				
				this.endBarrier.await();		
				
				/*
				 * Calculate statistics
				 * 
				 * Previously this was executed between startBarrier.await() and endBarrier.await().
				 * However, this seems to be not thread-safe. Therefore I moved it below the 
				 * endBarrier.await() line. cdobler, jun'12.
				 */
				for (Id householdId : this.informedHouseholdsTracker.getHouseholdsInformedInLastTimeStep()) {
					
					HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(householdId);
					
					Id homeFacilityId = hdd.getHomeFacilityId();
					Id meetingPointFacilityId = hdd.getMeetingPointFacilityId();
					
					if (homeFacilityId.equals(meetingPointFacilityId)) meetAtHome++;
					else meetAtRescue++;
					
					ActivityFacility meetingFacility = ((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities().get(meetingPointFacilityId);
					if (this.coordAnalyzer.isFacilityAffected(meetingFacility)) meetInsecure++;
					else meetSecure++;
				}
				
				/*
				 * Check whether all households have been informed and handled. If yes,
				 * do not execute the code from this method anymore.
				 */
				boolean terminate = (informedHouseholdsTracker.allHouseholdsInformed());
				if (terminate) {
					/*
					 * Inform the threads that no further meeting points have to be
					 * selected. Then reach the start barrier to trigger the threads.
					 * As as result, the threads will terminate.
					 */
					this.allMeetingsPointsSelected.set(true);
					this.startBarrier.await();
					
					/*
					 * Write some files for debugging.
					 * Agents should be sorted by the PopulationWriter.
					 */
					if (writeRoutesToFiles) {
						Scenario sc;
						
						sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
						for (Runnable runnable : this.runnables) {
							for (Person person : ((SelectHouseholdMeetingPointRunner) runnable).toHomePlans) {
								sc.getPopulation().addPerson(person);
							}
							((SelectHouseholdMeetingPointRunner) runnable).toHomePlans.clear();
						}
						new PopulationWriter(sc.getPopulation(), this.scenario.getNetwork()).write(this.toHomePlansFile);
						
						/*
						 * For the evacuation from home multiple modes can be checked. Therefore,
						 * merge all of them in a single person having multiple plans.
						 */
						sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
						for (Runnable runnable : this.runnables) {
							for (Person person : ((SelectHouseholdMeetingPointRunner) runnable).directEvacuationPlans) {
								Person existingPerson = (sc.getPopulation().getPersons().get(person.getId()));
								if (existingPerson != null) existingPerson.addPlan(person.getSelectedPlan());
								else sc.getPopulation().addPerson(person);
							}
							((SelectHouseholdMeetingPointRunner) runnable).directEvacuationPlans.clear();
						}
						new PopulationWriter(sc.getPopulation(), this.evacuationSubNetwork).write(this.directEvacuationPlansFile);
					}
					
					/*
					 * Finally, print some statistics.
					 */
					log.info("Households meet at home facility:   " + meetAtHome);
					log.info("Households meet at rescue facility: " + meetAtRescue);
					log.info("Households meet at secure place:   " + meetSecure);
					log.info("Households meet at insecure place: " + meetInsecure);					
				}		
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			} catch (BrokenBarrierException ex) {
				throw new RuntimeException(ex);
			}	
		}
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		initThreads();
		
		int iter = event.getIteration();
		OutputDirectoryHierarchy outputDirectoryHierarchy = event.getControler().getControlerIO();
		this.subScenarioNetworkFile = outputDirectoryHierarchy.getIterationFilename(iter, "EvacuationAreaSubScenarioNetwork.xml.gz");
		this.toHomePlansFile = outputDirectoryHierarchy.getIterationFilename(iter, "SelectHouseholdMeetingPointToHomePlans.xml.gz");
		this.directEvacuationPlansFile = outputDirectoryHierarchy.getIterationFilename(iter, "SelectHouseholdMeetingPointDirectEvacuationPlans.xml.gz");
		
		new NetworkWriter(this.evacuationSubNetwork).write(this.subScenarioNetworkFile);
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		
		shutdownThreads();
	}
	
	private void initThreads() {
		threads = new Thread[this.numOfThreads];
		runnables = new SelectHouseholdMeetingPointRunner[this.numOfThreads];
		
		this.startBarrier = new CyclicBarrier(numOfThreads + 1);
		this.endBarrier = new CyclicBarrier(numOfThreads + 1);
		
		// use a TravelTimeWrapper that returns a travel time of 1.0 second for exit links
		TravelTime travelTime = new TravelTimeWrapper(this.travelTimes.get(TransportMode.car), false);
		TravelDisutility travelDisutility = this.disutilityFactory.createTravelDisutility(travelTime, this.scenario.getConfig().planCalcScore());
		RoutingContext toHomeRoutingContext = new RoutingContextImpl(travelDisutility, travelTime);
		
		/*
		 * Use a TravelTimeWrapper that returns a travel time of 1.0 second for exit links and
		 * enable link wrapping to prevent the TravelTimeCollector using indices from the
		 * EvacuationSubNetwork (which would not match the indices in the Collector's data structure).
		 */
		TravelTime fromHomeTravelTime = new TravelTimeWrapper(this.travelTimes.get(TransportMode.car), true);
		TravelDisutility fromHomeTravelDisutility = this.disutilityFactory.createTravelDisutility(fromHomeTravelTime, this.scenario.getConfig().planCalcScore());
		RoutingContext fromHomeRoutingContext = new RoutingContextImpl(fromHomeTravelDisutility, fromHomeTravelTime);
		
		for (int i = 0; i < this.numOfThreads; i++) {			
			SelectHouseholdMeetingPointRunner runner = new SelectHouseholdMeetingPointRunner(scenario, 
					coordAnalyzer.createInstance(), mobsimDataProvider, modeAvailabilityChecker.createInstance(), 
					decisionDataProvider, startBarrier, endBarrier, allMeetingsPointsSelected,
					toHomeFacilityRouterFactory.instantiateAndConfigureTripRouter(toHomeRoutingContext), 
					fromHomeFacilityRouterFactory.instantiateAndConfigureTripRouter(fromHomeRoutingContext));
			runner.setTime(Time.UNDEFINED_TIME);
			runnables[i] = runner; 
			
			Thread thread = new Thread(runner);
			thread.setDaemon(true);	
			thread.setName(runner.getClass().getName() + i);
			threads[i] = thread;
		}
		
		// start threads
		for (Thread thread : threads) thread.start();
	}
	
	private void shutdownThreads() {
		try {
			for (Thread thread : threads) thread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	/*
	 * Wrapper around a travel time object that returns 1.0 as travel time on
	 * exit links.
	 * 
	 * This class should be thread-safe now... (Having a single LinkWrapper which
	 * is re-used would not be thread-safe!)
	 */
	private static class TravelTimeWrapper implements TravelTime {

		private final TravelTime travelTime;
		private final boolean useLinkWrapper;
		
		public TravelTimeWrapper(TravelTime travelTime, boolean useLinkWrapper) {
			this.travelTime = travelTime;
			this.useLinkWrapper = useLinkWrapper;
		}
		
		@Override
		public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
			if (link.getId().toString().startsWith(AddExitLinksToNetwork.exitLink)) return 1.0;
			else {
				if (useLinkWrapper) {
					LinkWrapper linkWrapper = new LinkWrapper(link);
					return travelTime.getLinkTravelTime(linkWrapper, time, person, vehicle);
				} else return travelTime.getLinkTravelTime(link, time, person, vehicle);
			}
		}
	}
	
	/*
	 * Wrapper around a link object which prevents a link from being
	 * identified as object that implements the HasIndex interface.
	 * 
	 * This is, because otherwise the TravelTimeCollector would return wrong
	 * results in combination with an ArrayRouterNetwork and the EvacuationSubScenario.
	 * 
	 * In an ArrayRouterNetwork all links are enumerated. However, the 
	 * EvacuationSubScenario contains fewer links than the original network.
	 * Therefore, the indices do not match. This is fine for the router but
	 * not for the TravelTimeCollector. Therefore, this class "removes" the
	 * indices from the links.
	 */
	private static class LinkWrapper implements Link {

		private final Link link;

		public LinkWrapper(Link link) {
			this.link = link;
		}
		
		@Override
		public Coord getCoord() { return link.getCoord(); }

		@Override
		public Id getId() { return link.getId(); }

		@Override
		public boolean setFromNode(Node node) { return link.setFromNode(node); }

		@Override
		public boolean setToNode(Node node) { return link.setToNode(node); }

		@Override
		public Node getToNode() { return link.getToNode(); }

		@Override
		public Node getFromNode() { return link.getFromNode(); }

		@Override
		public double getLength() { return link.getLength(); }

		@Override
		public double getNumberOfLanes() { return link.getNumberOfLanes(); }

		@Override
		public double getNumberOfLanes(double time) { return link.getNumberOfLanes(time); }

		@Override
		public double getFreespeed() { return link.getFreespeed(); }

		@Override
		public double getFreespeed(double time) { return link.getFreespeed(time); }

		@Override
		public double getCapacity() { return link.getCapacity(); }

		@Override
		public double getCapacity(double time) { return link.getCapacity(); }

		@Override
		public void setFreespeed(double freespeed) { link.setFreespeed(freespeed); }

		@Override
		public void setLength(double length) { link.setLength(length); }

		@Override
		public void setNumberOfLanes(double lanes) { link.setNumberOfLanes(lanes); }

		@Override
		public void setCapacity(double capacity) { link.setCapacity(capacity); }

		@Override
		public void setAllowedModes(Set<String> modes) { link.setAllowedModes(modes); }

		@Override
		public Set<String> getAllowedModes() { return link.getAllowedModes(); }
	}
}