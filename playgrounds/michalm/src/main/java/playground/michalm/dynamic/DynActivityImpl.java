package playground.michalm.dynamic;

public class DynActivityImpl
    implements DynActivity
{
    private String activityType;
    private double endTime;


    public DynActivityImpl(String activityType, double endTime)
    {
        this.activityType = activityType;
        this.endTime = endTime;
    }


    @Override
    public String getActivityType()
    {
        return activityType;
    }


    @Override
    public double getEndTime()
    {
        return endTime;
    }
}
