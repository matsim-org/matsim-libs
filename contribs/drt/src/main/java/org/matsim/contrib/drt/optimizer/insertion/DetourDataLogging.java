/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.insertion;

import java.util.Objects;

import org.apache.log4j.Logger;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData.InsertionDetourData;

/**
 * @author Michal Maciejewski (michalm)
 */
public final class DetourDataLogging {
	private static final Logger log = Logger.getLogger(DetourDataLogging.class);

	public static void printoutDetour(InsertionDetourData data) {
		log.info("toPickup=" + Objects.toString(data.detourToPickup.getTravelTime()));
		log.info("fromPickup=" + Objects.toString(data.detourFromPickup.getTravelTime()));
		log.info("toDropoff=" + Objects.toString(data.detourToDropoff.getTravelTime()));
		log.info("fromDropoff=" + Objects.toString(data.detourFromDropoff.getTravelTime()));
	}
}
