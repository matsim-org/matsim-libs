/* *********************************************************************** *
 * project: org.matsim.*
 * HTStatsFactory.java
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
package org.matsim.contrib.socnetgen.sna.snowball.analysis;


import org.matsim.contrib.common.stats.DescriptivePiStatistics;
import org.matsim.contrib.socnetgen.sna.math.DescriptivePiStatisticsFactory;
import org.matsim.contrib.socnetgen.sna.math.HorwitzThompsonEstimator;

/**
 * @author illenberger
 *
 */
public class HTStatsFactory implements DescriptivePiStatisticsFactory {

	private int N;
	
	public HTStatsFactory(int N) {
		this.N = N;
	}
	
	@Override
	public DescriptivePiStatistics newInstance() {
		DescriptivePiStatistics stats = new DescriptivePiStatistics();
		stats.setMeanImpl(new HorwitzThompsonEstimator(N));
		return stats;
	}

}
