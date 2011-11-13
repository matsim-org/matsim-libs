package playground.michalm.vrp.supply;

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.schedule.*;


class ScheduleActivityBehaviour
    extends VRPActivityBehaviour
{
    private Vehicle vehicle;


    private ScheduleActivityBehaviour(String activityType, Vehicle vehicle)
    {
        super(activityType);
        this.vehicle = vehicle;
    }


    @Override
    double getEndTime()
    {
        Schedule s = vehicle.getSchedule();

        switch (s.getStatus()) {
            case PLANNED:
                return s.getBeginTime() - 1;
            case UNPLANNED: // before schedule
                return vehicle.getT1();
            case COMPLETED: // after schedule
                return s.getBeginTime();// without "-1"
            default:
                throw new IllegalStateException();
        }
    }


    static ScheduleActivityBehaviour createWaitingBeforeSchedule(final Vehicle vehicle)
    {
        return new ScheduleActivityBehaviour("Before_" + vehicle.getId(), vehicle);
    }


    static ScheduleActivityBehaviour createWaitingAfterSchedule(final Vehicle vehicle)
    {
        return new ScheduleActivityBehaviour("After_" + vehicle.getId(), vehicle);
    }
}
