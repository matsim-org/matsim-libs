// code by jph
package playground.clruch.utils;

import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVPickupTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;

public interface AVTaskListener {
    void handle(AVPickupTask avPickupTask);

    void handle(AVDropoffTask avDropoffTask);

    void handle(AVDriveTask avDriveTask);

    void handle(AVStayTask avStayTask);

}
