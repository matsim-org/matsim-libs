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

package org.matsim.contrib.dvrp.vrpagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.AFTER_SCHEDULE_ACTIVITY_TYPE;
import static org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.schedule.Task.TaskType;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynActivity;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.DynAgentLogic;
import org.matsim.contrib.dynagent.IdleDynActivity;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.testcases.fakes.FakeLink;
import org.mockito.ArgumentCaptor;

/**
 * @author Michal Maciejewski (michalm)
 */
public class VrpAgentLogicTest {
	private enum TestTaskType implements TaskType {
		TYPE
	}

	private static final String DVRP_MODE = "dvrp_mode";

	private final EventsManager eventsManager = mock(EventsManager.class);

	private final VrpOptimizer optimizer = new VrpOptimizer() {
		@Override
		public void requestSubmitted(Request request) {
		}

		@Override
		public void nextTask(DvrpVehicle vehicle) {
			vehicle.getSchedule().nextTask();
		}
	};

	private final Link startLink = new FakeLink(Id.createLinkId("link_1"));
	private final ImmutableDvrpVehicleSpecification vehicleSpecification = ImmutableDvrpVehicleSpecification.newBuilder()
			.id(Id.create("veh_1", DvrpVehicle.class))
			.serviceBeginTime(0)
			.serviceEndTime(100)
			.capacity(1)
			.startLinkId(startLink.getId())
			.build();
	private final DvrpVehicle vehicle = new DvrpVehicleImpl(vehicleSpecification, startLink);

	private final DynAgentLogic dynAgentLogic = new VrpAgentLogic(optimizer, VrpAgentLogicTest::createAction, vehicle,
			DVRP_MODE, eventsManager);
	private final DynAgent dynAgent = new DynAgent(Id.createPersonId(vehicleSpecification.getId()), startLink.getId(),
			null, dynAgentLogic);

	@Test
	void testInitialActivity_unplanned() {
		DynActivity initialActivity = dynAgentLogic.computeInitialActivity(dynAgent);

		assertThat(initialActivity.getActivityType()).isEqualTo(BEFORE_SCHEDULE_ACTIVITY_TYPE);
		assertThat(initialActivity.getEndTime()).isEqualTo(vehicleSpecification.getServiceEndTime());
		verifyEvents();
	}

	@Test
	void testInitialActivity_planned() {
		DynActivity initialActivity = dynAgentLogic.computeInitialActivity(dynAgent);

		StayTask task0 = new DefaultStayTask(TestTaskType.TYPE, 10, 90, startLink);
		vehicle.getSchedule().addTask(task0);

		assertThat(initialActivity.getActivityType()).isEqualTo(BEFORE_SCHEDULE_ACTIVITY_TYPE);
		assertThat(initialActivity.getEndTime()).isEqualTo(task0.getBeginTime());
		verifyEvents();
	}

	@Test
	void testInitialActivity_started_failure() {
		DynActivity initialActivity = dynAgentLogic.computeInitialActivity(dynAgent);

		StayTask task0 = new DefaultStayTask(TestTaskType.TYPE, 10, 90, startLink);
		vehicle.getSchedule().addTask(task0);
		vehicle.getSchedule().nextTask();

		assertThat(initialActivity.getActivityType()).isEqualTo(BEFORE_SCHEDULE_ACTIVITY_TYPE);
		assertThatThrownBy(initialActivity::getEndTime).isExactlyInstanceOf(IllegalStateException.class)
				.hasMessage("Only PLANNED or UNPLANNED schedules allowed.");
		verifyEvents();
	}

	@Test
	void testNextAction_unplanned_completed() {
		IdleDynActivity nextAction = (IdleDynActivity)dynAgentLogic.computeNextAction(null,
				vehicle.getServiceEndTime());

		assertThat(nextAction.getActivityType()).isEqualTo(AFTER_SCHEDULE_ACTIVITY_TYPE);
		assertThat(nextAction.getEndTime()).isEqualTo(Double.POSITIVE_INFINITY);
		verifyEvents();
	}

	@Test
	void testNextAction_planned_started() {
		double time = 10;
		StayTask task0 = new DefaultStayTask(TestTaskType.TYPE, time, 90, startLink);
		vehicle.getSchedule().addTask(task0);

		DynActivity nextActivity = (DynActivity)dynAgentLogic.computeNextAction(null, time);
		assertDynActivity(nextActivity, task0 + "", task0.getEndTime());
		verifyEvents(taskStartedEvent(time, task0));
	}

	@Test
	void testNextAction_started_started() {
		double time = 50;
		StayTask task0 = new DefaultStayTask(TestTaskType.TYPE, 10, time, startLink);
		vehicle.getSchedule().addTask(task0);
		StayTask task1 = new DefaultStayTask(TestTaskType.TYPE, time, 90, startLink);
		vehicle.getSchedule().addTask(task1);
		vehicle.getSchedule().nextTask();//current: task0

		DynActivity nextActivity = (DynActivity)dynAgentLogic.computeNextAction(null, time);
		assertDynActivity(nextActivity, task1 + "", task1.getEndTime());
		verifyEvents(taskEndedEvent(time, task0), taskStartedEvent(time, task1));
	}

	@Test
	void testNextAction_started_completed() {
		double time = 90;
		StayTask task0 = new DefaultStayTask(TestTaskType.TYPE, 10, time, startLink);
		vehicle.getSchedule().addTask(task0);
		vehicle.getSchedule().nextTask();//current: task0

		DynActivity nextActivity = (DynActivity)dynAgentLogic.computeNextAction(null, time);

		assertDynActivity(nextActivity, AFTER_SCHEDULE_ACTIVITY_TYPE, Double.POSITIVE_INFINITY);
		verifyEvents(taskEndedEvent(time, task0));
	}

	private static DynAction createAction(DynAgent dynAgent, DvrpVehicle vehicle, double now) {
		Task currentTask = vehicle.getSchedule().getCurrentTask();
		return new IdleDynActivity(currentTask + "", currentTask::getEndTime);
	}

	private void assertDynActivity(DynActivity activity, String type, double endTime) {
		assertThat(activity.getActivityType()).isEqualTo(type);
		assertThat(activity.getEndTime()).isEqualTo(endTime);
	}

	private void verifyEvents(AbstractTaskEvent... events) {
		ArgumentCaptor<AbstractTaskEvent> captor = ArgumentCaptor.forClass(AbstractTaskEvent.class);
		verify(eventsManager, times(events.length)).processEvent(captor.capture());
		assertThat(captor.getAllValues()).usingRecursiveFieldByFieldElementComparator().containsExactly(events);
	}

	private TaskStartedEvent taskStartedEvent(double time, Task task) {
		return new TaskStartedEvent(time, DVRP_MODE, vehicleSpecification.getId(), dynAgent.getId(), task);
	}

	private TaskEndedEvent taskEndedEvent(double time, Task task) {
		return new TaskEndedEvent(time, DVRP_MODE, vehicleSpecification.getId(), dynAgent.getId(), task);
	}
}
