package playground.dhosse.prt.scheduler;

import java.util.List;

import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.schedule.TaxiDriveWithPassengerTask;

public class NPersonsDropoffDriveTask extends TaxiDriveWithPassengerTask {

	private List<TaxiRequest> requests;
	
	public NPersonsDropoffDriveTask(VrpPathWithTravelData path, List<TaxiRequest> requests) {
		
		super(path, requests.get(0));
		this.requests = requests;
		
		for(TaxiRequest request : requests){
			request.setDriveWithPassengerTask(this);
		}
		
	}
	
	@Override
    public void removeFromRequest()
    {
		for(TaxiRequest request : this.requests){
			request.setDriveWithPassengerTask(null);
		}
    }


    @Override
    public TaxiTaskType getTaxiTaskType()
    {
        return TaxiTaskType.DRIVE_OCCUPIED;
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
