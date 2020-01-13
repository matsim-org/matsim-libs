/*
 * *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.run;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Stream;

/**
 * @author Michal Maciejewski (michalm)
 */
public class MultiModals {
	public static void requireAllModesUnique(MultiModal<?> multiModal) {
		if (!isAllModesUnique(multiModal.modes())) {
			throw new RuntimeException("There are non-unique modes in: " + multiModal);
		}
	}

	public static void requireAllModesUnique(String... modes) {
		if (!isAllModesUnique(Arrays.stream(modes))) {
			throw new RuntimeException("There are non-unique modes in: " + modes);
		}
	}

	public static boolean isAllModesUnique(Stream<String> modes) {
		return modes.allMatch(new HashSet<>()::add);
	}

	public static boolean isAllDvrpModesUnique(Stream<DvrpMode> dvrpModes) {
		return dvrpModes.allMatch(new HashSet<>()::add);
	}
}
