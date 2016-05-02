package playground.dhosse.prt.passenger;

import java.util.*;

import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.vrpagent.VrpActivity;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.taxi.data.TaxiRequest;

public class NPersonsDropoffActivity extends VrpActivity {

	private final PassengerEngine passengerEngine;
    private final StayTask dropoffTask;
    private final List<TaxiRequest> requests;
	
	public NPersonsDropoffActivity(PassengerEngine passengerEngine,
			StayTask dropoffTask, List<TaxiRequest> requests) {
		super("PassengerDropoff", dropoffTask);
		this.passengerEngine = passengerEngine;
        this.dropoffTask = dropoffTask;
        this.requests = requests;
	}
	
	@Override
    public void finalizeAction(double now)
    {
        DynAgent driver = dropoffTask.getSchedule().getVehicle().getAgentLogic().getDynAgent();
        List<TaxiRequest> processed = new ArrayList<TaxiRequest>();
        for(TaxiRequest request : this.requests){
        	if(!processed.contains(request)){
        		passengerEngine.dropOffPassenger(driver, request, now);
        		processed.add(request);
        	}
        }
    }

}
