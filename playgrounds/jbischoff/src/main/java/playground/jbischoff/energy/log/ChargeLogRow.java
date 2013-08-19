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

package playground.jbischoff.energy.log;

import org.matsim.api.core.v01.Id;
/**
 * 
 * 
 * 
 * @author jbischoff
 *
 */
public class ChargeLogRow implements Comparable<ChargeLogRow> {
	private Id chargerId;
	private double time;
	private double absoluteOcc;
	private double relativeOcc;
	

	public ChargeLogRow(Id chargertId, double time, double aocc, double rocc) {
		this.chargerId = chargertId;
		this.time = time;
		this.absoluteOcc = aocc;
		this.relativeOcc = rocc;
	}

	public Id getChargerId() {
		return chargerId;
	}


	public double getTime() {
		return time;
	}

	public double getAbsoluteLOC() {
		return absoluteOcc;
	}

	public double getRelativeLOC() {
		return relativeOcc;
	}

	@Override
	public int compareTo(ChargeLogRow arg0) {
		return getChargerId().compareTo(arg0.getChargerId());
	}
 
}
