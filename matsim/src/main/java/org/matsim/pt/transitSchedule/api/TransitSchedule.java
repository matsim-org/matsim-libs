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

package org.matsim.pt.transitSchedule.api;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimToplevelContainer;
import org.matsim.utils.objectattributes.FailingObjectAttributes;
import org.matsim.utils.objectattributes.attributable.Attributable;

/**
 * Stores a complete transit schedules with multiple lines, multiple routes per line, all the time data
 * and the infrastructure in form of stop facilities.
 *
 * @author mrieser
 */
public interface TransitSchedule extends MatsimToplevelContainer, Attributable {

	void addTransitLine(final TransitLine line);

	/**
	 * @param line the transit line to be removed
	 * @return <code>true</code> if the transit line was successfully removed from the transit schedule.
	 */
	boolean removeTransitLine(final TransitLine line);

	void addStopFacility(final TransitStopFacility stop);

	Map<Id<TransitLine>, TransitLine> getTransitLines();

	Map<Id<TransitStopFacility>, TransitStopFacility> getFacilities();

	/**
	 * @param stop the stop facility to be removed
	 * @return <code>true</code> if the transit stop facility was successfully removed from the transit schedule.
	 */
	boolean removeStopFacility(final TransitStopFacility stop);

	@Override
	TransitScheduleFactory getFactory();

	@Deprecated
	FailingObjectAttributes getTransitLinesAttributes();

	@Deprecated
	FailingObjectAttributes getTransitStopsAttributes();

	MinimalTransferTimes getMinimalTransferTimes();
}
