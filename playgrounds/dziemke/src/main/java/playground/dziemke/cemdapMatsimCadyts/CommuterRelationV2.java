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
 * Storage object for commuter relations.
 * 
 * @author dziemke
 */
public class CommuterRelationV2 {

	private int origin;
	private int destination;
	private int tripsAll;
	private int tripsMale;
	private int tripsFemale;

		
	public CommuterRelationV2(int origin, int destination, int TripsAll, int tripsMale, int tripsFemale) {
		this.origin = origin;
		this.destination = destination;
		this.tripsAll = tripsAll;
		this.tripsMale = tripsMale;
		this.tripsFemale = tripsFemale;
	}

	public int getFrom() {
		return this.origin;
	}

	public int getTo() {
		return this.destination;
	}

	public int getTripsMale() {
		return this.tripsMale;
	}

	public int getTripsFeale() {
		return this.tripsFemale;
	}
}