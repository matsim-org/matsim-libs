/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.parking.parkingChoice.manager;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.obj.SortableMapObject;
import org.matsim.contrib.parking.lib.obj.network.EnclosingRectangle;
import org.matsim.contrib.parking.lib.obj.network.QuadTreeInitializer;
import org.matsim.contrib.parking.parkingChoice.infrastructure.Parking;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.utils.collections.QuadTree;

import java.util.Collection;
import java.util.HashSet;
import java.util.PriorityQueue;

//TODO:
// get parking for agent
// utility function vom agent beruecksichtigen
// preference hinzufuegen (allow user to programm code to overwride pure utility based appraoch, but also allow some other appraoch,
//e.g. filtering available parking)
// there should be a function, which can be overwritten by subclasses, to do that.


public class ParkingManager {

	private QuadTree<Parking> parkings;
	private HashSet<Parking> fullParking;
	private double initialParkingSearchRadiusInMeter;
	private MatsimServices controller;
	
	public ParkingManager(Collection<Parking> parkingCollection) {
		EnclosingRectangle rect=new EnclosingRectangle();
		
		for (Parking parking:parkingCollection){
			rect.registerCoord(parking.getCoordinate());
		}
		parkings=(new QuadTreeInitializer<Parking>()).getQuadTree(rect);
		
	}
	
	/**
	 * 
	 * returns the parkingId
	 * 
	 */
	public Id parkVehicle(Id agentId, Id actFacilityId, String actType){
        Coord actCoordinate = controller.getScenario().getActivityFacilities().getFacilities().get(actFacilityId).getCoord();
		
		double radius=initialParkingSearchRadiusInMeter;
		Collection<Parking> collection = parkings.getDisk(actCoordinate.getX(), actCoordinate.getY(), radius);
		removeAllUnusableParking(collection,agentId, actFacilityId, actType);
		
		while (collection.size()==0){
			radius*=2;
			collection = parkings.getDisk(actCoordinate.getX(), actCoordinate.getY(), radius);
			removeAllUnusableParking(collection,agentId, actFacilityId, actType);
		}
		
		
		
		
		
		return selectParking(collection, actFacilityId, actFacilityId, actType).getId();
	}

	//TODO: document, that this method can be overwritten, to make new logic or model the perference of the agent
	// can already make default impl. for some ev related preference.
	protected Parking selectParking(Collection<Parking> collection,Id agentId, Id actFacilityId, String actType) {
		// score parking in collection
		PriorityQueue<SortableMapObject<Parking>> queue=new PriorityQueue<SortableMapObject<Parking>>();
		
		// walk
		// TODO: take utility function of claude and insert it here!
		
		double score=0.0; // TODO: this should not be zero
		for (Parking parking:collection){
			queue.add(new SortableMapObject<Parking>(parking, score));
		}
		
		return null;
		
	}

	private void removeAllUnusableParking(Collection<Parking> collection,Id agentId, Id actFacilityId, String actType) {
		for (Parking parking:collection){
			//if (!parking.isAllowedToUseParking(agentId, actFacilityId, actType)){
			//	collection.remove(parking);
			//}
		}
	}

}
