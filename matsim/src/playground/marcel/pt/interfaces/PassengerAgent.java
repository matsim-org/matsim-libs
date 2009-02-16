/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerAgent.java
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

package playground.marcel.pt.interfaces;

import org.matsim.facilities.Facility;

/**
 * @author mrieser
 */
public interface PassengerAgent {

	/**
	 * Informs a passenger waiting at a stop that a transit line
	 * has arrived and is ready to be boarded.
	 *
	 * TODO [MR] find better name for method
	 *
	 * @return <code>true<code> if the passenger wants to board the line, <code>false</code> otherwise
	 */
	public boolean ptLineAvailable();

	/**
	 * Informs a passenger in a transit vehicle that the vehicle has
	 * arrived at the specified stop.
	 *
	 * TODO [MR] find better name for method
	 * @param stop the stop the vehicle arrived
	 *
	 * @return <code>true</code> if the passenger wants to exit the vehicle, <code>false</code> otherwise
	 */
	public boolean arriveAtStop(final Facility stop);

}
