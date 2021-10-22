package org.matsim.contrib.drt.extension.eshifts.charging;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.BusStopActivity;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dynagent.DynActivity;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.evrp.ChargingActivity;
import org.matsim.contrib.evrp.ChargingTask;

import java.util.Map;

/**
 * @author nkuehnel / MOIA
 */
public class ChargingChangeoverActivity implements DynActivity {

    private final ChargingActivity chargingDelegate;
    private final BusStopActivity busStopDelegate;
	private final double endTime;

	public ChargingChangeoverActivity(ChargingTask chargingTask, PassengerHandler passengerHandler,
                                      DynAgent driver, StayTask task,
                                      Map<Id<Request>, ? extends PassengerRequest> dropoffRequests,
                                      Map<Id<Request>, ? extends PassengerRequest> pickupRequests) {
        chargingDelegate = new ChargingActivity(chargingTask);
        busStopDelegate = new BusStopActivity(passengerHandler, driver, task, dropoffRequests, pickupRequests, "");
		endTime = task.getEndTime();
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
