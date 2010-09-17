/* *********************************************************************** *
 * project: org.matsim.*
 * SignalDataImpl
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
package org.matsim.signalsystems.data.signalsystems.v20;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;


/**
 * @author dgrether
 *
 */
public class SignalDataImpl implements SignalData {

	private Id id;
	
	private Id linkId;
	
	private Set<Id> laneIds = null;
	
	private Set<Id> turningMoveRestrictions = null;

	public SignalDataImpl(Id id) {
		this.id = id;
	}

	@Override
	public void addLaneId(Id laneId) {
		if (this.laneIds == null){
			this.laneIds = new HashSet<Id>();
		}
		this.laneIds.add(laneId);
	}

	@Override
	public void addTurningMoveRestriction(Id linkId) {
		if (this.turningMoveRestrictions == null){
			this.turningMoveRestrictions = new HashSet<Id>();
		}
		this.turningMoveRestrictions.add(linkId);
	}

	@Override
	public Set<Id> getLaneIds() {
		return this.laneIds;
	}

	@Override
	public Id getLinkId() {
		return this.linkId;
	}

	@Override
	public Set<Id> getTurningMoveRestrictions() {
		return this.turningMoveRestrictions;
	}

	@Override
	public void setLinkId(Id id) {
		this.linkId = id;
	}

	@Override
	public Id getId() {
		return this.id;
	}

}
