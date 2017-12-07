// code by jph
package playground.clruch.utils;

import org.matsim.contrib.dvrp.schedule.Task;

import ch.ethz.matsim.av.schedule.AVDriveTask;
import ch.ethz.matsim.av.schedule.AVDropoffTask;
import ch.ethz.matsim.av.schedule.AVPickupTask;
import ch.ethz.matsim.av.schedule.AVStayTask;
import ch.ethz.matsim.av.schedule.AVTask;
import ch.ethz.matsim.av.schedule.AVTask.AVTaskType;

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
