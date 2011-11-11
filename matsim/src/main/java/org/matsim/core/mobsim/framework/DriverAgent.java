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
import org.matsim.ptproject.qsim.interfaces.MobsimVehicle;
import org.matsim.ptproject.qsim.qnetsimengine.QVehicle;

/**
 * @author nagel
 *
 */
public interface DriverAgent extends NetworkAgent {

	/**
	 * @return The next link the vehicle will drive on, or null if an error has happened.
	 */
	public Id chooseNextLinkId();

	/**
	 * notifies the agent that it was moved over the node.  Design thoughts:<ul>
	 * <li> I find it difficult to see how one should do without this.  Somehow the mobsim needs to tell the 
	 * driver where she is. At best, the mobsim could tell the vehicle, which tells the agent.  kai, nov'11
	 * </ul>
	 */
	public void notifyMoveOverNode(Id newLinkId);
	
	public void setVehicle( final MobsimVehicle veh ) ;
	
	/**
	 * Design thoughts:<ul>
	 * <li> MZ states (in his AdapterAgent) that the DriverAgent should not have this reference.  
	 * I am, in fact, not so sure (any more); maybe it is not so bad to have this.  Clearly, symmetric
	 * connectors would be better.  kai, nov'11
	 * </ul>
	 */
	public MobsimVehicle getVehicle() ;
	
	public Id getPlannedVehicleId() ;
	
//	/**
//	 * List of vehicles the DriverAgent has access to.  This could be vehicles she has the key for.  But it could also
//	 * be a temporary access provided by some car sharing company.  Design thoughts:<ul>
//	 * <li> Automagic vehicle generation, if switched on (!), could be done exactly for those DriverAgents who have
//	 * this list empty. 
//	 * </ul> 
//	 */
//	public List<QVehicle> getAccessibleVehicles() ;
	

	
}
