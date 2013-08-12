/* *********************************************************************** *
 * project: org.matsim.*
 * HistogramData
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis.categoryhistogram;

import java.util.SortedMap;
import java.util.TreeMap;
/**
 * @author dgrether
 */
final class CategoryHistogramData {
	final SortedMap<Integer, Integer> departuresByBin;
	final SortedMap<Integer, Integer> arrivalsByBin;
	final SortedMap<Integer, Integer> abortByBin;

	CategoryHistogramData() {
		this.departuresByBin = new TreeMap<Integer, Integer>();
		this.arrivalsByBin = new TreeMap<Integer, Integer>();
		this.abortByBin = new TreeMap<Integer, Integer>();
	}
}