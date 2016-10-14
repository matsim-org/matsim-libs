package playground.sebhoerl.avtaxi.schedule;

import com.google.inject.Singleton;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.schedule.TaxiSchedules;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import playground.sebhoerl.avtaxi.data.AVOperator;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class AVOptimizer implements VrpOptimizer, MobsimBeforeSimStepListener {
    private double now;

    AVDispatcher getDispatcher(Request request) {
        return ((AVRequest) request).getOperator().getDispatcher();
    }

    AVDispatcher getDispatcher(Schedule<? extends Task> schedule) {
        return ((AVVehicle) ((Schedule<AVTask>) schedule).getVehicle()).getOperator().getDispatcher();
    }

    @Override
    public void requestSubmitted(Request request) {
        getDispatcher(request).onRequestSubmitted((AVRequest) request);
    }

    @Override
    public void nextTask(Schedule<? extends Task> schedule) {
        if (schedule.getStatus() != Schedule.ScheduleStatus.STARTED) {
            schedule.nextTask();
            return;
        }

        Task currentTask = schedule.getCurrentTask();
        currentTask.setEndTime(now);

        List<AVTask> tasks = ((Schedule<AVTask>) schedule).getTasks();
        int index = currentTask.getTaskIdx() + 1;
        AVTask nextTask = null;

        if (index < tasks.size()) {
            nextTask = tasks.get(index);
        }

        double startTime = now;

        AVTask indexTask;
        while (index < tasks.size()) {
            indexTask = tasks.get(index);

            if (indexTask.getAVTaskType() == AVTask.AVTaskType.STAY) {
                if (indexTask.getEndTime() < startTime) indexTask.setEndTime(startTime);
            } else {
                indexTask.setEndTime(indexTask.getEndTime() - indexTask.getBeginTime() + startTime);
            }

            indexTask.setBeginTime(startTime);
            startTime = indexTask.getEndTime();
            index++;
        }

        schedule.nextTask();

        if (nextTask != null) {
            getDispatcher(schedule).onNextTaskStarted(nextTask);
        }
    }

    @Override
    public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
        now = e.getSimulationTime();
    }
}
