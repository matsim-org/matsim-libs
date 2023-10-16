package org.matsim.contrib.drt.extension.prebooking.dvrp;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.extension.prebooking.events.PassengerEnteringVehicleEvent;
import org.matsim.contrib.drt.extension.prebooking.events.PassengerRequestBookedEvent;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
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
public class PrebookingManager implements MobsimEngine {
	private static final String PREBOOKED_REQUEST_PREFIX = "prebookedRequestId";

	private final String mode;

	private final Network network;
	private final EventsManager eventsManager;

	private final PassengerRequestCreator requestCreator;
	private final PassengerRequestValidator requestValidator;
	private final VrpOptimizer optimizer;

	private final AtomicInteger currentRequestIndex = new AtomicInteger(-1);

	private final String requestAttribute;

	private final List<PrebookingQueueItem> prebookingQueue = new LinkedList<>();
	private final IdMap<Request, PassengerRequest> prebookedRequests = new IdMap<>(Request.class);

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

	PassengerRequest consumePrebookedRequest(MobsimAgent agent, Leg leg) {
		Verify.verify(leg.getMode().equals(mode), "Invalid mode for this prebooking manager");

		String rawRequestId = (String) leg.getAttributes().getAttribute(requestAttribute);

		if (rawRequestId == null) {
			return null;
		}

		return prebookedRequests.remove(Id.create(rawRequestId, Request.class));
	}

	private Id<Request> createRequestId() {
		return Id.create(mode + "_prebooked_" + currentRequestIndex.incrementAndGet(), Request.class);
	}

	public boolean isPrebookedRequest(Id<Request> requestId) {
		return requestId.toString().startsWith(mode + "_prebooked_");
	}

	public void prebook(Person person, Leg leg, double earliestDepartureTime) {
		Verify.verify(leg.getMode().equals(mode), "Invalid mode for this prebooking manager");

		synchronized (prebookingQueue) {
			this.prebookingQueue.add(new PrebookingQueueItem(person, leg, earliestDepartureTime));
		}
	}

	private Link getLink(Id<Link> linkId) {
		return Preconditions.checkNotNull(network.getLinks().get(linkId),
				"Link id=%s does not exist in network for mode %s. Agent departs from a link that does not belong to that network?",
				linkId, mode);
	}

	private record PrebookingQueueItem(Person person, Leg leg, double earliestDepartureTime) {
	}

	@Override
	public void onPrepareSim() {
	}

	@Override
	public void doSimStep(double now) {
		synchronized (prebookingQueue) {
			for (PrebookingQueueItem item : prebookingQueue) {
				Verify.verify(item.leg.getMode().equals(mode), "Invalid mode for this prebooking manager");

				Id<Request> requestId = createRequestId();

				eventsManager.processEvent(new PassengerRequestBookedEvent(now, mode, requestId, item.person.getId()));

				PassengerRequest request = requestCreator.createRequest(requestId, item.person.getId(),
						item.leg.getRoute(), getLink(item.leg.getRoute().getStartLinkId()),
						getLink(item.leg.getRoute().getEndLinkId()), item.earliestDepartureTime, now);

				Set<String> violations = requestValidator.validateRequest(request);
				if (!violations.isEmpty()) {
					String cause = String.join(", ", violations);
					eventsManager.processEvent(new PassengerRequestRejectedEvent(now, mode, request.getId(),
							request.getPassengerId(), cause));
				} else {
					synchronized (optimizer) {
						optimizer.requestSubmitted(request);
					}

					item.leg.getAttributes().putAttribute(requestAttribute, request.getId().toString());
					prebookedRequests.put(request.getId(), request);
				}
			}

			prebookingQueue.clear();
		}
	}
	
	void notifyEntering(double now, AcceptedDrtRequest request) {
		eventsManager
				.processEvent(new PassengerEnteringVehicleEvent(now, mode, request.getId(), request.getPassengerId()));
	}

	@Override
	public void afterSim() {
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
	};
}
