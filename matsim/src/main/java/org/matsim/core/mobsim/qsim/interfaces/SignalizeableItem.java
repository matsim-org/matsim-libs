/* *********************************************************************** *
 * project: org.matsim.*
 * SignalizedItem
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
package org.matsim.core.mobsim.qsim.interfaces;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;


/**
 * A simulation element that leads to something with an Id. In usual traffic/network applications
 * this would be a link or lane that is leading to one to several links.
 * 
 * The SignalizeableItem must be notified by its property to be signalized by calling the setSignalized(..) method.
 * 
 * @author dgrether
 *
 */
public interface SignalizeableItem {
	
	void setSignalized(final boolean isSignalized);
	
	void setSignalStateAllTurningMoves(final SignalGroupState state);
	
	void setSignalStateForTurningMove(final SignalGroupState state, final Id<Link> toLinkId);
	
	boolean hasGreenForAllToLinks();
	
	boolean hasGreenForToLink(final Id<Link> toLinkId);
}
