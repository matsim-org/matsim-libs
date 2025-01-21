/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.controller;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.ActivityFacility;
import org.matsim.freight.carriers.TimeWindow;
import org.matsim.utils.objectattributes.attributable.Attributes;

public class FreightActivity implements Activity {

	private final Activity act;

	private final TimeWindow timeWindow;

	public FreightActivity(Activity act, TimeWindow timeWindow) {
		super();
		this.act = act;
		this.timeWindow = timeWindow;
	}

	public TimeWindow getTimeWindow(){
		return timeWindow;
	}

	@Override
	public OptionalTime getEndTime() {
		return act.getEndTime();
	}

	@Override
	public void setEndTime(double seconds) {
		act.setEndTime(seconds);
	}

	@Override
	public void setEndTimeUndefined() {
		act.setEndTimeUndefined();
	}

	@Override
	public String getType() {
		return act.getType();
	}

	@Override
	public void setType(String type) {
		act.setType(type);
	}

	@Override
	public Coord getCoord() {
		return act.getCoord();
	}

	@Override
	public OptionalTime getStartTime() {
		return act.getStartTime();
	}

	@Override
	public void setStartTime(double seconds) {
		act.setStartTime(seconds);
	}

	@Override
	public void setStartTimeUndefined() {
		act.setStartTimeUndefined();
	}

	@Override
	public OptionalTime getMaximumDuration() {
		return act.getMaximumDuration();
	}

	@Override
	public void setMaximumDuration(double seconds) {
		act.setMaximumDuration(seconds);
	}

	@Override
	public void setMaximumDurationUndefined() {
		act.setMaximumDurationUndefined();
	}

	@Override
	public Id<Link> getLinkId() {
		return act.getLinkId();
	}

	@Override
	public Id<ActivityFacility> getFacilityId() {
		return act.getFacilityId();
	}

	@Override
	public void setLinkId(Id<Link> id) {
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public void setFacilityId(Id<ActivityFacility> id) {
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public void setCoord(Coord coord) {
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Attributes getAttributes() {
		throw new UnsupportedOperationException();
	}
}
