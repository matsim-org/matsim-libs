/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.common.timeprofile;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.stream.IntStream;

import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.trafficmonitoring.TimeBinUtils;

public class TimeDiscretizer {
	private final int intervalCount;
	private final double timeInterval;
	private final int maxTime;

	public TimeDiscretizer(TravelTimeCalculatorConfigGroup ttcConfig) {
		this(ttcConfig.getMaxTime(), ttcConfig.getTraveltimeBinSize());
	}

	public TimeDiscretizer(int maxTime, double timeInterval) {
		checkArgument(timeInterval > 0, "interval size must be positive");
		checkArgument(maxTime >= 0, "maxTime must not be negative");
		this.timeInterval = timeInterval;
		this.maxTime = maxTime;
		intervalCount = TimeBinUtils.getTimeBinCount(maxTime, timeInterval);
	}

	public int getIdx(double time) {
		checkArgument(time >= 0);
		checkArgument(time <= maxTime);
		return TimeBinUtils.getTimeBinIndex(time, timeInterval, intervalCount);
	}

	public double discretize(double time) {
		return getIdx(time) * timeInterval;
	}

	public double getTimeInterval() {
		return timeInterval;
	}

	public int getIntervalCount() {
		return intervalCount;
	}

	public double[] getTimes() {
		return IntStream.range(0, intervalCount).mapToDouble(i -> i * timeInterval).toArray();
	}

	public interface TimeBinConsumer {
		void accept(int bin, double time);
	}

	public void forEach(TimeBinConsumer consumer) {
		for (int i = 0; i < intervalCount; i++) {
			consumer.accept(i, i * timeInterval);
		}
	}
}
