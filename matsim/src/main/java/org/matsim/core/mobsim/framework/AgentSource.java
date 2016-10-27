/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.mobsim.framework;


/**
 * If you add an AgentSource into the QSim, the method insertAgentsIntoMobsim() will be called during the initialization phase.
 * <p></p>
 * For an example see {@link tutorial.programming.ownMobsimAgent.RunAgentSourceExample}
 */
public interface AgentSource {
	// keep stable: referenced from book

    public void insertAgentsIntoMobsim();

}
