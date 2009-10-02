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

package org.matsim.transitSchedule.api;

import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.internal.MatsimToplevelContainer;

/**
 * Stores a complete transit schedules with multiple lines, multiple routes per line, all the time data
 * and the infrastructure in form of stop facilities.
 * 
 * @author mrieser
 */
public interface TransitSchedule extends MatsimToplevelContainer {

	public abstract void addTransitLine(final TransitLine line);

	public abstract void addStopFacility(final TransitStopFacility stop);

	public abstract Map<Id, TransitLine> getTransitLines();

	public abstract Map<Id, TransitStopFacility> getFacilities();
	
	public abstract TransitScheduleFactory getFactory();

}