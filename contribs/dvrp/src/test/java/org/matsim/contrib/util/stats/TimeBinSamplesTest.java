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
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.analysis.ExecutedTask;
import org.matsim.contrib.dvrp.schedule.Task;

/**
 * @author Michal Maciejewski (michalm)
 */
public class TimeBinSamplesTest {
	@Test
	public void taskSamples_zeroDuration() {
		var task = task(10, 10);
		assertThat(taskSamples(task, 100)).isEmpty();
	}

	@Test
	public void taskSamples_oneSample() {
		var task = task(110, 190);
		assertThat(taskSamples(task, 100)).containsExactly(new TimeBinSample<>(1, task));
	}

	@Test
	public void taskSamples_threeSamples() {
		var task = task(110, 390);
		assertThat(taskSamples(task, 100)).containsExactly(new TimeBinSample<>(1, task), new TimeBinSample<>(2, task),
				new TimeBinSample<>(3, task));
	}

	@Test
	public void taskSamples_taskEndEqualToTimeBinEnd() {
		var task = task(110, 300);
		assertThat(taskSamples(task, 100)).containsExactly(new TimeBinSample<>(1, task), new TimeBinSample<>(2, task));
	}

	private ExecutedTask task(double beginTime, double endTime) {
		return new ExecutedTask(mock(Task.TaskType.class), beginTime, endTime, Id.createLinkId("a"),
				Id.createLinkId("b"));
	}
}
