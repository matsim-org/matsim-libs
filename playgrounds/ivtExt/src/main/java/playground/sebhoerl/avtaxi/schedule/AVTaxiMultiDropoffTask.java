package playground.sebhoerl.avtaxi.schedule;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;
import org.matsim.contrib.taxi.data.TaxiRequest;

import playground.sebhoerl.avtaxi.schedule.AVTaxiMultiTask.AVTaxiMultiTaskType;

public class AVTaxiMultiDropoffTask extends StayTaskImpl implements AVTaxiTaskWithMultipleRequests, AVTaxiMultiTask {
	private final List<TaxiRequest> requests = new LinkedList<>();
	private Link link;
	
	public AVTaxiMultiDropoffTask(double beginTime, double endTime) {
		super(beginTime, endTime, null);
	}

	@Override
	public TaxiTaskType getTaxiTaskType() {
		return TaxiTaskType.DROPOFF;
	}

	@Override
	public AVTaxiMultiTaskType getMultiTaxiTaskType() {
		return AVTaxiMultiTaskType.MULTI_DROPOFF;
	}

	@Override
	public List<TaxiRequest> getRequests() {
		return requests;
	}

	@Override
	public void disconnectFromRequests() {
		for (TaxiRequest request : requests) request.setPickupTask(null);
	}

	@Override
	public void addRequest(TaxiRequest request) {
    	requests.add(request);
    	
    	if (link == null) {
    		link = request.getToLink();
    	} else if (link != request.getToLink()) {
    		// Must be the same for the same pickup task!
    		throw new IllegalStateException();
    	}
	}
	
    @Override
    public Link getLink() {
    	return link;
    }

    @Override
    protected String commonToString()
    {
        return "[" + getMultiTaxiTaskType().name() + "]" + super.commonToString();
    }
}
