package playground.dhosse.prt.task;

import java.util.List;

import org.matsim.contrib.dvrp.router.VrpPathWithTravelData;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.schedule.TaxiDropoffDriveTask;

public class MPDropoffDriveTask extends TaxiDropoffDriveTask {

	private List<TaxiRequest> requests;
	
	public MPDropoffDriveTask(VrpPathWithTravelData path, List<TaxiRequest> requests) {
		
		super(path, requests.get(0));
		this.requests = requests;
		
		for(TaxiRequest request : requests){
			request.setDropoffDriveTask(this);
		}
		
	}
	
	@Override
    public void removeFromRequest()
    {
		for(TaxiRequest request : this.requests){
			request.setDropoffDriveTask(null);
		}
    }


    @Override
    public TaxiTaskType getTaxiTaskType()
    {
        return TaxiTaskType.DROPOFF_DRIVE;
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
