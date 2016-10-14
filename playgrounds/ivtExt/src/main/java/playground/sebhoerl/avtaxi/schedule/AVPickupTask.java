package playground.sebhoerl.avtaxi.schedule;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public class AVPickupTask extends StayTaskImpl implements AVTaskWithRequests, AVTask {
	final private Set<AVRequest> requests = new HashSet<>();
	
	public AVPickupTask(double beginTime, double endTime, Link link) {
        super(beginTime, endTime, link);
	}

    public AVPickupTask(double beginTime, double endTime, Link link, Collection<AVRequest> requests) {
        this(beginTime, endTime, link);

        this.requests.addAll(requests);
        for (AVRequest request : requests) request.setPickupTask(this);
    }
    
    @Override
    public AVTaskType getAVTaskType() {
    	return AVTaskType.PICKUP;
    }
    
    @Override
    public void addRequest(AVRequest request) {
        requests.add(request);
        request.setPickupTask(this);
    }

    @Override
    public Set<AVRequest> getRequests() {
        return requests;
    }

    @Override
    protected String commonToString()
    {
        return "[" + getAVTaskType().name() + "]" + super.commonToString();
    }
}
