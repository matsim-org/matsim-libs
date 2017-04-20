package playground.sebhoerl.avtaxi.passenger;

import java.util.Set;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.vrpagent.VrpActivity;
import org.matsim.contrib.dynagent.DynAgent;

public class AVPassengerDropoffActivity extends VrpActivity {
    private final PassengerEngine passengerEngine;
    private final DynAgent driver;
    private final Set<? extends PassengerRequest> requests;
    
    public AVPassengerDropoffActivity(PassengerEngine passengerEngine, DynAgent driver, Vehicle vehicle, StayTask dropoffTask,
                                      Set<? extends PassengerRequest> requests, String activityType)
    {
        super(activityType, dropoffTask);

        this.passengerEngine = passengerEngine;
        this.driver = driver;
        this.requests = requests;
        
        if (requests.size() > vehicle.getCapacity()) {
        	// Number of requests exceeds number of seats
        	throw new IllegalStateException();
        }
    }


    @Override
    public void finalizeAction(double now)
    {
        for (PassengerRequest request : requests) {
        	passengerEngine.dropOffPassenger(driver, request, now);
        }
    }
}
