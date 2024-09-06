package org.matsim.contrib.drt.extension.operations.eshifts.charging;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.drt.passenger.DrtStopActivity;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dynagent.DynActivity;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.evrp.ChargingTask;

import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public class ChargingChangeoverActivity implements DynActivity {

    private final FixedTimeChargingActivity chargingDelegate;
    private final DrtStopActivity busStopDelegate;
	private final double endTime;

	public ChargingChangeoverActivity(ChargingTask chargingTask, PassengerHandler passengerHandler,
                                      DynAgent driver, StayTask task,
                                      Map<Id<Request>, ? extends AcceptedDrtRequest> dropoffRequests,
                                      Map<Id<Request>, ? extends AcceptedDrtRequest> pickupRequests) {
		endTime = task.getEndTime();
        chargingDelegate = new FixedTimeChargingActivity(chargingTask, endTime);
        busStopDelegate = new DrtStopActivity(passengerHandler, driver, () -> endTime, dropoffRequests, pickupRequests, "");
	}

    @Override
    public double getEndTime() {
        return Math.max(endTime, Math.max(chargingDelegate.getEndTime(), busStopDelegate.getEndTime()));
    }

    @Override
    public void doSimStep(double now) {
        chargingDelegate.doSimStep(now);
        busStopDelegate.doSimStep(now);
    }

    @Override
    public void finalizeAction(double now) {
    }

    @Override
    public String getActivityType() {
        return "Charging Changeover";
    }

}
