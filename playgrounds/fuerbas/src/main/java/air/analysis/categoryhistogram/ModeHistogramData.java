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
package air.analysis.modehistogram;

import java.util.SortedMap;
import java.util.TreeMap;

public class ModeHistogramData {
	final SortedMap<Integer, Integer> countsDep;
	final SortedMap<Integer, Integer> countsArr;
	public final SortedMap<Integer, Integer> countsStuck;

	public ModeHistogramData() {
		this.countsDep = new TreeMap<Integer, Integer>();
		this.countsArr = new TreeMap<Integer, Integer>();
		this.countsStuck = new TreeMap<Integer, Integer>();
	}
}