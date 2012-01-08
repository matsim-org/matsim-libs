/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.ptproject.qsim.interfaces;

import org.matsim.api.core.v01.Id;
import org.matsim.ptproject.qsim.qnetsimengine.NetsimNetwork;


/**Mobsim could, at least in theory, also operate without a network (e.g. based on activities with
 * coordinates).  Netsim has a NetsimNetwork.
 * 
 * @author nagel
 */
public interface Netsim extends Mobsim {

	NetsimNetwork getNetsimNetwork();
	
//	/**Registering and unregistering agents on links for visualization (and maybe other purposes).  
//	 * 
//	 * Design decision: In theory, the links could also ask the central
//	 * "activities" or "agentTracker" data structures, but in practice this is too slow.  kai, aug'10
//	 * 
//	 * This needs the "getCurrentLinkId()" of the MobsimAgent filled with meaningful information.
//	 * 	 */
//	void registerAdditionalAgentOnLink( final MobsimAgent agent ) ;

//	MobsimAgent unregisterAdditionalAgentOnLink(Id agentId, Id linkId);

	void addParkedVehicle(MobsimVehicle veh, Id startLinkId);


	
}