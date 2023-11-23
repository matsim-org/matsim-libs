package org.matsim.contrib.drt.extension.operations.eshifts.charging;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.drt.passenger.DrtStopActivity;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.passenger.PassengerPickupActivity;
import org.matsim.contrib.dynagent.DynActivity;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.FirstLastSimStepDynActivity;
import org.matsim.contrib.evrp.ChargingActivity;
import org.matsim.contrib.evrp.ChargingTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftBreakTask;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * based on {@link DrtStopActivity} and {@link ChargingActivity}
 * @author nkuehnel / MOIA
 */
public class ChargingBreakActivity extends FirstLastSimStepDynActivity implements DynActivity, PassengerPickupActivity {

    public static final String CHARGING_BREAK = "Charging Break";
    private final FixedTimeChargingActivity chargingDelegate;
    private final Map<Id<Request>, ? extends AcceptedDrtRequest> dropoffRequests;
    private final Map<Id<Request>, ? extends AcceptedDrtRequest> pickupRequests;
    private final PassengerHandler passengerHandler;
    private final DynAgent driver;
	private final double endTime;

	private int passengersPickedUp = 0;

    public ChargingBreakActivity(ChargingTask chargingTask, PassengerHandler passengerHandler,
                                 DynAgent driver, ShiftBreakTask task,
                                 Map<Id<Request>, ? extends AcceptedDrtRequest> dropoffRequests,
                                 Map<Id<Request>, ? extends AcceptedDrtRequest> pickupRequests) {
        super(CHARGING_BREAK);
        chargingDelegate = new FixedTimeChargingActivity(chargingTask, task.getEndTime());
        this.dropoffRequests = dropoffRequests;
        this.pickupRequests = pickupRequests;
        this.passengerHandler = passengerHandler;
        this.driver = driver;
		endTime = task.getEndTime();
	}

    @Override
    protected boolean isLastStep(double now) {
        if(chargingDelegate.getEndTime() < now && now >= endTime) {
            for (var request : pickupRequests.values()) {
                if (passengerHandler.tryPickUpPassengers(this, driver, request.getId(), now)) {
                    passengersPickedUp++;
                }
            }
            return passengersPickedUp == pickupRequests.size();
        }
        return false;
    }

    @Override
    public void finalizeAction(double now) {
    }

    @Override
    public void notifyPassengersAreReadyForDeparture(List<MobsimPassengerAgent> passengers, double now) {
        if (!isLastStep(now)) {
            return;// pick up only at the end of stop activity
        }

        var request = getRequestForPassengers(passengers.stream().map(Identifiable::getId).toList());
        if (passengerHandler.tryPickUpPassengers(this, driver, request.getId(), now)) {
            passengersPickedUp++;
        } else {
            throw new IllegalStateException("The passenger is not on the link or not available for departure!");
        }
    }

    @Override
    protected void beforeFirstStep(double now) {
        // TODO probably we should simulate it more accurately (passenger by passenger, not all at once...)
        for (var request : dropoffRequests.values()) {
            passengerHandler.dropOffPassengers(driver, request.getId(), now);
        }
    }

    @Override
    protected void afterLastStep(double now) {

    }

    @Override
    protected void simStep(double now) {
        chargingDelegate.doSimStep(now);
    }

    private AcceptedDrtRequest getRequestForPassengers(List<Id<Person>> passengerIds) {
        return pickupRequests.values().stream()
                .filter(r -> r.getPassengerIds().size() == passengerIds.size() && r.getPassengerIds().containsAll(passengerIds))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("I am waiting for different passengers!"));
    }
}
