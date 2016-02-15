package playground.dhosse.prt.scheduler;

import java.util.List;

import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.schedule.TaxiDropoffTask;

public class NPersonsDropoffStayTask extends TaxiDropoffTask {

	List<TaxiRequest> requests;
	
	public NPersonsDropoffStayTask(double beginTime, double endTime,
			List<TaxiRequest> requests) {
		super(beginTime, endTime, requests.get(0));
		this.requests = requests;
		
		for(TaxiRequest request : requests){
			request.setDropoffTask(this);
		}
		
	}
	
	@Override
    public void removeFromRequest()
    {
		for(TaxiRequest request : this.requests){
			request.setDropoffTask(null);
		}
    }


    @Override
    public TaxiTaskType getTaxiTaskType()
    {
        return TaxiTaskType.DROPOFF;
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

}
