package playground.sebhoerl.avtaxi.schedule;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import playground.sebhoerl.avtaxi.data.AVOperator;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

import java.util.*;

@Singleton
public class AVOptimizer implements VrpOptimizer, MobsimBeforeSimStepListener {
    private double now;
    final private List<AVRequest> submittedRequestsBuffer = Collections.synchronizedList(new LinkedList<>());


    @Inject private Map<Id<AVOperator>, AVDispatcher> dispatchers;

    @Override
    public void requestSubmitted(Request request) {
        // TODO: IS this necessary?
        submittedRequestsBuffer.add((AVRequest) request);
    }

    private void processSubmittedRequestsBuffer() {
        for (AVRequest request : submittedRequestsBuffer) {
            request.getDispatcher().onRequestSubmitted(request);
        }

        submittedRequestsBuffer.clear();
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
            ((AVVehicle) schedule.getVehicle()).getDispatcher().onNextTaskStarted(nextTask);
        }
    }

    @Override
    public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
        now = e.getSimulationTime();
        processSubmittedRequestsBuffer();
    }
}
