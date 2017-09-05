// code by jph
package playground.clruch.utils;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;

import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.schedule.AVDriveTask;
import playground.sebhoerl.avtaxi.schedule.AVDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVPickupTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;

public class AVDestination extends AVTaskAdapter {

    public static Link of(AVVehicle avVehicle) {
        return new AVDestination(Schedules.getLastTask(avVehicle.getSchedule())).link;
    }

    /**
     * DO NOT INITIALIZE THE LINK VARIABLE !!!
     */
    private Link link;

    private AVDestination(Task task) {
        super(task);
    }

    @Override
    public void handle(AVPickupTask avPickupTask) {
        GlobalAssert.that(false);
    }

    @Override
    public void handle(AVDropoffTask avDropoffTask) {
        GlobalAssert.that(false);
    }

    @Override
    public void handle(AVDriveTask avDriveTask) {
        GlobalAssert.that(false);
    }

    @Override
    public void handle(AVStayTask avStayTask) {
        link = avStayTask.getLink();
    }
}
