/* *********************************************************************** *
 * project: org.matsim.*
 * Signal
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
package org.matsim.contrib.signals.model;

import java.util.Collection;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.core.mobsim.qsim.interfaces.SignalizeableItem;


/**
 * @author dgrether
 *
 */
public interface Signal {

	public Id<Signal> getId();
	
	public Id<Link> getLinkId();
	
	public Set<Id<Lane>> getLaneIds();

	public void addSignalizeableItem(SignalizeableItem signalizedItem);

	public Collection<SignalizeableItem> getSignalizeableItems();
	
	public void setState(SignalGroupState state);
	
	
}
