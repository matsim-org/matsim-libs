package playground.clruch.dispatcher;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.AbstractTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;

import playground.clruch.router.FuturePathContainer;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;

class StayVehicleDiversionDirective extends VehicleDiversionDirective {

    StayVehicleDiversionDirective(VehicleLinkPair vehicleLinkPair, Link destination, FuturePathContainer futurePathContainer) {
        super(vehicleLinkPair, destination, futurePathContainer);
    }

    @Override
    void execute() {
        // TODO Auto-generated method stub
        final Schedule<AbstractTask> schedule = (Schedule<AbstractTask>) vehicleLinkPair.avVehicle.getSchedule();
        AbstractTask abstractTask = schedule.getCurrentTask(); // <- implies that task is started
        final AVStayTask avStayTask = (AVStayTask) abstractTask;

        final double scheduleEndTime = schedule.getEndTime(); // typically 108000.0
        if (avStayTask.getStatus() == Task.TaskStatus.STARTED) {
            avStayTask.setEndTime(vehicleLinkPair.linkTimePair.time);
        } else {
            schedule.removeLastTask();
            System.out.println("The last task was removed for " + vehicleLinkPair.avVehicle.getId());
            throw new RuntimeException("task should be started since current!");
        }
        VrpPathWithTravelData vrpPathWithTravelData = futurePathContainer.getVrpPathWithTravelData();
        
        final AVDriveTask avDriveTask = new AVDriveTask(vrpPathWithTravelData);
        schedule.addTask(avDriveTask);
        final double endDriveTime = avDriveTask.getEndTime();

        // TODO redundant
        if (endDriveTime < scheduleEndTime)
            schedule.addTask(new AVStayTask(endDriveTime, scheduleEndTime, destination));

    }

}
