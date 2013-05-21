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

package playground.anhorni.csestimation;

import java.util.ArrayList;
import java.util.TreeMap;
import org.matsim.api.core.v01.Id;


public class PersonLocations {
	private TreeMap<Id, ShopLocation> locations = new TreeMap<Id, ShopLocation>();
	private Location workLocation = null;
	private Location homeLocation = null;
	private ArrayList<Id> awareStoresInQuerySet = new ArrayList<Id>();
	private ArrayList<ShopLocation> unawareStoresInQuerySet = new ArrayList<ShopLocation>();
	
	private ArrayList<Id> visitedStoresInQuerySet = new ArrayList<Id>();
	private ArrayList<ShopLocation> unvisitedStoresInQuerySet = new ArrayList<ShopLocation>();
	private ArrayList<Id> nullAwareOrnullVisitedStoresInQuerySet = new ArrayList<Id>();
	
	
	public Location getWorkLocation() {
		return workLocation;
	}
	public Location getHomeLocation() {
		return homeLocation;
	}
	public void setWorkLocation(Location workLocation) {
		this.workLocation = workLocation;
	}
	public void setHomeLocation(Location homeLocation) {
		this.homeLocation = homeLocation;
	}
	public void addStore(ShopLocation store, int aware, int visited) {
		this.locations.put(store.getId(), store);
		if (aware == -1) {
			this.unawareStoresInQuerySet.add(store);
		}
		else if (aware == 1) {
			this.awareStoresInQuerySet.add(store.getId());
		}
		if (visited == -1) {
			this.unvisitedStoresInQuerySet.add(store);
		}
		else if (visited == 1) {
			this.visitedStoresInQuerySet.add(store.getId());
		}
	}
	
	public void addNullStore(ShopLocation store) {
		this.nullAwareOrnullVisitedStoresInQuerySet.add(store.getId());
	}
	
	public ArrayList<ShopLocation> getStoresPerFrequency(String frequency) {
		ArrayList<ShopLocation> returnSet = new ArrayList<ShopLocation>();
		for (Id location : this.awareStoresInQuerySet) {
			if (this.locations.get(location).getVisitFrequency().equals(frequency)) {
				returnSet.add(this.locations.get(location));
			}
		}
		return returnSet;
	}
	public ArrayList<Id> getNullAwareOrnullVisitedStoresInQuerySet() {
		return nullAwareOrnullVisitedStoresInQuerySet;
	}
	public ArrayList<Id> getAwareStoresInQuerySet() {
		return awareStoresInQuerySet;
	}
	public ArrayList<Id> getVisitedStoresInQuerySet() {
		return visitedStoresInQuerySet;
	}
}
