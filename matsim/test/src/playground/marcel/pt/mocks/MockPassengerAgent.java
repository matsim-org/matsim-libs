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

package playground.marcel.pt.mocks;

import org.matsim.core.facilities.ActivityFacility;
import org.matsim.transitSchedule.TransitStopFacility;

import playground.marcel.pt.interfaces.PassengerAgent;
import playground.marcel.pt.transitSchedule.api.TransitLine;

public class MockPassengerAgent implements PassengerAgent {

	private final ActivityFacility exitStop;
	
	public MockPassengerAgent(final ActivityFacility exitStop) {
		this.exitStop = exitStop;
	}
	
	public boolean arriveAtStop(final TransitStopFacility stop) {
		return stop == exitStop;
	}

	public boolean ptLineAvailable(final TransitLine line) {
		return true;
	}

}
