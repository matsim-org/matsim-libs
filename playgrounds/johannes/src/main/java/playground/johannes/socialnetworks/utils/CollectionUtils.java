/* *********************************************************************** *
 * project: org.matsim.*
 * CollectionUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class CollectionUtils {

	public static <T> List<T>[]split(Set<T> set, int n) {
		if(set.size() >= n) {
			List<T>[] arrays = new List[n];
			int minSegmentSize = (int) Math.floor(set.size()/(double)n);
			
			int start = 0;
			int stop = minSegmentSize;
			
			Iterator<T> it = set.iterator();
			
			for(int i = 0; i < n - 1; i++) {
				int segmentSize = stop - start;
				List<T> segment = new ArrayList<T>(segmentSize);
				for(int k = 0; k < segmentSize; k++) {
					segment.add(it.next());
				}
				arrays[i] = segment;
				start = stop;
				stop += segmentSize;
			}
			
			int segmentSize = set.size() - start;
			List<T> segment = new ArrayList<T>(segmentSize);
			for(int k = 0; k < segmentSize; k++) {
				segment.add(it.next());
			}
			arrays[n - 1] = segment;
			
			return arrays;
		} else {
			throw new IllegalArgumentException("n must not be smaller set size!");
		}
	}
}
