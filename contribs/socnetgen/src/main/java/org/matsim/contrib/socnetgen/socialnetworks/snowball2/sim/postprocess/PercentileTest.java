/* *********************************************************************** *
 * project: org.matsim.*
 * PercentileTest.java
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
package org.matsim.contrib.socnetgen.socialnetworks.snowball2.sim.postprocess;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

/**
 * @author illenberger
 *
 */
public class PercentileTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		
		for(int i = 0; i < 20; i++) {
			stats.addValue(i);
		}
		
		System.out.println(stats.getPercentile(10));

	}

}
