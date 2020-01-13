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

import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * 
 * @author aneumann
 *
 */
public class TransitStopEntry {
	
	final TransitStopFacility transitStopFacility;
	final int numberOfTransfers;
	final int indexOfFirstTransfer;
	
	TransitStopEntry(TransitStopFacility transitStopFacility, int numberOfTransfers, int indexOfFirstTransfer) {
		this.transitStopFacility = transitStopFacility;
		this.numberOfTransfers = numberOfTransfers;
		this.indexOfFirstTransfer = indexOfFirstTransfer;
	}

	@Override
	public String toString() {
		return "Id " + transitStopFacility.getId() + ", " + numberOfTransfers + " transfers from " + indexOfFirstTransfer;
	}
}
