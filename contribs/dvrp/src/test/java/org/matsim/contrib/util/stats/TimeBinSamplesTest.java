/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.util.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.contrib.util.stats.TimeBinSamples.taskSamples;

import org.junit.Test;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;

/**
 * @author Michal Maciejewski (michalm)
 */
public class TimeBinSamplesTest {
	@Test
	public void taskSamples_zeroDuration() {
		Task task = task(10, 10);
		assertThat(taskSamples(task, 100)).isEmpty();
	}

	@Test
	public void taskSamples_oneSample() {
		Task task = task(110, 190);
		assertThat(taskSamples(task, 100)).containsExactly(new TimeBinSample<>(1, task));
	}

	@Test
	public void taskSamples_threeSamples() {
		Task task = task(110, 390);
		assertThat(taskSamples(task, 100)).containsExactly(new TimeBinSample<>(1, task), new TimeBinSample<>(2, task),
				new TimeBinSample<>(3, task));
	}

	@Test
	public void taskSamples_taskEndEqualToTimeBinEnd() {
		Task task = task(110, 300);
		assertThat(taskSamples(task, 100)).containsExactly(new TimeBinSample<>(1, task), new TimeBinSample<>(2, task));
	}

	private Task task(double beginTime, double endTime) {
		return new StayTask(() -> "name", beginTime, endTime, null);
	}
}
