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

package playground.anhorni.choiceSetGeneration.helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;

public class ZHFacilities {
	
	private TreeMap<Id<ActivityFacility>, ZHFacility> zhFacilities = new TreeMap<Id<ActivityFacility>, ZHFacility>();	
	private TreeMap<Id<Link>, ArrayList<Id<ActivityFacility>>> zhFacilitiesByLink = new TreeMap<Id<Link>, ArrayList<Id<ActivityFacility>>>();
	private int numberOfFacilities;
	
	
	public ZHFacilities() {
		this.zhFacilities = new TreeMap<>();
		this.zhFacilitiesByLink = new TreeMap<>();
	}
	
	public void addFacilityByLink(Id<Link> linkId, ZHFacility facility) {
	
		if (this.zhFacilitiesByLink.containsKey(linkId)) {
			this.zhFacilitiesByLink.get(linkId).add(facility.getId());
		}
		else {
			ArrayList<Id<ActivityFacility>> list = new ArrayList<>();
			list.add(facility.getId());
			this.zhFacilitiesByLink.put(linkId,list);
		}
		
		if (!this.zhFacilities.containsKey(facility.getId())) {
			this.zhFacilities.put(facility.getId(), facility);
			this.numberOfFacilities += 1;
		}			
	}
	
	public ArrayList<ZHFacility> getFacilitiesByLinkId(Id<Link> linkId) {
		ArrayList<Id<ActivityFacility>> idList = this.zhFacilitiesByLink.get(linkId);
		
		ArrayList<ZHFacility> facilitiesList = new ArrayList<ZHFacility>();
		
		Iterator<Id<ActivityFacility>> idList_it = idList.iterator();
		while (idList_it.hasNext()) {
			Id<ActivityFacility> id = idList_it.next();
			facilitiesList.add(this.zhFacilities.get(id));					
		}
		return facilitiesList;
	}
	
	
	public void addFacilitiesByLink(Id<Link> linkId, ArrayList<ZHFacility> facilitiesList) {		
		Iterator<ZHFacility> facility_it = facilitiesList.iterator();
		while (facility_it.hasNext()) {
			ZHFacility facility = facility_it.next();
			this.addFacilityByLink(linkId /*facility.getId()*/, facility);					
		}
	}

	public TreeMap<Id<ActivityFacility>, ZHFacility> getZhFacilities() {
		return zhFacilities;
	}

	public void setZhFacilities(TreeMap<Id<ActivityFacility>, ZHFacility> zhFacilities) {
		this.zhFacilities = zhFacilities;
	}

	public TreeMap<Id<Link>, ArrayList<Id<ActivityFacility>>> getZhFacilitiesByLink() {
		return zhFacilitiesByLink;
	}

	public void setZhFacilitiesByLink(TreeMap<Id<Link>, ArrayList<Id<ActivityFacility>>> zhFacilitiesByLink) {
		this.zhFacilitiesByLink = zhFacilitiesByLink;
	}

	public int getNumberOfFacilities() {
		return numberOfFacilities;
	}
	
	private void calculateAccesibilities() {		
		Iterator<ZHFacility> referenceFacility_it = this.zhFacilities.values().iterator();
		while (referenceFacility_it.hasNext()) {
			ZHFacility referenceFacility = referenceFacility_it.next();
			double accessibility02 = 0.0;
			double accessibility10 = 0.0;
			double accessibility20 = 0.0;
		
			Iterator<ZHFacility> facilities_it = this.zhFacilities.values().iterator();
			while (facilities_it.hasNext()) {
				ZHFacility facility = facilities_it.next();
				
				// distance in km
				double distance = 0.001 * CoordUtils.calcDistance(facility.getExactPosition(), referenceFacility.getExactPosition());
				accessibility02 += Math.exp(0.2 * distance * (-1.0));
				accessibility10 += Math.exp(1.0 * distance * (-1.0));
				accessibility20 += Math.exp(2.0 * distance * (-1.0));	
			}
			referenceFacility.setAccessibility02(accessibility02);
			referenceFacility.setAccessibility10(accessibility10);
			referenceFacility.setAccessibility20(accessibility20);
		}
	}	
	
	public void finish() {
		this.calculateAccesibilities();
	}
}
