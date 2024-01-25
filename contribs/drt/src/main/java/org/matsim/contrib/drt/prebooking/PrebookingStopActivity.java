package org.matsim.contrib.drt.prebooking;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.prebooking.abandon.AbandonVoter;
import org.matsim.contrib.drt.stops.PassengerStopDurationProvider;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.passenger.PassengerPickupActivity;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.FirstLastSimStepDynActivity;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

import com.google.common.base.Verify;

/**
 * Modified version of DrtStopActivity which handles parallel mounting and
 * alighting of passengers according to individual pickup and dropoff times.
 *
 * @author Sebastian Hörl, IRT SystemX (sebhoerl)
 */
public class PrebookingStopActivity extends FirstLastSimStepDynActivity implements PassengerPickupActivity {
	private final DynAgent driver;
	private final DvrpVehicle vehicle;

	private final Map<Id<Request>, ? extends AcceptedDrtRequest> pickupRequests;
	private final Map<Id<Request>, ? extends AcceptedDrtRequest> dropoffRequests;

	private final IdMap<Request, Double> enterTimes = new IdMap<>(Request.class);
	private final IdMap<Request, Double> leaveTimes = new IdMap<>(Request.class);
	private final Set<Id<Request>> enteredRequests = new HashSet<>();

	private final PrebookingManager prebookingManager;
	private final PassengerHandler passengerHandler;

	private final PassengerStopDurationProvider stopDurationProvider;
	private final AbandonVoter abandonVoter;

	private final Supplier<Double> endTime;

	public PrebookingStopActivity(PassengerHandler passengerHandler, DynAgent driver, StayTask task,
			Map<Id<Request>, ? extends AcceptedDrtRequest> dropoffRequests,
			Map<Id<Request>, ? extends AcceptedDrtRequest> pickupRequests, String activityType,
			Supplier<Double> endTime, PassengerStopDurationProvider stopDurationProvider, DvrpVehicle vehicle,
			PrebookingManager prebookingManager, AbandonVoter abandonVoter) {
		super(activityType);
		this.passengerHandler = passengerHandler;
		this.driver = driver;
		this.dropoffRequests = dropoffRequests;
		this.pickupRequests = pickupRequests;
		this.stopDurationProvider = stopDurationProvider;
		this.vehicle = vehicle;
		this.prebookingManager = prebookingManager;
		this.abandonVoter = abandonVoter;
		this.endTime = endTime;
	}

	@Override
	protected boolean isLastStep(double now) {
		return updatePickupRequests(now) && leaveTimes.size() == 0 && now >= endTime.get();
	}

	@Override
	protected void beforeFirstStep(double now) {
		initDropoffRequests(now);
		updatePickupRequests(now);
	}

	private void initDropoffRequests(double now) {
		for (var request : dropoffRequests.values()) {
			double leaveTime = now + stopDurationProvider.calcDropoffDuration(vehicle, request.getRequest());
			leaveTimes.put(request.getId(), leaveTime);
		}

		processDropoffRequests(now);
	}

	private void processDropoffRequests(double now) {
		var iterator = leaveTimes.entrySet().iterator();

		while (iterator.hasNext()) {
			var entry = iterator.next();

			if (entry.getValue() <= now) { // Request should leave now
				passengerHandler.dropOffPassengers(driver, entry.getKey(), now);
				prebookingManager.notifyDropoff(entry.getKey());
				iterator.remove();
			}
		}
	}

	private boolean updatePickupRequests(double now) {
		var pickupIterator = pickupRequests.values().iterator();

		while (pickupIterator.hasNext()) {
			var request = pickupIterator.next();

			if (!enteredRequests.contains(request.getId()) && !enterTimes.containsKey(request.getId())) {
				// this is a new request that has been added after the activity has been created
				// or that had not arrived yet

				if (passengerHandler.notifyWaitForPassengers(this, this.driver, request.getId())) {
					// agent starts to enter
					queuePickup(request, now);
				} else if (now > request.getEarliestStartTime()) {
					if (abandonVoter.abandonRequest(now, vehicle, request)) {
						prebookingManager.abandon(request.getId());
					}
				}
			}
		}

		var enterIterator = enterTimes.entrySet().iterator();

		while (enterIterator.hasNext()) {
			var entry = enterIterator.next();

			if (entry.getValue() <= now) {
				// let agent enter now
				Verify.verify(passengerHandler.tryPickUpPassengers(this, driver, entry.getKey(), now));
				enteredRequests.add(entry.getKey());
				enterIterator.remove();
			}
		}

		return enterTimes.size() == 0 && pickupRequests.size() == enteredRequests.size();
	}

	private void queuePickup(AcceptedDrtRequest request, double now) {
		prebookingManager.notifyPickup(now, request);
		double enterTime = now + stopDurationProvider.calcPickupDuration(vehicle, request.getRequest());
		enterTimes.put(request.getId(), enterTime);
	}

	@Override
	protected void simStep(double now) {
		// dynamics are handled in isLastStep -> updatePickupRequests
	}

	@Override
	public void notifyPassengersAreReadyForDeparture(List<MobsimPassengerAgent> passengers, double now) {
		var request = getRequestForPassengers(passengers.stream().map(Identifiable::getId).toList());
		queuePickup(request, now);
	}


	private AcceptedDrtRequest getRequestForPassengers(List<Id<Person>> passengerIds) {
		return pickupRequests.values()
				.stream()
				.filter(r -> r.getPassengerIds().size() == passengerIds.size() && r.getPassengerIds().containsAll(passengerIds))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("I am waiting for different passengers!"));
	}
}
