/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayDuringLegReplanner.java
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

package playground.christoph.withinday.replanning;

import org.matsim.api.core.v01.Id;

/*
 * Replans only Agents that are currently performing
 * a Leg.
 */
public abstract class WithinDayDuringLegReplanner extends WithinDayReplanner {

	public WithinDayDuringLegReplanner(Id id)
	{
		super(id);
	}
	
	public abstract WithinDayDuringLegReplanner clone();
}
