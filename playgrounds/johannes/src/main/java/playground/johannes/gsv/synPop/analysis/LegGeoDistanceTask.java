/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.analysis;

import java.util.Collection;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyPerson;

/**
 * @author johannes
 * 
 */
public class LegGeoDistanceTask extends LegDistanceTask {

	private final double threshold;

	public LegGeoDistanceTask(String mode) {
		super(CommonKeys.LEG_GEO_DISTANCE, mode);
		threshold = 0;
	}
	
	public LegGeoDistanceTask(String mode, double threshold) {
		super(CommonKeys.LEG_GEO_DISTANCE, mode);
		this.threshold = threshold;
	}

	@Override
	protected DescriptiveStatistics statistics(Collection<ProxyPerson> persons, String purpose, String mode) {
		DescriptiveStatistics stats = super.statistics(persons, purpose, mode);

		if (threshold > 0) {
			DescriptiveStatistics newStats = new DescriptiveStatistics();
			for (int i = 0; i < stats.getN(); i++) {
				double val = stats.getElement(i);
				if (val >= threshold) {
					newStats.addValue(val);
				}
			}

			return newStats;
		} else {
			return stats;
		}
	}

	@Override
	protected String getKey(String purpose, String attKey) {
		String key = super.getKey(purpose, attKey);
		if(threshold > 0) {
			key = String.format("%s.%d", key, (int)threshold);
		}
		
		return key;
	}

	
}
