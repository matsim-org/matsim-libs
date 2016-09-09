package playground.sebhoerl.avtaxi.schedule;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;
import org.matsim.contrib.dvrp.schedule.Tasks;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.schedule.TaxiTaskWithRequest;

import playground.sebhoerl.avtaxi.schedule.AVTaxiMultiTask.AVTaxiMultiTaskType;

import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;

public class AVTaxiMultiPickupTask extends StayTaskImpl implements AVTaxiTaskWithMultipleRequests, AVTaxiMultiTask {
	private final List<TaxiRequest> requests = new LinkedList<>();
	private Link link;
	
	public AVTaxiMultiPickupTask(double beginTime, double endTime) {
		super(beginTime, endTime, null);
	}
	
    @Override
    public void disconnectFromRequests()
    {
    	for (TaxiRequest request : requests) request.setPickupTask(null);
    }
    
    @Override
    public TaxiTaskType getTaxiTaskType()
    {
        return TaxiTaskType.PICKUP;
    }
    
    @Override
    public AVTaxiMultiTaskType getMultiTaxiTaskType() {
    	return AVTaxiMultiTaskType.MULTI_PICKUP;
    }
    
    @Override
    public void addRequest(TaxiRequest request) {
    	requests.add(request);
    	
    	if (link == null) {
    		link = request.getFromLink();
    	} else if (link != request.getFromLink()) {
    		// Must be the same for the same pickup task!
    		throw new IllegalStateException();
    	}
    }

    @Override
    public List<TaxiRequest> getRequests() {
        return requests;
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
