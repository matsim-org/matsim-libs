/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.parkingSearch.planLevel.linkFacilityMapping;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

import playground.wrashid.lib.tools.kml.Color;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;

public class LinkParkingFacilityAssociation extends LinkFacilityAssociation {

	public LinkParkingFacilityAssociation(ActivityFacilities facilities, NetworkImpl network) {
		this.network=network;
		
		for (ActivityFacility facility: facilities.getFacilities().values()){
			addFacilityToHashMap((ActivityFacilityImpl) facility);
		}		
	}
	
	private void addFacilityToHashMap(ActivityFacilityImpl facility) {
		Id facilityLink=getClosestLink(facility);
		
		assureHashMapInitializedForLink(facilityLink);
		
		ArrayList<ActivityFacilityImpl> list=linkFacilityMapping.get(facilityLink);
		
		if (facility.getActivityOptions().containsKey("parking")){
			if (list.contains(facility)){
				throw new Error("!!!!!!!!!!!!!!");
			}
			
			list.add(facility);
			
			
			
			
			// DEBUG INFO: display parking facilities
			if (GeneralLib.getDistance(new CoordImpl(4528426.090831845,5822407.437950259), network.getLinks().get(facilityLink).getCoord())<5000){
				ParkingRoot.getMapDebugTrace().addPointCoordinate(network.getLinks().get(facilityLink).getCoord(), facilityLink.toString(), Color.GREEN);
			}
		}
	}

	
}
