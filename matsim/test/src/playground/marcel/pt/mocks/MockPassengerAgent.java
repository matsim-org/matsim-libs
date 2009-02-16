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

import org.matsim.facilities.Facility;

import playground.marcel.pt.interfaces.PassengerAgent;

public class MockPassengerAgent implements PassengerAgent {

	public boolean arriveAtStop(final Facility stop) {
		// TODO [MR] Auto-generated method stub
		return false;
	}

	public boolean ptLineAvailable() {
		// TODO [MR] Auto-generated method stub
		return false;
	}

}
