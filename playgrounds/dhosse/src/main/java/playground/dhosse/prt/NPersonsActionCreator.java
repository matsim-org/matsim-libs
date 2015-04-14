package playground.dhosse.prt;

import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dvrp.vrpagent.VrpLegs.LegCreator;
import org.matsim.contrib.dynagent.DynAction;

import playground.dhosse.prt.passenger.*;
import playground.dhosse.prt.scheduler.*;
import playground.michalm.taxi.TaxiActionCreator;
import playground.michalm.taxi.schedule.*;

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
            case DRIVE:
            case DRIVE_WITH_PASSENGER:
                return legCreator.createLeg((DriveTask)task);

            case PICKUP:
                final NPersonsPickupStayTask pst = (NPersonsPickupStayTask)task;
                return new NPersonsPickupActivity(passengerEngine, pst, pst.getRequests(),
                        pickupDuration);

            case DROPOFF:
                final NPersonsDropoffStayTask dst = (NPersonsDropoffStayTask)task;
                return new NPersonsDropoffActivity(passengerEngine, dst, dst.getRequests());

            case STAY:
                return new VrpActivity("Waiting", (TaxiStayTask)task);

            default:
                throw new IllegalStateException();
        }
    }

}
