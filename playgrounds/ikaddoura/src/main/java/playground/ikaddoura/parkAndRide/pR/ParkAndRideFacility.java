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
import org.matsim.api.core.v01.network.Link;

/**
 * @author Ihab
 *
 */
public class ParkAndRideFacility {

	private int nr;
	private Id linkArein; // erster Link (hin)
	private Id linkAraus; // erster Link (zurück)
	private Id linkBrein; // zweiter Link (hin)
	private Id linkBraus; // zweiter Link (zurück)
	
	public ParkAndRideFacility(int nummer, Id linkArein, Id linkAraus, Id linkBrein, Id linkBraus) {
		this.nr = nummer;
		this.linkArein = linkArein;
		this.linkAraus = linkAraus;
		this.linkBrein = linkBrein;
		this.linkBraus = linkBraus;
	}

	/**
	 * @return the linkArein
	 */
	public Id getLinkArein() {
		return linkArein;
	}

	/**
	 * @param linkArein the linkArein to set
	 */
	public void setLinkArein(Id linkArein) {
		this.linkArein = linkArein;
	}

	/**
	 * @return the linkAraus
	 */
	public Id getLinkAraus() {
		return linkAraus;
	}

	/**
	 * @param linkAraus the linkAraus to set
	 */
	public void setLinkAraus(Id linkAraus) {
		this.linkAraus = linkAraus;
	}

	/**
	 * @return the linkBrein
	 */
	public Id getLinkBrein() {
		return linkBrein;
	}

	/**
	 * @param linkBrein the linkBrein to set
	 */
	public void setLinkBrein(Id linkBrein) {
		this.linkBrein = linkBrein;
	}

	/**
	 * @return the linkBraus
	 */
	public Id getLinkBraus() {
		return linkBraus;
	}

	/**
	 * @param linkBraus the linkBraus to set
	 */
	public void setLinkBraus(Id linkBraus) {
		this.linkBraus = linkBraus;
	}

	/**
	 * @return the nr
	 */
	public int getNr() {
		return nr;
	}

	/**
	 * @param nr the nr to set
	 */
	public void setNr(int nr) {
		this.nr = nr;
	}

	
	

	
}
