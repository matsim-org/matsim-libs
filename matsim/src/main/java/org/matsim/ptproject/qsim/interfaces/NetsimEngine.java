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

package org.matsim.ptproject.qsim.interfaces;

import org.matsim.core.mobsim.framework.Steppable;


/**
 * Coordinates the movement of vehicles on the links and the nodes.
 * <p/>
 * Design decisions:<ul>
 * <li> The NetsimEngine carries the NetsimNetwork.  It can internally do what it wants, but externally needs
 * to fulfill the corresponding interfaces as a "service" for the other pieces of the Netsim.  
 * <li> In "consequence", also the c'tor for the NetsimNetwork is provided by the NetsimEngine (this could
 * be solved differently, in more than one way).
 *
 * @author cdobler
 * @author dgrether
 */
public interface NetsimEngine extends MobsimEngine {

//	AgentSnapshotInfoBuilder getAgentSnapshotInfoBuilder();

	/**
	 * for logging purposes; otherwise this is not important.  kai, oct'10
	 */
	int getNumberOfSimulatedLinks();
	int getNumberOfSimulatedNodes();
	
	NetsimNetwork getNetsimNetwork();
	
	NetsimNetworkFactory<? extends NetsimNode, ? extends NetsimLink> getNetsimNetworkFactory() ;
	
	DepartureHandler getDepartureHandler() ;

}
