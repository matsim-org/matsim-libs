/* *********************************************************************** *
 * project: org.matsim.*
 * DriverAgent.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

/**
 * 
 * @author nagel
 *
 */
public interface DriverAgent extends Identifiable<Person>, NetworkAgent, VehicleUsingAgent {

	/**
	 * @return The next link the vehicle will drive on, or null if an error has happened.
	 */
	public Id<Link> chooseNextLinkId();

	/**
	 * notifies the agent that it was moved over the node.  
	 * <p></p>
	 * Design thoughts:<ul>
	 * <li> I find it difficult to see how one should do without this.  Somehow the mobsim needs to tell the 
	 * driver where she is. The mobsim could tell the vehicle, which tells the agent.  The DriverAgent would still 
	 * need this method. kai, nov'11
	 * </ul>
	 */
	public void notifyMoveOverNode(Id<Link> newLinkId);
	
	public boolean isWantingToArriveOnCurrentLink( ) ;
	
}
