package org.matsim.contrib.dvrp.passenger;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.modal.ModalProviders;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class GroupPassengerEngine implements PassengerEngine, PassengerRequestRejectedEventHandler {

	private final String mode;
	private final MobsimTimer mobsimTimer;

	private final PassengerRequestCreator requestCreator;
	private final VrpOptimizer optimizer;
	private final Network network;
	private final PassengerRequestValidator requestValidator;

	private final InternalPassengerHandling internalPassengerHandling;

	private InternalInterface internalInterface;



	//accessed in doSimStep() and handleDeparture() (no need to sync)
	private final Map<Id<Request>, Set<MobsimPassengerAgent>> activePassengers = new HashMap<>();

	//accessed in doSimStep() and handleEvent() (potential data races)
	private final Queue<PassengerRequestRejectedEvent> rejectedRequestsEvents = new ConcurrentLinkedQueue<>();

	private final Set<MobsimPassengerAgent> currentDepartures = new LinkedHashSet<>();

	private final PassengerGroupIdentifier groupIdentifier;

	GroupPassengerEngine(String mode, EventsManager eventsManager, MobsimTimer mobsimTimer,
						 PassengerRequestCreator requestCreator, VrpOptimizer optimizer, Network network,
						 PassengerRequestValidator requestValidator, PassengerGroupIdentifier groupIdentifier) {
		this.mode = mode;
		this.mobsimTimer = mobsimTimer;
		this.requestCreator = requestCreator;
		this.optimizer = optimizer;
		this.network = network;
		this.requestValidator = requestValidator;
		this.groupIdentifier = groupIdentifier;

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
		handleDepartures();
		while (!rejectedRequestsEvents.isEmpty()) {
			Set<MobsimPassengerAgent> passengers = activePassengers.remove(rejectedRequestsEvents.poll().getRequestId());
			//not much else can be done for immediate requests
			//set the passenger agent to abort - the event will be thrown by the QSim
			for (MobsimPassengerAgent passenger: passengers) {
				passenger.setStateToAbort(mobsimTimer.getTimeOfDay());
				internalInterface.arrangeNextAgentState(passenger);
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
		currentDepartures.add(passenger);
		return true;
	}

	private void handleDepartures() {
		Map<Id<PassengerGroupIdentifier.PassengerGroup>, List<MobsimPassengerAgent>> agentGroups
				= currentDepartures.stream().collect(Collectors.groupingBy(groupIdentifier));

		for (List<MobsimPassengerAgent> group : agentGroups.values()) {

			Iterator<MobsimPassengerAgent> iterator = group.iterator();
			MobsimAgent firstAgent = iterator.next();
			Id<Link> toLinkId = firstAgent.getDestinationLinkId();
			Route route = ((Leg) ((PlanAgent) firstAgent).getCurrentPlanElement()).getRoute();

			while(iterator.hasNext()) {
				MobsimAgent next = iterator.next();
				Gbl.assertIf(firstAgent.getCurrentLinkId().equals(next.getCurrentLinkId()));
				Gbl.assertIf(toLinkId.equals(next.getDestinationLinkId()));
			}

			PassengerRequest request = requestCreator.createRequest(internalPassengerHandling.createRequestId(),
					group.stream().map(Identifiable::getId).toList(), route,
					getLink(firstAgent.getCurrentLinkId()), getLink(toLinkId),
					mobsimTimer.getTimeOfDay(), mobsimTimer.getTimeOfDay());
			validateAndSubmitRequest(new HashSet<>(group), request, mobsimTimer.getTimeOfDay());
		}

		currentDepartures.clear();
	}

	private void validateAndSubmitRequest(Set<MobsimPassengerAgent> passengers, PassengerRequest request, double now) {
		activePassengers.put(request.getId(), passengers);
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
	public boolean tryPickUpPassengers(PassengerPickupActivity pickupActivity, MobsimDriverAgent driver,
									   Id<Request> requestId, double now) {
		boolean pickedUp = internalPassengerHandling.tryPickUpPassengers(driver, activePassengers.get(requestId),
				requestId, now);
		Verify.verify(pickedUp, "Not possible without prebooking");
		return pickedUp;
	}

	@Override
	public void dropOffPassengers(MobsimDriverAgent driver, Id<Request> requestId, double now) {
		internalPassengerHandling.dropOffPassengers(driver, activePassengers.remove(requestId), requestId, now);
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
			public GroupPassengerEngine get() {
				return new GroupPassengerEngine(getMode(), eventsManager, mobsimTimer,
						getModalInstance(PassengerRequestCreator.class), getModalInstance(VrpOptimizer.class),
						getModalInstance(Network.class), getModalInstance(PassengerRequestValidator.class), getModalInstance(PassengerGroupIdentifier.class));
			}
		};
	}
}
