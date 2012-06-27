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

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;

/**
 * @author Ihab
 *
 */
public class FacilityLoadInfo {
	private Id facilityId;
	private List<Double> personEntering = new ArrayList<Double>();
	private List<Double> personLeaving = new ArrayList<Double>();

	public FacilityLoadInfo(Id id) {
		this.facilityId = id;
	}
	
	public Id getFacilityId() {
		return facilityId;
	}
	public List<Double> getPersonEntering() {
		return personEntering;
	}
	public void setPersonEntering(List<Double> personEntering) {
		this.personEntering = personEntering;
	}
	public List<Double> getPersonLeaving() {
		return personLeaving;
	}
	public void setPersonLeaving(List<Double> personLeaving) {
		this.personLeaving = personLeaving;
	}
	
}
