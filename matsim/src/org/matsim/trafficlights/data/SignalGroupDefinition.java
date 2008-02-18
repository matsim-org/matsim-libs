/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.trafficlights.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.utils.identifiers.IdI;


/**
 * This data class holds all information needed for the definition of
 * a traffic light's signal group.
 * @author dgrether
 *
 */
public class SignalGroupDefinition {

	private IdI id;
	private IdI linkId;
	private Map<IdI, SignalLane> fromLanesIdMap;
	private Map<IdI, SignalLane> toLanesIdLanesMap;
	private int passingClearingTime;
	private boolean turnIfRed;


	public SignalGroupDefinition(IdI id) {
		this.id = id;
		this.toLanesIdLanesMap = new HashMap<IdI, SignalLane>();
		this.fromLanesIdMap = new HashMap<IdI, SignalLane>();
	}

	public void setLink(IdI id) {
		this.linkId = id;
	}

	public void addFromLane(SignalLane lane) {
		this.fromLanesIdMap.put(lane.getId(), lane);
	}

	public void addToLane(SignalLane lane) {
		this.toLanesIdLanesMap.put(lane.getId(), lane);
	}

	public void setPassingClearingTime(int timeSec) {
		this.passingClearingTime = timeSec;
	}

	public void setTurnIfRed(boolean turnIfRed) {
		this.turnIfRed = turnIfRed;
	}


	/**
	 * @return the id
	 */
	public IdI getId() {
		return this.id;
	}


	/**
	 * @return the fromLinkId
	 */
	public IdI getLinkId() {
		return this.linkId;
	}

	public Collection<SignalLane> getFromLanes() {
		return this.fromLanesIdMap.values();
	}

	public Collection<SignalLane> getToLanes() {
		return this.toLanesIdLanesMap.values();
	}

	public  SignalLane getFromSignalLane(IdI id) {
		return this.fromLanesIdMap.get(id);
	}

	public SignalLane getToSignalLane(IdI id) {
		return this.toLanesIdLanesMap.get(id);
	}

	/**
	 * @return the time needed for all vehicles to pass the signal and clear the node. this
	 * is needed to determine the interimtime between to phases.
	 */
	public int getPassingClearingTime() {
		return this.passingClearingTime;
	}


	/**
	 * @return the turnIfRed
	 */
	public boolean isTurnIfRed() {
		return this.turnIfRed;
	}



}
