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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public class ShopLocation extends Location {	 
	private int prevLoc = -99;
	private int nextLoc = -99;
	private int plz;
	
	private String visitFrequency;
	private int [] reasonsForVisit = new int[7];
	private boolean [] reasonsForNonVisit = new boolean[8];
	
	private int size = -1;
	private int price = -1;
	
	public ShopLocation(Id<Location> id) {
		super(id);
	}
	
	public ShopLocation(Id<Location> id, Coord coord) {
		super(id, coord);
	}
	
	public int getPrevLoc() {
		return prevLoc;
	}
	public int getNextLoc() {
		return nextLoc;
	}
	public void setPrevLoc(int prevLoc) {
		this.prevLoc = prevLoc;
	}
	public void setNextLoc(int nextLoc) {
		this.nextLoc = nextLoc;
	}
	public void setReasonsForVisit(int index, int value) {
		this.reasonsForVisit[index] = value;
	}
	public void setReasonsForNonVisit(int index, boolean value) {
		this.reasonsForNonVisit[index] = value;
	}
	public int[] getReasonsForVisit() {
		return reasonsForVisit;
	}
	public boolean[] getReasonsForNonVisit() {
		return reasonsForNonVisit;
	}
	public String getVisitFrequency() {
		return visitFrequency;
	}
	public void setVisitFrequency(String visitFrequency) {
		this.visitFrequency = visitFrequency;
	}
	public int getSize() {
		return size;
	}
	public int getPrice() {
		return price;
	}
	public void setSize(int size) {
		// invert size by category
		this.size = 6 - size;
	}
	public void setPrice(int price) {
		this.price = price;
	}
	public int getPlz() {
		return plz;
	}
	public void setPlz(int plz) {
		this.plz = plz;
	}
}
