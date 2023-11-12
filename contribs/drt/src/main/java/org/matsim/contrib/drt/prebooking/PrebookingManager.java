package org.matsim.contrib.drt.prebooking;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.AdvanceRequestProvider;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import com.google.common.base.Preconditions;

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
public class PrebookingManager implements MobsimEngine, AdvanceRequestProvider {
	private final String mode;

	private final Network network;
	private final EventsManager eventsManager;

	private final VrpOptimizer optimizer;

	public PrebookingManager(String mode, Network network, PassengerRequestCreator requestCreator,
			VrpOptimizer optimizer, PassengerRequestValidator requestValidator, EventsManager eventsManager) {
		this.network = network;
		this.mode = mode;
		this.requestCreator = requestCreator;
		this.optimizer = optimizer;
		this.requestAttribute = PREBOOKED_REQUEST_PREFIX + ":" + mode;
		this.requestValidator = requestValidator;
		this.eventsManager = eventsManager;
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

	private class RequestItem {
		// this class looks minimal for now, but will be extended with canceling
		// functionality
		final PassengerRequest request;

		RequestItem(PassengerRequest request) {
			this.request = request;
		}
	}

	// Engine code

	@Override
	public void doSimStep(double now) {
		processQueue(now);
	}

	@Override
	public void onPrepareSim() {
	}

	@Override
	public void afterSim() {
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
	}
}
