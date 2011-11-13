package playground.michalm.vrp.supply;

import playground.michalm.withinday.*;


abstract class VRPActivityBehaviour
    implements ActivityBehavior
{
    private String activityType;


    VRPActivityBehaviour(String activityType)
    {
        this.activityType = activityType;
    }


    @Override
    public void doSimStep(ActivityWorld world)
    {
        if (world.getTime() >= getEndTime()) {
            world.stopActivity();
        }
    }


    abstract double getEndTime();


    @Override
    public String getActivityType()
    {
        return activityType;
    }
}
