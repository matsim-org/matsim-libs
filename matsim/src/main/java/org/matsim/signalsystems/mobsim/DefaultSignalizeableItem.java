/* *********************************************************************** *
 * project: org.matsim.*
 * QSignalizedItem
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
package org.matsim.signalsystems.mobsim;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.signalsystems.model.SignalGroupState;


/**
 * @author dgrether
 *
 */
public final class DefaultSignalizeableItem implements SignalizeableItem {

	private Map<Id, SignalGroupState> toLinkIdSignalStates = null;
	private SignalGroupState allToLinksState = SignalGroupState.GREEN;
	private boolean linkGreen = true;
	private Set<Id> outLinks;
	
	public DefaultSignalizeableItem(Set<Id> outLinks){
		this.outLinks = outLinks;
	}

	@Override
	public void setSignalStateAllTurningMoves(SignalGroupState state) {
		this.allToLinksState = state;
		this.linkGreen = this.checkGreen(state);
	}

	private void initToLinkIdSignalStates(){
		this.allToLinksState = null;
		this.toLinkIdSignalStates = new HashMap<Id, SignalGroupState>();
		for (Id outLinkId : this.outLinks){
			this.toLinkIdSignalStates.put(outLinkId, SignalGroupState.GREEN);
		}
	}
	
	@Override
	public void setSignalStateForTurningMove(SignalGroupState state, Id toLinkId) {
		if (this.toLinkIdSignalStates == null){
			this.initToLinkIdSignalStates();
		}
		this.toLinkIdSignalStates.put(toLinkId, state);
		if (this.checkGreen(state)){
			this.linkGreen = true;
		}
		else {
			boolean foundGreen = false;
			for (SignalGroupState sgs : this.toLinkIdSignalStates.values()){
				if (checkGreen(sgs)){
					foundGreen = true;
				}
			}
			this.linkGreen = foundGreen;
		}
	}

	private boolean checkGreen(SignalGroupState state) {
		return (state.equals(SignalGroupState.GREEN) || state.equals(SignalGroupState.REDYELLOW));
	}

	public boolean isLinkGreen() {
		return linkGreen;
	}
	
	public boolean isLinkGreenForToLink(Id toLinkId){
		if (this.allToLinksState != null) {
			return this.checkGreen(this.allToLinksState);
		}
		return this.checkGreen(this.toLinkIdSignalStates.get(toLinkId));
	}
	
	@Override
	public void setSignalized(boolean isSignalized) {
		//nothing to do
	}

}
