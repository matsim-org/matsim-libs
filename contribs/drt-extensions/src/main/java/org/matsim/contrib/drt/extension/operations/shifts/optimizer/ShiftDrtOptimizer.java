package org.matsim.contrib.drt.extension.operations.shifts.optimizer;

import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.DrtShiftDispatcher;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftBreakTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftChangeOverTask;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public class ShiftDrtOptimizer implements DrtOptimizer, MobsimInitializedListener {

    private final DrtOptimizer optimizer;

    private final DrtShiftDispatcher dispatcher;
    private final ScheduleTimingUpdater scheduleTimingUpdater;

    public ShiftDrtOptimizer(DrtOptimizer optimizer,
							 DrtShiftDispatcher dispatcher, ScheduleTimingUpdater scheduleTimingUpdater) {
        this.optimizer = optimizer;
        this.dispatcher = dispatcher;
        this.scheduleTimingUpdater = scheduleTimingUpdater;
    }

    @Override
    public void nextTask(DvrpVehicle vehicle) {
        scheduleTimingUpdater.updateBeforeNextTask(vehicle);
        final Task task = vehicle.getSchedule().nextTask();

		if(task != null) {
			// previous task
			int previousTaskIdx = task.getTaskIdx() - 1;
			if (previousTaskIdx >= 0) {
				Task previousTask = vehicle.getSchedule().getTasks().get(previousTaskIdx);
				if (previousTask instanceof ShiftBreakTask) {
					dispatcher.endBreak((ShiftDvrpVehicle) vehicle, (ShiftBreakTask) previousTask);
				}
			}
		}

        if(task instanceof ShiftChangeOverTask) {
			dispatcher.endShift(
					(ShiftDvrpVehicle) vehicle,
					((ShiftChangeOverTask) task).getLink().getId(),
					((ShiftChangeOverTask) task).getFacility().getId()
			);
        } else if(task instanceof ShiftBreakTask) {
            dispatcher.startBreak((ShiftDvrpVehicle) vehicle, ((ShiftBreakTask) task).getFacility().getLinkId());
        }
    }


    @Override
    public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
        dispatcher.dispatch(e.getSimulationTime());
        this.optimizer.notifyMobsimBeforeSimStep(e);
    }

    @Override
    public void requestSubmitted(Request request) {
        this.optimizer.requestSubmitted(request);
    }

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		dispatcher.initialize();
	}
}
