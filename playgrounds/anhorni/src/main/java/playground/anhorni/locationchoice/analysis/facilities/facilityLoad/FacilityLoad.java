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

package playground.anhorni.locationchoice.analysis.facilities.facilityLoad;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;

public class FacilityLoad {
	
	private Id<ActivityFacility> facilityId;
	private Coord coord;
	private double load0 = 0.0;
	private double load1 = 0.0;
	
	public Id<ActivityFacility> getFacilityId() {
		return facilityId;
	}
	public void setFacilityId(Id<ActivityFacility> facilityId) {
		this.facilityId = facilityId;
	}
	public double getLoad0() {
		return load0;
	}
	public void setLoad0(double load0) {
		this.load0 = load0;
	}
	public double getLoad1() {
		return load1;
	}
	public void setLoad1(double load1) {
		this.load1 = load1;
	}

	public Coord getCoord() {
		return coord;
	}
	public void setCoord(Coord coord) {
		this.coord = coord;
	}
	
	public double getLoadDiffRel() {
		if (load0 > 0.0) {
			return (load1 - load0) / load0;
		}
		else {
			if (load1 > 0) {
				return 1.0;
			}
			else {
				return 0.0;
			}
		}
		
	}
}
