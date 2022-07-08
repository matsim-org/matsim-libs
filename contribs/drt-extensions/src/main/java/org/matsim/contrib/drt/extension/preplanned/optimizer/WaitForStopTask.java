/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.extension.preplanned.optimizer;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STAY;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;

/**
 * @author Michal Maciejewski (michalm)
 */
public class WaitForStopTask extends DefaultStayTask {

	public static final DrtTaskType TYPE = new DrtTaskType("WAIT_FOR_STOP", STAY);

	public WaitForStopTask(double beginTime, double endTime, Link link) {
		super(TYPE, beginTime, endTime, link);
	}
}
