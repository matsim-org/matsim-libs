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
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.router.MultimodalTripRouterFactory;
import org.matsim.contrib.multimodal.tools.MultiModalNetworkCreator;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.VehiclesTracker;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisiondata.HouseholdDecisionData;
import playground.christoph.evacuation.mobsim.decisionmodel.DecisionModelRunner;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.Participating;
import playground.christoph.evacuation.mobsim.decisionmodel.LatestAcceptedLeaveTimeModel;
import playground.christoph.evacuation.network.AddZCoordinatesToNetwork;
import playground.christoph.evacuation.withinday.replanning.identifiers.InformedHouseholdsTracker;

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
public class SelectHouseholdMeetingPoint implements MobsimInitializedListener, MobsimBeforeSimStepListener {

	private static final Logger log = Logger.getLogger(SelectHouseholdMeetingPoint.class);
	
	private final Controler controler;
	private final Scenario scenario;
	private final Map<String, TravelTime> travelTimes;
	private final Map<Id, MobsimAgent> agents;
	private final VehiclesTracker vehiclesTracker;
	private final CoordAnalyzer coordAnalyzer;
	private final Geometry affectedArea;
	private final ModeAvailabilityChecker modeAvailabilityChecker;
	private final InformedHouseholdsTracker informedHouseholdsTracker;
	private final int numOfThreads;
	private final EvacuationDecisionModel evacuationDecisionModel;
	private final LatestAcceptedLeaveTimeModel latestAcceptedLeaveTimeModel;
	private final DecisionDataProvider decisionDataProvider;

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
	
	public SelectHouseholdMeetingPoint(Controler controler, Map<String,TravelTime> travelTimes,
			VehiclesTracker vehiclesTracker, CoordAnalyzer coordAnalyzer, Geometry affectedArea, 
			ModeAvailabilityChecker modeAvailabilityChecker, InformedHouseholdsTracker informedHouseholdsTracker,
			DecisionDataProvider decisionDataProvider, DecisionModelRunner decisionModelRunner) {
		this.controler = controler;
		this.travelTimes = travelTimes;
		this.vehiclesTracker = vehiclesTracker;
		this.coordAnalyzer = coordAnalyzer;
		this.affectedArea = affectedArea;
		this.modeAvailabilityChecker = modeAvailabilityChecker;
		this.informedHouseholdsTracker = informedHouseholdsTracker;
		this.decisionDataProvider = decisionDataProvider;
		
		this.scenario = this.controler.getScenario();
		this.numOfThreads = this.scenario.getConfig().global().getNumberOfThreads();
		this.allMeetingsPointsSelected = new AtomicBoolean(false);
		this.evacuationDecisionModel = decisionModelRunner.getEvacuationDecisionModel();
		this.latestAcceptedLeaveTimeModel = decisionModelRunner.getLatestAcceptedLeaveTimeModel();
		this.agents = new HashMap<Id, MobsimAgent>();
		
		init();
	}
	
	private void init() {
		
		Config config = scenario.getConfig();
		
		this.disutilityFactory = new OnlyTimeDependentTravelCostCalculatorFactory();
		
		this.toHomeFacilityRouterFactory = new MultimodalTripRouterFactory(this.scenario, this.travelTimes, disutilityFactory);
				
		/*
		 * Create a subnetwork that only contains the Evacuation area plus some exit nodes.
		 * This network is used to calculate estimated evacuation times starting from the 
		 * home locations which are located inside the evacuation zone.
		 */
		Scenario subScenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(subScenario).readFile(config.network().getInputFile());
		Network subNetwork = subScenario.getNetwork();

		/*
		 * If enabled in config file, convert subNetwork to a multi-modal network.
		 */
        // TODO: Refactored out of core config
        // Please just create and add the config group instead.
        MultiModalConfigGroup multiModalConfigGroup1 = (MultiModalConfigGroup) this.scenario.getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
        if (multiModalConfigGroup1 == null) {
            multiModalConfigGroup1 = new MultiModalConfigGroup();
            this.scenario.getConfig().addModule(multiModalConfigGroup1);
        }
        if (multiModalConfigGroup1.isCreateMultiModalNetwork()) {
			log.info("Creating multi modal network.");
            // TODO: Refactored out of core config
            // Please just create and add the config group instead.
            MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) this.scenario.getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
            if (multiModalConfigGroup == null) {
                multiModalConfigGroup = new MultiModalConfigGroup();
                this.scenario.getConfig().addModule(multiModalConfigGroup);
            }
            new MultiModalNetworkCreator(multiModalConfigGroup).run(subNetwork);
		}
		
		/*
		 * Adding z-coordinates to the network
		 */
		AddZCoordinatesToNetwork zCoordinateAdder = new AddZCoordinatesToNetwork(subScenario, EvacuationConfig.dhm25File, EvacuationConfig.srtmFile);
		zCoordinateAdder.addZCoordinatesToNetwork();
		
		/*
		 * Identify affected nodes.
		 */
		Set<Id> affectedNodes = new HashSet<Id>();
		for (Node node : subNetwork.getNodes().values()) {
			if (coordAnalyzer.isNodeAffected(node)) affectedNodes.add(node.getId());
		}
		log.info("Found " + affectedNodes.size() + " nodes inside affected area.");

		/*
		 * Identify buffered affected nodes.
		 */
		CoordAnalyzer bufferedCoordAnalyzer = this.defineBufferedArea();
		Set<Id> bufferedAffectedNodes = new HashSet<Id>();
		for (Node node : subNetwork.getNodes().values()) {
			if (bufferedCoordAnalyzer.isNodeAffected(node) && !affectedNodes.contains(node.getId())) {
				bufferedAffectedNodes.add(node.getId());
			}
		}
		log.info("Found " + bufferedAffectedNodes.size() + " additional nodes inside buffered affected area.");
		
		/*
		 * Identify link that cross the evacuation line and their start and
		 * end nodes which are located right after the evacuation line.
		 */
		Set<Id> crossEvacuationLineNodes = new HashSet<Id>(bufferedAffectedNodes);
		Set<Id> crossEvacuationLineLinks = new HashSet<Id>();
		for (Link link : subNetwork.getLinks().values()) {
			boolean fromNodeInside = affectedNodes.contains(link.getFromNode().getId());
			boolean toNodeInside = affectedNodes.contains(link.getToNode().getId());
			
			if (fromNodeInside && !toNodeInside) {
				crossEvacuationLineLinks.add(link.getId());
				crossEvacuationLineNodes.add(link.getToNode().getId());
			} else if (!fromNodeInside && toNodeInside) {
				crossEvacuationLineLinks.add(link.getId());
				crossEvacuationLineNodes.add(link.getFromNode().getId());
			}
		}
		log.info("Found " + crossEvacuationLineLinks.size() + " links crossing the evacuation boarder.");
		log.info("Found " + crossEvacuationLineNodes.size() + " nodes outside the evacuation boarder.");
		
		/*
		 * Remove links and nodes.
		 */
		Set<Id> nodesToRemove = new HashSet<Id>();
		for (Node node : subNetwork.getNodes().values()) {
			if (!crossEvacuationLineNodes.contains(node.getId()) && !affectedNodes.contains(node.getId())) {
				nodesToRemove.add(node.getId());
			}
		}
		for (Id id : nodesToRemove) subNetwork.removeNode(id);	
		log.info("Remaining nodes " + subNetwork.getNodes().size());
		log.info("Remaining links " + subNetwork.getLinks().size());
	
		Set<String> transportModes = new HashSet<String>();
		transportModes.add(TransportMode.bike);
		transportModes.add(TransportMode.car);
		transportModes.add(TransportMode.pt);
		transportModes.add(TransportMode.walk);
		
		NetworkFactory networkFactory = subNetwork.getFactory();
		Coord exitNode1Coord = subScenario.createCoord(EvacuationConfig.centerCoord.getX() + 50000.0, EvacuationConfig.centerCoord.getY() + 50000.0); 
		Coord exitNode2Coord = subScenario.createCoord(EvacuationConfig.centerCoord.getX() + 50001.0, EvacuationConfig.centerCoord.getY() + 50001.0);
		Node exitNode1 = networkFactory.createNode(subScenario.createId("exitNode1"), exitNode1Coord);
		Node exitNode2 = networkFactory.createNode(subScenario.createId("exitNode2"), exitNode2Coord);
		Link exitLink = networkFactory.createLink(subScenario.createId("exitLink"), exitNode1, exitNode2);
		exitLink.setAllowedModes(transportModes);
		exitLink.setLength(1.0);
		subNetwork.addNode(exitNode1);
		subNetwork.addNode(exitNode2);
		subNetwork.addLink(exitLink);
		
		/*
		 * Create exit links for links that cross the evacuation line.
		 */
		int i = 0;
		for (Id id : crossEvacuationLineNodes) {
			Node node = subNetwork.getNodes().get(id);
			Link link = networkFactory.createLink(subScenario.createId("exitLink" + i), node, exitNode1);
			link.setAllowedModes(transportModes);
			link.setLength(1.0);
			subNetwork.addLink(link);
			i++;
		}
		
		Map<String, TravelTime> tts = new HashMap<String, TravelTime>();
		for (Entry<String, TravelTime> entry : this.travelTimes.entrySet()) {
			tts.put(entry.getKey(), new TravelTimeWrapper(entry.getValue()));
		}
			
		// use a ScenarioWrapper that returns the sub-network instead of the network
		Scenario fromHomeScenario = new ScenarioWrapper(scenario, subNetwork);
		this.fromHomeFacilityRouterFactory = new MultimodalTripRouterFactory(fromHomeScenario, this.travelTimes, disutilityFactory);
	}


	
	/*
	 * Identify facilities that are located inside the affected area. They
	 * might be attached to links which are NOT affected. However, those links
	 * still have to be included in the sub network.
	 * The links themselves are secure, therefore we can directly connect them
	 * to the exit node.
	 */
	private CoordAnalyzer defineBufferedArea() {
	
		double buffer = 0.0;
		double dBuffer = 50.0;	// buffer increase per iteration
		
		/*
		 * Identify not affected links where affected facilities are attached.
		 */
		Set<Link> links = new HashSet<Link>();
		for (ActivityFacility facility : ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().values()) {
			if (this.coordAnalyzer.isFacilityAffected(facility)) {
				Id linkId = facility.getLinkId();
				Link link = scenario.getNetwork().getLinks().get(linkId);
				
				if (!this.coordAnalyzer.isLinkAffected(link)) {
					links.add(link);
				}
			}
		}
		
		/*
		 * Increase the buffer until all links are included in the geometry.
		 */
		if (links.size() == 0) return this.coordAnalyzer;
		else {
			while (true) {
				buffer += dBuffer;
				Geometry geometry = this.affectedArea.buffer(buffer);
				
				CoordAnalyzer bufferedCoordAnalyzer = new CoordAnalyzer(geometry);
				
				boolean increaseBuffer = false;
				for (Link link : links) {
					/*
					 * If the link and/or its from/to nodes is not affected, 
					 * the buffer has to be increased.
					 */
					if (!bufferedCoordAnalyzer.isLinkAffected(link) ||
							!bufferedCoordAnalyzer.isNodeAffected(link.getFromNode()) ||
							!bufferedCoordAnalyzer.isNodeAffected(link.getToNode())) {
						increaseBuffer = true;
						break;
					}
				}
				
				if (!increaseBuffer) {
					log.info("A buffer of  " + buffer + " was required to catch all links where affected" +
							"facilities are attached to.");
					return bufferedCoordAnalyzer;
				}
			}
		}		
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
		Participating participating = this.decisionDataProvider.getHouseholdDecisionData(householdId).getParticipating();
		if (participating == Participating.TRUE) householdParticipates = true;
		else if (participating == Participating.FALSE) householdParticipates = false;
		else throw new RuntimeException("Households participation state is undefined: " + householdId.toString());
		
		if (householdParticipates) {
			Id rescueMeetingPointId = scenario.createId("rescueFacility");
			hdd.setMeetingPointFacilityId(rescueMeetingPointId);
			return rescueMeetingPointId;
		}
		// otherwise meet and stay at home
		else return this.decisionDataProvider.getHouseholdDecisionData(householdId).getHomeFacilityId();
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		
		this.agents.clear();
		QSim sim = (QSim) e.getQueueSimulation();
		for (MobsimAgent agent : sim.getAgents()) this.agents.put(agent.getId(), agent);
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
					
					this.evacuationDecisionModel.printStatistics();
					this.latestAcceptedLeaveTimeModel.printStatistics();
				} else {
					// set current Time
					for (Runnable runnable : this.runnables) {
						((SelectHouseholdMeetingPointRunner) runnable).setTime(time);
					}
					
					Queue<Id> informedHouseholds = informedHouseholdsTracker.getInformedHouseholdsInCurrentTimeStep();
					
					// If no household was informed in the current time step, we don't have to trigger the runners.
					if (informedHouseholds.size() == 0) {
//						log.info("No households informed in the current timestep.");
						return;
					}
					
					// assign households to threads
					int roundRobin = 0;
					
					Households households = ((ScenarioImpl) scenario).getHouseholds();
					for (Id householdId : informedHouseholds) {
						
						Household household = households.getHouseholds().get(householdId);
						
						// ignore empty households
						if (household.getMemberIds().size() == 0) continue;
						
						this.evacuationDecisionModel.runModel(household);
						this.latestAcceptedLeaveTimeModel.runModel(household);
												
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
					for (Id householdId : informedHouseholds) {
						
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
		
		RoutingContext routingContext = new RoutingContextImpl(disutilityFactory, this.travelTimes.get(TransportMode.car), this.scenario.getConfig().planCalcScore());
		
		for (int i = 0; i < this.numOfThreads; i++) {
			
			SelectHouseholdMeetingPointRunner runner = new SelectHouseholdMeetingPointRunner(scenario, 
					toHomeFacilityRouterFactory.instantiateAndConfigureTripRouter(routingContext), 
					fromHomeFacilityRouterFactory.instantiateAndConfigureTripRouter(routingContext), 
					vehiclesTracker, coordAnalyzer.createInstance(), modeAvailabilityChecker.createInstance(), 
					decisionDataProvider, agents, startBarrier, endBarrier, allMeetingsPointsSelected);
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

	/*
	 * Returns a sub-network instead of the full network.
	 */
	private static class ScenarioWrapper implements Scenario {

		private final Scenario scenario;
		private final Network network;
		
		public ScenarioWrapper(Scenario scenario, Network network) {
			this.scenario = scenario;
			this.network = network;
		}
		
		@Override
		public Id createId(String id) {
			return this.scenario.createId(id);
		}

		@Override
		public Network getNetwork() {
			return this.network;
		}

		@Override
		public Population getPopulation() {
			return this.scenario.getPopulation();
		}

		@Override
		public TransitSchedule getTransitSchedule() {
			return this.scenario.getTransitSchedule();
		}

		@Override
		public Config getConfig() {
			return this.scenario.getConfig();
		}

		@Override
		public Coord createCoord(double x, double y) {
			return this.scenario.createCoord(x, y);
		}

		@Override
		public void addScenarioElement(Object o) {
			this.scenario.addScenarioElement(o);
		}

		@Override
		public boolean removeScenarioElement(Object o) {
			return this.scenario.removeScenarioElement(o);
		}

		@Override
		public <T> T getScenarioElement(Class<? extends T> klass) {
			return this.scenario.getScenarioElement(klass);
		}
	}
}
