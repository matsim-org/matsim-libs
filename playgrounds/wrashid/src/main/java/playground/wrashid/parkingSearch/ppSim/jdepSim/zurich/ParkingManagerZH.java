/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.wrashid.parkingSearch.ppSim.jdepSim.zurich;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.network.EnclosingRectangle;
import org.matsim.contrib.parking.lib.obj.network.QuadTreeInitializer;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.withinday.utils.EditRoutes;

import playground.wrashid.lib.obj.IntegerValueHashMap;
import playground.wrashid.lib.obj.TwoHashMapsConcatenated;
import playground.wrashid.lib.tools.network.obj.RectangularArea;
import playground.wrashid.parkingChoice.ParkingChoiceLib;
import playground.wrashid.parkingChoice.api.ParkingSelectionManager;
import playground.wrashid.parkingChoice.api.PreferredParkingManager;
import playground.wrashid.parkingChoice.api.ReservedParkingManager;
import playground.wrashid.parkingChoice.apiDefImpl.ShortestWalkingDistanceParkingSelectionManager;
import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.infrastructure.PrivateParking;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.EditRoute;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.threads.RerouteTask;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.threads.RerouteTaskAddInitialPartToRoute;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.threads.RerouteThread;
import playground.wrashid.parkingSearch.ppSim.jdepSim.routing.threads.RerouteThreadPool;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.AvoidRoutingThroughTolledArea;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.ParkingSearchStrategy;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.manager.EvaluationContainer;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.manager.ParkingStrategyManager;
import playground.wrashid.parkingSearch.ppSim.jdepSim.searchStrategies.manager.StrategyEvaluation;
import playground.wrashid.parkingSearch.ppSim.ttmatrix.TTMatrix;
import playground.wrashid.parkingSearch.withindayFW.interfaces.ParkingCostCalculator;

public class ParkingManagerZH {

	private static final Logger log = Logger.getLogger(ParkingManagerZH.class);

	private ParkingCostCalculator parkingCostCalculator;
	// key: parkingId
	private HashMap<Id, Parking> parkingsHashMap;
	private Network network;

	protected QuadTree<Parking> nonFullPublicParkingFacilities;
	protected QuadTree<Parking> garageParkings;

	protected HashSet<Parking> fullPublicParkingFacilities;
	private Map<Id, List<Id>> parkingFacilitiesOnLinkMapping; // <LinkId,
																// List<FacilityId>>
	// private final Map<Id, Id> facilityToLinkMapping; // <FacilityId, LinkId>
	private IntegerValueHashMap<Id> occupiedParking; // number of reserved
														// parkings
	private HashMap<String, HashSet<Id>> parkingTypes;
	// parkingId, linkId
	private HashMap<Id, Id> parkingIdToLinkIdMapping;

	// personId, parkingId
	private HashMap<Id, Id> agentVehicleIdParkingIdMapping;

	private TTMatrix ttMatrix;

	private LinkedList<Parking> parkings;
	// activity facility Id, activityType, parking facility id
	private TwoHashMapsConcatenated<Id, String, Id> privateParkingFacilityIdMapping;
	
	private HashMap<Id, Route> firstAdaptedLegCache;

	public ParkingManagerZH(HashMap<String, HashSet<Id>> parkingTypes, ParkingCostCalculator parkingCostCalculator,
			LinkedList<Parking> parkings, Network network, TTMatrix ttMatrix) {
		this.setParkings(parkings);
		this.network = network;
		this.ttMatrix = ttMatrix;
		this.setParkingCostCalculator(parkingCostCalculator);
		this.network = network;
		this.parkingTypes = parkingTypes;
		// don't move this to reset method, as set in iteration 1!
		firstAdaptedLegCache=new HashMap<Id, Route>();

		initializeGarageParkings(parkings);

		reset();

	}

	private void initializeGarageParkings(LinkedList<Parking> parking) {
		EnclosingRectangle rect = new EnclosingRectangle();

		for (Parking p : parking) {
			if (p.getId().toString().contains("gp")) {
				rect.registerCoord(p.getCoord());
			}
		}
		garageParkings = (new QuadTreeInitializer<Parking>()).getQuadTree(rect);

		for (Parking p : parking) {
			if (p.getId().toString().contains("gp")) {
				garageParkings.put(p.getCoord().getX(), p.getCoord().getY(), p);
			}
		}
	}

	public void reset() {
		agentVehicleIdParkingIdMapping = new HashMap<Id, Id>();
		parkingIdToLinkIdMapping = new HashMap<Id, Id>();
		parkingsHashMap = new HashMap<Id, Parking>();

		initializeQuadTree(this.getParkings());
		addParkings(this.getParkings());

		setOccupiedParking(new IntegerValueHashMap<Id>());
		parkingFacilitiesOnLinkMapping = new HashMap<Id, List<Id>>();

		for (Parking parking : this.getParkings()) {
			Id linkId = ((NetworkImpl) this.network).getNearestLink(parking.getCoord()).getId();
			assignFacilityToLink(linkId, parking.getId());

			parkingIdToLinkIdMapping.put(parking.getId(), linkId);

			Id oppositeDirectionLinkId = getOppositeDirectionLinkId(linkId, this.network);
			if (oppositeDirectionLinkId != null) {
				assignFacilityToLink(oppositeDirectionLinkId, parking.getId());
			}
		}

		this.fullPublicParkingFacilities = new HashSet<Parking>();

		privateParkingFacilityIdMapping = new TwoHashMapsConcatenated<Id, String, Id>();
		for (Parking parking : this.getParkings()) {
			if (parking.getId().toString().contains("private")) {
				PrivateParking privateParking = (PrivateParking) parking;
				nonFullPublicParkingFacilities.remove(parking.getCoord().getX(), parking.getCoord().getY(), parking);

				privateParkingFacilityIdMapping.put(privateParking.getActInfo().getFacilityId(), privateParking.getActInfo()
						.getActType(), parking.getId());
			}

			if (parking.getId().toString().contains("illegal")) {
				nonFullPublicParkingFacilities.remove(parking.getCoord().getX(), parking.getCoord().getY(), parking);
			}
		}
	}

	public HashMap<Id, Parking> getParkingsHashMap() {
		return parkingsHashMap;
	}

	public void addParkings(Collection<Parking> parkingCollection) {
		RectangularArea rectangularArea = new RectangularArea(new CoordImpl(nonFullPublicParkingFacilities.getMinEasting(),
				nonFullPublicParkingFacilities.getMinNorthing()), new CoordImpl(nonFullPublicParkingFacilities.getMaxEasting(),
				nonFullPublicParkingFacilities.getMaxNorthing()));

		for (Parking parking : parkingCollection) {

			if (rectangularArea.isInArea(parking.getCoord())) {
				nonFullPublicParkingFacilities.put(parking.getCoord().getX(), parking.getCoord().getY(), parking);
				parkingsHashMap.put(parking.getId(), parking);
			} else {
				DebugLib.emptyFunctionForSettingBreakPoint();
				DebugLib.stopSystemAndReportInconsistency("only add points, which are inside defined area.");
			}

		}

	}

	private void initializeQuadTree(Collection<Parking> parkingColl) {
		EnclosingRectangle rect = new EnclosingRectangle();

		for (Parking parking : parkingColl) {
			rect.registerCoord(parking.getCoord());
		}
		nonFullPublicParkingFacilities = (new QuadTreeInitializer<Parking>()).getQuadTree(rect);
	}

	private void assignFacilityToLink(Id linkId, Id facilityId) {
		List<Id> list = parkingFacilitiesOnLinkMapping.get(linkId);
		if (list == null) {
			list = new ArrayList<Id>();
			parkingFacilitiesOnLinkMapping.put(linkId, list);
		}
		list.add(facilityId);
	}

	private Id getOppositeDirectionLinkId(Id linkId, Network network) {
		Link link = network.getLinks().get(linkId);
		if (link == null)
			link = null;
		Node toNode = link.getToNode();
		Node fromNode = link.getFromNode();

		for (Link tmpLink : network.getNodes().get(fromNode.getId()).getInLinks().values()) {
			if (tmpLink.getFromNode() == toNode) {
				return tmpLink.getId();
			}
		}

		return null;
	}

	public int getFreeCapacity(Id facilityId) {

		int freeCapacity = parkingsHashMap.get(facilityId).getIntCapacity() - getOccupiedParking().get(facilityId);

		if (freeCapacity < 0) {
			DebugLib.stopSystemAndReportInconsistency();
		}

		if (freeCapacity > parkingsHashMap.get(facilityId).getIntCapacity()) {
			DebugLib.stopSystemAndReportInconsistency();
		}

		return freeCapacity;
	}

	public void parkVehicle(Id agentId, Id parkingId) {
		if (agentId == null || parkingId == null) {
			DebugLib.stopSystemAndReportInconsistency("null adding not allowed");
		}

		agentVehicleIdParkingIdMapping.put(agentId, parkingId);
		parkVehicle(parkingId);
	}

	private void parkVehicle(Id facilityId) {
		getOccupiedParking().increment(facilityId);

		if (getFreeCapacity(facilityId) < 0) {
			DebugLib.stopSystemAndReportInconsistency();
		}

		if (getFreeCapacity(facilityId) == 0) {
			markFacilityAsFull(facilityId);
		}
	}

	private void markFacilityAsFull(Id facilityId) {
		Parking parking = parkingsHashMap.get(facilityId);
		nonFullPublicParkingFacilities.remove(parking.getCoord().getX(), parking.getCoord().getY(), parking);
		fullPublicParkingFacilities.add(parking);
	}

	public void unParkAgentVehicle(Id agentId) {
		Id parkingId = agentVehicleIdParkingIdMapping.remove(agentId);

		if (parkingId == null) {
			DebugLib.stopSystemAndReportInconsistency("parkingId missing");
		}

		unParkVehicle(parkingId);
	}

	public Id getCurrentParkingId(Id agentId) {
		Id parkingId = agentVehicleIdParkingIdMapping.get(agentId);

		if (parkingId == null) {
			DebugLib.stopSystemAndReportInconsistency("parkingId missing");
		}

		return parkingId;
	}

	private void unParkVehicle(Id facilityId) {
		getOccupiedParking().decrement(facilityId);

		if (getFreeCapacity(facilityId) == 1) {
			markFacilityAsNonFull(facilityId);
		}
	}

	private void markFacilityAsNonFull(Id facilityId) {
		Parking parking = parkingsHashMap.get(facilityId);
		nonFullPublicParkingFacilities.put(parking.getCoord().getX(), parking.getCoord().getY(), parking);
		fullPublicParkingFacilities.remove(parking);
	}

	public List<Id> getParkingsOnLink(Id linkId) {
		if (parkingFacilitiesOnLinkMapping.containsKey(linkId)) {
			return parkingFacilitiesOnLinkMapping.get(linkId);
		} else {
			return new LinkedList<Id>();
		}
	}

	public Id getFreeParkingFacilityOnLink(Id linkId, String parkingType) {
		HashSet<Id> parkings = null;
		if (parkingTypes != null) {
			parkings = parkingTypes.get(parkingType);
		}

		List<Id> list = getParkingsOnLink(linkId);
		if (list == null)
			return null;
		else {
			int maxCapacity = 0;
			Id facilityId = null;
			for (Id id : list) {
				if (parkings != null && !parkings.contains(id)) {
					continue;
				}

				int capacity = parkingsHashMap.get(id).getIntCapacity();
				int occupied = getOccupiedParking().get(id);
				if ((capacity - occupied) > maxCapacity)
					facilityId = id;
			}
			return facilityId;
		}
	}

	/*
	 * public Parking getClosestFreeParking(Coord coord) { LinkedList<Parking>
	 * tmpList=new LinkedList<Parking>(); Parking
	 * parking=nonFullPublicParkingFacilities.get(coord.getX(), coord.getY());
	 * 
	 * // if parking full, try finding other free parkings in the quadtree
	 * 
	 * while (getFreeCapacity(parking.getId())<=0){
	 * removeFullParkingFromQuadTree(tmpList, parking);
	 * parking=nonFullPublicParkingFacilities.get(coord.getX(), coord.getY()); }
	 * 
	 * resetParkingFacilitiesQuadTree(tmpList);
	 * 
	 * return parkingsHashMap.get(parking.getId()); }
	 */
	public Id getClosestFreeParkingFacilityNotOnLink(Coord coord, Id linkId) {
		LinkedList<Parking> tmpList = new LinkedList<Parking>();
		Parking parkingFacility = nonFullPublicParkingFacilities.get(coord.getX(), coord.getY());

		// if parking full or on specified link, try finding other free parkings
		// in the quadtree
		while (getFreeCapacity(parkingFacility.getId()) <= 0
				|| parkingIdToLinkIdMapping.get(parkingFacility.getId()).equals(linkId)) {
			removeFullParkingFromQuadTree(tmpList, parkingFacility);
			parkingFacility = nonFullPublicParkingFacilities.get(coord.getX(), coord.getY());
		}

		resetParkingFacilitiesQuadTree(tmpList);

		return parkingFacility.getId();
	}

	private void removeFullParkingFromQuadTree(LinkedList<Parking> tmpList, Parking parkingFacility) {
		tmpList.add(parkingFacility);
		nonFullPublicParkingFacilities
				.remove(parkingFacility.getCoord().getX(), parkingFacility.getCoord().getY(), parkingFacility);
	}

	private void resetParkingFacilitiesQuadTree(LinkedList<Parking> tmpList) {
		for (Parking parking : tmpList) {
			nonFullPublicParkingFacilities.put(parking.getCoord().getX(), parking.getCoord().getY(), parking);
		}
	}

	public ParkingCostCalculator getParkingCostCalculator() {
		return parkingCostCalculator;
	}

	// TODO: rename to include world free
	public Id getClosestParkingFacility(Coord coord) {
		if (nonFullPublicParkingFacilities.size() == 0) {
			DebugLib.emptyFunctionForSettingBreakPoint();
		}

		if (coord == null) {
			DebugLib.emptyFunctionForSettingBreakPoint();
		}

		return nonFullPublicParkingFacilities.get(coord.getX(), coord.getY()).getId();
	}

	public synchronized Id getClosestParkingFacilityNotOnLink(Coord coord, Id linkId) {
		LinkedList<Parking> tmpList = new LinkedList<Parking>();
		Parking parkingFacility = nonFullPublicParkingFacilities.get(coord.getX(), coord.getY());

		// if parking full or on specified link, try finding other free parkings
		// in the quadtree
		while (parkingIdToLinkIdMapping.get(parkingFacility.getId()).equals(linkId)) {
			removeFullParkingFromQuadTree(tmpList, parkingFacility);
			parkingFacility = nonFullPublicParkingFacilities.get(coord.getX(), coord.getY());
		}

		resetParkingFacilitiesQuadTree(tmpList);

		return parkingFacility.getId();
	}

	public Collection<Parking> getAllFreeParkingWithinDistance(double distance, Coord coord) {
		Collection<Parking> parkings = getAllParkingWithinDistance(distance, coord);

		// for (ActivityFacility parking:parkings){
		// if (getFreeCapacity(parking.getId())==0){
		// parkings.remove(parking.getId());
		// }
		// }

		return parkings;
	}

	private Collection<Parking> getAllParkingWithinDistance(double distance, Coord coord) {
		return nonFullPublicParkingFacilities.get(coord.getX(), coord.getY(), distance);
	}

	public Collection<Parking> getParkingFacilities() {
		return nonFullPublicParkingFacilities.values();
	}

	public Id getClosestFreeParkingFacilityId(Id linkId) {
		return getClosestParkingFacility(this.network.getLinks().get(linkId).getCoord());
	}
	
	public Id getFreePrivateParking(Id actFacilityId, String actType) {
		Id parkingFacilityId = privateParkingFacilityIdMapping.get(actFacilityId, actType);

		if (parkingFacilityId == null) {
			return null;
		}

		if (getFreeCapacity(parkingFacilityId) > 0) {
			return parkingFacilityId;
		} else {
			return null;
		}
	}

	public void initFirstParkingOfDay(Population population) {
		log.info("starting initFirstParkingOfDay");

		// RerouteThread[] rerouteThreads = new
		// RerouteThread[ZHScenarioGlobal.numberOfRoutingThreadsAtBeginning];
		//
		// CyclicBarrier cyclicBarrier = new CyclicBarrier(rerouteThreads.length
		// + 1);
		// for (int i = 0; i < rerouteThreads.length; i++) {
		// rerouteThreads[i] = new RerouteThread(ttMatrix, network,
		// cyclicBarrier);
		// }

			RerouteThreadPool rtPool = new RerouteThreadPool(ZHScenarioGlobal.numberOfRoutingThreadsAtBeginning, ttMatrix, network);

			for (Person person : population.getPersons().values()) {
				for (int i = 0; i < person.getSelectedPlan().getPlanElements().size(); i++) {
					PlanElement pe = person.getSelectedPlan().getPlanElements().get(i);
					if (pe instanceof LegImpl) {
						LegImpl leg = (LegImpl) pe;
						if (leg.getMode().equalsIgnoreCase(TransportMode.car)) {
							ActivityImpl act = (ActivityImpl) person.getSelectedPlan().getPlanElements().get(i - 3);
							ActivityImpl prevParkingAct = (ActivityImpl) person.getSelectedPlan().getPlanElements().get(i - 1);
							ActivityImpl nextParkingAct = (ActivityImpl) person.getSelectedPlan().getPlanElements().get(i + 1);
							ActivityImpl nextNonParkingAct = (ActivityImpl) person.getSelectedPlan().getPlanElements().get(i + 3);

							DebugLib.traceAgent(person.getId(), 19);

							Id parkingId = getFreePrivateParking(act.getFacilityId(), act.getType());

							if (parkingId == null) {
								parkingId = getClosestFreeParkingFacilityId(act.getLinkId());
								// parkingId =
								// getClosestAcceptableFreeParkingDuringInitialization(act.getLinkId());
							}

							if (getLinkOfParking(parkingId).toString().equalsIgnoreCase(nextNonParkingAct.getLinkId().toString())) {
								parkingId = getClosestParkingFacilityNotOnLink(network.getLinks().get(act.getLinkId()).getCoord(),
										nextNonParkingAct.getLinkId());
							}

							if (getLinkOfParking(parkingId).toString().equalsIgnoreCase(nextNonParkingAct.getLinkId().toString())) {
								DebugLib.stopSystemAndReportInconsistency();
							}

							// TODO!!!!!!!!!!!!!!!!!!!
							// add check here, if on same link or not!!!

							parkVehicle(person.getId(), parkingId);

							prevParkingAct.setLinkId(getLinkOfParking(parkingId));

							// leg.setRoute(editRoute.getRoute(act.getEndTime(),
							// prevParkingAct.getLinkId(),
							// nextParkingAct.getLinkId()));
							// leg.setRoute(editRoute.addInitialPartToRoute(act.getEndTime(),
							// prevParkingAct.getLinkId(),
							// (LinkNetworkRouteImpl)
							// leg.getRoute()));
							// pool.submit(new
							// AddInitialPartToRouteTask(act.getEndTime(),
							// prevParkingAct.getLinkId(),
							// leg,ttMatrix,network));

							if (person.getId().toString().equalsIgnoreCase("504")) {
								DebugLib.emptyFunctionForSettingBreakPoint();
							}
							
							
							if (firstAdaptedLegCache.containsKey(person.getId())){
								leg.setRoute(firstAdaptedLegCache.get(person.getId()).clone());
							} else {
								rtPool.addTask(new RerouteTaskAddInitialPartToRoute(act.getEndTime(), prevParkingAct.getLinkId(), leg));
								// rerouteThreads[numberOfTasks %
								// rerouteThreads.length].addTask();
							}
							

							break;
						}
					}
				}
			}

			rtPool.start();

		if (ZHScenarioGlobal.iteration == 0) {
			logInitialOccupancyToTxtFile();
			logInitialParkingOfEachAgent();
			
			
			for (Person person : population.getPersons().values()) {
				for (int i = 0; i < person.getSelectedPlan().getPlanElements().size(); i++) {
					PlanElement pe = person.getSelectedPlan().getPlanElements().get(i);
					if (pe instanceof LegImpl) {
						LegImpl leg = (LegImpl) pe;
						if (leg.getMode().equalsIgnoreCase(TransportMode.car)) {
							
							if (person.getId().toString().equalsIgnoreCase("504")) {
								DebugLib.emptyFunctionForSettingBreakPoint();
							}
							
							firstAdaptedLegCache.put(person.getId(), leg.getRoute().clone());
							break;
						}
					}
				}
			}
		}

		log.info("completed initFirstParkingOfDay");
		
		
		if (ZHScenarioGlobal.iteration == 0) {
			for (ParkingSearchStrategy pss:ParkingStrategyManager.allStrategies){
				if (pss instanceof AvoidRoutingThroughTolledArea){
					if (AvoidRoutingThroughTolledArea.routes==null){
						AvoidRoutingThroughTolledArea.initRoutes();
					} 
				}
			}
			
		}
	}

	private void logInitialParkingOfEachAgent() {
		ArrayList<String> list = new ArrayList<String>();
		list.add("personId\tparkingId");

		for (Id personId : agentVehicleIdParkingIdMapping.keySet()) {
			list.add(personId.toString() + "\t" + agentVehicleIdParkingIdMapping.get(personId));
		}

		GeneralLib.writeList(list, ZHScenarioGlobal.getItersFolderPath() + ZHScenarioGlobal.iteration
				+ ".initialPersonParkingLocation.txt");
	}

	private Id getClosestAcceptableFreeParkingDuringInitialization(Id linkId) {
		Coord coord = this.network.getLinks().get(linkId).getCoord();

		double distance = 100;
		while (true) {
			Collection<Parking> collection = nonFullPublicParkingFacilities.get(coord.getX(), coord.getY(), distance);

			for (Parking p : collection) {
				if (!p.getId().toString().contains("illegal")) {
					return p.getId();
				}
			}
			if (distance < 1000) {
				distance += 100;
			} else {
				distance *= 2;
			}
		}
	}

	private void logInitialOccupancyToTxtFile() {
		ArrayList<String> list = new ArrayList<String>();
		list.add("parkingFacilityId\toccupancy");

		for (Id facilityId : getOccupiedParking().getKeySet()) {
			list.add(facilityId.toString() + "\t" + getOccupiedParking().get(facilityId));
		}

		GeneralLib.writeList(list, ZHScenarioGlobal.getItersFolderPath() + ZHScenarioGlobal.iteration
				+ ".initialParkingOccupancy.txt");
	}

	public Id getLinkOfParking(Id parkingId) {
		return parkingIdToLinkIdMapping.get(parkingId);
	}

	public LinkedList<Parking> getParkings() {
		return parkings;
	}

	public void setParkings(LinkedList<Parking> parkings) {
		this.parkings = parkings;
	}

	public void setParkingCostCalculator(ParkingCostCalculator parkingCostCalculator) {
		this.parkingCostCalculator = parkingCostCalculator;
	}

	public void printNumberOfOccupiedParking(String comment) {
		int sum = 0;
		for (Id parkingId : getOccupiedParking().getKeySet()) {
			sum += getOccupiedParking().get(parkingId);
		}

		System.out.println("number of occupied parking: " + sum + " (" + comment + ")");
	}

	// return null if 
	public Id getClosestFreeGarageParkingNotOnLink(Coord coord, Id linkId) {
		Coord coordinatesLindenhofZH = ParkingHerbieControler.getCoordinatesLindenhofZH();

		if (GeneralLib.getDistance(coordinatesLindenhofZH, coord) < 8000) {
			double distance = 100;
			while (true) {
				Collection<Parking> collection = garageParkings.get(coord.getX(), coord.getY(), distance);

				for (Parking p : collection) {
					if (p.getIntCapacity() - getOccupiedParking().get(p.getId()) > 0 && !parkingIdToLinkIdMapping.get(p.getId()).equals(linkId)) {
						return p.getId();
					}
				}

				if (distance < 1000) {
					distance += 100;
				} else {
					distance *= 2;
				}
			}
		} else {
			Collection<Parking> collection = garageParkings.values();

			for (Parking p : collection) {
				if (p.getIntCapacity() - getOccupiedParking().get(p.getId()) > 0 && !parkingIdToLinkIdMapping.get(p.getId()).equals(linkId)) {
					return p.getId();
				}
			}
		}
		
		return null;
	}

	public Collection<Parking> getParkingWithinDistance(Coord coord, double distance) {
		return nonFullPublicParkingFacilities.get(coord.getX(), coord.getY(), distance);
	}

	public IntegerValueHashMap<Id> getOccupiedParking() {
		return occupiedParking;
	}

	public void setOccupiedParking(IntegerValueHashMap<Id> occupiedParking) {
		this.occupiedParking = occupiedParking;
	}

}
