/* *********************************************************************** *
 * project: org.matsim.*
 * LCActivity.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.locationchoice.population;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * @author cdobler
 */
public class LCActivity implements Activity, LCPlanElement {

	private final LCPlan plan;
	private final int arrayIndex;
	private final int planElementIndex;
	
	public LCActivity(final LCPlan plan, final int arrayIndex, final int planElementIndex) {
		this.plan = plan;
		this.arrayIndex = arrayIndex;
		this.planElementIndex = planElementIndex;
	}
	
	@Override
	public final double getEndTime() {
		return this.plan.endTimes[this.arrayIndex];
	}

	@Override
	public final void setEndTime(double seconds) {
		this.plan.endTimes[this.arrayIndex] = seconds;
	}

	@Override
	public final String getType() {
		return this.plan.types[this.arrayIndex];
	}

	@Override
	public final void setType(String type) {
		this.plan.types[this.arrayIndex] = type;
	}

	@Override
	public final Coord getCoord() {
		return this.plan.coords[this.arrayIndex];
	}

	public final void setCoord(Coord coord) {
		this.plan.coords[this.arrayIndex] = coord;
	}
	
	@Override
	public final double getStartTime() {
		return this.plan.startTimes[this.arrayIndex];
	}

	@Override
	public final void setStartTime(double seconds) {
		this.plan.startTimes[this.arrayIndex] = seconds;
	}

	@Override
	public final double getMaximumDuration() {
		return this.plan.durations[this.arrayIndex];
	}

	@Override
	public final void setMaximumDuration(double seconds) {
		this.plan.durations[this.arrayIndex] = seconds;
	}

	@Override
	public final Id<Link> getLinkId() {
		return this.plan.linkIds[this.arrayIndex];
	}

	public final void setLinkId(Id<Link> linkId) {
		this.plan.linkIds[this.arrayIndex] = linkId;
	}
	
	@Override
	public final Id<ActivityFacility> getFacilityId() {
		return this.plan.facilityIds[this.arrayIndex];
	}
	
	public final void setFacilityId(Id<ActivityFacility> facilityId) {
		this.plan.facilityIds[this.arrayIndex] = facilityId;
	}

	public final double getArrivalTime() {
		return this.plan.arrTimes[this.arrayIndex];
	}

	public final void setArrivalTime(final double arrTime) {
		this.plan.arrTimes[this.arrayIndex] = arrTime;
	}
	
	@Override
	public final int getArrayIndex() {
		return this.arrayIndex;
	}
	
	@Override
	public final int getPlanElementIndex() {
		return this.planElementIndex;
	}

	@Override
	public Attributes getAttributes() {
		throw new UnsupportedOperationException();
	}
}