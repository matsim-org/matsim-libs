package playground.sebhoerl.avtaxi.vrpagent;

import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.PassengerPickupActivity;
import org.matsim.contrib.dvrp.passenger.SinglePassengerDropoffActivity;
import org.matsim.contrib.dvrp.passenger.SinglePassengerPickupActivity;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.VrpActivity;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.taxi.schedule.TaxiDropoffTask;
import org.matsim.contrib.taxi.schedule.TaxiPickupTask;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

import playground.sebhoerl.avtaxi.passenger.AVMultiPassengerDropoffActivity;
import playground.sebhoerl.avtaxi.passenger.AVMultiPassengerPickupActivity;
import playground.sebhoerl.avtaxi.schedule.AVTaxiMultiDropoffTask;
import playground.sebhoerl.avtaxi.schedule.AVTaxiMultiPickupTask;
import playground.sebhoerl.avtaxi.schedule.AVTaxiMultiTask;

public class AVTaxiActionCreator implements VrpAgentLogic.DynActionCreator {
    public static final String MULTI_PICKUP_ACTIVITY_TYPE = "TaxiMultiPickup";
    public static final String MULTI_DROPOFF_ACTIVITY_TYPE = "TaxiMultiDropoff";

    private final PassengerEngine passengerEngine;
    private final VrpLegs.LegCreator legCreator;
    private final double pickupDuration;
    private final TaxiActionCreator delegate;

    public AVTaxiActionCreator(PassengerEngine passengerEngine, VrpLegs.LegCreator legCreator, double pickupDuration, TaxiActionCreator delegate)
    {
        this.passengerEngine = passengerEngine;
        this.legCreator = legCreator;
        this.pickupDuration = pickupDuration;
        this.delegate = delegate;
    }

    @Override
    public DynAction createAction(final Task task, double now)
    {
    	if (task instanceof AVTaxiMultiTask) {
    		switch (((AVTaxiMultiTask) task).getMultiTaxiTaskType()) {
    			case MULTI_PICKUP:
    				AVTaxiMultiPickupTask mpt = (AVTaxiMultiPickupTask) task;
    	    		return new AVMultiPassengerPickupActivity(passengerEngine, mpt, mpt.getRequests(),
    	                    pickupDuration, MULTI_PICKUP_ACTIVITY_TYPE);
    			case MULTI_DROPOFF:
    				AVTaxiMultiDropoffTask mdt = (AVTaxiMultiDropoffTask) task;
    				return new AVMultiPassengerDropoffActivity(passengerEngine, mdt, mdt.getRequests(),
    						MULTI_DROPOFF_ACTIVITY_TYPE);
    			case MULTI_OCCUPIED_DRIVE:
    				return legCreator.createLeg((DriveTask)task);
    	    	default:
    	    		throw new IllegalStateException();
    		}
    	} else {
    		return delegate.createAction(task, now);
        }
    }
}
