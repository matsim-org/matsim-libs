// code by jph
package playground.clruch.utils;

import ch.ethz.matsim.av.schedule.AVDriveTask;
import ch.ethz.matsim.av.schedule.AVDropoffTask;
import ch.ethz.matsim.av.schedule.AVPickupTask;
import ch.ethz.matsim.av.schedule.AVStayTask;

public interface AVTaskListener {
    void handle(AVPickupTask avPickupTask);

    void handle(AVDropoffTask avDropoffTask);

    void handle(AVDriveTask avDriveTask);

    void handle(AVStayTask avStayTask);

}
