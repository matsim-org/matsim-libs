/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.facilities.ActivityFacilities;
import playground.johannes.synpop.data.PlainPerson;

import java.util.Collection;

/**
 * @author johannes
 *
 */
public class ActivityDistanceTruncatedTask extends ActivityDistanceTask {

	private final double threshold;
	
	@Override
	protected DescriptiveStatistics statistics(Collection<PlainPerson> persons, String purpose, String mode) {
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
	protected String getKey(String type) {
		String key = super.getKey(type);
		if(threshold > 0) {
			key = String.format("%s.%d", key, (int)threshold);
		}
		
		return key;
	}

	public ActivityDistanceTruncatedTask(ActivityFacilities facilities, String mode, double threshold) {
		super(facilities, mode);
		this.threshold = threshold;
	}

}
