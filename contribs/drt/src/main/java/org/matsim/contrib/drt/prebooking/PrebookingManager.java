package org.matsim.contrib.drt.prebooking;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.prebooking.unscheduler.RequestUnscheduler;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.AdvanceRequestProvider;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

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
public class PrebookingManager implements MobsimEngine, AdvanceRequestProvider, PassengerRequestScheduledEventHandler,
		PassengerRequestRejectedEventHandler, PersonStuckEventHandler {
	private final String mode;

	private final Network network;
	private final EventsManager eventsManager;

	private final VrpOptimizer optimizer;
	private final RequestUnscheduler unscheduler;

	public PrebookingManager(String mode, Network network, PassengerRequestCreator requestCreator,
			VrpOptimizer optimizer, PassengerRequestValidator requestValidator, EventsManager eventsManager,
			RequestUnscheduler unscheduler) {
		this.network = network;
		this.mode = mode;
		this.requestCreator = requestCreator;
		this.optimizer = optimizer;
		this.requestAttribute = PREBOOKED_REQUEST_PREFIX + ":" + mode;
		this.requestValidator = requestValidator;
		this.eventsManager = eventsManager;
		this.unscheduler = unscheduler;
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

	// Booking functionality

	private final PassengerRequestCreator requestCreator;
	private final PassengerRequestValidator requestValidator;
	private final List<QueueItem> queue = new LinkedList<>();

	public void prebook(MobsimAgent person, Leg leg, double earliestDepartureTime) {
		Preconditions.checkArgument(leg.getMode().equals(mode), "Invalid mode for this prebooking manager");

		synchronized (queue) {
			queue.add(new QueueItem(person, leg, earliestDepartureTime));
		}
	}

	private void processQueue(double now) {
		synchronized (queue) {
			for (QueueItem item : queue) {
				Preconditions.checkState(!item.person.getState().equals(State.ABORT), "Cannot prebook aborted agent");

				Id<Request> requestId = createRequestId();

				eventsManager.processEvent(new PassengerRequestBookedEvent(now, mode, requestId, item.person.getId()));

				PassengerRequest request = requestCreator.createRequest(requestId, item.person.getId(),
						item.leg.getRoute(), getLink(item.leg.getRoute().getStartLinkId()),
						getLink(item.leg.getRoute().getEndLinkId()), item.earliestDepartureTime, now);

				Set<String> violations = requestValidator.validateRequest(request);

				Plan plan = WithinDayAgentUtils.getModifiablePlan(item.person);
				int currentLegIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(item.person);
				int prebookingLegIndex = plan.getPlanElements().indexOf(item.leg);

				if (prebookingLegIndex <= currentLegIndex) {
					violations = new HashSet<>(violations);
					violations.add("past leg"); // the leg for which the booking was made has already happened
				}

				if (!violations.isEmpty()) {
					String cause = String.join(", ", violations);
					eventsManager.processEvent(new PassengerRequestRejectedEvent(now, mode, request.getId(),
							request.getPassengerId(), cause));
				} else {
					synchronized (optimizer) {
						optimizer.requestSubmitted(request);
					}

					item.leg.getAttributes().putAttribute(requestAttribute, request.getId().toString());
					requests.put(requestId, new RequestItem(request));
				}
			}

			queue.clear();
		}
	}

	private Link getLink(Id<Link> linkId) {
		return Preconditions.checkNotNull(network.getLinks().get(linkId),
				"Link id=%s does not exist in network for mode %s. Agent departs from a link that does not belong to that network?",
				linkId, mode);
	}

	private record QueueItem(MobsimAgent person, Leg leg, double earliestDepartureTime) {
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

		RequestItem item = requests.remove(requestId);

		if (item == null) {
			return null;
		}

		return item.request;
	}

	// Housekeeping of requests

	private IdMap<Request, RequestItem> requests = new IdMap<>(Request.class);
	private IdSet<Request> unscheduleUponVehicleAssignment = new IdSet<>(Request.class);

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

	@Override
	public void handleEvent(PassengerRequestScheduledEvent event) {
		RequestItem item = requests.get(event.getRequestId());

		if (item != null) {
			item.vehicleId = event.getVehicleId();
		}

		if (unscheduleUponVehicleAssignment.contains(event.getRequestId())) {
			// this is the case if a request has been rejected / canceled after submission
			// but before scheduling
			unscheduler.unscheduleRequest(event.getTime(), event.getVehicleId(), event.getRequestId());
			unscheduleUponVehicleAssignment.remove(event.getRequestId());
		}
	}

	// Functionality for canceling requests

	private static final String CANCEL_REASON = "canceled";
	private final List<CancelItem> cancelQueue = new LinkedList<>();

	private void processCanceledRequests(double now) {
		synchronized (cancelQueue) {
			for (CancelItem cancelItem : cancelQueue) {
				Id<Request> requestId = cancelItem.requestId;
				RequestItem item = requests.remove(requestId);

				if (item != null) { // might be null if abandoned before canceling
					Verify.verify(!item.onboard);

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

					eventsManager.processEvent(new PassengerRequestRejectedEvent(now, mode, requestId,
							item.request.getPassengerId(), reason));
				}
			}

			cancelQueue.clear();
		}
	}

	public boolean cancel(Leg leg) {
		return cancel(leg, null);
	}

	public boolean cancel(Leg leg, String reason) {
		Id<Request> requestId = getRequestId(leg);

		if (requestId != null) {
			return cancel(requestId);
		}

		return false;
	}

	public boolean cancel(Id<Request> requestId) {
		return cancel(requestId, null);
	}

	public boolean cancel(Id<Request> requestId, String reason) {
		RequestItem item = requests.get(requestId);

		if (item != null) {
			if (!item.onboard) {
				synchronized (cancelQueue) {
					cancelQueue.add(new CancelItem(requestId, reason));
					return true;
				}
			}
		}

		return false;
	}

	private record CancelItem(Id<Request> requestId, String reason) {
	}

	// Functionality for abandoning requests

	private static final String ABANDONED_REASON = "abandoned by vehicle";
	private final List<Id<Request>> abandonQueue = new LinkedList<>();

	void abandon(Id<Request> requestId) {
		synchronized (abandonQueue) {
			RequestItem item = Objects.requireNonNull(requests.get(requestId));
			Verify.verify(!item.onboard, "Cannot abandon request, person has already entered");
			abandonQueue.add(requestId);
		}
	}

	private void processAbandonedRequests(double now) {
		synchronized (abandonQueue) {
			for (Id<Request> requestId : abandonQueue) {
				RequestItem item = Objects.requireNonNull(requests.remove(requestId));
				Verify.verify(!item.onboard);

				// remove request from scheduled if already scheduled
				if (item.vehicleId != null) {
					unscheduler.unscheduleRequest(now, item.vehicleId, item.request.getId());
				} else {
					unscheduleUponVehicleAssignment.add(item.request.getId());
				}

				eventsManager.processEvent(new PassengerRequestRejectedEvent(now, mode, item.request.getId(),
						item.request.getPassengerId(), ABANDONED_REASON));
			}

			abandonQueue.clear();
		}
	}

	// React to external rejections or stuck agents

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		RequestItem item = requests.remove(event.getRequestId());

		if (item != null) {
			Verify.verify(!item.onboard);

			// unschedule if already scheduled
			if (item.vehicleId != null) {
				unscheduler.unscheduleRequest(event.getTime(), item.vehicleId, event.getRequestId());
			} else {
				unscheduleUponVehicleAssignment.add(event.getRequestId());
			}
		}
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		Iterator<RequestItem> iterator = requests.values().iterator();

		while (iterator.hasNext()) {
			RequestItem item = iterator.next();

			if (item.request.getPassengerId().equals(event.getPersonId())) {
				cancel(item.request.getId());
			}
			
			queue.clear();
		}
	}

	// Engine code

	@Override
	public void doSimStep(double now) {
		processAbandonedRequests(now);
		processCanceledRequests(now);
		processQueue(now);
	}

	@Override
	public void onPrepareSim() {
		eventsManager.addHandler(this);
	}

	@Override
	public void afterSim() {
		eventsManager.removeHandler(this);
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
	}
}
