package playground.michalm.vrp.supply;

import playground.mzilske.withinday.*;


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
            onFinish(world);
        }
    }


    abstract double getEndTime();


    // just for overriding
    void onFinish(ActivityWorld world)
    {}


    @Override
    public String getActivityType()
    {
        return activityType;
    }
}
