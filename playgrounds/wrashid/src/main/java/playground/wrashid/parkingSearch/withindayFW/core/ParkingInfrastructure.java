/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingInfrastructure.java
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

package playground.wrashid.parkingSearch.withindayFW.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.algorithms.WorldConnectLocations;

import playground.wrashid.parkingSearch.withindayFW.interfaces.ParkingCostCalculator;

public class ParkingInfrastructure  {

	protected final QuadTree<ActivityFacility> nonFullPublicParkingFacilities;
	protected final HashSet<ActivityFacility> fullPublicParkingFacilities;
	private final Map<Id, List<Id>> parkingFacilitiesOnLinkMapping; // <LinkId, List<FacilityId>>
	//private final Map<Id, Id> facilityToLinkMapping;	// <FacilityId, LinkId>
	private IntegerValueHashMap<Id> reservedCapacities;	// number of reserved parkings
	private IntegerValueHashMap<Id> facilityCapacities;	
	private final HashMap<String, HashSet<Id>> parkingTypes;
	private final ParkingCostCalculator parkingCostCalculator;
	protected final Scenario scenario;
	private HashMap<Id,ActivityFacility> initialParkingFacilityOfAgent;
	
	
	public void resetParkingFacilityForNewIteration(){
		for (ActivityFacility parkingFacility:fullPublicParkingFacilities){
			nonFullPublicParkingFacilities.put(parkingFacility.getCoord().getX(), parkingFacility.getCoord().getY(), parkingFacility);
		}
		reservedCapacities=new IntegerValueHashMap<Id>();
	}
	
	public void setInitialParkingFacilityOfAgent(HashMap<Id, ActivityFacility> initialParkingFacilityOfAgent) {
		this.initialParkingFacilityOfAgent = initialParkingFacilityOfAgent;
	}

	public ParkingInfrastructure(Scenario scenario, HashMap<String, HashSet<Id>> parkingTypes, ParkingCostCalculator parkingCostCalculator) {
		this.scenario = scenario;
		this.parkingCostCalculator = parkingCostCalculator;
		facilityCapacities = new IntegerValueHashMap<Id>();
		reservedCapacities = new IntegerValueHashMap<Id>();
	//	facilityToLinkMapping = new HashMap<Id, Id>(); 
		parkingFacilitiesOnLinkMapping = new HashMap<Id, List<Id>>();
		
		new WorldConnectLocations(scenario.getConfig()).connectFacilitiesWithLinks(((ScenarioImpl) scenario).getActivityFacilities(), (NetworkImpl) scenario.getNetwork());
		
		// Create a quadtree containing all parking facilities
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (ActivityFacility facility : ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().values()) {
			if (facility.getCoord().getX() < minx) { minx = facility.getCoord().getX(); }
			if (facility.getCoord().getY() < miny) { miny = facility.getCoord().getY(); }
			if (facility.getCoord().getX() > maxx) { maxx = facility.getCoord().getX(); }
			if (facility.getCoord().getY() > maxy) { maxy = facility.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		
		nonFullPublicParkingFacilities = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		for (ActivityFacility facility : ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().values()) {
			
			// if the facility offers a parking activity
			if (facility.getActivityOptions().containsKey("parking")) {
				
				// add the facility to the quadtree
				nonFullPublicParkingFacilities.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
				
				// add the facility to the facilitiesOnLinkMapping
				
				Id linkId = facility.getLinkId();
				Id facilityId = facility.getId();
				assignFacilityToLink(linkId, facilityId);
				
				Id oppositeDirectionLinkId = getOppositeDirectionLinkId(linkId,scenario);
				if (oppositeDirectionLinkId!=null){
					assignFacilityToLink(oppositeDirectionLinkId, facilityId);
				}
				
				// add the facility to the facilityToLinkMapping
		//		getFacilityToLinkMapping().put(facilityId, linkId);
			}
		}
		
		this.parkingTypes=parkingTypes;
		
		this.initialParkingFacilityOfAgent=new HashMap<Id, ActivityFacility>();
		this.fullPublicParkingFacilities=new HashSet<ActivityFacility>();
		// cont' here -> fill this in parking population agent source.
		// there, make that if variable is empty, initialize it based on closest parking
		
		// could also set this from outside at the beginning and allow to change it from inside.
		// make it TODO (for starting, the initial approach is enough)
		
	}

	private void assignFacilityToLink(Id linkId, Id facilityId) {
		List<Id> list = parkingFacilitiesOnLinkMapping.get(linkId);
		if (list == null) {
			list = new ArrayList<Id>();
			parkingFacilitiesOnLinkMapping.put(linkId, list);
		}
		list.add(facilityId);
	}

	private Id getOppositeDirectionLinkId(Id linkId, Scenario scenario) {
		Link link = scenario.getNetwork().getLinks().get(linkId);
		if (link == null)
			link = null;
		Node toNode = link.getToNode();
		Node fromNode = link.getFromNode();
		
		for (Link tmpLink: scenario.getNetwork().getNodes().get(fromNode.getId()).getInLinks().values()){
			if (tmpLink.getFromNode()==toNode){
				return tmpLink.getId();
			}
		}
		
		return null;
	}
	
	public int getFreeCapacity(Id facilityId) {
		int freeCapacity = getFacilityCapacities().get(facilityId)-reservedCapacities.get(facilityId);
		
		if (freeCapacity<0){
			DebugLib.stopSystemAndReportInconsistency();
		}
		
		if (freeCapacity>getFacilityCapacities().get(facilityId)){
			DebugLib.stopSystemAndReportInconsistency();
		}
		
		return freeCapacity;
	}

	public void parkVehicle(Id facilityId) {
		reservedCapacities.increment(facilityId);
		
		if (getFreeCapacity(facilityId)<0){
			DebugLib.stopSystemAndReportInconsistency();
		}
		
		if (getFreeCapacity(facilityId)==0){
			markFacilityAsFull(facilityId);
		}
	}

	private void markFacilityAsFull(Id facilityId) {
		ActivityFacility activityFacility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(facilityId);
		nonFullPublicParkingFacilities.remove(activityFacility.getCoord().getX(), activityFacility.getCoord().getY(), activityFacility);
		fullPublicParkingFacilities.add(activityFacility);
	}

	public void unParkVehicle(Id facilityId) {
		reservedCapacities.decrement(facilityId);
		
		if (getFreeCapacity(facilityId)==1){
			markFacilityAsNonFull(facilityId);
		}
	}
	
	private void markFacilityAsNonFull(Id facilityId) {
		ActivityFacility activityFacility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(facilityId);
		nonFullPublicParkingFacilities.put(activityFacility.getCoord().getX(), activityFacility.getCoord().getY(), activityFacility);
		fullPublicParkingFacilities.remove(activityFacility);
	}

	public List<Id> getParkingsOnLink(Id linkId) {
		return parkingFacilitiesOnLinkMapping.get(linkId);
	}

	public Id getFreeParkingFacilityOnLink(Id linkId, String parkingType) {
		HashSet<Id> parkings=null;
		if (parkingTypes!=null){
			parkings = parkingTypes.get(parkingType);
		}
		
		List<Id> list = getParkingsOnLink(linkId);
		if (list == null) return null;
		else {
			int maxCapacity = 0;
			Id facilityId = null;
			for (Id id : list) {
				if (parkings!=null && !parkings.contains(id)){
					continue;
				}
				
				int capacity = getFacilityCapacities().get(id);
				int reserved = reservedCapacities.get(id);
				if ((capacity - reserved) > maxCapacity) facilityId = id;
			}
			return facilityId;
		}
	}
	
	public ActivityFacility getClosestFreeParkingFacility(Coord coord) {
		LinkedList<ActivityFacility> tmpList=new LinkedList<ActivityFacility>();
		ActivityFacility parkingFacility=nonFullPublicParkingFacilities.getClosest(coord.getX(), coord.getY());
		
		// if parking full, try finding other free parkings in the quadtree
		
		while (getFreeCapacity(parkingFacility.getId())<=0){
			removeFullParkingFromQuadTree(tmpList, parkingFacility);
			parkingFacility=nonFullPublicParkingFacilities.getClosest(coord.getX(), coord.getY());
		}
		
		resetParkingFacilitiesQuadTree(tmpList);
		
		return ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(parkingFacility.getId());
	}
	
	public Id getClosestFreeParkingFacilityNotOnLink(Coord coord, Id linkId){
		LinkedList<ActivityFacility> tmpList=new LinkedList<ActivityFacility>();
		ActivityFacility parkingFacility=nonFullPublicParkingFacilities.getClosest(coord.getX(), coord.getY());
		
		// if parking full or on specified link, try finding other free parkings in the quadtree
		while (getFreeCapacity(parkingFacility.getId())<=0 || parkingFacility.getLinkId().equals(linkId)){
			removeFullParkingFromQuadTree(tmpList, parkingFacility);
			parkingFacility=nonFullPublicParkingFacilities.getClosest(coord.getX(), coord.getY());
		}
		
		resetParkingFacilitiesQuadTree(tmpList);
		
		return parkingFacility.getId();
	}
	

	private void removeFullParkingFromQuadTree(LinkedList<ActivityFacility> tmpList, ActivityFacility parkingFacility) {
		tmpList.add(parkingFacility);
		nonFullPublicParkingFacilities.remove(parkingFacility.getCoord().getX(), parkingFacility.getCoord().getY(), parkingFacility);
	}

	private void resetParkingFacilitiesQuadTree(LinkedList<ActivityFacility> tmpList) {
		for (ActivityFacility parking:tmpList){
			nonFullPublicParkingFacilities.put(parking.getCoord().getX(), parking.getCoord().getY(), parking);
		}
	}


	public ParkingCostCalculator getParkingCostCalculator() {
		return parkingCostCalculator;
	}

	//TODO: rename to include world free
	public Id getClosestParkingFacility(Coord coord) {
		if (nonFullPublicParkingFacilities.size()==0){
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
		
		if (coord==null){
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
		
		return nonFullPublicParkingFacilities.getClosest(coord.getX(), coord.getY()).getId();
	}
	
	public synchronized Id getClosestParkingFacilityNotOnLink(Coord coord, Id linkId) {		
		LinkedList<ActivityFacility> tmpList=new LinkedList<ActivityFacility>();
		ActivityFacility parkingFacility=nonFullPublicParkingFacilities.getClosest(coord.getX(), coord.getY());
		
		// if parking full or on specified link, try finding other free parkings in the quadtree
		while (parkingFacility.getLinkId().equals(linkId)){
			removeFullParkingFromQuadTree(tmpList, parkingFacility);
			parkingFacility=nonFullPublicParkingFacilities.getClosest(coord.getX(), coord.getY());
		}
		
		resetParkingFacilitiesQuadTree(tmpList);
		
		return parkingFacility.getId();
	}
	
	public Collection<ActivityFacility> getAllFreeParkingWithinDistance(double distance,Coord coord){
		Collection<ActivityFacility> parkings = getAllParkingWithinDistance(distance, coord);
		
//		for (ActivityFacility parking:parkings){
//			if (getFreeCapacity(parking.getId())==0){
//				parkings.remove(parking.getId());
//			}
//		}
		
		return parkings;
	}

	private Collection<ActivityFacility> getAllParkingWithinDistance(double distance, Coord coord) {
		return nonFullPublicParkingFacilities.getDisk(coord.getX(), coord.getY(), distance);
	}

	public IntegerValueHashMap<Id> getFacilityCapacities() {
		return facilityCapacities;
	}
	
	public void setFacilityCapacities(IntegerValueHashMap<Id> facilityCapacities) {
		this.facilityCapacities=facilityCapacities;
	}
	
	public Collection<ActivityFacility> getParkingFacilities(){
		return nonFullPublicParkingFacilities.values();
	}

	public HashMap<Id, ActivityFacility> getInitialParkingFacilityOfAgent() {
		return initialParkingFacilityOfAgent;
	}

	public Id getClosestFreeParkingFacility(Id linkId) {
		return getClosestParkingFacility(scenario.getNetwork().getLinks().get(linkId).getCoord());
	}
	
}
