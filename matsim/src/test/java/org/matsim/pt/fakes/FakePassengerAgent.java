/* *********************************************************************** *
 * project: org.matsim.*
 * MockPassengerAgent.java
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

package org.matsim.pt.fakes;

import java.util.List;

import org.matsim.pt.qsim.PassengerAgent;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitStopFacility;


/**
 * A very simple implementation of the interface {@link PassengerAgent} for
 * use in tests. Enters every available line and exits at the specified stop.
 *
 * @author mrieser
 */
public class FakePassengerAgent implements PassengerAgent {

	private final TransitStopFacility exitStop;

	/**
	 * @param exitStop can be <code>null</code>
	 */
	public FakePassengerAgent(final TransitStopFacility exitStop) {
		this.exitStop = exitStop;
	}

	public boolean getExitAtStop(final TransitStopFacility stop) {
		return stop == this.exitStop;
	}

	@Override
	public boolean getEnterTransitRoute(TransitLine line,
			TransitRoute transitRoute, List<TransitRouteStop> stopsToCome) {
		return true;
	}

}
