package playground.sebhoerl.avtaxi.vrpagent;

import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;

import playground.sebhoerl.avtaxi.passenger.AVPassengerDropoffActivity;
import playground.sebhoerl.avtaxi.passenger.AVPassengerPickupActivity;
import playground.sebhoerl.avtaxi.schedule.AVDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVPickupTask;
import playground.sebhoerl.avtaxi.schedule.AVTask;

public class AVActionCreator implements VrpAgentLogic.DynActionCreator {
    public static final String PICKUP_ACTIVITY_TYPE = "AVPickup";
    public static final String DROPOFF_ACTIVITY_TYPE = "AVDropoff";

    private final PassengerEngine passengerEngine;
    private final VrpLegs.LegCreator legCreator;
    private final double pickupDuration;
    private final TaxiActionCreator delegate;

    public AVActionCreator(PassengerEngine passengerEngine, VrpLegs.LegCreator legCreator, double pickupDuration, TaxiActionCreator delegate)
    {
        this.passengerEngine = passengerEngine;
        this.legCreator = legCreator;
        this.pickupDuration = pickupDuration;
        this.delegate = delegate;
    }

    @Override
    public DynAction createAction(final Task task, double now)
    {
    	if (task instanceof AVTask) {
    		switch (((AVTask) task).getAVTaskType()) {
    			case PICKUP:
    				AVPickupTask mpt = (AVPickupTask) task;
    	    		return new AVPassengerPickupActivity(passengerEngine, mpt, mpt.getRequests(),
    	                    pickupDuration, PICKUP_ACTIVITY_TYPE);
                case DROPOFF:
    				AVDropoffTask mdt = (AVDropoffTask) task;
    				return new AVPassengerDropoffActivity(passengerEngine, mdt, mdt.getRequests(),
                            DROPOFF_ACTIVITY_TYPE);
                case DRIVE:
    				return legCreator.createLeg((DriveTask)task);
    	    	default:
    	    		throw new IllegalStateException();
    		}
    	} else {
    		throw new IllegalArgumentException();
        }
    }
}
