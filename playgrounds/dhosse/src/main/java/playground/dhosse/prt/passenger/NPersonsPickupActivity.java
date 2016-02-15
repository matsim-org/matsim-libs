package playground.dhosse.prt.passenger;

import java.util.List;

import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.PassengerPickupActivity;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

public class NPersonsPickupActivity implements PassengerPickupActivity {

	private final List<TaxiRequest> requests;
	private final PassengerEngine passengerEngine;
    private final StayTask pickupTask;
    private final double pickupDuration;
	
	private double endTime;
	private boolean passengerAboard = false;
	private double maxT0;
	
	public NPersonsPickupActivity(PassengerEngine passengerEngine,
			StayTask pickupTask, List<TaxiRequest> requests, double pickupDuration) {
		this.requests = requests;
		this.passengerEngine = passengerEngine;
		this.pickupTask = pickupTask;
		this.pickupDuration = pickupDuration;
		
		double now = pickupTask.getBeginTime();
		DynAgent driver = pickupTask.getSchedule().getVehicle().getAgentLogic().getDynAgent();
		int n = 0;
		maxT0 = 0;
		
		for(TaxiRequest request : requests){
			passengerAboard = passengerEngine.pickUpPassenger(this, driver, request, now);
			if (passengerAboard) n++;
			if(request.getT0() > maxT0) maxT0 = request.getT0();
		}
		
		passengerAboard = n == requests.size();
		
		if (passengerAboard) {
            endTime = now + n*pickupDuration;
        }
        else {
            //try to predict the end time
            endTime = Math.max(now, maxT0 + requests.size()*pickupDuration);
        }
		
	}
	
	@Override
    public void doSimStep(double now)
    {
		double end = maxT0;
		if(!passengerAboard){
			end = Math.max(now, maxT0 + this.requests.size()*pickupDuration);
			endTime = end;
		}
    }
	
	@Override
    public void notifyPassengerIsReadyForDeparture(MobsimPassengerAgent passenger, double now)
    {
		
		DynAgent driver = pickupTask.getSchedule().getVehicle().getAgentLogic().getDynAgent();
		
		endTime = now;
		
		for(TaxiRequest request : this.requests){
			
			if(passenger != request.getPassenger())
				continue;
			
			passengerAboard = passengerEngine.pickUpPassenger(this, driver, request, now);

	        if (!passengerAboard) {
	            throw new IllegalStateException("The passenger is not on the link!");
	        }

	        endTime += pickupDuration;

			
		}

    }

	@Override
	public String getActivityType() {
		return "PassengerPickup";
	}

	@Override
	public double getEndTime() {
		return this.endTime;
	}

	@Override
	public void finalizeAction(double now) {
		
	}

}
