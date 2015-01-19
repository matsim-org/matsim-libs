package playground.dhosse.prt.task;

import java.util.List;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.schedule.TaxiDropoffStayTask;

public class NPersonsDropoffStayTask extends TaxiDropoffStayTask {

	List<TaxiRequest> requests;
	
	public NPersonsDropoffStayTask(double beginTime, double endTime,
			List<TaxiRequest> requests) {
		super(beginTime, endTime, requests.get(0));
		this.requests = requests;
		
		for(TaxiRequest request : requests){
			request.setDropoffStayTask(this);
		}
		
	}
	
	@Override
    public void removeFromRequest()
    {
		for(TaxiRequest request : this.requests){
			request.setDropoffStayTask(null);
		}
    }


    @Override
    public TaxiTaskType getTaxiTaskType()
    {
        return TaxiTaskType.DROPOFF_STAY;
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
