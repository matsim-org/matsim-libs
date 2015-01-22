package playground.dhosse.prt;

import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.SinglePassengerDropoffActivity;
import org.matsim.contrib.dvrp.passenger.SinglePassengerPickupActivity;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.VrpActivity;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.contrib.dynagent.DynAction;

import playground.dhosse.prt.passenger.NPersonsDropoffActivity;
import playground.dhosse.prt.passenger.NPersonsPickupActivity;
import playground.dhosse.prt.scheduler.NPersonsDropoffStayTask;
import playground.dhosse.prt.scheduler.NPersonsPickupStayTask;
import playground.michalm.taxi.TaxiActionCreator;
import playground.michalm.taxi.schedule.TaxiDropoffStayTask;
import playground.michalm.taxi.schedule.TaxiPickupStayTask;
import playground.michalm.taxi.schedule.TaxiTask;
import playground.michalm.taxi.schedule.TaxiWaitStayTask;

public class NPersonsActionCreator extends TaxiActionCreator{

	private PassengerEngine passengerEngine;
	private LegCreator legCreator;
	private double pickupDuration;
	
	public NPersonsActionCreator(PassengerEngine passengerEngine,
			LegCreator legCreator, double pickupDuration) {
		super(passengerEngine, legCreator, pickupDuration);
		this.passengerEngine = passengerEngine;
		this.legCreator = legCreator;
		this.pickupDuration = pickupDuration;
	}
	
	@Override
    public DynAction createAction(final Task task, double now)
    {
        TaxiTask tt = (TaxiTask)task;

        switch (tt.getTaxiTaskType()) {
            case PICKUP_DRIVE:
            case DROPOFF_DRIVE:
            case CRUISE_DRIVE:
                return legCreator.createLeg((DriveTask)task);

            case PICKUP_STAY:
                final NPersonsPickupStayTask pst = (NPersonsPickupStayTask)task;
                return new NPersonsPickupActivity(passengerEngine, pst, pst.getRequests(),
                        pickupDuration);

            case DROPOFF_STAY:
                final NPersonsDropoffStayTask dst = (NPersonsDropoffStayTask)task;
                return new NPersonsDropoffActivity(passengerEngine, dst, dst.getRequests());

            case WAIT_STAY:
                return new VrpActivity("Waiting", (TaxiWaitStayTask)task);

            default:
                throw new IllegalStateException();
        }
    }

}
