/* *********************************************************************** *
 * project: org.matsim.*
 * TransitSchedule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.pt.transitSchedule;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.MinimalTransferTimes;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;


/**
 * Default implementation of {@link TransitSchedule}.
 *
 * {@inheritDoc}
 *
 * @author mrieser
 */
public class TransitScheduleImpl implements TransitSchedule {

	private final Map<Id<TransitLine>, TransitLine> transitLines = new TreeMap<>();
	private final Map<Id<TransitStopFacility>, TransitStopFacility> stopFacilities = new TreeMap<>();
	private final TransitScheduleFactory factory;
	private final Attributes attributes = new AttributesImpl();
	private final MinimalTransferTimes minimalTransferTimes = new MinimalTransferTimesImpl();

	protected TransitScheduleImpl(final TransitScheduleFactory builder) {
		this.factory = builder;
	}

	@Override
	public void addTransitLine(final TransitLine line) {
		final Id<TransitLine> id = line.getId();
		if (this.transitLines.containsKey(id)) {
			throw new IllegalArgumentException("There is already a transit line with id " + id.toString());
		}
		this.transitLines.put(id, line);
	}

	@Override
	public boolean removeTransitLine(TransitLine line) {
		TransitLine oldLine = this.transitLines.remove(line.getId());
		if (oldLine == null) {
			return false;
		}
		if (oldLine != line) {
			this.transitLines.put(oldLine.getId(), oldLine);
			return false;
		}
		return true;
	}

	@Override
	public void addStopFacility(final TransitStopFacility stop) {
		final Id<TransitStopFacility> id = stop.getId();
		if (this.stopFacilities.containsKey(id)) {
			throw new IllegalArgumentException("There is already a stop facility with id " + id.toString());
		}
		this.stopFacilities.put(id, stop);
	}

	@Override
	public Map<Id<TransitLine>, TransitLine> getTransitLines() {
		return Collections.unmodifiableMap(this.transitLines);
	}

	@Override
	public Map<Id<TransitStopFacility>, TransitStopFacility> getFacilities() {
		return Collections.unmodifiableMap(this.stopFacilities);
	}

	@Override
	public boolean removeStopFacility(TransitStopFacility stop) {
		TransitStopFacility oldStop = this.stopFacilities.remove(stop.getId());
		if (oldStop == null) {
			return false;
		}
		if (oldStop != stop) {
			this.stopFacilities.put(oldStop.getId(), oldStop);
			return false;
		}
		return true;
	}

	@Override
	public TransitScheduleFactory getFactory() {
		return this.factory;
	}

	@Override
	public Attributes getAttributes() {
		return this.attributes;
	}

	@Override
	public MinimalTransferTimes getMinimalTransferTimes() {
		return this.minimalTransferTimes;
	}
}
