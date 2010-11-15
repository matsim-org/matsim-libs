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
package org.matsim.signalsystems.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.signalsystems.mobsim.SignalizeableItem;


/**
 * @author dgrether
 *
 */
public class SignalImpl implements Signal {

	private List<SignalizeableItem> signalizeableItems = new ArrayList<SignalizeableItem>();
	
	private Id linkId;
	
	private Set<Id> laneIds = null;

	private Id id;
	
	public SignalImpl(Id id, Id linkId){
		this.linkId = linkId;
		this.id = id;
	}
	
	@Override
	public void addSignalizeableItem(SignalizeableItem signalizedItem) {
		this.signalizeableItems.add(signalizedItem);
	}

	@Override
	public Id getId() {
		return this.id;
	}

	@Override
	public Set<Id> getLaneIds() {
		if (this.laneIds  == null) {
			this.laneIds = new HashSet<Id>();
		}
		return this.laneIds;
	}

	@Override
	public Id getLinkId() {
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
