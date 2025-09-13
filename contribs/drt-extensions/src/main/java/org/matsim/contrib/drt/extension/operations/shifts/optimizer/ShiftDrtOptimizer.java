package org.matsim.contrib.drt.extension.operations.shifts.optimizer;

import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.DrtShiftDispatcher;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.OperationalStop;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.Schedule;
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

        Schedule schedule = vehicle.getSchedule();
        if (schedule.getStatus() == Schedule.ScheduleStatus.STARTED) {
            Task currentTask = schedule.getCurrentTask();
            if (currentTask != null) {
                if (currentTask instanceof OperationalStop opStop) {
                    dispatcher.endOperationalTask((ShiftDvrpVehicle) vehicle, opStop);
                }
            }
        }

        final Task nextTask = schedule.nextTask();

        if (nextTask instanceof OperationalStop opStop) {
            dispatcher.startOperationalTask((ShiftDvrpVehicle) vehicle, opStop);
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
