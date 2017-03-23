package playground.sebhoerl.recharging_avs;

import org.matsim.api.core.v01.network.Link;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;

public class RechargingTask extends AVStayTask {
    public RechargingTask(double beginTime, double endTime, Link link)
    {
        super(beginTime, endTime, link);
    }

    @Override
    protected String commonToString()
    {
        return "[" + getAVTaskType().name() + "]" + super.commonToString();
    }

    @Override
    public AVTaskType getAVTaskType() {
        return AVTaskType.STAY;
    }
}
