/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2024 by the members listed in the COPYING,        *
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
package org.matsim.contrib.drt.extension.services.services.tracker;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.services.events.DrtServiceEndedEventHandler;
import org.matsim.contrib.drt.extension.services.events.DrtServiceScheduledEventHandler;
import org.matsim.contrib.drt.extension.services.events.DrtServiceStartedEventHandler;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.core.controler.listener.IterationEndsListener;

import java.util.Map;

/**
 * @author steffenaxer
 */
public interface ServiceExecutionTrackers extends IterationEndsListener, DrtServiceStartedEventHandler, DrtServiceEndedEventHandler, DrtServiceScheduledEventHandler {
	Map<Id<DvrpVehicle>, ServiceExecutionTracker> getTrackers();
}
