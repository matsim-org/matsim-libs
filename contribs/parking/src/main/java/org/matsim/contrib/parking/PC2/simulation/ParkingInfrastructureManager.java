/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.contrib.parking.PC2.simulation;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.PC2.infrastructure.PPRestrictedToFacilities;
import org.matsim.contrib.parking.PC2.infrastructure.PrivateParking;
import org.matsim.contrib.parking.PC2.infrastructure.PublicParking;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;
import org.matsim.contrib.parking.lib.obj.SortableMapObject;
import org.matsim.contrib.parking.lib.obj.network.EnclosingRectangle;
import org.matsim.contrib.parking.lib.obj.network.QuadTreeInitializer;
import org.matsim.contrib.parking.PC2.infrastructure.Parking;
import org.matsim.contrib.parking.PC2.scoring.ParkingScoreManager;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.collections.QuadTree;

// TODO: make abstract and create algorithm in Zuerich case -> provide protected helper methods already here.
public class ParkingInfrastructureManager {

	private ParkingScoreManager parkingScoreManager;
	
	private HashMap<Id, Parking> allParkings;
	
	// personId, parkingFacilityId
	private HashMap<Id,Id> parkedVehicles;
	private EventsManager eventsManager;
	
	
	// facilityId -> parkings available to users of that facility
	private LinkedListValueHashMap<Id, PPRestrictedToFacilities> privateParkingsRestrictedToFacilities;

	// TODO: later - to improve parformance, a second variable could be added, where full parking are put.
	private QuadTree<Parking> publicParkingsQuadTree;
	private LinkedList<PublicParking> allPublicParkings;
	private HashMap<String, QuadTree<Parking>> publicParkingGroupQuadTrees;
	

	// TODO: make private parking (attached to facility)
	// + also private parking, which is attached to activity
	// both should be checked.

	
	
	public ParkingInfrastructureManager(ParkingScoreManager parkingScoreManager, EventsManager eventsManager){
		this.parkingScoreManager = parkingScoreManager;
		this.eventsManager = eventsManager;
		parkedVehicles=new HashMap<Id, Id>();
		allParkings=new HashMap<Id, Parking>();
		privateParkingsRestrictedToFacilities=new LinkedListValueHashMap<Id, PPRestrictedToFacilities>();
	}
	
	
	
	
	public void setPublicParkings(LinkedList<PublicParking> publicParkings) {
		publicParkingGroupQuadTrees=new HashMap<String, QuadTree<Parking>>();
		EnclosingRectangle allPublicParkingRect = new EnclosingRectangle();
		HashMap<String,EnclosingRectangle> groupRects=new HashMap<String, EnclosingRectangle>();

		for (PublicParking parking : publicParkings) {
			allPublicParkingRect.registerCoord(parking.getCoordinate());
			allParkings.put(parking.getId(), parking);
			
			if (!groupRects.containsKey(parking.getGroupName())){
				groupRects.put(parking.getGroupName(), new EnclosingRectangle()); 
			}
			EnclosingRectangle groupRect=groupRects.get(parking.getGroupName());
			groupRect.registerCoord(parking.getCoordinate());
		}
		this.publicParkingsQuadTree = (new QuadTreeInitializer<Parking>()).getQuadTree(allPublicParkingRect);
		this.allPublicParkings=publicParkings;
		
		for (String groupNames:groupRects.keySet()){
			publicParkingGroupQuadTrees.put(groupNames,  (new QuadTreeInitializer<Parking>()).getQuadTree(groupRects.get(groupNames)));
		}
		
		for (PublicParking parking : publicParkings) {
			addParkingToQuadTree(publicParkingsQuadTree,parking);
			addParkingToQuadTree(publicParkingGroupQuadTrees.get(parking.getGroupName()),parking);
		}
	}
	
	public static void addParkingToQuadTree(QuadTree<Parking> quadTree, Parking parking){
		quadTree.put(parking.getCoordinate().getX(), parking.getCoordinate().getY(), parking);
	}

	public void setPrivateParkingRestrictedToFacilities(LinkedList<PPRestrictedToFacilities> ppRestrictedToFacilities) {
		for (PPRestrictedToFacilities pp : ppRestrictedToFacilities) {
			for (Id facilityId : pp.getFacilityIds()) {
				privateParkingsRestrictedToFacilities.put(facilityId, pp);
				allParkings.put(pp.getId(), pp);
			}
		}
	}
	
	public void reset(){
		parkedVehicles.clear();
		
		for (Id parkingFacilityId: allParkings.keySet()){
			allParkings.get(parkingFacilityId).resetAvailability();
		}
	}
	
	protected QuadTree<Parking> getPublicParkingQuadTree(){
		return publicParkingsQuadTree;
	}
	
	public Parking parkAtClosestPublicParkingNonPersonalVehicle(Coord destCoordinate, String groupName){
		Parking parking=null;
		if (groupName==null){
			parking = publicParkingsQuadTree.get(destCoordinate.getX(), destCoordinate.getY());
		} else {
			QuadTree<Parking> quadTree = publicParkingGroupQuadTrees.get(groupName);
			parking= quadTree.get(destCoordinate.getX(), destCoordinate.getY());
			
			if (parking==null){
				DebugLib.stopSystemAndReportInconsistency("not enough parking available for parkingGroupName:" + groupName);
			}
		}
		parkVehicle(parking);
		
		return parking;
	}
	
	public void logArrivalEventAtTimeZero(Parking parking){
		eventsManager.processEvent(new ParkingArrivalEvent(0, parking.getId()));
	}
	
	public Parking parkAtClosestPublicParkingNonPersonalVehicle(Coord destCoordinate, String groupName, Id personId, double parkingDurationInSeconds, double arrivalTime){
		Parking parking=parkAtClosestPublicParkingNonPersonalVehicle(destCoordinate, groupName);
		
		double walkScore = parkingScoreManager.calcWalkScore(destCoordinate, parking.getCoordinate(), personId, parkingDurationInSeconds);
		parkingScoreManager.addScore(personId, walkScore);
		
		eventsManager.processEvent(new ParkingArrivalEvent(arrivalTime, parking.getId()));
		
		return parking;
	}
	
	
	
	// TODO: make this method abstract
	// when person/vehicleId is clearly distinct, then I can change this to vehicleId - check, if this is the case now.
	public Parking parkVehicle(ParkingOperationRequestAttributes parkingOperationRequestAttributes){
		
		
		
		
		Parking selectedParking=null;
		boolean parkingFound=false;
		for (PPRestrictedToFacilities pp:privateParkingsRestrictedToFacilities.get(parkingOperationRequestAttributes.facilityId)){
			if (pp.getAvailableParkingCapacity()>0){
				pp.parkVehicle();
				parkedVehicles.put(parkingOperationRequestAttributes.personId, pp.getId());
				parkingFound=true;
				selectedParking=pp;
			}
		}
		
		
		double distance=300;
		if (!parkingFound){
			Collection<Parking> collection = getNonFullParking(getPublicParkingQuadTree().get(parkingOperationRequestAttributes.destCoordinate.getX(), parkingOperationRequestAttributes.destCoordinate.getY(), distance));
			
			if(!parkingFound){
				while (collection.size()==0){
					distance*=2;
					collection = getNonFullParking(getPublicParkingQuadTree().get(parkingOperationRequestAttributes.destCoordinate.getX(), parkingOperationRequestAttributes.destCoordinate.getY(), distance));
				}
				
				PriorityQueue<SortableMapObject<Parking>> queue=new PriorityQueue<SortableMapObject<Parking>>();
				
				for (Parking parking:collection){
					double score=parkingScoreManager.calcScore(parkingOperationRequestAttributes.destCoordinate, parkingOperationRequestAttributes.arrivalTime, parkingOperationRequestAttributes.parkingDurationInSeconds, parking, parkingOperationRequestAttributes.personId);
					queue.add(new SortableMapObject<Parking>(parking, -1.0*score));
				}
				
				//TODO: should I make MNL only on top 5 here?
				
				SortableMapObject<Parking> poll = queue.poll();
				selectedParking = poll.getKey();
				parkedVehicles.put(parkingOperationRequestAttributes.personId, selectedParking.getId());
				
				parkVehicle(selectedParking);
			}
			
			
		}
		
		eventsManager.processEvent(new ParkingArrivalEvent(parkingOperationRequestAttributes.arrivalTime, selectedParking.getId()));
		
		return selectedParking;
	}




	private void parkVehicle(Parking parking) {
		parking.parkVehicle();
		if (parking.getAvailableParkingCapacity()==0){
			publicParkingsQuadTree.remove(parking.getCoordinate().getX(), parking.getCoordinate().getY(), parking);
			publicParkingGroupQuadTrees.get(parking.getGroupName()).remove(parking.getCoordinate().getX(), parking.getCoordinate().getY(), parking);
		}
	}
	
	public LinkedList<Parking> getNonFullParking(Collection<Parking> parkings){
		LinkedList<Parking> result=new LinkedList<Parking>();
		
		for (Parking p:parkings){
			if (p.getAvailableParkingCapacity()>0){
				result.add(p);
			}
		}
		return result;
	}
	
	// TODO: make this method abstract
	public Parking personCarDepartureEvent(ParkingOperationRequestAttributes parkingOperationRequestAttributes){
		Id parkingFacilityId=parkedVehicles.get(parkingOperationRequestAttributes.personId);
		Parking parking = allParkings.get(parkingFacilityId);
		parkedVehicles.remove(parkingOperationRequestAttributes.personId);
		unParkVehicle(parking,parkingOperationRequestAttributes.arrivalTime+ parkingOperationRequestAttributes.parkingDurationInSeconds);
		return parking;
	}




	public void scoreParkingOperation(ParkingOperationRequestAttributes parkingOperationRequestAttributes, Parking parking) {
		double score=parkingScoreManager.calcScore(parkingOperationRequestAttributes.destCoordinate, parkingOperationRequestAttributes.arrivalTime, parkingOperationRequestAttributes.parkingDurationInSeconds, parking, parkingOperationRequestAttributes.personId);
		parkingScoreManager.addScore(parkingOperationRequestAttributes.personId, score);
	}




	public void unParkVehicle(Parking parking, double departureTime) {
		if (parking==null){
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
		
		parking.unparkVehicle();
		
		if (parking.getAvailableParkingCapacity()==1){
			if (!(parking instanceof PrivateParking)){
				addParkingToQuadTree(publicParkingsQuadTree,parking);
				addParkingToQuadTree(publicParkingGroupQuadTrees.get(parking.getGroupName()),parking);
			}
		}
		
		eventsManager.processEvent(new ParkingDepartureEvent(departureTime, parking.getId()));
	}

	public ParkingScoreManager getParkingScoreManager() {
		return parkingScoreManager;
	}




	public EventsManager getEventsManager() {
		return eventsManager;
	}




	public void setEventsManager(EventsManager eventsManager) {
		this.eventsManager = eventsManager;
	}
	
	
	
	

	// TODO: allso allow to filter by group the parkings

	// Allow to reprogramm the decision making process of the agent => provide
	// default module for decision making and new one,
	// which could also cope with EVs.

	// provide interface for proper integration.

	// also loading of data should

}
