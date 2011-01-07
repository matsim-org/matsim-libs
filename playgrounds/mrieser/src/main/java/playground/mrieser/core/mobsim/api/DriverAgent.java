/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.mobsim.api;

import org.matsim.api.core.v01.Id;

import playground.mrieser.core.mobsim.network.api.MobsimLink;

public interface DriverAgent {

	/**
	 * Returns the next link the vehicle will drive along.
	 *
	 * @return The next link the vehicle will drive on, or null if an error has happened.
	 */
	public Id getNextLinkId();

	public void notifyMoveToNextLink();

	/**
	 * @return value between 0.0 and 1.0 (both included) for signaling there is an
	 * action to be performed, any other value (e.g. -1.0) for "no action on this link"
	 */
	public double getNextActionOnCurrentLink();

	public void handleNextAction(final MobsimLink link);

}
