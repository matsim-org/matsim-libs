/*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.fiss;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.api.experimental.events.VehicleTeleportationDepartureEvent;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.dsim.DistributedDepartureHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetworkModeDepartureHandler;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * FISS Flow-inflated selective sampling (working title).
 * <p>
 * The aim is to only assign a specified fraction of vehicular agents and teleport the rest.
 * This is achieved by making use of a VehicularDepartureHandler (so far not enforced, as the
 * implementation is not exposed, so in theory could be any DepartureHandler) and a TeleportationEngine.
 * Both used as delegates.
 * <p>
 * Transit driver agents are always assigned. In addition does not handle DynAgents (e.g., DVRP agents)
 * <p>
 * <p>
 * Also implements MobsimEngine to delegate required teleportation steps (such as arrivals).
 *
 * @author nkuehnel / MOIA, hrewald
 *
 * <p>
 * Remarks @kainagel: <ul>
 * <li> I have renamed the VehicularDepartureHandler to {@link NetworkModeDepartureHandler}.  Also, this is now an interface, bound against a default implementation. </li>
 * <li>There are probably more changes to come, like _replacing_ the {@link NetworkModeDepartureHandler} rather than adding another {@link DepartureHandler} which effectively over-writes it. </li>
 * <li> yyyy I have changed a fair amount of things so I am not sure how much of the above text is still correct. </li>
 * </ul>
 * </p>
 */
public class FISS implements NetworkModeDepartureHandler, DistributedDepartureHandler, MobsimEngine {

	private static final Logger LOG = LogManager.getLogger(FISS.class);

	private final DepartureHandler delegate;
	private final FISSConfigGroup fissConfigGroup;
	private final TeleportationEngine teleport;
	private final Network network;
	private final TravelTime travelTime;
	private final Random random;

	private final MatsimServices matsimServices;
	private final QSimConfigGroup qsimConfig;
	private final Map<Id<Vehicle>, Vehicle> vehicles;
	private final QNetsimEngineI qNetsimEngine;

	private final PriorityQueue<VehicleArrivalEntry> vehicleArrivals = new PriorityQueue<>();
	private final Map<Id<Vehicle>, VehicleArrivalEntry> vehicleArrivalsIndex = new HashMap<>();
	private final Map<Id<Vehicle>, Deque<DeferredDeparture>> deferredDepartures = new HashMap<>();

	private InternalInterface internalInterface;

	private long teleportedTrips = 0;
	private long teleportedDeferredTrips = 0;
	private long simulatedTrips = 0;
	private long simulatedQsimFallbackTrips = 0;

	@Inject
	FISS(MatsimServices matsimServices, Scenario scenario, FISSConfigGroup fissConfigGroup, Network network, TeleportationEngine teleport,
		 @Named(TransportMode.car) TravelTime travelTime,
		 @Named("base-network-mode-departure-handler") NetworkModeDepartureHandler networkModeDepartureHandler,
		 QNetsimEngineI qNetsimEngine) {
		this.delegate = networkModeDepartureHandler;
		this.fissConfigGroup = fissConfigGroup;
		this.network = network;
		this.teleport = teleport;
		this.travelTime = travelTime;
		this.matsimServices = matsimServices;
		this.vehicles = scenario.getVehicles().getVehicles();
		this.qsimConfig = scenario.getConfig().qsim();
		this.random = MatsimRandom.getLocalInstance();
		this.qNetsimEngine = qNetsimEngine;
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
		if (!this.qsimConfig.getMainModes().contains(agent.getMode())) {
			return false;
		} else if (agent instanceof DynAgent) {
			simulatedTrips++;
			return delegate.handleDeparture(now, agent, linkId);
		} else if (!this.fissConfigGroup.getSampledModes().contains(agent.getMode())) {
			simulatedTrips++;
			return delegate.handleDeparture(now, agent, linkId); // not covered by test
			// (the earlier design would have such agents fall through, in which case they would be treated by the standard network mode
			// dp handler)
		} else if (random.nextDouble() < fissConfigGroup.getSampleFactor() || agent instanceof TransitDriverAgent || this.switchOffFISS()) {
			simulatedTrips++;
			return delegate.handleDeparture(now, agent, linkId);
		}

		// This updates the travel time.  Teleportation departure is handled further down.
		double newTravelTime = 0.;
		Id<Link> destinationLinkId = agent.getDestinationLinkId();
		if (agent instanceof PlanAgent planAgent) {
			Leg currentLeg = (Leg) planAgent.getCurrentPlanElement();
			NetworkRoute networkRoute = (NetworkRoute) currentLeg.getRoute();
			Person person = planAgent.getCurrentPlan().getPerson();
			Vehicle vehicle = this.vehicles.get(networkRoute.getVehicleId());

			newTravelTime = calcQSimTravelTime(networkRoute, now, person, vehicle);
			LOG.debug("New travelTime: {}, was {}", newTravelTime, networkRoute.getTravelTime().orElseGet(() -> Double.NaN));

			networkRoute.setTravelTime(newTravelTime);
		}

		// Remove vehicle from departure link and schedule delayed arrival at
		// destination.
		if (agent instanceof MobsimDriverAgent driverAgent) {
			Id<Vehicle> vehicleId = driverAgent.getPlannedVehicleId();
			QLinkI qLinkI = (QLinkI) this.qNetsimEngine.getNetsimNetwork().getNetsimLink(linkId);
			QVehicle removedVehicle = qLinkI.removeParkedVehicle(vehicleId);
			if (removedVehicle == null) {
				// Vehicle not on departure link -- check if it's in our arrivals queue.
				VehicleArrivalEntry inTransit = vehicleArrivalsIndex.get(vehicleId);
				if (inTransit != null) {
					QSimConfigGroup.VehicleBehavior vehicleBehavior = qsimConfig.getVehicleBehavior();
					if (vehicleBehavior == QSimConfigGroup.VehicleBehavior.teleport) {
						// Vehicle location is irrelevant in teleport mode. Redirect
						// the in-transit vehicle to the new destination and depart now.
						removeVehicleArrival(inTransit);
						addVehicleArrival(now + newTravelTime, inTransit.vehicle, destinationLinkId);
						internalInterface.getMobsim().getEventsManager().processEvent(
								new VehicleTeleportationDepartureEvent(now, driverAgent.getId(), vehicleId, linkId,
										agent.getMode()));
						teleportedTrips++;
						boolean result = teleport.handleDeparture(now, agent, linkId);
						Gbl.assertIf(result);
						return result;
					} else if (vehicleBehavior == QSimConfigGroup.VehicleBehavior.wait
							&& inTransit.destinationLinkId.equals(linkId)) {
						// Vehicle is heading to this link. Defer the departure
						// until the vehicle arrives, then teleport. Only agents on
						// the matching link are deferred -- agents on other links
						// are delegated to QSim. This is safe because agents on
						// different links never compete at arrival time (QSim's
						// makeVehicleAvailableToNextDriver only wakes agents
						// registered on the arrival link). Deferring agents on
						// non-matching links would risk orphaning them: if a
						// subsequent trip is physically simulated (random selection),
						// FISS never sees the vehicle arrive and the agent is
						// silently lost.
						deferredDepartures.computeIfAbsent(vehicleId, k -> new ArrayDeque<>())
								.addLast(new DeferredDeparture(now, agent, linkId));
						teleportedTrips++;
						teleportedDeferredTrips++;
						return true;
					}
				}
				// Vehicle not in FISS queue -- delegate to QSim.
				LOG.debug(
						"Vehicle {} not found on link {} for agent {} at time {}. Falling back to standard departure.",
						vehicleId, linkId, driverAgent.getId(), now);
				simulatedTrips++;
				simulatedQsimFallbackTrips++;
				return delegate.handleDeparture(now, agent, linkId);
			}
			addVehicleArrival(now + newTravelTime, removedVehicle, destinationLinkId);

			// Signal that a vehicular trip is being teleported. This fires at departure time
			// (same timestamp as PersonDepartureEvent) and serves as the equivalent of
			// PersonEntersVehicleEvent for teleported trips.
			internalInterface.getMobsim().getEventsManager().processEvent(
					new VehicleTeleportationDepartureEvent(now, driverAgent.getId(), vehicleId, linkId,
							agent.getMode()));
		}

		teleportedTrips++;
		boolean result = teleport.handleDeparture(now, agent, linkId);
		Gbl.assertIf(result); // otherwise we are now confused

		return result;

	}

	@Override
	public void doSimStep(double time) {
		// Deliver vehicles that have reached their arrival time.
		while (!vehicleArrivals.isEmpty() && vehicleArrivals.peek().arrivalTime <= time) {
			VehicleArrivalEntry entry = vehicleArrivals.poll();
			vehicleArrivalsIndex.remove(entry.vehicle.getId());

			Deque<DeferredDeparture> queue = deferredDepartures.get(entry.vehicle.getId());
			DeferredDeparture deferred = (queue != null) ? queue.pollFirst() : null;
			if (queue != null && queue.isEmpty()) {
				deferredDepartures.remove(entry.vehicle.getId());
			}
			if (deferred != null) {
				// Vehicle arrived -- serve the deferred agent (FIFO).
				MobsimAgent agent = deferred.agent;
				Id<Link> departureLinkId = deferred.linkId;
				Id<Link> destinationLinkId = agent.getDestinationLinkId();

				double driveTravelTime = 0.;
				if (agent instanceof PlanAgent planAgent) {
					Leg currentLeg = (Leg) planAgent.getCurrentPlanElement();
					NetworkRoute networkRoute = (NetworkRoute) currentLeg.getRoute();
					Person person = planAgent.getCurrentPlan().getPerson();
					Vehicle vehicle = this.vehicles.get(networkRoute.getVehicleId());
					driveTravelTime = calcQSimTravelTime(networkRoute, time, person, vehicle);
					networkRoute.setTravelTime(driveTravelTime);
				}

				addVehicleArrival(time + driveTravelTime, entry.vehicle, destinationLinkId);

				if (agent instanceof MobsimDriverAgent driverAgent) {
					internalInterface.getMobsim().getEventsManager().processEvent(
							new VehicleTeleportationDepartureEvent(time, driverAgent.getId(),
									entry.vehicle.getId(), departureLinkId, agent.getMode()));
				}

				boolean result = teleport.handleDeparture(time, agent, departureLinkId);
				Gbl.assertIf(result);
			} else {
				// Normal arrival -- park vehicle and wake waiting agents.
				QLinkI qLinkDest = (QLinkI) this.qNetsimEngine.getNetsimNetwork()
						.getNetsimLink(entry.destinationLinkId);
				qLinkDest.addParkedVehicle(entry.vehicle);
				qLinkDest.makeVehicleAvailableToNextDriver(entry.vehicle);
			}
		}
		teleport.doSimStep(time);
	}

	@Override
	public void beforeMobsim() {
		if (switchOffFISS()) {
			deflateVehicleTypes(matsimServices.getScenario(), this.fissConfigGroup);
			// (This is before the QVehicles are generated, so it wil be taken into account when generating them.)
		}
		teleport.beforeMobsim();
	}

	private void deflateVehicleTypes(Scenario scenario, FISSConfigGroup fissConfigGroup) {
		for (String sampledMode : fissConfigGroup.getSampledModes()) {
			for (VehicleType vehicleType : scenario.getVehicles().getVehicleTypes().values()) {
				if (vehicleType.hasNetworkMode() && vehicleType.getNetworkMode().equals(sampledMode)) {
					vehicleType.setPcuEquivalents(vehicleType.getPcuEquivalents() * fissConfigGroup.getSampleFactor());
				}
			}
		}
	}

	/**
	 * Calculates travel time to match QSim physical simulation timing:
	 * <ul>
	 * <li>Departure link is NOT traversed (vehicle goes from parking straight
	 * to the link's buffer via {@code addFromWait}).</li>
	 * <li>Each intermediate link is fully traversed.</li>
	 * <li>Arrival link IS fully traversed (agent arrives when the vehicle
	 * reaches the buffer at the downstream end).</li>
	 * <li>Each link-to-link transition crosses a node, costing one QSim time
	 * step.</li>
	 * <li>Per-link travel time is lower-bounded by the vehicle type's max
	 * speed (matching {@code DefaultLinkSpeedCalculator}).</li>
	 * </ul>
	 */
	private double calcQSimTravelTime(NetworkRoute networkRoute, double now, Person person, Vehicle vehicle) {
		boolean zeroLengthRoute = networkRoute.getStartLinkId().equals(networkRoute.getEndLinkId())
				&& networkRoute.getLinkIds().isEmpty();
		if (zeroLengthRoute) {
			return 0;
		}
		double vehicleMaxSpeed = vehicle != null ? vehicle.getType().getMaximumVelocity() : Double.POSITIVE_INFINITY;
		double timeStepSize = qsimConfig.getTimeStepSize();

		double totalTT = 0;
		// Intermediate links
		for (Id<Link> linkId : networkRoute.getLinkIds()) {
			Link link = network.getLinks().get(linkId);
			double linkTT = travelTime.getLinkTravelTime(link, now + totalTT, person, vehicle);
			totalTT += applyVehicleSpeedFloor(link, linkTT, vehicleMaxSpeed, timeStepSize);
		}
		// Arrival link
		Link endLink = network.getLinks().get(networkRoute.getEndLinkId());
		double arrivalLinkTT = travelTime.getLinkTravelTime(endLink, now + totalTT, person, vehicle);
		totalTT += applyVehicleSpeedFloor(endLink, arrivalLinkTT, vehicleMaxSpeed, timeStepSize);
		// Node transitions: one per link boundary crossed
		int numNodeTransitions = networkRoute.getLinkIds().size() + 1;
		totalTT += numNodeTransitions * timeStepSize;
		return totalTT;
	}

	/**
	 * Ensures a link travel time is not shorter than what the vehicle's max
	 * speed allows, and floors the result to the QSim time step size. The
	 * {@link TravelTime} object may return congested times (longer than
	 * free-speed) which are kept as-is, but if the link's free-speed exceeds
	 * the vehicle's max speed the travel time must be at least
	 * {@code link.length / vehicleMaxSpeed}. The result is always floored to
	 * match QSim's discrete time-step behaviour in
	 * {@code QueueWithBuffer.addFromUpstream}.
	 */
	private static double applyVehicleSpeedFloor(Link link, double linkTT,
			double vehicleMaxSpeed, double timeStepSize) {
		double minTT = link.getLength() / vehicleMaxSpeed;
		double effectiveTT = Math.max(linkTT, minTT);
		return timeStepSize * Math.floor(effectiveTT / timeStepSize);
	}

	@Override
	public void afterMobsim() {
		for (VehicleArrivalEntry entry : vehicleArrivals) {
			LOG.warn("FISS: vehicle {} still in transit at end of mobsim. arrivalTime={}, destinationLinkId={}",
					entry.vehicle.getId(), entry.arrivalTime, entry.destinationLinkId);
		}
		for (Map.Entry<Id<Vehicle>, Deque<DeferredDeparture>> mapEntry : deferredDepartures.entrySet()) {
			for (DeferredDeparture deferred : mapEntry.getValue()) {
				LOG.warn(
						"FISS: agent {} still waiting for vehicle {} at end of mobsim. desiredDepartureTime={}, linkId={}",
						deferred.agent.getId(), mapEntry.getKey(), deferred.desiredDepartureTime, deferred.linkId);
			}
		}
		teleport.afterMobsim();

		long total = teleportedTrips + simulatedTrips;
		double teleportedShare = total > 0 ? (double) teleportedTrips / total : 0.0;
		double fallbackShare = total > 0 ? (double) simulatedQsimFallbackTrips / total : 0.0;
		LOG.info("FISS stats (sampleFactor={}):" +
				"\n\ttotal departures: {}" +
				"\n\t  teleported:     {} (share={})" +
				"\n\t    of which deferred: {}" +
				"\n\t  simulated:      {}" +
				"\n\t    of which qsim fallback: {} (share={})",
				fissConfigGroup.getSampleFactor(),
				total,
				teleportedTrips, String.format("%.4f", teleportedShare),
				teleportedDeferredTrips,
				simulatedTrips,
				simulatedQsimFallbackTrips, String.format("%.4f", fallbackShare));
		teleportedTrips = 0;
		teleportedDeferredTrips = 0;
		simulatedTrips = 0;
		simulatedQsimFallbackTrips = 0;
	}

	private boolean switchOffFISS() {
		return (this.fissConfigGroup.isSwitchOffFISSLastIteration() && this.matsimServices.getConfig().controller().getLastIteration() == this.matsimServices.getIterationNumber());
		// yyyy note that with current implementation one canNOT change this to "switchOffFISS every x timesteps", since the pces are
		// deflated to their original values in onPrepareSim, and there is nothing in the code that multiply them to the FISS values
		// afterwards again.
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
		teleport.setInternalInterface(internalInterface);
	}

	private record VehicleArrivalEntry(double arrivalTime, QVehicle vehicle,
			Id<Link> destinationLinkId) implements Comparable<VehicleArrivalEntry> {
		@Override
		public int compareTo(VehicleArrivalEntry o) {
			return Double.compare(this.arrivalTime, o.arrivalTime);
		}
	}

	private record DeferredDeparture(double desiredDepartureTime, MobsimAgent agent, Id<Link> linkId) {
	}

	private void addVehicleArrival(double arrivalTime, QVehicle vehicle, Id<Link> destinationLinkId) {
		VehicleArrivalEntry entry = new VehicleArrivalEntry(arrivalTime, vehicle, destinationLinkId);
		vehicleArrivals.add(entry);
		vehicleArrivalsIndex.put(vehicle.getId(), entry);
	}

	private void removeVehicleArrival(VehicleArrivalEntry entry) {
		vehicleArrivals.remove(entry);
		vehicleArrivalsIndex.remove(entry.vehicle.getId());
	}

}
