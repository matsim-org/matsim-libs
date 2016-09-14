package playground.sebhoerl.avtaxi.passenger;

import java.util.List;

import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.vrpagent.VrpActivity;
import org.matsim.contrib.dynagent.DynAgent;

public class AVMultiPassengerDropoffActivity extends VrpActivity {
    private final PassengerEngine passengerEngine;
    private final StayTask dropoffTask;
    private final List<? extends PassengerRequest> requests;
    
    public AVMultiPassengerDropoffActivity(PassengerEngine passengerEngine, StayTask dropoffTask,
    		List<? extends PassengerRequest> requests, String activityType)
    {
        super(activityType, dropoffTask);

        this.passengerEngine = passengerEngine;
        this.dropoffTask = dropoffTask;
        this.requests = requests;
        
        if (requests.size() > dropoffTask.getSchedule().getVehicle().getCapacity()) {
        	// Number of requests exceeds number of seats
        	throw new IllegalStateException();
        }
    }


    @Override
    public void finalizeAction(double now)
    {
        DynAgent driver = dropoffTask.getSchedule().getVehicle().getAgentLogic().getDynAgent();
        
        for (PassengerRequest request : requests) {
        	passengerEngine.dropOffPassenger(driver, request, now);
        }
    }
}
