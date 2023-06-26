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

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.common.util.WeightedRandomSelection;
import org.matsim.core.gbl.MatsimRandom;

/**
 *
 * @author Steffen Axer
 *
 */
public class DrtCompanionUtils {

	public final static String ADDITIONAL_GROUP_SIZE_ATTRIBUTE = "additionalGroupSize";
	public final static String ADDITIONAL_GROUP_PART_ATTRIBUTE = "additionalGroupPart";
	public static final String COMPANION_TYPE_ATTRIBUTE = "companionType";

	private DrtCompanionUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static boolean isDrtCompanion(Person person) {
		return getDRTCompanionType(person) != null;
	}

	public static void setDRTCompanionType(Person person, String drtCompanionType) {
		if (drtCompanionType == null) {
			person.getAttributes().removeAttribute(COMPANION_TYPE_ATTRIBUTE);
		} else {
			person.getAttributes().putAttribute(COMPANION_TYPE_ATTRIBUTE, drtCompanionType);
		}
	}

	public static String getDRTCompanionType(Person person) {
		return (String) person.getAttributes().getAttribute(COMPANION_TYPE_ATTRIBUTE);
	}

	public static WeightedRandomSelection<Integer> createIntegerSampler(final List<Double> distribution) {
		WeightedRandomSelection<Integer> wrs = new WeightedRandomSelection<>(MatsimRandom.getLocalInstance());
		for (int i = 0; i < distribution.size(); ++i) {
			wrs.add(i, distribution.get(i));
		}
		return wrs;
	}

	public static Integer getAdditionalGroupSize(Person person) {
		if (person.getAttributes().getAttribute(ADDITIONAL_GROUP_SIZE_ATTRIBUTE) == null) {
			return null;
		} else {
			return Integer.parseInt(person.getAttributes().getAttribute(ADDITIONAL_GROUP_SIZE_ATTRIBUTE).toString());
		}
	}

	public static Integer getAdditionalGroupPart(Person person) {
		if (person.getAttributes().getAttribute(ADDITIONAL_GROUP_PART_ATTRIBUTE) == null) {
			return null;
		} else {
			return Integer.parseInt(person.getAttributes().getAttribute(ADDITIONAL_GROUP_PART_ATTRIBUTE).toString());
		}
	}

	public static void setAdditionalGroupSize(Person person, int additionalGroupSize) {
		person.getAttributes().putAttribute(ADDITIONAL_GROUP_SIZE_ATTRIBUTE, additionalGroupSize);
	}

	public static void setAdditionalGroupPart(Person person, int additionalgroupPart) {
		person.getAttributes().putAttribute(ADDITIONAL_GROUP_PART_ATTRIBUTE, additionalgroupPart);
	}

}
