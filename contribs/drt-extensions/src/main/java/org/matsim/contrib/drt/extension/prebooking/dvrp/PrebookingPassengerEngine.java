package org.matsim.contrib.drt.extension.prebooking.dvrp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.InternalPassengerHandling;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.PassengerPickupActivity;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.passenger.PassengerWaitingEvent;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.modal.ModalProviders;

import com.google.common.base.Preconditions;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @author Michal Maciejewski (michalm)
 * @author Sebastian Hörl, IRT SystemX (sebhoerl)
 * 
 * This class is based on DefaultPassengerEngine, but is able to handle prebooked requests in combination with 
 * PrebookingStopActivity. This means that persons may depart (PersonDepartureEvent) after the vehicle has arrived.
 */
public final class PrebookingPassengerEngine implements PassengerEngine, PassengerRequestRejectedEventHandler {

	private final String mode;
	private final MobsimTimer mobsimTimer;
	private final EventsManager eventsManager;

	private final PassengerRequestCreator requestCreator;
	private final VrpOptimizer optimizer;
	private final Network network;
	private final PassengerRequestValidator requestValidator;

	private final InternalPassengerHandling internalPassengerHandling;

	private InternalInterface internalInterface;

	private final PrebookingManager prebookingManager;

	//accessed in doSimStep() and handleDeparture() (no need to sync)
	private final Map<Id<Request>, MobsimPassengerAgent> activePassengers = new HashMap<>();
	
	 // prebooking: holds vehicle stop activities for requests that have not arrived at departure point yet
	private final Map<Id<Request>, PassengerPickupActivity> waitingForPassenger = new HashMap<>();

	//accessed in doSimStep() and handleEvent() (potential data races)
	private final Queue<PassengerRequestRejectedEvent> rejectedRequestsEvents = new ConcurrentLinkedQueue<>();

	// prebooking: pass PrebookingManager
	PrebookingPassengerEngine(String mode, EventsManager eventsManager, MobsimTimer mobsimTimer,
			PassengerRequestCreator requestCreator, VrpOptimizer optimizer, Network network,
			PassengerRequestValidator requestValidator, PrebookingManager prebookingManager) {
		this.mode = mode;
		this.mobsimTimer = mobsimTimer;
		this.requestCreator = requestCreator;
		this.optimizer = optimizer;
		this.network = network;
		this.requestValidator = requestValidator;
		this.eventsManager = eventsManager;
		this.prebookingManager = prebookingManager;

		internalPassengerHandling = new InternalPassengerHandling(mode, eventsManager);
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
		internalPassengerHandling.setInternalInterface(internalInterface);
	}

	@Override
	public void onPrepareSim() {
	}

	@Override
	public void doSimStep(double time) {		
		// prebooking: converted the initial while into an iterator to selectively clear the
		// list. If prebooked requests are rejected (by the optimizer, through an
		// event) after submission, but before departure, the PassengerEngine does not
		// know this agent yet. Hence, we wait with setting the state to abort until the
		// agent has arrived here (if ever). An alternative approach would be to save
		// the ID to a list and then set the state to abort once the agent shows up (may
		// be less memory consumption if we only need to save the IDs)
		
		Iterator<PassengerRequestRejectedEvent> iterator = rejectedRequestsEvents.iterator();
		while (iterator.hasNext()) {
			PassengerRequestRejectedEvent event = iterator.next();
			MobsimPassengerAgent passenger = activePassengers.remove(event.getRequestId());
			
			if (passenger != null) {
				//not much else can be done for immediate requests
				//set the passenger agent to abort - the event will be thrown by the QSim
				passenger.setStateToAbort(mobsimTimer.getTimeOfDay());
				internalInterface.arrangeNextAgentState(passenger);
				iterator.remove();
			}
		}
	}

	@Override
	public void afterSim() {
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> fromLinkId) {
		if (!agent.getMode().equals(mode)) {
			return false;
		}

		MobsimPassengerAgent passenger = (MobsimPassengerAgent)agent;
		internalInterface.registerAdditionalAgentOnLink(passenger);

		Id<Link> toLinkId = passenger.getDestinationLinkId();
		
		// prebooking: try to find a prebooked requests that is associated to this leg
		Leg leg = (Leg)((PlanAgent)passenger).getCurrentPlanElement();
		PassengerRequest request = prebookingManager.consumePrebookedRequest(agent, leg);
		
		if (request == null) { // prebooking: immediate request, default behavior
			request = requestCreator.createRequest(internalPassengerHandling.createRequestId(),
					passenger.getId(), leg.getRoute(), getLink(fromLinkId), getLink(toLinkId), now, now);
			validateAndSubmitRequest(passenger, request, now);
		} else { // prebooking: found a prebooked request for this customer departure
			activePassengers.put(request.getId(), passenger);
			
			PassengerPickupActivity pickupActivity = waitingForPassenger.remove(request.getId());
			if (pickupActivity != null) {
				// prebooking: the vehicle is already waiting for the customer, notify it
				pickupActivity.notifyPassengerIsReadyForDeparture(passenger, now);
			}
		}
		
		eventsManager.processEvent(new PassengerWaitingEvent(now, mode, request.getId(), request.getPassengerId()));
		
		return true;
	}

	private void validateAndSubmitRequest(MobsimPassengerAgent passenger, PassengerRequest request, double now) {
		activePassengers.put(request.getId(), passenger);
		if (internalPassengerHandling.validateRequest(request, requestValidator, now)) {
			//need to synchronise to address cases where requestSubmitted() may:
			// - be called from outside DepartureHandlers
			// - interfere with VrpOptimizer.nextTask()
			// - impact VrpAgentLogic.computeNextAction()
			synchronized (optimizer) {
				//optimizer can also reject request if cannot handle it
				// (async operation, notification comes via the events channel)
				optimizer.requestSubmitted(request);
			}
		}
	}

	private Link getLink(Id<Link> linkId) {
		return Preconditions.checkNotNull(network.getLinks().get(linkId),
				"Link id=%s does not exist in network for mode %s. Agent departs from a link that does not belong to that network?",
				linkId, mode);
	}

	@Override
	public boolean tryPickUpPassenger(PassengerPickupActivity pickupActivity, MobsimDriverAgent driver,
			Id<Request> requestId, double now) {
		if (!activePassengers.containsKey(requestId)) {
			// prebooking: vehicle queries customer, which has not departed yet, note it down
			waitingForPassenger.put(requestId, pickupActivity);
			return false;
		}
		
		boolean pickedUp = internalPassengerHandling.tryPickUpPassenger(driver, activePassengers.get(requestId),
				requestId, now);
		
		// prebooking: commented the following line, this is a valid situation now!
		// Verify.verify(pickedUp, "Not possible without prebooking");
		
		return pickedUp;
	}
	
	/*
	 * prebooking: new method that does not actually pick up a passenger but only
	 * announces that a vehicle would like to. This is necessary to get the
	 * stopDuration timing of the vehicles right.
	 */
	public boolean queryPassengerReady(PassengerPickupActivity pickupActivity, Id<Request> requestId) {
		// Copy & paste, see above, may make sense to restructure things
		if (!activePassengers.containsKey(requestId)) {
			waitingForPassenger.put(requestId, pickupActivity);
			return false;
		}

		return true;
	}
	
	@Override
	public void dropOffPassenger(MobsimDriverAgent driver, Id<Request> requestId, double now) {
		internalPassengerHandling.dropOffPassenger(driver, activePassengers.remove(requestId), requestId, now);
	}

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		if (event.getMode().equals(mode)) {
			rejectedRequestsEvents.add(event);
		}
	}

	public static Provider<PassengerEngine> createProvider(String mode) {
		return new ModalProviders.AbstractProvider<>(mode, DvrpModes::mode) {
			@Inject
			private EventsManager eventsManager;

			@Inject
			private MobsimTimer mobsimTimer;

			@Override
			public PrebookingPassengerEngine get() {
				return new PrebookingPassengerEngine(getMode(), eventsManager, mobsimTimer,
						getModalInstance(PassengerRequestCreator.class), getModalInstance(VrpOptimizer.class),
						getModalInstance(Network.class), getModalInstance(PassengerRequestValidator.class),
						getModalInstance(PrebookingManager.class));
			}
		};
	}
}
