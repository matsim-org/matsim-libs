/* *********************************************************************** *
 * project: org.matsim.*
 * DatabasedSignal
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
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.signalsystems.control.SignalGroupState;
import org.matsim.signalsystems.data.signalsystems.v20.SignalData;


/**
 * @author dgrether
 *
 */
public class DatabasedSignal implements Signal {

	private SignalData data;
	private List<SignalizeableItem> signalizedItems = new ArrayList<SignalizeableItem>();

	public DatabasedSignal(SignalData signalData) {
		this.data = signalData;
	}

	@Override
	public Id getLinkId() {
		return this.data.getLinkId();
	}

	@Override
	public void setState(SignalGroupState state) {
		if (this.data.getTurningMoveRestrictions() == null || this.data.getTurningMoveRestrictions().isEmpty()){
			for (SignalizeableItem item : this.signalizedItems){
				item.setSignalStateAllTurningMoves(state);
			}
		}
		else {
			for (SignalizeableItem item : this.signalizedItems){
				for (Id toLinkId : this.data.getTurningMoveRestrictions()){
					item.setSignalStateForTurningMove(state, toLinkId);
				}
			}
		}
	}

	
	@Override
	public void addSignalizedItem(SignalizeableItem signalizedItem) {
		this.signalizedItems.add(signalizedItem);
	}

	@Override
	public Set<Id> getLaneIds() {
		return this.data.getLaneIds();
	}

	@Override
	public Id getId() {
		return this.data.getId();
	}


}
