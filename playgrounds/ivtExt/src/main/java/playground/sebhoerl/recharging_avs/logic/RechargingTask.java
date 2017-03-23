package playground.sebhoerl.recharging_avs.logic;

import org.matsim.api.core.v01.network.Link;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;

public class RechargingTask extends AVStayTask {
    public RechargingTask(double beginTime, double endTime, Link link)
    {
        super(beginTime, endTime, link, "AVRecharge");
    }

    @Override
    public AVTaskType getAVTaskType() {
        return AVTaskType.STAY;
    }
}
