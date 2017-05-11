package playground.sebhoerl.avtaxi.vrpagent;

<<<<<<< HEAD
import com.google.inject.Inject;

import org.matsim.contrib.dvrp.data.Vehicle;
=======
import javax.inject.Named;

>>>>>>> master
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.VrpActivity;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.contrib.dynagent.*;

import com.google.inject.Inject;

import playground.sebhoerl.avtaxi.passenger.AVPassengerDropoffActivity;
import playground.sebhoerl.avtaxi.passenger.AVPassengerPickupActivity;
import playground.sebhoerl.avtaxi.schedule.AVDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVPickupTask;
import playground.sebhoerl.avtaxi.schedule.AVStayTask;
import playground.sebhoerl.avtaxi.schedule.AVTask;

public class AVActionCreator implements VrpAgentLogic.DynActionCreator {
    public static final String PICKUP_ACTIVITY_TYPE = "AVPickup";
    public static final String DROPOFF_ACTIVITY_TYPE = "AVDropoff";
    public static final String STAY_ACTIVITY_TYPE = "AVStay";

    @Inject
    private PassengerEngine passengerEngine;

    @Inject
    private VrpLegs.LegCreator legCreator;

    @Inject
    @Named("pickupDuration")
    private Double pickupDuration;

    @Override
    public DynAction createAction(DynAgent dynAgent, Vehicle vehicle, double now)
    {
		Task task = vehicle.getSchedule().getCurrentTask(); 
    	if (task instanceof AVTask) {
    		switch (((AVTask) task).getAVTaskType()) {
    			case PICKUP:
    				AVPickupTask mpt = (AVPickupTask) task;
    	    		return new AVPassengerPickupActivity(passengerEngine, dynAgent, vehicle, mpt, mpt.getRequests(),
    	                    pickupDuration, PICKUP_ACTIVITY_TYPE);
                case DROPOFF:
    				AVDropoffTask mdt = (AVDropoffTask) task;
    				return new AVPassengerDropoffActivity(passengerEngine, dynAgent, vehicle, mdt, mdt.getRequests(),
                            DROPOFF_ACTIVITY_TYPE);
                case DRIVE:
    				return legCreator.createLeg(vehicle);
                case STAY:
                    return new VrpActivity(((AVStayTask)task).getName(), (StayTask) task);
    	    	default:
    	    		throw new IllegalStateException();
    		}
    	} else {
    		throw new IllegalArgumentException();
        }
    }
}
