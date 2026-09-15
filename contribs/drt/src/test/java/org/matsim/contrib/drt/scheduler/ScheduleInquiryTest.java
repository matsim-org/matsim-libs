package org.matsim.contrib.drt.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.matsim.contrib.dvrp.schedule.ScheduleInquiry;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.mobsim.framework.MobsimTimer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DrtScheduleInquiryTest {

    @Mock MobsimTimer timer;
    @Mock DvrpVehicle vehicle;
    @Mock Schedule schedule;

    private DrtScheduleInquiry inquiry() {
        DrtScheduleInquiry inq = new DrtScheduleInquiry(timer);
        when(vehicle.getSchedule()).thenReturn(schedule);
        return inq;
    }

    @Test @DisplayName("Not started schedule -> not idle")
    void notStartedSchedule_returnsFalse() {
        DrtScheduleInquiry inq = inquiry();
        when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.PLANNED);
        assertFalse(inq.isIdle(vehicle));
    }

    @Test @DisplayName("Current task not DrtStayTask -> not idle")
    void nonStayTask_returnsFalse() {
        DrtScheduleInquiry inq = inquiry();
        when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.STARTED);

        Task t = mock(Task.class);
        when(schedule.getCurrentTask()).thenReturn(t);

        assertFalse(inq.isIdle(vehicle));
    }

    @Test @DisplayName("At/after service end -> not idle")
    void atOrAfterServiceEnd_returnsFalse() {
        DrtScheduleInquiry inq = inquiry();
        when(timer.getTimeOfDay()).thenReturn(1000.0);
        when(vehicle.getServiceEndTime()).thenReturn(1000.0);
        when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.STARTED);

        DrtStayTask s = mock(DrtStayTask.class);
        when(schedule.getCurrentTask()).thenReturn(s);

        assertFalse(inq.isIdle(vehicle));
    }

    @Test @DisplayName("none() behaves like baseline when elapsed > 0")
    void noneCriteria_behavesLikeBaseline() {

        DrtScheduleInquiry inq = inquiry();

        DrtStayTask s = mock(DrtStayTask.class);
        when(s.getBeginTime()).thenReturn((double) 100);
        when(s.getEndTime()).thenReturn((double) 1000);
        when(s.getTaskIdx()).thenReturn(0);

        when(vehicle.getServiceEndTime()).thenReturn(10_000.0);
        when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.STARTED);
        when(schedule.getCurrentTask()).thenReturn(s);
        when(schedule.getTaskCount()).thenReturn(2); // used by last-task logic
        assertTrue(inq.isIdle(vehicle, ScheduleInquiry.IdleCriteria.none()));
    }

    @Test @DisplayName("none() with zero elapsed -> idle")
    void noneCriteria_zeroElapsed_isFalse() {
        DrtScheduleInquiry inq = inquiry();
        when(timer.getTimeOfDay()).thenReturn(100.0); // elapsed == 0
        when(vehicle.getServiceEndTime()).thenReturn(10_000.0);
        when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.STARTED);
        DrtStayTask s = mock(DrtStayTask.class);
        when(s.getBeginTime()).thenReturn((double) 100);
        when(s.getEndTime()).thenReturn((double) 1000);
        when(schedule.getCurrentTask()).thenReturn(s);
        assertTrue(inq.isIdle(vehicle, ScheduleInquiry.IdleCriteria.none()));
    }

    @Test @DisplayName("elapsed == minElapsed -> idle")
    void elapsedEqualsMin_returnsTrue() {
        DrtScheduleInquiry inq = inquiry();
        when(timer.getTimeOfDay()).thenReturn(150.0); // elapsed = 50
        when(vehicle.getServiceEndTime()).thenReturn(10_000.0);
        when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.STARTED);

        DrtStayTask s = mock(DrtStayTask.class);
        when(s.getBeginTime()).thenReturn((double) 100);

        when(schedule.getCurrentTask()).thenReturn(s);

        var c = new ScheduleInquiry.IdleCriteria(50.0, 0.0);
        assertTrue(inq.isIdle(vehicle, c));
    }

    @Test @DisplayName("elapsed > minElapsed and last task -> idle even with small remaining")
    void lastTask_bypassesRemaining() {
        DrtScheduleInquiry inq = inquiry();
        when(timer.getTimeOfDay()).thenReturn(151.0); // elapsed=51, remaining=9
        when(vehicle.getServiceEndTime()).thenReturn(10_000.0);
        when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.STARTED);

        DrtStayTask s = mock(DrtStayTask.class);
        when(s.getBeginTime()).thenReturn((double) 100);
        when(s.getEndTime()).thenReturn((double) 160);
        when(s.getTaskIdx()).thenReturn(1);
        when(schedule.getCurrentTask()).thenReturn(s);
        when(schedule.getTaskCount()).thenReturn(2); // last task

        var c = new ScheduleInquiry.IdleCriteria(50.0, 60.0);
        assertTrue(inq.isIdle(vehicle, c));
    }

    @Test @DisplayName("remaining == minRemaining (not last task) -> idle")
    void remainingEqualsMin_notLastTask_returnsTrue() {
        DrtScheduleInquiry inq = inquiry();
        when(timer.getTimeOfDay()).thenReturn(250.0); // elapsed=150, remaining=50
        when(vehicle.getServiceEndTime()).thenReturn(10_000.0);
        when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.STARTED);

        DrtStayTask s = mock(DrtStayTask.class);
        when(s.getBeginTime()).thenReturn((double) 100);
        when(s.getEndTime()).thenReturn((double) 300);
        when(s.getTaskIdx()).thenReturn(0);
        when(schedule.getCurrentTask()).thenReturn(s);
        when(schedule.getTaskCount()).thenReturn(3); // not last

        var c = new ScheduleInquiry.IdleCriteria(100.0, 50.0);
        assertTrue(inq.isIdle(vehicle, c));
    }

    @Test @DisplayName("remaining > minRemaining (not last task) -> idle")
    void remainingGreaterThanMin_notLastTask_returnsTrue() {
        DrtScheduleInquiry inq = inquiry();
        when(timer.getTimeOfDay()).thenReturn(250.0); // elapsed=150, remaining=60
        when(vehicle.getServiceEndTime()).thenReturn(10_000.0);
        when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.STARTED);

        DrtStayTask s = mock(DrtStayTask.class);
        when(s.getBeginTime()).thenReturn((double) 100);
        when(s.getEndTime()).thenReturn((double) 310);
        when(s.getTaskIdx()).thenReturn(0);
        when(schedule.getCurrentTask()).thenReturn(s);
        when(schedule.getTaskCount()).thenReturn(3);

        var c = new ScheduleInquiry.IdleCriteria(100.0, 50.0);
        assertTrue(inq.isIdle(vehicle, c));
    }

    @Test @DisplayName("remaining < minRemaining (not last task) -> not idle")
    void remainingLessThanMin_notLastTask_returnsFalse() {
        DrtScheduleInquiry inq = inquiry();
        when(timer.getTimeOfDay()).thenReturn(250.0); // elapsed=150, remaining=60
        when(vehicle.getServiceEndTime()).thenReturn(10_000.0);
        when(schedule.getStatus()).thenReturn(Schedule.ScheduleStatus.STARTED);

        DrtStayTask s = mock(DrtStayTask.class);
        when(s.getBeginTime()).thenReturn((double) 100);
        when(s.getEndTime()).thenReturn((double) 310);
        when(s.getTaskIdx()).thenReturn(0);
        when(schedule.getCurrentTask()).thenReturn(s);
        when(schedule.getTaskCount()).thenReturn(3);

        var c = new ScheduleInquiry.IdleCriteria(100.0, 61.0);
        assertFalse(inq.isIdle(vehicle, c));
    }

}