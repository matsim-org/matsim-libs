package org.matsim.contrib.drt.prebooking;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.Iterables;
import jakarta.annotation.Nullable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.passenger.DrtStopCandidates;
import org.matsim.contrib.drt.prebooking.unscheduler.RequestUnscheduler;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.HasModifiablePlan;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class manages prebooked requests. One instance of PrebookingManager
 * exists per mode. The entry point is PrebookingManager::prebook to which you
 * need to pass a person, a leg with the respective DRT mode, the
 * requested/expected earliest departure time, and the time at which the request
 * should be submitted / taken into account in the system.
 *
 * Preplanned requests can be submitted any time before the planned
 * departure/submission times.
 *
 * Internally, the prebooking manager will create a request identifier and
 * return the request once the agent actually wants to depart on the planned
 * leg. The link between a leg and a request is managed by inserting a special
 * attribute in the leg instance.
 *
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class PrebookingManager implements MobsimEngine, MobsimAfterSimStepListener, AdvanceRequestProvider,
	PassengerRequestScheduledEventHandler, PassengerRequestRejectedEventHandler, PersonStuckEventHandler {
	private final String mode;

	private final Network network;
	private final EventsManager eventsManager;

	private final VrpOptimizer optimizer;
	private final RequestUnscheduler unscheduler;
	private final boolean abortRejectedPrebookings;

	private final MobsimTimer mobsimTimer;

	private InternalInterface internalInterface;


	public PrebookingManager(String mode, Network network, PassengerRequestCreator requestCreator,
							 VrpOptimizer optimizer, MobsimTimer mobsimTimer, PassengerRequestValidator requestValidator,
							 EventsManager eventsManager, RequestUnscheduler unscheduler, boolean abortRejectedPrebookings) {
		this.network = network;
		this.mode = mode;
		this.requestCreator = requestCreator;
		this.optimizer = optimizer;
		this.requestAttribute = PREBOOKED_REQUEST_PREFIX + ":" + mode;
		this.requestValidator = requestValidator;
		this.mobsimTimer = mobsimTimer;
		this.eventsManager = eventsManager;
		this.unscheduler = unscheduler;
		this.abortRejectedPrebookings = abortRejectedPrebookings;
	}

	// Functionality for ID management

	private static final String PREBOOKED_REQUEST_PREFIX = "prebookedRequestId";
	private final AtomicInteger currentRequestIndex = new AtomicInteger(-1);
	private final String requestAttribute;

	private Id<Request> createRequestId() {
		return Id.create(mode + "_prebooked_" + currentRequestIndex.incrementAndGet(), Request.class);
	}

	public boolean isPrebookedRequest(Id<Request> requestId) {
		return requestId.toString().startsWith(mode + "_prebooked_");
	}

	public Id<Request> getRequestId(Leg leg) {
		String rawRequestId = (String) leg.getAttributes().getAttribute(requestAttribute);

		if (rawRequestId == null) {
			return null;
		}

		return Id.create(rawRequestId, Request.class);
	}

	// Event handling: We track events in parallel and process them later in
	// notifyMobsimAfterSimStep

	private final ConcurrentLinkedQueue<PassengerRequestScheduledEvent> scheduledEvents = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<Id<Request>> rejectedEventIds = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<Id<Person>> stuckPersonsIds = new ConcurrentLinkedQueue<>();

	@Override
	public void handleEvent(PassengerRequestScheduledEvent event) {
		scheduledEvents.add(event);
	}

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		rejectedEventIds.add(event.getRequestId());
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		stuckPersonsIds.add(event.getPersonId());
	}

	// Event handling: We don't want to process events in notifyMobsimAfterSimStep,
	// so we do it at the next time step
	private record RejectionItem(Id<Request> requestId, List<Id<Person>> personIds, String cause) {
	}

	public record PersonLeg(MobsimAgent agent, Leg leg) {}

	private final ConcurrentLinkedQueue<RejectionItem> rejections = new ConcurrentLinkedQueue<>();

	private void processRejection(PassengerRequest request, String cause) {
		rejections.add(new RejectionItem(request.getId(), request.getPassengerIds(), cause));
	}

	private void flushRejections(double now) {
		for (RejectionItem item : rejections) {
			eventsManager.processEvent(
				new PassengerRequestRejectedEvent(now, mode, item.requestId, item.personIds, item.cause));
		}

		rejections.clear();
	}

	// Booking functionality

	private final PassengerRequestCreator requestCreator;
	private final PassengerRequestValidator requestValidator;

	// collects new bookings that need to be submitted
	private final ConcurrentLinkedQueue<PassengerRequest> bookingQueue = new ConcurrentLinkedQueue<>();

	public void prebook(MobsimAgent agent, Leg leg, double earliestDepartureTime) {
		prebook(List.of(new PersonLeg(agent, leg)), earliestDepartureTime);
	}

	public void prebook(List<PersonLeg> personsLegs, double earliestDepartureTime) {
		for (PersonLeg personLeg : personsLegs) {
			Preconditions.checkArgument(personLeg.leg().getMode().equals(mode), "Invalid mode for this prebooking manager");
			Preconditions.checkState(!personLeg.agent().getState().equals(State.ABORT), "Cannot prebook aborted agent");
		}

		Id<Request> requestId = createRequestId();
		double now = mobsimTimer.getTimeOfDay();

		List<Id<Person>> personIds = personsLegs.stream().map(p -> p.agent().getId()).toList();
		eventsManager.processEvent(new PassengerRequestBookedEvent(now, mode, requestId, personIds));

		List<Route> routes = personsLegs.stream().map(PersonLeg::leg).map(Leg::getRoute).toList();
		Route representativeRoute = routes.get(0);
		PassengerRequest request = requestCreator.createRequest(requestId, personIds, routes,
			getLink(representativeRoute.getStartLinkId()), getLink(representativeRoute.getEndLinkId()), earliestDepartureTime,
			now);

		Optional<DrtStopCandidates> stopCandidates = Optional.empty();
		// only passengers with own access/egress walks can be moved to another stop; a group gets candidates only if
		// all its passengers have them (passengers without own walks, e.g. DRT companions, are not supported yet)
		List<Route> routesOfAll = personsLegs.stream().map(personLeg -> personLeg.leg().getRoute()).toList();
		List<StopCandidatePassenger> passengers = List.of();
		if (request instanceof DrtRequest drtRequest && PrebookingStopCandidates.haveStopCandidates(routesOfAll)
				&& personsLegs.stream().allMatch(PrebookingManager::hasAccessAndEgressWalks)) {
			passengers = personsLegs.stream().map(StopCandidatePassenger::new).toList();
			boolean pickupMayChange = passengers.stream().noneMatch(StopCandidatePassenger::hasLeftOrigin);
			stopCandidates = PrebookingStopCandidates.toDrtStopCandidates(routesOfAll, network, pickupMayChange);
			request = PrebookingStopCandidates.withStopCandidates(drtRequest, stopCandidates);
		}

		Set<String> violations = requestValidator.validateRequest(request);

		for (PersonLeg personLeg : personsLegs) {
			Plan plan = WithinDayAgentUtils.getModifiablePlan(personLeg.agent());
			int currentLegIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(personLeg.agent());
			int prebookingLegIndex = plan.getPlanElements().indexOf(personLeg.leg());

			if (prebookingLegIndex <= currentLegIndex) {
				violations = new HashSet<>(violations);
				violations.add("past leg"); // the leg for which the booking was made has already happened
			}
		}

		if (!violations.isEmpty()) {
			String cause = String.join(", ", violations);
			processRejection(request, cause);
		} else {
			for (PersonLeg personLeg : personsLegs) {
				personLeg.leg().getAttributes().putAttribute(requestAttribute, request.getId().toString());
			}
			if (stopCandidates.isPresent()) {
				PendingStopCandidates pending = new PendingStopCandidates(passengers,
						!stopCandidates.get().alternativeAccess().isEmpty());
				pendingStopCandidates.put(request.getId(), pending);
				// a booking at the activity end is submitted before the activity end of this step is processed: hold the
				// passengers right away, the regular holding after the step would come too late
				holdPassengersAboutToLeave(now, pending);
			}
			bookingQueue.add(request);
		}
	}

	private void processBookingQueue(double now) {
		for (PassengerRequest request : bookingQueue) {

			synchronized (optimizer) { // needed?
				optimizer.requestSubmitted(request);
			}

			requests.put(request.getId(), new RequestItem(request));
		}

		bookingQueue.clear();
	}

	private Link getLink(Id<Link> linkId) {
		return Preconditions.checkNotNull(network.getLinks().get(linkId),
			"Link id=%s does not exist in network for mode %s. Agent departs from a link that does not belong to that network?",
			linkId, mode);
	}

	// Interface with PassengerEngine

	@Override
	@Nullable
	public PassengerRequest retrieveRequest(MobsimAgent agent, Leg leg) {
		Preconditions.checkArgument(leg.getMode().equals(mode), "Invalid mode for this prebooking manager");

		Id<Request> requestId = getRequestId(leg);

		if (requestId == null) {
			return null;
		}

		RequestItem item = requests.get(requestId);

		if (item == null) {
			return null;
		}

		return item.request;
	}

	// Housekeeping of requests

	private IdMap<Request, RequestItem> requests = new IdMap<>(Request.class);

	private class RequestItem {
		final PassengerRequest request;

		Id<DvrpVehicle> vehicleId = null;
		boolean onboard = false;

		RequestItem(PassengerRequest request) {
			this.request = request;
		}
	}

	void notifyPickup(double now, AcceptedDrtRequest request) {
		RequestItem item = requests.get(request.getId());

		if (item != null) {
			// may be null, we treat all (also non-prebooked) requests here
			item.onboard = true;
		}
	}

	void notifyDropoff(Id<Request> requestId) {
		requests.remove(requestId);
	}

	private IdSet<Request> unscheduleUponVehicleAssignment = new IdSet<>(Request.class);

	private void processScheduledRequests(double now) {
		for (PassengerRequestScheduledEvent event : scheduledEvents) {
			RequestItem item = requests.get(event.getRequestId());

			if (item != null) {
				item.vehicleId = event.getVehicleId();
				materializeStopCandidates(now, event);
			}

			if (unscheduleUponVehicleAssignment.contains(event.getRequestId())) {
				// this is the case if a request has been rejected / canceled after submission
				// but before scheduling
				unscheduler.unscheduleRequest(now, event.getVehicleId(), event.getRequestId());
				unscheduleUponVehicleAssignment.remove(event.getRequestId());
			}
		}

		scheduledEvents.clear();
	}

	// Stop candidates: the insertion search may choose other stops than the routed ones

	/**
	 * A request with stop candidates that has not been scheduled yet. If it offers alternative pickup stops, its
	 * passengers are held in the activity before the trip until the request is scheduled (or rejected), so that their
	 * plans can still be rewritten to the chosen pickup stop. All passengers have own access/egress walks.
	 */
	private record PendingStopCandidates(List<StopCandidatePassenger> passengers, boolean alternativePickups) {
	}

	/**
	 * A passenger of a request with stop candidates: the activity before the trip (looked up once at booking) and, while
	 * held in it, the originally planned end and the end set for holding.
	 */
	private static final class StopCandidatePassenger {
		private final PersonLeg personLeg;
		private final Activity origin;
		private double originalEndTime = Double.NaN;
		private double heldUntil = Double.NaN;

		private StopCandidatePassenger(PersonLeg personLeg) {
			this.personLeg = personLeg;
			this.origin = PrebookingStopCandidates.originActivity(plan(personLeg), personLeg.leg());
		}

		private boolean isInOrigin() {
			return WithinDayAgentUtils.getCurrentPlanElement(personLeg.agent()) == origin;
		}

		private boolean hasLeftOrigin() {
			int originIndex = Iterables.indexOf(plan(personLeg).getPlanElements(), e -> e == origin);
			return WithinDayAgentUtils.getCurrentPlanElementIndex(personLeg.agent()) > originIndex;
		}

		private boolean isHeld() {
			return !Double.isNaN(heldUntil) && origin.getEndTime().isDefined()
					&& origin.getEndTime().seconds() == heldUntil;
		}
	}

	private final IdMap<Request, PendingStopCandidates> pendingStopCandidates = new IdMap<>(Request.class);

	private static Plan plan(PersonLeg personLeg) {
		return WithinDayAgentUtils.getModifiablePlan(personLeg.agent());
	}

	private static boolean hasAccessAndEgressWalks(PersonLeg personLeg) {
		return PrebookingStopCandidates.hasAccessAndEgressLegs(plan(personLeg), personLeg.leg());
	}

	private void materializeStopCandidates(double now, PassengerRequestScheduledEvent event) {
		PendingStopCandidates pending = pendingStopCandidates.remove(event.getRequestId());
		if (pending == null) {
			return;
		}
		Route referenceRoute = pending.passengers().getFirst().personLeg.leg().getRoute();
		Id<Link> pickupLinkId = event.getPickupLinkId().orElse(referenceRoute.getStartLinkId());
		Id<Link> dropoffLinkId = event.getDropoffLinkId().orElse(referenceRoute.getEndLinkId());

		for (StopCandidatePassenger passenger : pending.passengers()) {
			Leg leg = passenger.personLeg.leg();
			// passengers are held until scheduled, so the pickup can only differ if they are still at the origin
			Verify.verify(pickupLinkId.equals(leg.getRoute().getStartLinkId()) || !passenger.hasLeftOrigin(),
					"Passenger %s has left for the routed pickup stop, but %s was scheduled",
					passenger.personLeg.agent().getId(), pickupLinkId);
			if (PrebookingStopCandidates.materialize(plan(passenger.personLeg), leg, pickupLinkId, dropoffLinkId)) {
				WithinDayAgentUtils.resetCaches(passenger.personLeg.agent());
			}
		}
		release(now, pending);
	}

	/**
	 * Holds passengers of unscheduled requests with alternative pickups in their activity and releases those of
	 * requests that are no longer pending (rejected, canceled). Runs after each sim step, i.e. after the activity ends of
	 * the step have been processed; the end is therefore set two steps ahead, so that it is extended again before the
	 * passenger would leave.
	 */
	private void holdPassengersOfPendingRequests(double now) {
		Iterator<Map.Entry<Id<Request>, PendingStopCandidates>> iterator = pendingStopCandidates.entrySet().iterator();
		while (iterator.hasNext()) {
			var entry = iterator.next();
			PendingStopCandidates pending = entry.getValue();
			if (!requests.containsKey(entry.getKey())) {
				// rejected or canceled before being scheduled: the plan still leads to the routed stop
				iterator.remove();
				release(now, pending);
				continue;
			}
			holdPassengersAboutToLeave(now, pending);
		}
	}

	/**
	 * Holds the passengers of a pending request with alternative pickups who would leave their activity in this or the
	 * next time step. The end is set two steps ahead, so that it is extended again before the passenger would leave.
	 */
	private void holdPassengersAboutToLeave(double now, PendingStopCandidates pending) {
		if (!pending.alternativePickups()) {
			return;
		}
		double timeStep = mobsimTimer.getSimTimestepSize();
		for (StopCandidatePassenger passenger : pending.passengers()) {
			MobsimAgent agent = passenger.personLeg.agent();
			if (!passenger.isInOrigin() || agent.getActivityEndTime() > now + timeStep) {
				continue; // not yet in the activity, or not about to leave it
			}
			if (Double.isNaN(passenger.originalEndTime)) {
				passenger.originalEndTime = agent.getActivityEndTime();
			}
			passenger.heldUntil = now + 2 * timeStep;
			passenger.origin.setEndTime(passenger.heldUntil);
			WithinDayAgentUtils.resetCaches(agent);
			internalInterface.getMobsim().rescheduleActivityEnd(agent);
		}
	}

	/**
	 * Lets held passengers leave, at the earliest at their originally planned activity end. Passengers whose activity
	 * end was changed meanwhile (e.g. aborted after a rejection) are left alone.
	 */
	private void release(double now, PendingStopCandidates pending) {
		double timeStep = mobsimTimer.getSimTimestepSize();
		for (StopCandidatePassenger passenger : pending.passengers()) {
			if (!passenger.isInOrigin() || !passenger.isHeld()) {
				continue;
			}
			passenger.origin.setEndTime(Math.max(passenger.originalEndTime, now + timeStep));
			MobsimAgent agent = passenger.personLeg.agent();
			WithinDayAgentUtils.resetCaches(agent);
			internalInterface.getMobsim().rescheduleActivityEnd(agent);
		}
	}

	// Functionality for canceling requests

	private static final String CANCEL_REASON = "canceled";
	private final List<CancelItem> cancelQueue = new LinkedList<>();

	private void processCanceledRequests(double now) {
		for (CancelItem cancelItem : cancelQueue) {
			Id<Request> requestId = cancelItem.requestId;
			RequestItem item = requests.remove(requestId);

			if (item != null) { // might be null if abandoned before canceling
				Verify.verify(!item.onboard, "cannot cancel onboard request");

				// unschedule if requests is scheduled already
				if (item.vehicleId != null) {
					unscheduler.unscheduleRequest(now, item.vehicleId, requestId);
				} else {
					unscheduleUponVehicleAssignment.add(requestId);
				}

				String reason = CANCEL_REASON;

				if (cancelItem.reason != null) {
					reason = CANCEL_REASON + ":" + cancelItem.reason;
				}

				processRejection(item.request, reason);
			}
		}

		cancelQueue.clear();
	}

	public void cancel(Leg leg) {
		cancel(leg, null);
	}

	public void cancel(Leg leg, String reason) {
		Id<Request> requestId = getRequestId(leg);

		if (requestId != null) {
			cancel(requestId, reason);
		}
	}

	public void cancel(Id<Request> requestId, String reason) {
		cancelQueue.add(new CancelItem(requestId, reason));
	}

	public void cancel(Id<Request> requestId) {
		cancel(requestId, null);
	}

	private record CancelItem(Id<Request> requestId, String reason) {
	}

	// Functionality for abandoning requests

	private static final String ABANDONED_REASON = "abandoned by vehicle";
	private final ConcurrentLinkedQueue<Id<Request>> abandonQueue = new ConcurrentLinkedQueue<>();

	void abandon(Id<Request> requestId) {
		abandonQueue.add(requestId);
	}

	private void processAbandonedRequests(double now) {
		for (Id<Request> requestId : abandonQueue) {
			RequestItem item = Objects.requireNonNull(requests.remove(requestId));
			Verify.verify(!item.onboard, "cannot abandon request that is already onboard");

			// remove request from scheduled if already scheduled
			if (item.vehicleId != null) {
				unscheduler.unscheduleRequest(now, item.vehicleId, item.request.getId());
			} else {
				unscheduleUponVehicleAssignment.add(item.request.getId());
			}

			processRejection(item.request, ABANDONED_REASON);
		}

		abandonQueue.clear();
	}

	// Rejections

	private static final String ABORT_STUCK_REASON = " rejected prebooked request";
	private final IdSet<Person> stuckUponActivity = new IdSet<>(Person.class);

	private void processRejections(double now) {
		for (Id<Request> requestId : rejectedEventIds) {
			RequestItem item = requests.remove(requestId);

			if (item != null) {
				// should this fail gracefully?
				Verify.verify(!item.onboard, "cannot reject onboard request");

				// unschedule if already scheduled
				if (item.vehicleId != null) {
					unscheduler.unscheduleRequest(now, item.vehicleId, requestId);
				} else {
					unscheduleUponVehicleAssignment.add(requestId);
				}

				if (abortRejectedPrebookings) {
					for (Id<Person> passengerId : item.request.getPassengerIds()) {
						MobsimAgent agent = internalInterface.getMobsim().getAgents().get(passengerId);

						int index = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
						Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
						PlanElement planElement = plan.getPlanElements().get(index);

						if (planElement instanceof Activity) {
							// the agent is on an activity, let it get stuck further below
							stuckUponActivity.add(passengerId);
						} else {
							// the agent is on a leg, so we cannot make it stuck directly.
							// instead, we set the next activity to an infinite duration and
							// make the agent get stuck right after the activity starts

							while (index < plan.getPlanElements().size()) {
								if (plan.getPlanElements().get(index) instanceof Activity activity) {
									activity.setEndTime(Double.MAX_VALUE); // very long but not infinite (= regular last activity)
									activity.setMaximumDurationUndefined();
									stuckUponActivity.add(agent.getId());
									break;
								}

								index++;
							}
						}
					}
				}
			}
		}

		rejectedEventIds.clear();

		Iterator<Id<Person>> stuckIterator = stuckUponActivity.iterator();
		while (stuckIterator.hasNext()) {
			Id<Person> personId = stuckIterator.next();
			MobsimAgent agent = internalInterface.getMobsim().getAgents().get(personId);

			if (agent != null) {
				if (WithinDayAgentUtils.getCurrentPlanElement(agent) instanceof Activity activity) {
					abortAgent(agent, activity, now);
					stuckIterator.remove();
				}
			} else {
				// the agent no longer exists in the mobsim, we assume it is already been handled elsewhere. nkuehnel, march'25
				stuckIterator.remove();
			}
		}
	}

	private void abortAgent(MobsimAgent agent, Activity activity, double now) {
		// this would be the simple way, but then it looks like the agent is stuck
		// because of something related to the activity. however, we want to make
		//it clear that drt is the cause here

		// agent.setStateToAbort(now);
		// internalInterface.arrangeNextAgentState(agent);

		activity.setEndTime(Double.POSITIVE_INFINITY);
		activity.setMaximumDurationUndefined();

		((HasModifiablePlan) agent).resetCaches();
		internalInterface.getMobsim().rescheduleActivityEnd(agent);
		eventsManager.processEvent(new PersonStuckEvent(now, agent.getId(), agent.getCurrentLinkId(),
			null, mode + ABORT_STUCK_REASON));

		internalInterface.getMobsim().getAgentCounter().incLost();

		// will be called by the activity engine
		// internalInterface.getMobsim().getAgentCounter().decLiving();
	}

	// Stuck

	private void processStuckAgents(double now) {
		bookingQueue.removeIf(request -> stuckPersonsIds.containsAll(request.getPassengerIds()));

		for (RequestItem item : requests.values()) {
			if (stuckPersonsIds.containsAll(item.request.getPassengerIds())) {
				cancel(item.request.getId());
			}
		}

		stuckPersonsIds.clear();
	}

	// Engine code

	@Override
	public void beforeMobsim() {
		eventsManager.addHandler(this);
	}

	@Override
	public void doSimStep(double now) {
		// avoid method as it runs in parallel with events, only process rejections
		flushRejections(now);
	}

	@Override
	public void notifyMobsimAfterSimStep(@SuppressWarnings("rawtypes") MobsimAfterSimStepEvent e) {
		// here we are back in the main thread and all events
		// have been processed

		double now = mobsimTimer.getTimeOfDay();

		// first process scheduled events (this happened, we cannot change it)
		processScheduledRequests(now);

		// process rejected requests, potential problem if a person has entered the
		// vehicle in just this simstep, but also a rejection has been sent
		processRejections(now);

		// process stuck agents, they are added to the cancel queue
		processStuckAgents(now);

		// process abandoned requests (by vehicle), here we are sure that the person
		// cannot have entered in this iteration
		processAbandonedRequests(now);

		// process cancel requests, same situation as for rejections
		processCanceledRequests(now);

		// submit requests
		processBookingQueue(now);

		// hold passengers whose request may still change their pickup stop
		holdPassengersOfPendingRequests(now);
	}

	@Override
	public void afterMobsim() {
		eventsManager.removeHandler(this);
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}
}
