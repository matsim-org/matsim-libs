/* *********************************************************************** *
 * project: org.matsim.*
 * ControlInputTestImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.withinday.trafficmanagement;



/**
 * @author dgrether
 *
 */
public class ControlInputTestImpl extends AbstractControlInputImpl {

	/**
	 * @see org.matsim.withinday.trafficmanagement.ControlInput#getNashTime()
	 */
	public double getNashTime() {
		return 0;
	}

	/**
	 * @see org.matsim.events.handler.EventHandlerI#reset(int)
	 */
	public void reset(final int iteration) {

	}

	public void finishIteration() {
	}

}
