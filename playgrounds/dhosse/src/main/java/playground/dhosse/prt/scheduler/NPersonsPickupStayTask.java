package playground.dhosse.prt.scheduler;

import java.util.List;

import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.schedule.TaxiPickupTask;

public class NPersonsPickupStayTask extends TaxiPickupTask {

	List<TaxiRequest> requests;
	
	public NPersonsPickupStayTask(double beginTime, double endTime,
			List<TaxiRequest> requests) {
		super(beginTime, endTime, requests.get(0));
		
		this.requests = requests;
		
		for(TaxiRequest request : requests){
			request.setPickupTask(this);
		}
		
	}
	
	@Override
    public void removeFromRequest()
    {
		for(TaxiRequest request : this.requests){
			request.setPickupTask(null);
		}
    }


    @Override
    public TaxiTaskType getTaxiTaskType()
    {
        return TaxiTaskType.PICKUP;
    }


    public TaxiRequest getRequest()
    {
        return this.requests.get(0);
    }
    
    public List<TaxiRequest> getRequests()
    {
        return requests;
    }


    @Override
    protected String commonToString()
    {
        return "[" + getTaxiTaskType().name() + "]" + super.commonToString();
    }
    
    public void appendRequest(TaxiRequest request, double pickupDuration){
		this.requests.add(request);
		request.setPickupTask(this);
		super.setEndTime(getEndTime() + pickupDuration);
	}

}
