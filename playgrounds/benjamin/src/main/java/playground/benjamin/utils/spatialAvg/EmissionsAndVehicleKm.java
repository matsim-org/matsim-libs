/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.benjamin.utils.spatialAvg;

public class EmissionsAndVehicleKm {
	
	Double emissionValue;
	Double linkLengthKm;

	public EmissionsAndVehicleKm(Double emissionValue, Double linkLenghtKm) {
		this.emissionValue = new Double (0.0);
		this.linkLengthKm = new Double (0.0);
	}

	public void add(Double emissionValue, Double linkLenghtKm) {
		this.emissionValue += emissionValue;
		this.linkLengthKm += linkLenghtKm;
	}

	public Double getLinkLenghtKm() {
		return this.linkLengthKm;
	}

	public Double getEmissionValue() {
		return this.emissionValue;
	}

}
