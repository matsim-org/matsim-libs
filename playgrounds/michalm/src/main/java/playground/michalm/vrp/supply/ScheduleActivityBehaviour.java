package playground.michalm.vrp.supply;

import pl.poznan.put.vrp.dynamic.data.schedule.*;


class ScheduleActivityBehaviour
    extends VRPActivityBehaviour
{
    private Schedule schedule;


    private ScheduleActivityBehaviour(String activityType, Schedule schedule)
    {
        super(activityType);
        this.schedule = schedule;
    }


    @Override
    double getEndTime()
    {
        switch (schedule.getStatus()) {
            case PLANNED:
                return schedule.getBeginTime() - 1;
            case UNPLANNED: // before schedule
                return schedule.getVehicle().t1;
            case COMPLETED: // after schedule
                return schedule.getBeginTime();//without "-1"
            default:
                throw new IllegalStateException();
        }
    }


    static ScheduleActivityBehaviour createWaitingBeforeSchedule(final Schedule schedule)
    {
        return new ScheduleActivityBehaviour("Before_" + schedule.getId(), schedule);
    }


    static ScheduleActivityBehaviour createWaitingAfterSchedule(final Schedule schedule)
    {
        return new ScheduleActivityBehaviour("After_" + schedule.getId(), schedule);
    }
}
