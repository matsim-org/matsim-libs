/* *********************************************************************** *
 * project: org.matsim.*
 * Act.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.core.population;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;

public class ActivityImpl implements Activity {

	private double endTime = Time.UNDEFINED_TIME;

	/** @deprecated I don't think this is used/interpreted anywhere. Kai, jun09 */
	@Deprecated
	private double startTime = Time.UNDEFINED_TIME;

	private double dur = Time.UNDEFINED_TIME;

	private String type;
	private Coord coord = null;
	protected Id linkId = null;
	protected Id facilityId = null;

	/*package*/ ActivityImpl(final String type) {
		this.type = type.intern();
	}

	public ActivityImpl(final String type, final Id linkId) {
		this(type);
		this.setLinkId(linkId);
	}

	public ActivityImpl(final String type, final Coord coord) {
		this(type);
		this.setCoord(coord);
	}

	public ActivityImpl(final String type, final Coord coord, final Id linkId) {
		this(type, linkId);
		this.setCoord(coord);
	}

	public ActivityImpl(final ActivityImpl act) {
		this(act.getType());
		// Act coord could be null according to first c'tor!
		Coord c = act.getCoord() == null ? null : new CoordImpl(act.getCoord());
		this.setCoord(c);
		this.linkId = act.getLinkId();
		this.setStartTime(act.getStartTime());
		this.setEndTime(act.getEndTime());
		this.setMaximumDuration(act.getMaximumDuration());
		this.setFacilityId(act.getFacilityId());
	}

	@Override
	public final double getEndTime() {
		return this.endTime;
	}

	@Override
	public final void setEndTime(final double endTime) {
		this.endTime = endTime;
	}

	/** @deprecated I don't think this is used/interpreted anywhere. Kai, jun09 */
	@Deprecated
	@Override
	public final double getStartTime() {
		return this.startTime;
	}

	/** @deprecated I don't think this is used/interpreted anywhere. Kai, jun09 */
	@Deprecated
	@Override
	public final void setStartTime(final double startTime) {
		this.startTime = startTime;
	}

	@Override
	public final String getType() {
		return this.type;
	}

	@Override
	public final void setType(final String type) {
		this.type = type.intern();
	}

	@Override
	public final Coord getCoord() {
		return this.coord;
	}

	public void setCoord(final Coord coord) {
		this.coord = coord;
	}

	@Override
	public final Id getLinkId() {
		return this.linkId;
	}

	@Override
	public final Id getFacilityId() {
		return this.facilityId;
	}

	public final void setFacilityId(final Id facilityId) {
		this.facilityId = facilityId;
	}

	public final void setLinkId(final Id linkId) {
		this.linkId = linkId;
	}

	@Override
	public final String toString() {
		return "[type=" + this.getType() + "]" +
				"[coord=" + this.getCoord() + "]" +
				"[linkId=" + this.linkId + "]" +
				"[startTime=" + Time.writeTime(this.getStartTime()) + "]" +
				"[endTime=" + Time.writeTime(this.getEndTime()) + "]";
	}

	public double getMaximumDuration() {
		return this.dur;
	}

	public void setMaximumDuration(final double dur) {
		this.dur = dur;
	}

}
