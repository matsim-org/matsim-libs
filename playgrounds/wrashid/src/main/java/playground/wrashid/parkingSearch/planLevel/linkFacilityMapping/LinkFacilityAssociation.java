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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacilityImpl;

import playground.wrashid.parkingSearch.planLevel.ParkingGeneralLib;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.parkingType.ParkingAttribute;

import java.util.ArrayList;
import java.util.HashMap;

public class LinkFacilityAssociation {

	protected HashMap<Id<Link>, ArrayList<ActivityFacilityImpl>> linkFacilityMapping = new HashMap<>();
	protected NetworkImpl network;

	protected LinkFacilityAssociation() {

	}

	public LinkFacilityAssociation(Controler controler) {
        ActivityFacilitiesImpl facilities = (ActivityFacilitiesImpl) controler.getScenario().getActivityFacilities();

        this.network = (NetworkImpl) controler.getScenario().getNetwork();

		init(facilities);
	}

	public LinkFacilityAssociation(ActivityFacilities facilities, NetworkImpl network) {
		this.network = network;
		init(facilities);
	}

	private void init(ActivityFacilities facilities) {
		for (ActivityFacility facility : facilities.getFacilities().values()) {
			addFacilityToHashMap((ActivityFacilityImpl) facility);
		}
	}

	/**
	 * put the facility into the arrayList for the appropriate link.
	 * 
	 * @param facility
	 */
	private void addFacilityToHashMap(ActivityFacilityImpl facility) {
		Id<Link> facilityLink = getClosestLink(facility);

		assureHashMapInitializedForLink(facilityLink);

		ArrayList<ActivityFacilityImpl> list = linkFacilityMapping.get(facilityLink);

		// implicit assumption: a facility will only get added once for the same
		// link
		list.add(facility);
	}

	/**
	 * need also to takle the case, if facility not assigned
	 * 
	 * @return
	 */
	protected Id<Link> getClosestLink(ActivityFacilityImpl facility) {
		if (facility.getLinkId() == null) {
			return NetworkUtils.getNearestLink(network, facility.getCoord()).getId();
		} else {
			return facility.getLinkId();
		}
	}

	/**
	 * Make sure, that in the HashMap an entry exists for the given linkId
	 * 
	 * @param linkId
	 */
	protected void assureHashMapInitializedForLink(Id<Link> linkId) {
		if (!linkFacilityMapping.containsKey(linkId)) {
			linkFacilityMapping.put(linkId, new ArrayList<ActivityFacilityImpl>());
		}
	}

	/**
	 * - post-cont: will never return null.
	 * @param linkId
	 * @return
	 */
	public ArrayList<ActivityFacilityImpl> getFacilities(Id<Link> linkId) {
		ArrayList<ActivityFacilityImpl> result = linkFacilityMapping.get(linkId);
		if (result == null) {
			result = new ArrayList<ActivityFacilityImpl>();
		}
		return result;
	}
	
	/**
	 * - post-cont: will never return null.
	 * @param linkId
	 * @return
	 */
	public ArrayList<ActivityFacilityImpl> getFacilitiesHavingParkingAttribute(Id<Link> linkId, ParkingAttribute parkingAttribute) {
		ArrayList<ActivityFacilityImpl> result = linkFacilityMapping.get(linkId);
		if (result == null) {
			result = new ArrayList<ActivityFacilityImpl>();
		} else if (parkingAttribute!=null) {
			for (int i=0;i<result.size();i++){
				Id<ActivityFacility> facilityId=result.get(i).getId();
				ArrayList<ActivityFacilityImpl> filteredResult=new ArrayList<ActivityFacilityImpl>();
				if (ParkingGeneralLib.containsParkingAttribute(ParkingRoot.getParkingFacilityAttributes().getParkingFacilityAttributes(facilityId), parkingAttribute)){
					filteredResult.add(result.get(i));
				}
			}
		}
		return result;
	}
	
	

}
