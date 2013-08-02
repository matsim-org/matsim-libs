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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.vehicles.Vehicle;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.controler.EvacuationTripRouterFactory;
import playground.christoph.evacuation.mobsim.InformedHouseholdsTracker;
import playground.christoph.evacuation.mobsim.MobsimDataProvider;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisiondata.HouseholdDecisionData;
import playground.christoph.evacuation.mobsim.decisionmodel.DecisionModelRunner;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.Participating;

import com.vividsolutions.jts.geom.Geometry;

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
public class SelectHouseholdMeetingPoint implements MobsimBeforeSimStepListener {

	private static final Logger log = Logger.getLogger(SelectHouseholdMeetingPoint.class);
	
	private final Scenario scenario;
	private final Map<String, TravelTime> travelTimes;
	private final CoordAnalyzer coordAnalyzer;
	private final Geometry affectedArea;
	private final ModeAvailabilityChecker modeAvailabilityChecker;
	private final InformedHouseholdsTracker informedHouseholdsTracker;
	private final int numOfThreads;
	private final DecisionDataProvider decisionDataProvider;
	private final MobsimDataProvider mobsimDataProvider;
	
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
	
	public SelectHouseholdMeetingPoint(Scenario scenario, Map<String,TravelTime> travelTimes,
			CoordAnalyzer coordAnalyzer, Geometry affectedArea, ModeAvailabilityChecker modeAvailabilityChecker, 
			InformedHouseholdsTracker informedHouseholdsTracker, DecisionModelRunner decisionModelRunner, 
			MobsimDataProvider mobsimDataProvider) {
		this.scenario = scenario;
		this.travelTimes = travelTimes;
		this.coordAnalyzer = coordAnalyzer;
		this.affectedArea = affectedArea;
		this.modeAvailabilityChecker = modeAvailabilityChecker;
		this.informedHouseholdsTracker = informedHouseholdsTracker;
		this.mobsimDataProvider = mobsimDataProvider;
		
		this.numOfThreads = this.scenario.getConfig().global().getNumberOfThreads();
		this.allMeetingsPointsSelected = new AtomicBoolean(false);
		this.decisionDataProvider = decisionModelRunner.getDecisionDataProvider();
		
		init();
	}
	
	private void init() {
		
		this.disutilityFactory = new OnlyTimeDependentTravelCostCalculatorFactory();
		TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults();
		LeastCostPathCalculatorFactory leastCostPathCalculatorFactory = builder.createDefaultLeastCostPathCalculatorFactory(this.scenario);
		TransitRouterFactory transitRouterFactory = null;
		if (this.scenario.getConfig().scenario().isUseTransit()) transitRouterFactory = builder.createDefaultTransitRouter(this.scenario);
		
		this.toHomeFacilityRouterFactory = new EvacuationTripRouterFactory(this.scenario, this.travelTimes, 
				this.disutilityFactory, leastCostPathCalculatorFactory, transitRouterFactory);
		
		Scenario fromHomeScenario = new CreateEvacuationAreaSubScenario(this.scenario, this.coordAnalyzer, this.affectedArea, 
				this.travelTimes.keySet()).createSubScenario();
		Map<String, TravelTime> fromHomeTravelTimes = new HashMap<String, TravelTime>();
		for (Entry<String, TravelTime> entry : this.travelTimes.entrySet()) {
			fromHomeTravelTimes.put(entry.getKey(), new TravelTimeWrapper(entry.getValue()));
		}
		
		this.fromHomeFacilityRouterFactory = new EvacuationTripRouterFactory(fromHomeScenario, fromHomeTravelTimes, 
				this.disutilityFactory, leastCostPathCalculatorFactory, transitRouterFactory); 
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
		HouseholdDecisionData hdd = decisionDataProvider.getHouseholdDecisionData(householdId);
		
		boolean householdParticipates;
		Participating participating = hdd.getParticipating();
		if (participating == Participating.TRUE) householdParticipates = true;
		else if (participating == Participating.FALSE) householdParticipates = false;
		else throw new RuntimeException("Households participation state is undefined: " + householdId.toString());
		
		if (householdParticipates) {
			Id rescueMeetingPointId = scenario.createId("rescueFacility");
			hdd.setMeetingPointFacilityId(rescueMeetingPointId);
			return rescueMeetingPointId;
		}
		// otherwise meet and stay at home
		else return hdd.getHomeFacilityId();
	}
	
	/*
	 * If the evacuation starts in the current time step, define the
	 * household meeting points.
	 */
	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		
		/*
		 * If this value is set to true, nothing else is left to do.
		 */
		if (this.allMeetingsPointsSelected.get()) return;
		
		double time = e.getSimulationTime();
		if (time == EvacuationConfig.evacuationTime) initThreads(time);
		
		if (time >= EvacuationConfig.evacuationTime) {
			try {
				if (informedHouseholdsTracker.allHouseholdsInformed()) {
					/*
					 * Inform the threads that no further meeting points have to be
					 * selected. Then reach the start barrier to trigger the threads.
					 * As as result, the threads will terminate.
					 */
					this.allMeetingsPointsSelected.set(true);
					this.startBarrier.await();
						
					/*
					 * Finally, print some statistics.
					 */
					log.info("Households meet at home facility:   " + meetAtHome);
					log.info("Households meet at rescue facility: " + meetAtRescue);
					log.info("Households meet at secure place:   " + meetSecure);
					log.info("Households meet at insecure place: " + meetInsecure);
				} else {
					// set current Time
					for (Runnable runnable : this.runnables) {
						((SelectHouseholdMeetingPointRunner) runnable).setTime(time);
					}
					
					// If no household was informed in the current time step, we don't have to trigger the runners.
					if (this.informedHouseholdsTracker.getHouseholdsInformedInLastTimeStep().size() == 0) {
//						log.info("No households informed in the current timestep.");
						return;
					}
					
					// assign households to threads
					int roundRobin = 0;
					
					Households households = ((ScenarioImpl) scenario).getHouseholds();
					for (Id householdId : this.informedHouseholdsTracker.getHouseholdsInformedInLastTimeStep()) {
						
						Household household = households.getHouseholds().get(householdId);
						
						// ignore empty households
						if (household.getMemberIds().size() == 0) continue;
												
						((SelectHouseholdMeetingPointRunner) runnables[roundRobin % this.numOfThreads]).addHouseholdToCheck(household);
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
						
						Household household = households.getHouseholds().get(householdId);
						
						// ignore empty households
						if (household.getMemberIds().size() == 0) continue;
						
						HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(householdId);

						Id homeFacilityId = hdd.getHomeFacilityId();
						Id meetingPointFacilityId = hdd.getMeetingPointFacilityId();
						
						if (homeFacilityId.equals(meetingPointFacilityId)) meetAtHome++;
						else meetAtRescue++;
						
						ActivityFacility meetingFacility = ((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities().get(meetingPointFacilityId);
						if (this.coordAnalyzer.isFacilityAffected(meetingFacility)) meetInsecure++;
						else meetSecure++;
					}
				}		
			} catch (InterruptedException ex) {
				Gbl.errorMsg(ex);
			} catch (BrokenBarrierException ex) {
				Gbl.errorMsg(ex);
			}	
		}
	}
	
	private void initThreads(double time) {
		threads = new Thread[this.numOfThreads];
		runnables = new SelectHouseholdMeetingPointRunner[this.numOfThreads];
		
		this.startBarrier = new CyclicBarrier(numOfThreads + 1);
		this.endBarrier = new CyclicBarrier(numOfThreads + 1);

		TravelTime travelTime = this.travelTimes.get(TransportMode.car);
		TravelDisutility travelDisutility = this.disutilityFactory.createTravelDisutility(travelTime, this.scenario.getConfig().planCalcScore());
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, travelTime);

		// use a TravelTimeWrapper that returns a travel time of 1.0 second for exit links
		TravelTime fromHomeTravelTime = new TravelTimeWrapper(travelTime);
		TravelDisutility fromHomeTravelDisutility = this.disutilityFactory.createTravelDisutility(fromHomeTravelTime, this.scenario.getConfig().planCalcScore());
		RoutingContext fromHomeRoutingContext = new RoutingContextImpl(fromHomeTravelDisutility, fromHomeTravelTime);
		
		for (int i = 0; i < this.numOfThreads; i++) {
			
			SelectHouseholdMeetingPointRunner runner = new SelectHouseholdMeetingPointRunner(scenario, 
					toHomeFacilityRouterFactory.instantiateAndConfigureTripRouter(routingContext), 
					fromHomeFacilityRouterFactory.instantiateAndConfigureTripRouter(fromHomeRoutingContext), 
					coordAnalyzer.createInstance(), mobsimDataProvider, modeAvailabilityChecker.createInstance(), 
					decisionDataProvider, startBarrier, endBarrier, allMeetingsPointsSelected);
			runner.setTime(time);
			runnables[i] = runner; 
					
			Thread thread = new Thread(runner);
			thread.setDaemon(true);
			thread.setName(SelectHouseholdMeetingPointRunner.class.toString() + i);
			threads[i] = thread;
		}
		
		// start threads
		for (Thread thread : threads) thread.start();
	}
	
	/*
	 * Wrapper around a travel time object that returns 1.0 as travel time on
	 * exit links.
	 */
	private static class TravelTimeWrapper implements TravelTime {

		private final TravelTime travelTime;
		
		public TravelTimeWrapper(TravelTime travelTime) {
			this.travelTime = travelTime;
		}
		
		@Override
		public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
			if (link.getId().toString().contains("exit")) return 1.0;
			else return travelTime.getLinkTravelTime(link, time, person, vehicle);
		}
	}
}