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
package playground.vsp.demandde.cemdap.input;

/**
 * Storage object for commuter relations.
 * 
 * @author dziemke
 */
public class CommuterRelationV2 {

	private String origin;
	private String destination;
	private Integer tripsAll;
	private Integer tripsMale;
	private Integer tripsFemale;

		
	public CommuterRelationV2(String origin, String destination, Integer tripsAll, Integer tripsMale, Integer tripsFemale) {
		this.origin = origin;
		this.destination = destination;
		this.tripsAll = tripsAll;
		this.tripsMale = tripsMale;
		this.tripsFemale = tripsFemale;
	}

	public String getFrom() {
		return this.origin;
	}

	public String getTo() {
		return this.destination;
	}
	
	public Integer getTrips() {
		return this.tripsAll;
	}

	public Integer getTripsMale() {
		return this.tripsMale;
	}
	
	public void setTripsMale(int tripsMale) {
		this.tripsMale = tripsMale;
	}

	public Integer getTripsFemale() {
		return this.tripsFemale;
	}
	
	public void setTripsFemale(int tripsFemale) {
		this.tripsFemale = tripsFemale;
	}
}