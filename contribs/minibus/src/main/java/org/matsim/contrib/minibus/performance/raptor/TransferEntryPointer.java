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

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitRoute;

public class TransferEntryPointer extends TransferEntry{

	final WrappedTransitRouteStop transferDestination;
	final Id<TransitRoute> routeId;

	public TransferEntryPointer(int indexOfRouteStop, double transferTime, WrappedTransitRouteStop transferDestination, Id<TransitRoute> routeId) {
		super(indexOfRouteStop, transferTime);
		this.transferDestination = transferDestination;
		this.routeId = routeId;
	}
	
	@Override
	public String toString() {
		return super.toString() + " target id " + transferDestination;
	}

}
