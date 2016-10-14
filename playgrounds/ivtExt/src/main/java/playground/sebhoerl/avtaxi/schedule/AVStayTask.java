package playground.sebhoerl.avtaxi.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;
import org.matsim.contrib.taxi.schedule.TaxiTask;

public class AVStayTask extends StayTaskImpl implements AVTask {
    public AVStayTask(double beginTime, double endTime, Link link)
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
