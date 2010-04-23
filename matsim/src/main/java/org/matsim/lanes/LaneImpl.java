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

package org.matsim.lanes;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;

/**
 * @author dgrether
 */
public class LaneImpl implements Lane {

	private Id id;
	/**
	 * the default according to the xml schema, never change the value if schema is not changed
	 */
	private double numberOfRepresentedLanes = 1;
	/**
	 * the default according to the xml schema, never change the value if schema is not changed
	 */
	private double startsAtMeterFromLinkEnd = 45.0;
	private List<Id> toLinkIds;
  private List<Id> toLaneIds;
  private int alignment = 0;
	/**
	 * @param id
	 */
	public LaneImpl(Id id) {
		this.id = id;
	}

	/**
	 * @param number
	 */
	public void setNumberOfRepresentedLanes(double number) {
		this.numberOfRepresentedLanes = number;
	}

	public void setStartsAtMeterFromLinkEnd(double meter) {
		this.startsAtMeterFromLinkEnd = meter;
	}

	public Id getId() {
		return id;
	}

	
	public double getNumberOfRepresentedLanes() {
		return numberOfRepresentedLanes;
	}

	
	public double getStartsAtMeterFromLinkEnd() {
		return startsAtMeterFromLinkEnd;
	}

	public void addToLinkId(Id id) {
		if (this.toLinkIds == null) {
			this.toLinkIds = new ArrayList<Id>();
		}
		this.toLinkIds.add(id);
	}
	
	public List<Id> getToLinkIds() {
		return this.toLinkIds;
	}
	
	public void addToLaneId(Id id) {
		if (this.toLaneIds == null) {
			this.toLaneIds = new ArrayList<Id>();
		}
		this.toLaneIds.add(id);
	}
	
	public List<Id> getToLaneIds() {
		return this.toLaneIds;
	}
	
	public int getAlignment() {
		return alignment;
	}
	
	public void setAlignment(int alignment) {
		this.alignment = alignment;
	}
	
}
