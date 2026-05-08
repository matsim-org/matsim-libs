package ch.sbb.matsim.mobsim.qsim.pt;

import ch.sbb.matsim.config.SBBTransitConfigGroup;
import com.google.inject.Inject;
import jakarta.annotation.Nullable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.MobsimMessageCollector;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.dsim.*;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.BasicPlanAgentImpl;
import org.matsim.core.mobsim.qsim.agents.TransitAgent;
import org.matsim.core.mobsim.qsim.interfaces.InsertableMobsim;
import org.matsim.core.mobsim.qsim.pt.*;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.dsim.simulation.AgentSourcesContainer;
import org.matsim.dsim.simulation.VehicleContainer;
import org.matsim.pt.Umlauf;
import org.matsim.pt.UmlaufImpl;
import org.matsim.pt.UmlaufStueck;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import java.util.*;

public class SBBTransitEngine
	implements DistributedMobsimEngine, DistributedDepartureHandler, AgentSource, DistributedAgentSource {

	private final Map<Id<Umlauf>, Umlauf> umlaufCache = new HashMap<>(); // we are also the agent source for TransitDriverAgents, which expect to have an umlauf cache.
	private final PriorityQueue<TransitEvent> eventQueue = new PriorityQueue<>();

	private final ReplanningContext context;
	private final SBBTransitConfigGroup config;
	private final TransitConfigGroup ptConfig;
	private final Scenario scenario;
	private final TransitStopAgentTracker agentTracker;
	private final EventsManager em;
	private final MobsimMessageCollector partitionTransfer;
	private final TimeInterpretation timeInterpretation;

	// we can use the implementation anyway, as we are relying on the types of drivers created by this factory.
	private final SBBTransitDriverAgentFactory driverFactory;
	private final TransitStopHandlerFactory stopHandlerFactory;

	// optionally required in case we run a distributed simulation
	// currently, we don't hava a noop version for this. It is not even an interface.
	@Inject(optional = true)
	private AgentSourcesContainer asc;

	private InternalInterface internalInterface;
	private boolean createLinkEvents = false;

	@Inject
	public SBBTransitEngine(ReplanningContext context, SBBTransitConfigGroup config, TransitConfigGroup ptConfig,
		MobsimMessageCollector partitionTransfer, TransitStopAgentTracker agentTracker, Scenario scenario,
		EventsManager em, TimeInterpretation timeInterpretation,
		SBBTransitDriverAgentFactory driverFactory, TransitStopHandlerFactory stopHandlerFactory) {
		this.context = context;
		this.config = config;
		this.ptConfig = ptConfig;
		this.partitionTransfer = partitionTransfer;
		this.scenario = scenario;
		this.agentTracker = agentTracker;
		this.em = em;
		this.timeInterpretation = timeInterpretation;
		this.driverFactory = driverFactory;
		this.stopHandlerFactory = stopHandlerFactory;
	}

	@Override
	public void beforeMobsim() {
		int iteration = this.context.getIteration();
		int createEventsInterval = this.config.getCreateLinkEventsInterval();
		final boolean writingEventsAtAll = createEventsInterval > 0;
		final boolean regularWriteEvents = writingEventsAtAll && iteration % createEventsInterval == 0;
		createLinkEvents = writingEventsAtAll && regularWriteEvents;
		validateModeConfiguration();
	}

	@Override
	public void afterMobsim() {
		double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
		for (Map.Entry<Id<TransitStopFacility>, List<PTPassengerAgent>> agentsAtStop : this.agentTracker.getAgentsAtStop().entrySet()) {
			TransitStopFacility stop = scenario.getTransitSchedule().getFacilities().get(agentsAtStop.getKey());
			for (PTPassengerAgent agent : agentsAtStop.getValue()) {
				em.processEvent(new PersonStuckEvent(now, agent.getId(), stop.getLinkId(), agent.getMode()));
			}
		}
		agentTracker.getAgentsAtStop().clear();

		for (var transitEvent : eventQueue) {
			var driver = transitEvent.context().driver();
			var vehicle = driver.getVehicle();
			var mode = driver.getMode();
			var linkId = vehicle.getCurrentLinkId();
			em.processEvent(new PersonStuckEvent(now, driver.getId(), linkId, mode));
			for (var p : vehicle.getPassengers()) {
				em.processEvent(new PersonStuckEvent(now, p.getId(), linkId, mode));
			}
		}
		eventQueue.clear();
	}

	private void validateModeConfiguration() {
		Set<String> deterministicModes = this.config.getDeterministicServiceModes();
		Set<String> passengerModes = this.ptConfig.getTransitModes();
		Set<String> commonModes = new HashSet<>(deterministicModes);
		commonModes.retainAll(passengerModes);
		if (!commonModes.isEmpty()) {
			throw new RuntimeException(
				"There are modes configured to be pt passenger modes as well as deterministic service modes. This will not work! common modes = "
					+ CollectionUtils.setToString(commonModes));
		}
		Set<String> mainModes = new HashSet<>(scenario.getConfig().qsim().getMainModes());
		mainModes.retainAll(deterministicModes);
		if (!mainModes.isEmpty()) {
			throw new RuntimeException(
				"There are modes configured to be deterministic service modes as well as qsim main modes. This will not work! common modes = "
					+ CollectionUtils.setToString(mainModes));
		}
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {

		String mode = agent.getMode();
		if (this.ptConfig.getTransitModes().contains(mode)) {
			handlePassengerDeparture(agent, linkId);
			return true;
		}
		if (this.config.getDeterministicServiceModes().contains(mode)) {
			handleDeterministicDriverDeparture(agent, now);
			return true;
		}
		return false;
	}

	private void handlePassengerDeparture(MobsimAgent agent, Id<Link> linkId) {

		if (!(agent instanceof PTPassengerAgent passenger))
			throw new IllegalStateException("SBB TransitEngine only expects PTPassengerAgents, but got: " + agent.getClass().getSimpleName());

		// this puts the agent into the transit stop.
		Id<TransitStopFacility> accessStopId = passenger.getDesiredAccessStopId();
		if (accessStopId == null) {
			// looks like this agent has a bad transit route, likely no
			// route could be calculated for it
			// the old code silently removed the agent. Crash instead for clear results.
			throw new IllegalStateException(
				"pt-agent doesn't know to what transit stop to go to. Removing agent from simulation. Agent " + passenger.getId().toString());
		}
		TransitStopFacility stop = scenario.getTransitSchedule().getFacilities().get(accessStopId);
		if (stop.getLinkId() == null || stop.getLinkId().equals(linkId)) {
			if (partitionTransfer.isLocal(linkId)) {
				double now = this.internalInterface.getMobsim().getSimTimer().getTimeOfDay();
				this.agentTracker.addAgentToStop(now, passenger, stop.getId());
			} else {
				throw new IllegalStateException("Passengers should depart on a local link. If the link is not local something went wrong.");
			}

		} else {
			throw new TransitQSimEngine.TransitAgentTriesToTeleportException(
				"Agent " + passenger.getId() + " tries to enter a transit stop at link " + stop.getLinkId() + " but really is at " + linkId + "!");
		}
	}

	private void handleDeterministicDriverDeparture(MobsimAgent agent, double now) {
		if (!(agent instanceof SBBTransitDriverAgent driver))
			throw new IllegalStateException("SBB TransitEngine only expects PTDriverAgents, but got: " + agent.getClass().getSimpleName());

		em.processEvent(new PersonEntersVehicleEvent(now, driver.getId(), driver.getVehicle().getId()));

		if (createLinkEvents) {
			var id = driver.getId();
			var linkId = driver.getCurrentLinkId();
			var vehicleId = driver.getVehicle().getId();
			var mode = driver.getMode();
			em.processEvent(new VehicleEntersTrafficEvent(now, id, linkId, vehicleId, mode, 1.0));
		}

		TransitContext context = new TransitContext(createLinkEvents, driver);
		Id<Link> firstStopLink = driver.getNextStop().getStopFacility().getLinkId();
		this.eventQueue.add(new TransitEvent(now, TransitEventType.ArrivalAtStop, context, firstStopLink));
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	@Override
	public void doSimStep(double now) {
		while (!eventQueue.isEmpty()) {
			var head = eventQueue.peek();
			if (head.time() > now)
				break;

			handleTransitEvent(eventQueue.poll(), now);
		}
	}

	private void handleTransitEvent(TransitEvent e, double now) {
		switch (e.type()) {
		case ArrivalAtStop -> handleArrivalAtStop(e);
		case PassengerExchange -> handlePassengerExchange(e);
		case DepartureAtStop -> handleDepartureAtStop(e, now);
		case LinkTransition -> handleLinkTransition(e, now);
		}
	}

	private void handleArrivalAtStop(TransitEvent e) {
		e.context.driver.arriveAtNextStop(e.time);
		handlePassengerExchange(e);
	}

	private void handlePassengerExchange(TransitEvent event) {
		SBBTransitDriverAgent driver = event.context.driver;
		TransitRouteStop stop = driver.getCurrentStop();
		Id<Link> linkId = stop.getStopFacility().getLinkId();
		double stopTime = driver.handleTransitStop(stop.getStopFacility(), event.time);
		if (stopTime > 0) {
			TransitEvent depEvent = new TransitEvent(event.time + stopTime, TransitEventType.PassengerExchange, event.context, linkId);
			this.eventQueue.add(depEvent);
		} else {
			this.eventQueue.add(new TransitEvent(event.time, TransitEventType.DepartureAtStop, event.context, linkId));
		}
	}

	private void handleDepartureAtStop(TransitEvent event, double now) {
		SBBTransitDriverAgent driver = event.context.driver;
		driver.departAtStop(event.time);

		TransitRouteStop nextStop = driver.getNextStop();
		if (nextStop != null) {
			var nextTransitEvent = event.context.computeEventsOnDeparture(scenario.getNetwork(), event.time);
			enqueueOrSend(nextTransitEvent);
		} else {
			if (this.createLinkEvents) {
				Id<Link> linkId = driver.getDestinationLinkId();
				String mode = driver.getMode();
				em.processEvent(new VehicleLeavesTrafficEvent(now, driver.getId(), linkId, driver.getVehicle().getId(),
						mode, 1.0));
			}
			em.processEvent(new PersonLeavesVehicleEvent(now, driver.getId(), driver.getVehicle().getId()));
			driver.endLegAndComputeNextState(now);
			this.internalInterface.arrangeNextAgentState(driver);
		}
	}

	private void handleLinkTransition(TransitEvent e, double now) {
		var transition = e.context.precomputedLinkTransitions.element();
		var vehicle = e.context.driver.getVehicle();
		em.processEvent(new LinkLeaveEvent(now, vehicle.getId(), transition.fromLink()));
		em.processEvent(new LinkEnterEvent(now, vehicle.getId(), transition.toLink()));

		var nextEvent = e.context.computeEventOnLinkTransition(e.time);
		enqueueOrSend(nextEvent);
	}

	private void enqueueOrSend(TransitEvent e) {
		if (partitionTransfer.isLocal(e.linkId())) {
			eventQueue.add(e);
		} else {
			// SAFETY: We can cast into distributed variants, as we only enter this branch using dsim.
			var veh = (DistributedMobsimVehicle) e.context.driver.getVehicle();
			var container = AgentSourcesContainer.vehicleToContainer(veh);
			partitionTransfer.collect(new TransitEventMessage(e, container), e.linkId());
			var toPartition = partitionTransfer.getPartitionIndex(e.linkId());
			internalInterface.notifyAgentLeavesPartition(e.context.driver, toPartition);
			for (var passenger : veh.getPassengers()) {
				internalInterface.notifyAgentLeavesPartition((DistributedMobsimAgent) passenger, toPartition);
			}
		}
	}

	@Override
	public Map<Class<? extends Message>, MessageHandler> getMessageHandlers() {
		return Map.of(
			TransitEventMessage.class,
			this::processTransitEventMessage
		);
	}

	private void processTransitEventMessage(List<Message> messages, double now) {

		for (var m : messages) {
			var msg = (TransitEventMessage) m;
			var vehicle = asc.vehicleFromContainer(msg.vehicleContainer);
			// SAFETY: We register as agent source (called from vehicleFromContainer) and produce this type. as well as the passengers.
			var driver = (SBBTransitDriverAgent) vehicle.getDriver();
			internalInterface.notifyAgentEntersPartition(driver);
			for (var passenger : vehicle.getPassengers()) {
				internalInterface.notifyAgentEntersPartition((DistributedMobsimAgent) passenger);
			}

			var context = new TransitContext(this.createLinkEvents, driver, msg.precomputedLinks);
			if (msg.eventType.equals(TransitEventType.ArrivalAtStop)) {
				var event = new TransitEvent(msg.time, TransitEventType.ArrivalAtStop, context,
					context.driver.getNextStop().getStopFacility().getLinkId());
				enqueueOrSend(event);
			} else if (msg.eventType.equals(TransitEventType.LinkTransition)) {
				var linkId = context.precomputedLinkTransitions.element().fromLink();
				var event = new TransitEvent(msg.time, TransitEventType.LinkTransition, context, linkId);
				enqueueOrSend(event);
			} else {
				throw new IllegalStateException(
					"Only expecting Messages with event type ArrivalAtStop or LinkTransition but received: " + msg.eventType);
			}
		}
	}

	@Override
	public void createAgentsAndVehicles(NetworkPartition partition, InsertableMobsim mobsim) {
		createVehiclesAndDrivers();
	}

	@Override
	public void insertAgentsIntoMobsim() {
		createVehiclesAndDrivers();
	}

	private void createVehiclesAndDrivers() {
		TransitSchedule schedule = scenario.getTransitSchedule();
		Vehicles vehicles = scenario.getTransitVehicles();
		Set<String> deterministicModes = this.config.getDeterministicServiceModes();
		Set<String> passengerModes = this.ptConfig.getTransitModes();
		Set<String> commonModes = new HashSet<>(deterministicModes);
		commonModes.retainAll(passengerModes);
		if (!commonModes.isEmpty()) {
			throw new RuntimeException(
				"There are modes configured to be pt passenger modes as well as deterministic service modes. This will not work! common modes = "
					+ CollectionUtils.setToString(commonModes));
		}
		Set<String> mainModes = new HashSet<>(scenario.getConfig().qsim().getMainModes());
		mainModes.retainAll(deterministicModes);
		if (!mainModes.isEmpty()) {
			throw new RuntimeException(
				"There are modes configured to be deterministic service modes as well as qsim main modes. This will not work! common modes = "
					+ CollectionUtils.setToString(mainModes));
		}

		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				String mode = route.getTransportMode();
				for (Departure dep : route.getDepartures().values()) {
					Vehicle veh = vehicles.getVehicles().get(dep.getVehicleId());
					Umlauf umlauf = createUmlauf(line, route, dep);
					// non-deterministic pt drivers need an umlauf from the cache. So store it. In the regular Transit engines, umlaufs can
					// include multiple trips, and routess. I.e., a vehicle is dispatched on route A and later on route B. This class however,
					// creates an umlauf for each departure. This is not the correct behavior, I think. My guess is that noone ever used deterministic
					// transit in combination with non-deterministic pt and multi umlauf scenarios. janek apr' 26.
					if (!deterministicModes.contains(mode)) {
						umlaufCache.put(umlauf.getId(), umlauf);
					}
					// but we only want to schedule local drivers, as other partitions will instantiate theirs.
					if (partitionTransfer.isLocal(umlauf.getUmlaufStuecke().getFirst().getCarRoute().getStartLinkId())) {
						createAndScheduleDriver(veh, umlauf, deterministicModes.contains(mode));
					}
				}
			}
		}
	}

	private void createAndScheduleDriver(Vehicle veh, Umlauf umlauf, boolean isDeterministic) {
		// the driver factory should figure out which driver to create based on the config.
		AbstractTransitDriverAgent driver = driverFactory.createTransitDriver(umlauf, internalInterface, agentTracker);
		TransitQVehicle qVeh = new TransitQVehicle(veh);
		qVeh.setDriver(driver);
		qVeh.setStopHandler(this.stopHandlerFactory.createTransitStopHandler(veh));
		driver.setVehicle(qVeh);

		Leg firstLeg = (Leg) driver.getNextPlanElement();
		if (!isDeterministic) {
			Id<Link> startLinkId = firstLeg.getRoute().getStartLinkId();
			internalInterface.getMobsim().addParkedVehicle(qVeh, startLinkId);
		}
		internalInterface.getMobsim().insertAgentIntoMobsim(driver);
	}

	private Umlauf createUmlauf(TransitLine line, TransitRoute route, Departure departure) {
		Id<Umlauf> id = Id.create(createId(line, route, departure), Umlauf.class);
		UmlaufImpl umlauf = new UmlaufImpl(id);
		UmlaufStueck part = new UmlaufStueck(line, route, departure);
		umlauf.getUmlaufStuecke().add(part);
		return umlauf;
	}

	/**
	 * Tha agent id for an umlauf.
	 */
	static String createId(TransitLine line, TransitRoute route, Departure departure) {
		return line.getId().toString() + "_" + route.getId().toString() + "_" + departure.getId().toString();
	}

	// -----------------------------
	// Below are AgentSource implementations. This needs to be inside an engine, as the transit driver has a references
	// to the internal interface, which is only passed to mobsim engines.
	// -----------------------------

	@Override
	public Set<Class<? extends DistributedMobsimAgent>> getAgentClasses() {
		return Set.of(SBBTransitDriverAgent.class, TransitDriverAgentImpl.class, TransitAgent.class);
	}

	@Override
	public DistributedMobsimAgent agentFromMessage(Class<? extends DistributedMobsimAgent> type,
		Message message) {

		if (type.equals(SBBTransitDriverAgent.class)) {
			var stdm = (SBBTransitDriverAgent.SBBTransitDriverMessage) message;
			var transitLine = scenario.getTransitSchedule().getTransitLines().get(stdm.transitLineId());
			var transitRoute = transitLine.getRoutes().get(stdm.transitRouteId());
			var departure = transitRoute.getDepartures().get(stdm.departureId());
			var umlauf = createUmlauf(transitLine, transitRoute, departure);
			return (DistributedMobsimAgent) driverFactory.createTransitDriverFromMessage(stdm, umlauf, internalInterface, agentTracker);
		} else if (type.equals(TransitDriverAgentImpl.class)) {
			var tdm = (TransitDriverAgentImpl.TransitDriverMessage) message;
			var umlauf = umlaufCache.get(tdm.umlaufId());
			return (DistributedMobsimAgent) driverFactory.createTransitDriverFromMessage(tdm, umlauf, internalInterface, agentTracker);
		} else if (type.equals(TransitAgent.class)) {
			var timer = internalInterface.getMobsim().getSimTimer();
			var baseAgent = new BasicPlanAgentImpl((BasicPlanAgentImpl.BasicPlanAgentMessage) message, scenario, em, timer, timeInterpretation);
			return TransitAgent.createTransitAgent(baseAgent, scenario);
		}

		throw new UnsupportedOperationException("handling of message of type: " + type + " not yet implemented");
	}

	public Set<Class<? extends DistributedMobsimVehicle>> getVehicleClasses() {
		return Set.of(TransitQVehicle.class);
	}

	@Nullable
	public DistributedMobsimVehicle vehicleFromMessage(Class<? extends DistributedMobsimVehicle> type, Message message) {

		if (message instanceof TransitQVehicle.Msg(Message baseMessage, Message handlerMessage)) {
			TransitQVehicle transitVehicle = new TransitQVehicle((QVehicleImpl.Msg) baseMessage);
			var handler = stopHandlerFactory.createTransitStopHandler(handlerMessage);
			transitVehicle.setStopHandler(handler);
			return transitVehicle;
		} else {
			throw new IllegalArgumentException("Unsupported message type: " + message.getClass());
		}
	}

	//-------------------
	// Internal Data structures
	//-------------------

	private enum TransitEventType {ArrivalAtStop, PassengerExchange, DepartureAtStop, LinkTransition}

	private record TransitContext(boolean createLinkEvents, SBBTransitDriverAgent driver, Deque<LinkTransition> precomputedLinkTransitions) {

		private TransitContext(boolean createLinkEvents, SBBTransitDriverAgent driver) {
			this(createLinkEvents, driver, new ArrayDeque<>());
		}

		TransitEvent computeEventsOnDeparture(Network network, double now) {

			var arrTime = arrivalTime(now);
			if (this.createLinkEvents) {
				this.precomputeLinksToNextStop(network, now, arrTime);
			}

			return computeArrivalOrTransition(arrTime);
		}

		TransitEvent computeEventOnLinkTransition(double now) {
			var _ = precomputedLinkTransitions.poll();
			return computeArrivalOrTransition(arrivalTime(now));
		}

		private TransitEvent computeArrivalOrTransition(double arrTime) {
			if (precomputedLinkTransitions.isEmpty()) {
				var linkId = driver.getNextStop().getStopFacility().getLinkId();
				return new TransitEvent(arrTime, TransitEventType.ArrivalAtStop, this, linkId);
			} else {
				var transition = precomputedLinkTransitions.peek();
				return new TransitEvent(transition.time(), TransitEventType.LinkTransition, this, transition.fromLink());
			}
		}

		private double arrivalTime(double now) {
			var nextStop = driver.getNextStop();
			double arrivalOffset = nextStop.getArrivalOffset().or(nextStop.getDepartureOffset()).seconds();
			double predictedArrival = driver.getDeparture().getDepartureTime() + arrivalOffset;
			return Math.max(predictedArrival, now);
		}

		private void precomputeLinksToNextStop(Network network, double now, double arrTime) {

			// driver is just departing. We ignore links before the first stop.
			if (driver.getPreviousStop() == null)
				return;

			var links = linksBetweenStops(network);
			var totalLength = links.stream().mapToDouble(Link::getLength).sum();
			precomputeLinkTransitions(now, arrTime, totalLength, links);
		}

		private Collection<Link> linksBetweenStops(Network network) {

			Collection<Link> result = new ArrayList<>();
			var prevStopId = driver.getPreviousStop().getStopFacility().getLinkId();
			var nextStopId = driver.getNextStop().getStopFacility().getLinkId();

			if (prevStopId.equals(nextStopId)) {
				return result;
			}

			var nr = driver.getTransitRoute().getRoute();

			var startIndex = 0;
			if (nr.getStartLinkId().equals(prevStopId)) {
				var link = network.getLinks().get(prevStopId);
				result.add(link);
			} else {
				startIndex = nr.getLinkIds().indexOf(prevStopId);
			}

			for (var i = startIndex; i < nr.getLinkIds().size(); i++) {
				var linkId = nr.getLinkIds().get(i);
				var link = network.getLinks().get(linkId);
				result.add(link);

				if (linkId.equals(nextStopId)) {
					break;
				}
			}

			if (nr.getEndLinkId().equals(nextStopId)) {
				var link = network.getLinks().get(nextStopId);
				result.add(link);
			}
			return result;
		}

		private void precomputeLinkTransitions(double depTime, double arrTime, double totalLength, Collection<Link> linksToNextStop) {

			double travelTime = arrTime - depTime;
			double secondsPerMeter = totalLength > 0 ? travelTime / totalLength : 0;
			// for zero travel time, it is expected that events are dispatched at the very same time as departure time...
			double minTravelTime = travelTime == 0 ? 0 : 1;
			double travelledLength = 0;
			var it = linksToNextStop.iterator();
			var fromLink = it.hasNext() ? it.next() : null;

			while (it.hasNext()) {
				var toLink = it.next();
				var elapsedTime = travelledLength * secondsPerMeter;
				// the first transition after the departure is expected one second after departure. Therefore use either elapsed time or the minimum time.
				var time = depTime + Math.max(elapsedTime, minTravelTime);
				this.precomputedLinkTransitions.add(new LinkTransition(fromLink.getId(), toLink.getId(), time));

				travelledLength += toLink.getLength();
				fromLink = toLink;
			}
		}
	}

	record LinkTransition(Id<Link> fromLink, Id<Link> toLink, double time) {
	}

	private record TransitEvent(
		double time,
		TransitEventType type,
		TransitContext context,
		Id<Link> linkId
	) implements Comparable<TransitEvent>, Message {

		@Override
		public int compareTo(TransitEvent o) {
			int result = Double.compare(this.time, o.time);
			if (result == 0) {
				if (this.type == o.type) {
					result = this.context.driver.getId().compareTo(o.context.driver.getId());
				} else {
					// arrivals should come before departures
					result = this.type == TransitEventType.ArrivalAtStop ? -1 : +1;
				}
			}
			return result;
		}
	}

	public static class TransitEventMessage implements Message {

		private final double time;
		private final TransitEventType eventType;
		private final VehicleContainer vehicleContainer;
		private final Deque<LinkTransition> precomputedLinks;

		TransitEventMessage(TransitEvent e, VehicleContainer vehicleContainer) {
			this.time = e.time;
			this.eventType = e.type;
			this.vehicleContainer = vehicleContainer;
			this.precomputedLinks = e.context.precomputedLinkTransitions;
		}
	}
}
