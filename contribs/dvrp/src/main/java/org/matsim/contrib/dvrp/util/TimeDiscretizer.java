/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.util;

import static com.google.common.base.Preconditions.checkArgument;

import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;

public class TimeDiscretizer {
	private final int intervalCount;
	private final int timeInterval;
	private final int maxTime;

	public TimeDiscretizer(TravelTimeCalculatorConfigGroup ttcConfig) {
		this(ttcConfig.getMaxTime(), ttcConfig.getTraveltimeBinSize());
	}

	public TimeDiscretizer(int maxTime, int timeInterval) {
		checkArgument(timeInterval > 0, "interval size must be positive");
		checkArgument(maxTime >= 0, "maxTime must not be negative");
		this.timeInterval = timeInterval;
		this.maxTime = maxTime;
		intervalCount = maxTime / timeInterval + 1;
	}

	public int getIdx(double time) {
		checkArgument(time >= 0);
		checkArgument(time <= maxTime);
		return (int)time / timeInterval;
	}

	public int discretize(double time) {
		return getIdx(time) * timeInterval;
	}

	public int getTimeInterval() {
		return timeInterval;
	}

	public int getIntervalCount() {
		return intervalCount;
	}

	public interface TimeBinConsumer {
		void accept(int bin, int time);
	}

	public void forEach(TimeBinConsumer consumer) {
		for (int i = 0; i < intervalCount; i++) {
			consumer.accept(i, i * timeInterval);
		}
	}
}
