package playground.dhosse.prt.passenger;

import java.util.*;

import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.vrpagent.VrpActivity;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.taxi.data.TaxiRequest;

public class NPersonsDropoffActivity extends VrpActivity {

	private final PassengerEngine passengerEngine;
    private final DynAgent driver;
    private final List<TaxiRequest> requests;
	
	public NPersonsDropoffActivity(PassengerEngine passengerEngine, DynAgent driver,
			StayTask dropoffTask, List<TaxiRequest> requests) {
		super("PassengerDropoff", dropoffTask);
		this.passengerEngine = passengerEngine;
        this.driver = driver;
        this.requests = requests;
	}
	
	@Override
    public void finalizeAction(double now)
    {
        List<TaxiRequest> processed = new ArrayList<TaxiRequest>();
        for(TaxiRequest request : this.requests){
        	if(!processed.contains(request)){
        		passengerEngine.dropOffPassenger(driver, request, now);
        		processed.add(request);
        	}
        }
    }

}
