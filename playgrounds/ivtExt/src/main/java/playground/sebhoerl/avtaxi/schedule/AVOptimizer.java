package playground.sebhoerl.avtaxi.schedule;


import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.tracker.TaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

@Singleton
public class AVOptimizer implements VrpOptimizerWithOnlineTracking, MobsimBeforeSimStepListener {
    private double now;
    final private List<AVRequest> submittedRequestsBuffer = Collections.synchronizedList(new LinkedList<>());

//    @Inject private Map<Id<AVOperator>, AVDispatcher> dispatchers; // jan: yet unused
    @Inject private EventsManager eventsManager;

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
    public void nextTask(Vehicle vehicle) {
        Schedule schedule = vehicle.getSchedule();
        if (schedule.getStatus() != Schedule.ScheduleStatus.STARTED) {
            schedule.nextTask();
            return;
        }

        Task currentTask = schedule.getCurrentTask();
        currentTask.setEndTime(now);

        List<? extends Task> tasks = schedule.getTasks();
        int index = currentTask.getTaskIdx() + 1;
        AVTask nextTask = null;

        if (index < tasks.size()) {
            nextTask = (AVTask)tasks.get(index);
        }

        double startTime = now;

        AVTask indexTask;
        while (index < tasks.size()) {
            indexTask = (AVTask)tasks.get(index);

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

        if (nextTask != null && nextTask instanceof AVDropoffTask) {
            processTransitEvent((AVDropoffTask) nextTask);
        }
    }

    @Override
    public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
        now = e.getSimulationTime();
        processSubmittedRequestsBuffer();
    }

    private void processTransitEvent(AVDropoffTask task) {
        for (AVRequest request : task.getRequests()) {
            eventsManager.processEvent(new AVTransitEvent(request, now));
        }
    }

    @Override
    public void nextLinkEntered(DriveTask driveTask) {
        TaskTracker taskTracker = driveTask.getTaskTracker();
        OnlineDriveTaskTracker onlineDriveTaskTracker = (OnlineDriveTaskTracker) taskTracker;
        LinkTimePair linkTimePair = onlineDriveTaskTracker.getDiversionPoint();
        AVVehicle avVehicle = (AVVehicle) driveTask.getSchedule().getVehicle();
        AVDispatcher avDispatcher = avVehicle.getDispatcher();
        avDispatcher.onNextLinkEntered(avVehicle, driveTask, linkTimePair);

    }
}
