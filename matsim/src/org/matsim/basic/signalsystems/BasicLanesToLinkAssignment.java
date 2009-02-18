/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.basic.signalsystems;

import java.util.ArrayList;
import java.util.List;

import org.matsim.interfaces.basic.v01.Id;

/**
 * @author dgrether
 */
public class BasicLanesToLinkAssignment {

	private Id linkId;

	private List<BasicLane> lanes;
	
	/**
	 * @param linkId
	 */
	public BasicLanesToLinkAssignment(Id linkId) {
		this.linkId = linkId;
	}

	public List<BasicLane> getLanes() {
		return this.lanes;
	}
	
	/**
	 * @param lane
	 */
	public void addLane(BasicLane lane) {
		if (this.lanes == null) {
			this.lanes = new ArrayList<BasicLane>();
		}
		this.lanes.add(lane); 
	}
	
	public Id getLinkId() {
		return linkId;
	}

}
