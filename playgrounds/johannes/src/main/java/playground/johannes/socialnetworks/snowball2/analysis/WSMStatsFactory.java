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
package playground.johannes.socialnetworks.snowball2.analysis;

import org.matsim.contrib.sna.math.DescriptivePiStatistics;
import org.matsim.contrib.sna.math.DescriptivePiStatisticsFactory;

import playground.johannes.socialnetworks.statistics.WeightedSampleMean;

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
