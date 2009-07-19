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

package playground.marcel.pt.fakes;

import org.matsim.core.facilities.ActivityFacility;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitStopFacility;

import playground.marcel.pt.queuesim.PassengerAgent;

/**
 * A very simple implementation of the interface {@link PassengerAgent} for
 * use in tests. Enters every available line and exits at the specified stop.
 *
 * @author mrieser
 */
public class FakePassengerAgent implements PassengerAgent {

	private final ActivityFacility exitStop;

	/**
	 * @param exitStop can be <code>null</code>
	 */
	public FakePassengerAgent(final ActivityFacility exitStop) {
		this.exitStop = exitStop;
	}

	public boolean arriveAtStop(final TransitStopFacility stop) {
		return stop == this.exitStop;
	}

	public boolean ptLineAvailable(final TransitLine line) {
		return true;
	}

}
