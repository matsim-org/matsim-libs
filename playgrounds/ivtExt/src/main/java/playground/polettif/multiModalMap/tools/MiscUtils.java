/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.multiModalMap.tools;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class MiscUtils {

	/**
	 * @return true, if two sets (e.g. scheduleTransportModes and
	 * networkTransportModes) have at least one identical entry.
	 */
	public static boolean setsShareMinOneEntry(Set<String> set1, Set<String> set2) {
		if(set1 == null || set2 == null) {
			return false;
		} else {
			for(String entry1 : set1) {
				for(String entry2 : set2) {
					if(entry1.equalsIgnoreCase(entry2)) {
						return true;
					}
				}
			}
			return false;
		}
	}

	public static Set<String> getSharedSetEntry(Set<String> set1, Set<String> set2) {
		Set<String> shared = new HashSet<>();
		for(String entry1 : set1) {
			shared.addAll(set2.stream().filter(entry1::equalsIgnoreCase).map(entry2 -> entry1).collect(Collectors.toList()));
		}
		return shared;
	}
}