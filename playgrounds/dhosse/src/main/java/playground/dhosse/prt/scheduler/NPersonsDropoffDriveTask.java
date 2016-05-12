package playground.dhosse.prt.scheduler;

import java.util.List;

import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.schedule.TaxiOccupiedDriveTask;

public class NPersonsDropoffDriveTask extends TaxiOccupiedDriveTask {

	private List<TaxiRequest> requests;
	
	public NPersonsDropoffDriveTask(VrpPathWithTravelData path, List<TaxiRequest> requests) {
		
		super(path, requests.get(0));
		this.requests = requests;
		
		for(TaxiRequest request : requests){
			request.setOccupiedDriveTask(this);
		}
		
	}
	
	@Override
    public void removeFromRequest()
    {
		for(TaxiRequest request : this.requests){
			request.setOccupiedDriveTask(null);
		}
    }


    @Override
    public TaxiTaskType getTaxiTaskType()
    {
        return TaxiTaskType.OCCUPIED_DRIVE;
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
