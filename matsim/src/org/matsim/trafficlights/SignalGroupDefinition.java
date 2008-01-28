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
package org.matsim.trafficlights;

import java.util.LinkedList;
import java.util.List;

import org.matsim.utils.identifiers.IdI;


/**
 * This data class holds all information needed for the definition of
 * a traffic light's signal group.
 * @author dgrether
 *
 */
public class SignalGroupDefinition {

	private IdI id;
	private IdI fromLinkId;
	private List<IdI> toLinkIds;
	private int passingClearingTime;
	private boolean turnIfRed;


	public SignalGroupDefinition(IdI id) {
		this.id = id;
		this.toLinkIds = new LinkedList<IdI>();
	}

	public void setFromLink(IdI id) {
		this.fromLinkId = id;
	}

	public void addToLink(IdI id) {
		this.toLinkIds.add(id);
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
	public IdI getFromLinkId() {
		return this.fromLinkId;
	}


	/**
	 * @return the toLinkIds
	 */
	public List<IdI> getToLinkIds() {
		return this.toLinkIds;
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
