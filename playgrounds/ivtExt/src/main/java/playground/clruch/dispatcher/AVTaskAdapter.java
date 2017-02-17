package playground.clruch.dispatcher;

import org.matsim.contrib.dvrp.schedule.AbstractTask;

import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVPickupTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.avtaxi.schedule.AVTask;

public class AVTaskAdapter implements AVTaskListener {

    public AVTaskAdapter(AbstractTask abstractTask) {
        AVTask avTask = (AVTask) abstractTask;
        switch (avTask.getAVTaskType()) {
        case PICKUP: {
            handle((AVPickupTask) avTask);
            break;
        }
        case DROPOFF: {
            handle((AVDropoffTask) avTask);
            break;
        }
        case DRIVE: {
            handle((AVDriveTask) avTask);
            break;
        }
        case STAY: {
            handle((AVStayTask) avTask);
            break;
        }
        }

    }

    @Override
    public void handle(AVPickupTask avPickupTask) {
        // empty by design
    }

    @Override
    public void handle(AVDropoffTask avDropoffTask) {
        // empty by design
    }

    @Override
    public void handle(AVDriveTask avDriveTask) {
        // empty by design
    }

    @Override
    public void handle(AVStayTask avStayTask) {
        // empty by design
    }

}
