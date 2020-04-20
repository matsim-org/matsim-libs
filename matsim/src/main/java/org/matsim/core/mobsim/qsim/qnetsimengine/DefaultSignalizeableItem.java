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
package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.core.mobsim.qsim.interfaces.SignalizeableItem;


/**
 * @author dgrether
 *
 */
public final class DefaultSignalizeableItem implements SignalizeableItem {

	private Map<Id<Link>, SignalGroupState> toLinkIdSignalStates = null;
	private SignalGroupState allToLinksState = SignalGroupState.GREEN;
	private boolean linkGreen = true;
	private Set<Id<Link>> outLinks;
	
	public DefaultSignalizeableItem(Set<Id<Link>> outLinks){
		this.outLinks = outLinks;
	}

	@Override
	public void setSignalStateAllTurningMoves(SignalGroupState state) {
		this.allToLinksState = state;
		this.linkGreen = checkGreen(state);
	}

	private void initToLinkIdSignalStates(){
		this.allToLinksState = null;
		this.toLinkIdSignalStates = new HashMap<>();
		for (Id<Link> outLinkId : this.outLinks){
			this.toLinkIdSignalStates.put(outLinkId, SignalGroupState.GREEN);
		}
	}
	
	@Override
	public void setSignalStateForTurningMove(SignalGroupState state, Id<Link> toLinkId) {
		if (this.toLinkIdSignalStates == null){
			this.initToLinkIdSignalStates();
		}
		this.toLinkIdSignalStates.put(toLinkId, state);
		if (checkGreen(state)){
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

	private static boolean checkGreen(SignalGroupState state) {
		return (state.equals(SignalGroupState.GREEN) || state.equals(SignalGroupState.YELLOW) || state.equals(SignalGroupState.OFF));
	}

	/**
	 * returns true, if at least on signal at the link/lane shows green
	 */
	public boolean hasGreenForAllToLinks() {
		return linkGreen;
	}
	
	public boolean hasGreenForToLink(Id<Link> toLinkId){
		if (this.allToLinksState != null) {
			return checkGreen(this.allToLinksState);
		}
		return checkGreen(this.toLinkIdSignalStates.get(toLinkId));
	}
	
	@Override
	public void setSignalized(boolean isSignalized) {
		//nothing to do
	}

}
