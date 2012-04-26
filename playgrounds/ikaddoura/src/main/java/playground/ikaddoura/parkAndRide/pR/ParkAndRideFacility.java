/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideFacility.java
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

/**
 * 
 */
package playground.ikaddoura.parkAndRide.pR;

import org.matsim.api.core.v01.Id;

/**
 * @author Ihab
 *
 */
public class ParkAndRideFacility {

	private int nr;
	private Id prLink1in; // erster Link (hin) (ampel)
	private Id prLink1out; // erster Link (zurück)
	private Id prLink2in; // zweiter Link (hin) (parkAndRideLink)
	private Id prLink2out; // zweiter Link (zurück)

	public int getNr() {
		return nr;
	}

	public void setNr(int nr) {
		this.nr = nr;
	}

	public Id getPrLink1in() {
		return prLink1in;
	}

	public void setPrLink1in(Id prLink1in) {
		this.prLink1in = prLink1in;
	}

	public Id getPrLink1out() {
		return prLink1out;
	}

	public void setPrLink1out(Id prLink1out) {
		this.prLink1out = prLink1out;
	}

	public Id getPrLink2in() {
		return prLink2in;
	}

	public void setPrLink2in(Id prLink2in) {
		this.prLink2in = prLink2in;
	}

	public Id getPrLink2out() {
		return prLink2out;
	}

	public void setPrLink2out(Id prLink2out) {
		this.prLink2out = prLink2out;
	}

	
}
