/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerAccessEgress.java
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

package org.matsim.pt.qsim;

/**
 * @author mrieser
 */
public interface PassengerAccessEgress {

	/** 
	 * @param agent agent to be handled
	 * @param time time the agent should be handled
	 * @return true, if handled correctly, otherwise false, e.g. vehicle has no capacity left
	 */
	public boolean handlePassengerEntering(final PassengerAgent agent, final double time);

	/** 
	 * @param agent agent to be handled
	 * @param time time the agent should be handled
	 * @return true, if handled correctly, otherwise false
	 */
	public boolean handlePassengerLeaving(final PassengerAgent agent, final double time);

}
