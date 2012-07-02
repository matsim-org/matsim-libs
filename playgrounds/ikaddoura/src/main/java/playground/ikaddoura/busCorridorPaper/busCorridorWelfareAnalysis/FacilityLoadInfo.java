/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityLoadInfo.java
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
package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

import org.matsim.api.core.v01.Id;

/**
 * @author Ihab
 *
 */
public class FacilityLoadInfo {
	private Id facilityId;
	private int personEntering = 0;
	private int personLeaving = 0;
	private int passengers = 0;

	public FacilityLoadInfo(Id id) {
		this.facilityId = id;
	}
	
	public Id getFacilityId() {
		return facilityId;
	}

	public void setPersonEntering(int personEntering) {
		this.personEntering = personEntering;
	}

	public int getPersonEntering() {
		return personEntering;
	}

	public void setPersonLeaving(int personLeaving) {
		this.personLeaving = personLeaving;
	}

	public int getPersonLeaving() {
		return personLeaving;
	}

	public void setPassengers(int passengers) {
		this.passengers = passengers;
	}

	public int getPassengers() {
		return passengers;
	}
}
