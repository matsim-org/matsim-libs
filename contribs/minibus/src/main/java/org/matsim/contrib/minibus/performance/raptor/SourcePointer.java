/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.performance.raptor;


/**
 * 
 * @author aneumann
 *
 */
public class SourcePointer {
	
	final double earliestArrivalTime;
	final int indexOfTargetRouteStop;
	final SourcePointer source;
	final boolean transfer;
	
	public SourcePointer(double earliestArrivalTime, int indexOfRouteStop, SourcePointer source, boolean transfer) {
		this.earliestArrivalTime = earliestArrivalTime;
		this.indexOfTargetRouteStop = indexOfRouteStop;
		this.source = source;
		this.transfer = transfer;
	}

	@Override
	public String toString() {
		String sourceId = null;
		if (source != null) {
			sourceId = String.valueOf(this.source.indexOfTargetRouteStop);
		}
		return "Id " + this.indexOfTargetRouteStop + ", EA " + this.earliestArrivalTime + " from " + sourceId;
	}
}
