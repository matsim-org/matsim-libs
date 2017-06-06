// code by jph
package playground.clruch.utils;

import org.matsim.contrib.dvrp.schedule.Task;

import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVPickupTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.avtaxi.schedule.AVTask;
import playground.sebhoerl.avtaxi.schedule.AVTask.AVTaskType;

/**
 * An {@link AVTaskAdapter} is created using a {@link Task}, which is casted
 * to {@link AVTask} internally. The adapter then invokes the handling function
 * corresponding to one of the four possible {@link AVTaskType}s of the given task.  
 */
public class AVTaskAdapter implements AVTaskListener {

    public AVTaskAdapter(Task task) {
        final AVTask avTask = (AVTask) task;
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
