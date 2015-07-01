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

package org.matsim.lanes.data.v11;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.data.v20.Lane;

/**
 * @author dgrether
 */
public class LaneData11Impl implements LaneData11 {

	private Id<Lane> id;
	/**
	 * the default according to the xml schema, never change the value if schema is not changed
	 */
	private double numberOfRepresentedLanes = 1;
	/**
	 * the default according to the xml schema, never change the value if schema is not changed
	 */
	private double startsAtMeterFromLinkEnd = 45.0;
	
	private List<Id<Link>> toLinkIds = null;

	public LaneData11Impl(Id<Lane> id) {
		this.id = id;
	}

	@Override
	public void setNumberOfRepresentedLanes(double number) {
		this.numberOfRepresentedLanes = number;
	}

	@Override
	public void setStartsAtMeterFromLinkEnd(double meter) {
		this.startsAtMeterFromLinkEnd = meter;
	}

	@Override
	public Id<Lane> getId() {
		return id;
	}

	
	@Override
	public double getNumberOfRepresentedLanes() {
		return numberOfRepresentedLanes;
	}

	
	@Override
	public double getStartsAtMeterFromLinkEnd() {
		return startsAtMeterFromLinkEnd;
	}

	@Override
	public void addToLinkId(Id<Link> id) {
		if (this.toLinkIds == null) {
			this.toLinkIds = new ArrayList<>();
		}
		this.toLinkIds.add(id);
	}
	
	@Override
	public List<Id<Link>> getToLinkIds() {
		return this.toLinkIds;
	}
}
