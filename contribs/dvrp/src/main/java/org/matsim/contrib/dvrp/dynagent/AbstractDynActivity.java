package org.matsim.contrib.dvrp.dynagent;

public abstract class AbstractDynActivity
    implements DynActivity
{
    private final String activityType;


    public AbstractDynActivity(String activityType)
    {
        this.activityType = activityType;
    }


    @Override
    public String getActivityType()
    {
        return activityType;
    }
}
