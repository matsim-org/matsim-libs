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

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.obj.network.EnclosingRectangle;
import org.matsim.contrib.parking.lib.obj.network.QuadTreeInitializer;
import org.matsim.contrib.parking.parkingChoice.infrastructure.Parking;
import org.matsim.core.utils.collections.QuadTree;

// get parking for agent
// utility function vom agent beruecksichtigen
// preference hinzufuegen (allow user to programm code to overwride pure utility based appraoch, but also allow some other appraoch,
//e.g. filtering available parking)
// there should be a function, which can be overwritten by subclasses, to do that.


public class ParkingManager {

	private QuadTree<Parking> parkings;
	private double initialParkingSearchRadiusInMeter;
	
	public ParkingManager(Collection<Parking> parkingCollection) {
		EnclosingRectangle rect=new EnclosingRectangle();
		
		for (Parking parking:parkingCollection){
			rect.registerCoord(parking.getCoordinate());
		}
		parkings=(new QuadTreeInitializer<Parking>()).getQuadTree(rect);
	}
	
	public Parking parkVehicle(Id agentId, Id actFacilityId, String actType){
		
		return null;
	}

}
