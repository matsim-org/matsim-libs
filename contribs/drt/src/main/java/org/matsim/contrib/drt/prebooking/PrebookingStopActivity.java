package org.matsim.contrib.drt.prebooking;

import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Supplier;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.prebooking.abandon.AbandonVoter;
import org.matsim.contrib.drt.stops.PassengerStopDurationProvider;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.load.DvrpLoad;
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

	private final Queue<QueuedRequest> enterTimes = new PriorityQueue<>();
	private final Queue<QueuedRequest> leaveTimes = new PriorityQueue<>();

	private final IdSet<Request> enteredRequests = new IdSet<>(Request.class);

	private final IdSet<Request> registeredPickups = new IdSet<>(Request.class);
	private final IdMap<Request, AcceptedDrtRequest> expectedPickups = new IdMap<>(Request.class);

	private final PrebookingManager prebookingManager;
	private final PassengerHandler passengerHandler;

	private final PassengerStopDurationProvider stopDurationProvider;
	private final AbandonVoter abandonVoter;

	private final Supplier<Double> endTime;
	private DvrpLoad onboard;

	public PrebookingStopActivity(PassengerHandler passengerHandler, DynAgent driver, StayTask task,
			Map<Id<Request>, ? extends AcceptedDrtRequest> dropoffRequests,
			Map<Id<Request>, ? extends AcceptedDrtRequest> pickupRequests, String activityType,
			Supplier<Double> endTime, PassengerStopDurationProvider stopDurationProvider, DvrpVehicle vehicle,
			PrebookingManager prebookingManager, AbandonVoter abandonVoter, DvrpLoad initialOccupancy) {
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
		this.onboard = initialOccupancy;
	}

	@Override
	protected boolean isLastStep(double now) {
		boolean dropoffsReady = updateDropoffRequests(now);
		boolean pickupsReady = updatePickupRequests(now, false);
		return pickupsReady && dropoffsReady && now >= endTime.get();
	}

	@Override
	protected void beforeFirstStep(double now) {
		initDropoffRequests(now);
		updatePickupRequests(now, true);
	}

	private void initDropoffRequests(double now) {
		for (var request : dropoffRequests.values()) {
			double leaveTime = now + stopDurationProvider.calcDropoffDuration(vehicle, request.getRequest());
			leaveTimes.add(new QueuedRequest(request.getId(), leaveTime));
		}

		updateDropoffRequests(now);
	}

	private boolean updateDropoffRequests(double now) {
		while (!leaveTimes.isEmpty() && leaveTimes.peek().time <= now) {
			Id<Request> requestId = leaveTimes.poll().id;
			passengerHandler.dropOffPassengers(driver, requestId, now);
			prebookingManager.notifyDropoff(requestId);
			onboard = onboard.subtract(dropoffRequests.get(requestId).getLoad());
		}

		return leaveTimes.isEmpty();
	}

	private record QueuedRequest(Id<Request> id, double time) implements Comparable<QueuedRequest> {
		@Override
		public int compareTo(QueuedRequest o) {
			return Double.compare(this.time, o.time);
		}
	}

	private int cachedPickupRequestsHash = -1;

	private boolean updatePickupRequests(double now, boolean isFirstStep) {
		int pickupRequestsHash = pickupRequests.hashCode();

		// part 1: check if the pickup list has been updated dynamically

		if (isFirstStep || pickupRequestsHash != cachedPickupRequestsHash) {
			cachedPickupRequestsHash = pickupRequestsHash;

			// added requests
			for (AcceptedDrtRequest request : pickupRequests.values()) {
				if (!registeredPickups.contains(request.getId())) {
					// in the first step, this is a standard pickup request, later this is a request that has been added after the activity has been created
					expectedPickups.put(request.getId(), request);
					registeredPickups.add(request.getId());
				}
			}

			// removed requests (for instance via cancellation)
			var expectedIterator = expectedPickups.iterator();
			while (expectedIterator.hasNext()) {
				if (!pickupRequests.containsKey(expectedIterator.next().getId())) {
					// a request has been removed from the list of expected pickups
					expectedIterator.remove();
				}
			}
		}

		// part 2: handle the requests that we expect but which have not arrived yet

		var expectedIterator = expectedPickups.values().iterator();
		while (expectedIterator.hasNext()) {
			AcceptedDrtRequest request = expectedIterator.next();

			if (passengerHandler.notifyWaitForPassengers(this, this.driver, request.getId())) {
				// agent starts to enter
				queuePickup(request, now);
				expectedIterator.remove();
			} else if (now > request.getEarliestStartTime() && !isFirstStep) {
				if (abandonVoter.abandonRequest(now, vehicle, request)) {
					// abandon the request, but not in the first time step for the sake of event timing
					prebookingManager.abandon(request.getId());
					expectedIterator.remove();
				}
			}
		}

		// part 3: handle the requests that are currently entering the vehicle

		var enterIterator = enterTimes.iterator();

		// logic is as follows:
		// - let people enter in the order at which they arrived + their interaction time
		// - but in case there is no capacity (others still disembarking) they need to wait

		while (enterIterator.hasNext()) {
			var entry = enterIterator.next();
			DvrpLoad availableCapacity = vehicle.getCapacity().subtract(onboard);

			if (entry.time <= now) {
				DvrpLoad requiredCapacity = pickupRequests.get(entry.id).getLoad();

				if (requiredCapacity.fitsIn(availableCapacity) ) {
					// let agent enter now
					Verify.verify(passengerHandler.tryPickUpPassengers(this, driver, entry.id, now));
					enteredRequests.add(entry.id);
					onboard = onboard.add(requiredCapacity);
					enterIterator.remove();
				}
			} else {
				break;
			}
		}

		return expectedPickups.size() == 0 && pickupRequests.size() == enteredRequests.size();
	}

	private void queuePickup(AcceptedDrtRequest request, double now) {
		prebookingManager.notifyPickup(now, request);
		double enterTime = now + stopDurationProvider.calcPickupDuration(vehicle, request.getRequest());
		enterTimes.add(new QueuedRequest(request.getId(), enterTime));
	}

	@Override
	protected void simStep(double now) {
		// dynamics are handled in isLastStep -> updatePickupRequests
	}

	@Override
	public void notifyPassengersAreReadyForDeparture(List<MobsimPassengerAgent> passengers, double now) {
		var request = getRequestForPassengers(passengers.stream().map(Identifiable::getId).toList());
		if(expectedPickups.containsKey(request.getId())) {
			queuePickup(request, now);
			expectedPickups.remove(request.getId());
		}
	}

	private AcceptedDrtRequest getRequestForPassengers(List<Id<Person>> passengerIds) {
		return pickupRequests.values().stream().filter(
				r -> r.getPassengerIds().size() == passengerIds.size() && r.getPassengerIds().containsAll(passengerIds))
				.findAny().orElseThrow(() -> new IllegalArgumentException("I am waiting for different passengers!"));
	}
}
