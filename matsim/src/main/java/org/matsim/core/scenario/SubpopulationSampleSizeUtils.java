
/* *********************************************************************** *
 * project: org.matsim.*
 * ProjectionUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.scenario;

import org.matsim.core.api.internal.MatsimToplevelContainer;
import org.matsim.utils.objectattributes.attributable.Attributable;

import java.util.Map;

public final class SubpopulationSampleSizeUtils {
	/**
	 * Name of the attribute to add to top-level containers to specify the projection the coordinates are in.
	 * When possible, the utility methods should be used instead of directly querying the attributes.
	 */
	public static final String SUBPOPULATION_SAMPLE_SIZES_ATT = "subpopulationSampleSizes";

	private SubpopulationSampleSizeUtils() {}

	public static <T extends MatsimToplevelContainer & Attributable> Map<String, Double> getSubpopulation2SampleSize(T container) {
		return (Map<String, Double>) container.getAttributes().getAttribute(SUBPOPULATION_SAMPLE_SIZES_ATT);
	}

	public static <T extends MatsimToplevelContainer & Attributable> void putSubpopulation2SampleSize(T container, Map<String, Double> subpopulation2SampleSize) {
		container.getAttributes().putAttribute(SUBPOPULATION_SAMPLE_SIZES_ATT, subpopulation2SampleSize);
	}
}
