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

public class PersonLocations {
	private Location workLocation = null;
	private Location homeLocation = null;
	private ArrayList<ShopLocation> awareStoresInQuerySet = new ArrayList<ShopLocation>();
	private ArrayList<ShopLocation> unawareStoresInQuerySet = new ArrayList<ShopLocation>();
	
	private ArrayList<ShopLocation> visitedStoresInQuerySet = new ArrayList<ShopLocation>();
	private ArrayList<ShopLocation> unvisitedStoresInQuerySet = new ArrayList<ShopLocation>();
	
	
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
		if (aware == -1) {
			this.unawareStoresInQuerySet.add(store);
		}
		else if (aware == 1) {
			this.awareStoresInQuerySet.add(store);
		}
		if (visited == -1) {
			this.unvisitedStoresInQuerySet.add(store);
		}
		else if (visited == 1) {
			this.visitedStoresInQuerySet.add(store);
		}
	}
	
	public ArrayList<ShopLocation> getStoresPerFrequency(String frequency) {
		ArrayList<ShopLocation> returnSet = new ArrayList<ShopLocation>();
		for (ShopLocation location : this.awareStoresInQuerySet) {
			if (location.getVisitFrequency().equals(frequency)) {
				returnSet.add(location);
			}
		}
		return returnSet;
	}
}
