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
	public enum Type {
		ACYCLIC, CYCLIC, OPEN_ENDED
	}

	public static final TimeDiscretizer ACYCLIC_1_SEC = new TimeDiscretizer(30 * 3600, 1, Type.ACYCLIC);
	public static final TimeDiscretizer ACYCLIC_15_MIN = new TimeDiscretizer(30 * 3600, 900, Type.ACYCLIC);
	public static final TimeDiscretizer ACYCLIC_1_HOUR = new TimeDiscretizer(30 * 3600, 3600, Type.ACYCLIC);
	public static final TimeDiscretizer ACYCLIC_30_HOURS = new TimeDiscretizer(30 * 3600, 30 * 3600, Type.ACYCLIC);

	// useful for routing when running over-night scenarios, such as a 5am-5am taxi simulation
	public static final TimeDiscretizer CYCLIC_1_SEC = new TimeDiscretizer(24 * 3600, 1, Type.CYCLIC);
	public static final TimeDiscretizer CYCLIC_15_MIN = new TimeDiscretizer(24 * 3600, 900, Type.CYCLIC);
	public static final TimeDiscretizer CYCLIC_1_HOUR = new TimeDiscretizer(24 * 3600, 3600, Type.CYCLIC);
	public static final TimeDiscretizer CYCLIC_24_HOURS = new TimeDiscretizer(24 * 3600, 24 * 3600, Type.CYCLIC);

	// approach used in TravelTimeCalculator, the last time bin is open ended
	public static final TimeDiscretizer OPEN_ENDED_1_SEC = new TimeDiscretizer(30 * 3600, 1, Type.OPEN_ENDED);
	public static final TimeDiscretizer OPEN_ENDED_15_MIN = new TimeDiscretizer(30 * 3600, 900, Type.OPEN_ENDED);
	public static final TimeDiscretizer OPEN_ENDED_1_HOUR = new TimeDiscretizer(30 * 3600, 3600, Type.OPEN_ENDED);
	public static final TimeDiscretizer OPEN_ENDED_30_HOURS = new TimeDiscretizer(30 * 3600, 30 * 3600,
			Type.OPEN_ENDED);

	private final int intervalCount;
	private final int timeInterval;
	private final Type type;

	public TimeDiscretizer(TravelTimeCalculatorConfigGroup ttcConfig) {
		this(ttcConfig.getMaxTime(), ttcConfig.getTraveltimeBinSize());
	}

	public TimeDiscretizer(int maxTime, int timeInterval) {
		this(maxTime, timeInterval, Type.OPEN_ENDED);
	}

	public TimeDiscretizer(int maxTime, int timeInterval, Type type) {
		checkArgument(timeInterval > 0, "interval size must be positive");
		checkArgument(maxTime >= 0, "maxTime must not be negative");
		this.timeInterval = timeInterval;
		this.type = type;
		// option: additional open-end bin
		intervalCount = maxTime / timeInterval + (type == Type.OPEN_ENDED ? 1 : 0);
	}

	public int getIdx(double time) {
		checkArgument(time >= 0);
		int idx = (int)time / timeInterval;// rounding down
		if (idx < intervalCount) {
			return idx;
		}

		switch (type) {
			case ACYCLIC:
				throw new IllegalArgumentException("Out of bound index: time="
						+ time
						+ ", timeInterval="
						+ timeInterval
						+ ", idx="
						+ idx
						+ ", intervalCount="
						+ intervalCount);

			case CYCLIC:
				return idx % intervalCount;

			case OPEN_ENDED:
				return intervalCount - 1;

			default:
				throw new RuntimeException();
		}
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
