/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.util;

import java.util.function.Function;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.contrib.util.XYDataCollector.XYDataCalculator;

/**
 * @author michalm
 */
public class XYDataCollectors {
	public static <T extends BasicLocation> XYDataCalculator<T> createCalculator(String[] header,
			Function<T, Object[]> valueCalculator) {
		return createCalculator(header, BasicLocation::getCoord, valueCalculator);
	}

	public static <T extends BasicLocation> XYDataCalculator<T> createCalculator(String[] header,
			Function<T, Coord> coordGetter, Function<T, Object[]> valueCalculator) {
		return new XYDataCalculator<T>() {
			@Override
			public String[] getHeader() {
				return header;
			}

			@Override
			public Coord getCoord(T object) {
				return coordGetter.apply(object);
			}

			@Override
			public Object[] calculate(T object) {
				return valueCalculator.apply(object);
			}
		};
	}
}
