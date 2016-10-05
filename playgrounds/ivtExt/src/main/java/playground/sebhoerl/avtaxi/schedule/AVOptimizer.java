package playground.sebhoerl.avtaxi.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import playground.sebhoerl.avtaxi.data.AVOperator;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

import java.util.HashMap;
import java.util.Map;

public class AVOptimizer implements VrpOptimizerWithOnlineTracking, MobsimBeforeSimStepListener {
    @Override
    public void nextLinkEntered(DriveTask driveTask) {
        Id<Link> linkId = driveTask.getSchedule().getVehicle().getAgentLogic().getDynAgent().getCurrentLinkId();
        ((AVVehicle) driveTask.getSchedule().getVehicle()).getOperator().getDispatcher().nextLinkEntered();
    }

    @Override
    public void requestSubmitted(Request request) {
        ((AVRequest) request).getOperator().getDispatcher().requestSubmitted((AVRequest) request);
    }

    @Override
    public void nextTask(Schedule<? extends Task> schedule) {

    }

    @Override
    public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {

    }
}
