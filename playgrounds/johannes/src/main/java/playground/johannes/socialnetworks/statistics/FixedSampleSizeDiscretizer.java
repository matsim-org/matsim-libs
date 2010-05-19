/* *********************************************************************** *
 * project: org.matsim.*
 * FixedSampleSizeDiscretizer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.statistics;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleIntHashMap;

import java.util.Arrays;

/**
 * @author illenberger
 *
 */
public class FixedSampleSizeDiscretizer implements Discretizer {

	private TDoubleArrayList borders;
	
	public FixedSampleSizeDiscretizer(double[] samples, int minSize) {
		Arrays.sort(samples);
		TDoubleIntHashMap hist = new TDoubleIntHashMap(samples.length);
		for(int i = 0; i < samples.length; i++) {
			hist.adjustOrPutValue(samples[i], 1, 1);
		}
		
		double keys[] = hist.keys();
		Arrays.sort(keys);
		borders = new TDoubleArrayList(keys.length);
		int size = 0;
		for(int i = 0; i < keys.length; i++) {
			size += hist.get(keys[i]);
			if(size >= minSize) {
				borders.add(keys[i]);
				size = 0;
			}
		}
		if(size > 0)
			borders.add(samples[samples.length - 1]);
		
//		System.out.println("----------- borders --------------");
//		for(int i = 0; i < borders.size(); i++) {
//			System.out.println(String.valueOf(borders.get(i)));
//		}
//		System.out.println("----------- borders --------------");
	}
	
	@Override
	public double discretize(double value) {
		int idx = borders.binarySearch(value);
		if(idx > -1) {
			return borders.get(idx);
		} else {
			return borders.get(-idx - 1);
		}
	}

}
