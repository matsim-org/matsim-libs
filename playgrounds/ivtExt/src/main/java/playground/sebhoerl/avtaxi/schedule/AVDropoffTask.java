package playground.sebhoerl.avtaxi.schedule;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public class AVDropoffTask extends StayTaskImpl implements AVTaskWithRequests, AVTask {
	private final Set<AVRequest> requests = new HashSet<>();
	
	public AVDropoffTask(double beginTime, double endTime, Link link) {
        super(beginTime, endTime, link);
	}

	public AVDropoffTask(double beginTime, double endTime, Link link, Collection<AVRequest> requests) {
		super(beginTime, endTime, link);

		this.requests.addAll(requests);
		for (AVRequest request : requests) request.setDropoffTask(this);
	}

	@Override
	public AVTaskType getAVTaskType() {
		return AVTaskType.DROPOFF;
	}

	@Override
	public Set<AVRequest> getRequests() {
        return requests;
	}

	@Override
	public void addRequest(AVRequest request) {
		requests.add(request);
		request.setDropoffTask(this);
	}

    @Override
    protected String commonToString()
    {
        return "[" + getAVTaskType().name() + "]" + super.commonToString();
    }
}
