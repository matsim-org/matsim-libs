package playground.clruch.dispatcher.core;

import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import playground.sebhoerl.avtaxi.data.AVVehicle;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;

public enum VehicleLinkPairs {
    ;

    /**
     * @param avVehicle
     *            in stay task
     * @param now
     * @return VehicleLinkPair of avVehicle with divertable time now
     * @throws Exception
     *             if vehicle is not in stay task
     */
    public static VehicleLinkPair ofStayVehicle(AVVehicle avVehicle, double now) {
        final Schedule schedule = avVehicle.getSchedule();
        Task task = schedule.getCurrentTask();
        AVStayTask avStayTask = (AVStayTask) task;
        LinkTimePair linkTimePair = new LinkTimePair(avStayTask.getLink(), now);
        return new VehicleLinkPair(avVehicle, linkTimePair, null);
    }
}
