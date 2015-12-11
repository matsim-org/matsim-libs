/* *********************************************************************** *
 * project: org.matsim.*
 * WSMStatsFactory.java
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
import org.matsim.contrib.common.stats.WeightedSampleMean;

/**
 * @author illenberger
 *
 */
public class WSMStatsFactory implements DescriptivePiStatisticsFactory {

	@Override
	public DescriptivePiStatistics newInstance() {
		DescriptivePiStatistics stats = new DescriptivePiStatistics();
		stats.setMeanImpl(new WeightedSampleMean());
		return stats;
	}

}
