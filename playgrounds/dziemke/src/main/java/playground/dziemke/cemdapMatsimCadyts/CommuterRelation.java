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
package playground.dziemke.cemdapMatsimCadyts;

/**
 * @author dziemke
 */
public class CommuterRelation {

	private int from;
	private String fromName;
	private int to;
	private String toName;
	private int trips;
		
	public CommuterRelation(int from, String fromName, int to, String toName, int trips) {
		this.from = from;
		this.fromName = fromName;
		this.to = to;
		this.toName = toName;
		this.trips = trips;
	}

	public int getFrom() {
		return this.from;
	}

//	public void setFrom(int from) {
//		this.from = from;
//	}
	
//	public String getFromName() {
//		return this.fromName;
//	}

//	public void setFromName(String fromName) {
//		this.fromName = fromName;
//	}

	public int getTo() {
		return this.to;
	}

//	public void setTo(int to) {
//		this.to = to;
//	}
	
//	public String getToName() {
//		return this.toName;
//	}

//	public void setToName(String toName) {
//		this.toName = toName;
//	}

	public int getQuantity() {
		return this.trips;
	}

//	public void setQuantity(int quantity) {
//		this.quantity = quantity;
//	}

}