/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.vsp.analysis.modules.ptTripAnalysis.distance;

import org.matsim.api.core.v01.Id;

/**
 * @author droeder
 *
 */
public class DistAnalysisPtDriver {
	
	private Id id;
	private DistAnalysisVehicle vehicle = null;
	private double distance = 0;

	/**
	 * @param driverId
	 * @param id 
	 */
	public DistAnalysisPtDriver(Id driverId) {
		this.id  = driverId;
	}

	/**
	 * @param v
	 */
	public void registerVehicle(DistAnalysisVehicle v) {
		this.vehicle = v;
	}

	/**
	 * @param length
	 */
	public void processLinkEnterEvent(double length) {
		this.vehicle.processLinkEnterEvent(length);		
		this.distance += length;
	}
	
	public Id getId(){
		return this.id;
	}
}
