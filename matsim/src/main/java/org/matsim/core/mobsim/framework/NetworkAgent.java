/* *********************************************************************** *
 * project: matsim
 * NetworkAgent.java
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

package org.matsim.core.mobsim.framework;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * This was separated out since there were some commonalities between "MobsimAgent" and "DriverAgent".
 * 
 * @author nagel
 */
public interface NetworkAgent extends MobsimAgentMarkerInterface {
	
	/**
	 * Design comments:<ul>
	 * <li> Agent in many situations knows where he/she is.  If not, return null.
	 * </ul>
	 * 
	 * @return Id of current link.  May be null (e.g. for teleporting agents)
	 */
	public Id<Link> getCurrentLinkId();

	/**
	 * Design comments:<ul>
	 * <li> Originally, agents knew where they were going.  This was then also used for teleportation.  Since then, agents may not know where
	 * they are ultimately going.  It might thus be more expressive to have "getDestinationLinkId" only for teleporting agents, and have
	 * "isArrivingOnCurrentLink" for all others (which would be parallel to how bus passengers handle this).  If someone would find this better,
	 * and wants to refactor the code, then please go ahead. kai, nov'14
	 * </ul>
	 * 
	 * @return Id of destination link.  May be null (e.g. for cruising taxi drivers)
	 */
	public Id<Link> getDestinationLinkId();
	
	    /**
	     * Convenience method for Leg.getMode(), in an attempt to get rid of getCurrentLeg().  If the agent is not on a leg,
	     * the behavior of this method is undefined (so don't rely on it).
	     * <p></p>
	     * Comments:<ul>
	     * <li>Should be renamed to getLegMode(), in my opinion, as the mode of an agent can be anything else. mrieser/jan'12
	     * <li>I don't mind, but my current eclipse can't do the refactoring (https://bugs.eclipse.org/bugs/show_bug.cgi?id=293861). kai, jan'13
	     * </ul>
	     */
	    public String getMode();



}
