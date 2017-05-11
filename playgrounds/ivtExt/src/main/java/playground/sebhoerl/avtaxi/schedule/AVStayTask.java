package playground.sebhoerl.avtaxi.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;

public class AVStayTask extends StayTaskImpl implements AVTask {
<<<<<<< HEAD
    public AVStayTask(double beginTime, double endTime, Link link, String name)
    {
        super(beginTime, endTime, link, name);
    }

    public AVStayTask(double beginTime, double endTime, Link link)
    {
        this(beginTime, endTime, link, "AVStay");
    }

    @Override
    protected String commonToString()
    {
        return "[" + getAVTaskType().toString() + "]" + super.commonToString();
=======
    public AVStayTask(double beginTime, double endTime, Link link) {
	super(beginTime, endTime, link);
    }

    @Override
    protected String commonToString() {
	return "[" + getAVTaskType().name() + "]" + super.commonToString();
>>>>>>> master
    }

    @Override
    public AVTaskType getAVTaskType() {
	return AVTaskType.STAY;
    }
}
