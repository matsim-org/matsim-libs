/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.schedule;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STAY;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;

/**
 * Task that identifies a time period in which a vehicle idles before a stop,
 * potentially with passengers on board.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class DrtWaitTask extends DefaultStayTask {
	public static final DrtTaskType TYPE = new DrtTaskType("WAIT", STAY);

	public DrtWaitTask(double beginTime, double endTime, Link link) {
		super(TYPE, beginTime, endTime, link);
	}
}
