package org.matsim.contrib.dvrp.dynagent;

import org.matsim.contrib.dynagent.DynActivity;

import pl.poznan.put.vrp.dynamic.data.schedule.*;


public class StayTaskActivity
    implements DynActivity
{
    private StayTask stayTask;
    private String activityType;


    public StayTaskActivity(String activityType, StayTask stayTask)
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


    public static StayTaskActivity createServeActivity(ServeTask serveTask)
    {
        return new StayTaskActivity("ServeTask" + serveTask.getRequest().getId(), serveTask);
    }


    public static StayTaskActivity createWaitActivity(WaitTask waitTask)
    {
        return new StayTaskActivity("WaitTask", waitTask);
    }
}
