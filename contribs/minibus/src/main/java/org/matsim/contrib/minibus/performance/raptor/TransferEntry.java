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
class TransferEntry {

	final int indexOfRouteStop;
	final double transferTime;
	
	public TransferEntry(int indexOfRouteStop, double transferTime) {
		this.indexOfRouteStop = indexOfRouteStop;
		this.transferTime = transferTime;
	}
	
	@Override
	public String toString() {
		return "To: " + this.indexOfRouteStop + " in " + transferTime + "s";
	}
	
}
