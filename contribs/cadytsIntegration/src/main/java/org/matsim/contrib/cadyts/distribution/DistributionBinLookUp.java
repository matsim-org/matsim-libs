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
package org.matsim.contrib.cadyts.distribution;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.cadyts.general.LookUp;

/**
 * @author dziemke
 *
 */
public class DistributionBinLookUp implements LookUp<Double>{
	
//	private Network network;
	private Map<Id<Link>, Double> measurementsMap;

//	DistributionBinLookUp( Scenario sc ) {
//		this.network = sc.getNetwork();
//	}
	
//	LinkLookUp( Network net ) {
	DistributionBinLookUp(Map<Id<Link>, Double> measurementsMap) {
//		this.network = net ;
		this.measurementsMap = measurementsMap;
	}
	
	@Override
	public Double lookUp( Id<Double> id ) {
		Double value = measurementsMap.get(id);
		return value;
	}
}