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

public class ShopLocation extends Location {	 
	private int prevLoc = -99;
	private int nextLoc = -99;
	
	private String visitFrequency;
	private int [] reasonsForVisit = new int[7];
	private boolean [] reasonsForNonVisit = new boolean[8];
	
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
}
