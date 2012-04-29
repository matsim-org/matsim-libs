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

package playground.anhorni.locationchoice.preprocess.facilities.assembleFacilitiesVariousSources;

import java.util.List;
import java.util.Vector;

import org.matsim.api.core.v01.Coord;

public class Hectare {
	
	private Coord coords;
	private List<Integer> shops = new Vector<Integer>();
	
	public Hectare(Coord coords) {
		super();
		this.coords = coords;
	}
	
	public Coord getCoords() {
		return this.coords;
	}

	public void addShop(int shop) {
		this.shops.add(shop);
	}
	
	public List<Integer> getShops() {
		return this.shops;
	}
}
