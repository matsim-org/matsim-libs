/* *********************************************************************** *
 * project: org.matsim.*
 * SignalImpl
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
public class SignalImpl implements Signal {

	private List<SignalizeableItem> signalizeableItems = new ArrayList<SignalizeableItem>();
	
	private Id<Link> linkId;
	
	private Set<Id<Lane>> laneIds = null;

	private Id<Signal> id;
	
	public SignalImpl(Id<Signal> id, Id<Link> linkId){
		this.linkId = linkId;
		this.id = id;
	}
	
	@Override
	public void addSignalizeableItem(SignalizeableItem signalizedItem) {
		this.signalizeableItems.add(signalizedItem);
	}

	@Override
	public Id<Signal> getId() {
		return this.id;
	}

	@Override
	public Set<Id<Lane>> getLaneIds() {
		if (this.laneIds  == null) {
			this.laneIds = new HashSet<>();
		}
		return this.laneIds;
	}

	@Override
	public Id<Link> getLinkId() {
		return this.linkId;
	}

	@Override
	public void setState(SignalGroupState state) {
		for (SignalizeableItem item : this.signalizeableItems){
			item.setSignalStateAllTurningMoves(state);
		}
	}

	@Override
	public Collection<SignalizeableItem> getSignalizeableItems() {
		return this.signalizeableItems;
	}

}
