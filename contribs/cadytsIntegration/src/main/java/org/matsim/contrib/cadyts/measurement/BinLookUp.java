/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.cadyts.measurement;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.cadyts.general.LookUp;

/**
 * @author dziemke
 *
 */
public class BinLookUp implements LookUp<Measurement>{
	
//	private Network network;
//	private Map<Id<Measurement>, Measurement> measurementsMap;
	private Measurements measurements;

//	DistributionBinLookUp( Scenario sc ) {
//		this.network = sc.getNetwork();
//	}
	
//	LinkLookUp( Network net ) {
//	BinLookUp(Map<Id<Measurement>, Measurement> measurementsMap) {
	BinLookUp(Measurements measurements) {
//		this.network = net ;
		this.measurements = measurements;
	}
	
	@Override
	public Measurement lookUp( Id<Measurement> id ) {
		Measurement value = measurements.getMeasurment(id);
		return value;
	}
}