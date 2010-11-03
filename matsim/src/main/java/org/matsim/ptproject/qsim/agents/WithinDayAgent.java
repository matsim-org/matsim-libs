/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayAgent.java
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

package org.matsim.ptproject.qsim.agents;

import org.matsim.core.mobsim.framework.PersonAgent;

/*
 * Interface for Agents with within-day functionality. Might get some
 * further methods like resetCaches(). cdobler, Nov'10
 */
public interface WithinDayAgent extends PersonAgent {

	public Integer getCurrentPlanElementIndex();

	public Integer getCurrentRouteLinkIdIndex();
}
