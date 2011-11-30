package playground.michalm.vrp.taxi.taxicab;

import pl.poznan.put.vrp.dynamic.data.schedule.*;
import playground.michalm.dynamic.*;


class TaxiTaskActivity
    implements DynActivity
{
    private StayTask stayTask;
    private String activityType;


    TaxiTaskActivity(String activityType, StayTask stayTask)
    {
        this.activityType = activityType;
        this.stayTask = stayTask;
    }


    @Override
    public double getEndTime()
    {
        return stayTask.getEndTime();
    }


    @Override
    public String getActivityType()
    {
        return activityType;
    }


    static TaxiTaskActivity createServeActivity(ServeTask serveTask)
    {
        return new TaxiTaskActivity("ServeTask" + serveTask.getRequest().getId(), serveTask);
    }


    static TaxiTaskActivity createWaitActivity(WaitTask waitTask)
    {
        return new TaxiTaskActivity("WaitTask", waitTask);
    }
}
