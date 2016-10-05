package playground.sebhoerl.avtaxi.schedule;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;
import org.matsim.contrib.taxi.data.TaxiRequest;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public class AVPickupTask extends StayTaskImpl implements AVTaskWithRequests, AVTask {
	final private Set<AVRequest> requests = new HashSet<>();
	
	public AVPickupTask(double beginTime, double endTime, Link link) {
        super(beginTime, endTime, link);
	}
    
    @Override
    public AVTaskType getAVTaskType() {
    	return AVTaskType.PICKUP;
    }
    
    @Override
    public void addRequest(AVRequest request) {
    	requests.add(request);
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
