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
package org.matsim.contrib.signals.data.signalsystems.v20;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.signals.data.signalsystems.v20.SignalData;
import org.matsim.signals.model.Signal;


/**
 * @author dgrether
 *
 */
public class SignalDataImpl implements SignalData {

	private Id<Signal> id;
	
	private Id<Link> linkId;
	
	private Set<Id<Lane>> laneIds = null;
	
	private Set<Id<Link>> turningMoveRestrictions = null;

	SignalDataImpl(Id<Signal> id) {
		this.id = id;
	}

	@Override
	public void addLaneId(Id<Lane> laneId) {
		if (this.laneIds == null){
			this.laneIds = new HashSet<>();
		}
		this.laneIds.add(laneId);
	}

	@Override
	public void addTurningMoveRestriction(Id<Link> linkId) {
		if (this.turningMoveRestrictions == null){
			this.turningMoveRestrictions = new HashSet<>();
		}
		this.turningMoveRestrictions.add(linkId);
	}

	@Override
	public Set<Id<Lane>> getLaneIds() {
		return this.laneIds;
	}

	@Override
	public Id<Link> getLinkId() {
		return this.linkId;
	}

	@Override
	public Set<Id<Link>> getTurningMoveRestrictions() {
		return this.turningMoveRestrictions;
	}

	@Override
	public void setLinkId(Id<Link> id) {
		this.linkId = id;
	}

	@Override
	public Id<Signal> getId() {
		return this.id;
	}

}
