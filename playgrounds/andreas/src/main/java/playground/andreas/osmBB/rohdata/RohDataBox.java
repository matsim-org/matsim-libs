/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.osmBB.rohdata;

public class RohDataBox {
	
	private final String date;
	private final String time;
	
	private final boolean valid; 
	
	private int dtvKfz;
	private int dtvLkw;
	private int dtvPkw;
	private int vKfz;
	private int vLkw;
	private int vPkw;
	
	public RohDataBox(String date, String time, boolean valid) {
		this.date = date;
		this.time = time;
		this.valid = valid;
	}
	
	protected boolean isValid(){
		return this.valid;
	}	

	public void setDtvKfz(int dtvKfz) {
		this.dtvKfz = dtvKfz;
	}

	public void setDtvLkw(int dtvLkw) {
		this.dtvLkw = dtvLkw;
	}

	public void setDtvPkw(int dtvPkw) {
		this.dtvPkw = dtvPkw;
	}

	public void setvKfz(int vKfz) {
		this.vKfz = vKfz;
	}

	public void setvLkw(int vLkw) {
		this.vLkw = vLkw;
	}

	public void setvPkw(int vPkw) {
		this.vPkw = vPkw;
	}
	
	@Override
	public String toString() {
		return this.date + " " + this.time + ", DTV Kfz " + this.dtvKfz + ", DTV Lkw " + this.dtvLkw + ", DTV Pkw " + this.dtvPkw + ", v Kfz " + this.vKfz + ", v Lkw " + this.vLkw + ", v Pkw " + this.vPkw;
	}
}
