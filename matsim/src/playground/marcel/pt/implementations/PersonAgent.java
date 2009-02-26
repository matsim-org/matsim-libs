/* *********************************************************************** *
 * project: org.matsim.*
 * PersonAgent.java
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

package playground.marcel.pt.implementations;

import org.matsim.facilities.Facility;
import org.matsim.interfaces.core.v01.Link;

import playground.marcel.pt.interfaces.DriverAgent;
import playground.marcel.pt.interfaces.PassengerAgent;

public class PersonAgent implements DriverAgent, PassengerAgent {

	public Link chooseNextLink() {
		// TODO [MR] Auto-generated method stub
		return null;
	}

	public void enterNextLink() {
		// TODO [MR] Auto-generated method stub
	}

	public void leaveCurrentLink() {
		// TODO [MR] Auto-generated method stub
	}

	public boolean arriveAtStop(final Facility stop) {
		// TODO [MR] Auto-generated method stub
		return false;
	}

	public boolean ptLineAvailable() {
		// TODO [MR] Auto-generated method stub
		return false;
	}

}
