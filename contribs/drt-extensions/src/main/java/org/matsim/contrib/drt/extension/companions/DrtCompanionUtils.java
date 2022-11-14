/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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
 * *********************************************************************** */

package org.matsim.contrib.drt.extension.companions;

import java.util.List;

import org.matsim.contrib.util.random.RandomUtils;
import org.matsim.contrib.util.random.UniformRandom;
import org.matsim.contrib.util.random.WeightedRandomSelection;

/**
 *
 * @author Steffen Axer
 *
 */
public class DrtCompanionUtils {

	public static WeightedRandomSelection<Integer> createIntegerSampler(final List<Double> distribution) {
		WeightedRandomSelection<Integer> wrs = new WeightedRandomSelection<>(
				new UniformRandom(RandomUtils.getLocalGenerator()));
		for (int i = 0; i < distribution.size(); ++i) {
			wrs.add(i, distribution.get(i));
		}
		return wrs;
	}

}
