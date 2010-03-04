/* *********************************************************************** *
 * project: org.matsim.*
 * QSimEngine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.ptproject.qsim;

/**
 * Coordinates the movement of vehicles on the links and the nodes.
 *
 * @author cdobler
 * @author dgrether
 */

public interface QSimEngine extends LinkActivator{

	/**
	 * Implements one simulation step, called from simulation framework
	 * @param time The current time in the simulation.
	 */
	public void simStep(final double time);
		
	/**
	 * Do some clean up.
	 */
	public void afterSim();
}
