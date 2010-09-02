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
import org.matsim.ptproject.qsim.helpers.AgentSnapshotInfoBuilder;
import org.matsim.ptproject.qsim.netsimengine.QLinkInternalI;


/**
 * Coordinates the movement of vehicles on the links and the nodes.
 *
 * @author cdobler
 * @author dgrether
 */

public interface QSimEngine extends SimEngine, Steppable {
	// can't make this an abstract class since QSimEngineThread already extends from Thread
	// (although one could solve that without inheritance).  kai, aug'10

	
	// yyyy I find that this exposes too much interior information to the interface.
	// I also don't find it logical to have it in a class that is meant to be replaceable for,
	// say, parallel execution.  On the other hand, it makes sense for a QNetworkEngine.  kai, jun'10
	public AgentSnapshotInfoBuilder getAgentSnapshotInfoBuilder();
	
	public void activateLink(final QLinkInternalI link) ;
	// seems to me that it should be possible to put this (both of these) into an internal interface.  kai, aug'10
	
}
