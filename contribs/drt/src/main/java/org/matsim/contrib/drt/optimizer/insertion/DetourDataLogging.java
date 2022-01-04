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

import static org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;

import java.util.Objects;
import java.util.function.Function;

import org.apache.log4j.Logger;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData.InsertionDetourData;

/**
 * @author Michal Maciejewski (michalm)
 */
public final class DetourDataLogging {
	private static final Logger log = Logger.getLogger(DetourDataLogging.class);

	public static void printoutEstimatedTimes(InsertionDetourData<Double> data) {
		printoutDetour(data, Objects::toString);
	}

	public static void printoutPathTimes(InsertionDetourData<PathData> data) {
		printoutDetour(data, pd -> String.valueOf(pd == null ? null : pd.getTravelTime()));
	}

	public static <D> void printoutDetour(InsertionDetourData<D> data, Function<D, String> toString) {
		log.info("toPickup=" + toString.apply(data.detourToPickup));
		log.info("fromPickup=" + toString.apply(data.detourFromPickup));
		log.info("toDropoff=" + toString.apply(data.detourToDropoff));
		log.info("fromDropoff=" + toString.apply(data.detourFromDropoff));
	}
}
