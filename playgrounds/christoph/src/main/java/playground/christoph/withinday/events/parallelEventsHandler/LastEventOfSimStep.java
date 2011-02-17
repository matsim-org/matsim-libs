/* *********************************************************************** *
 * project: org.matsim.*
 * LastEventOfSimStep.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.withinday.events.parallelEventsHandler;

import org.matsim.core.events.EventImpl;

/**
 * Special event needed for synchronizing the threads at the end of each simstep.
 *
 * @author christoph dobler
 */
public class LastEventOfSimStep extends EventImpl {

	public static final String EVENT_TYPE = "simstepend";
	
	public LastEventOfSimStep(final double time) {
		super(time);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

}
