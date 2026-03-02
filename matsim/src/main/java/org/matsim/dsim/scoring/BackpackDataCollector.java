package org.matsim.dsim.scoring;


import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.DistributedEventHandler;
import org.matsim.api.core.v01.events.handler.DistributedMode;
import org.matsim.api.core.v01.events.handler.ProcessingMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.dsim.SimStepMessage;
import org.matsim.core.mobsim.dsim.VehicleContainer;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.pt.AbstractTransitDriverAgent;
import org.matsim.core.mobsim.qsim.pt.TransitVehicle;
import org.matsim.dsim.simulation.AgentSourcesContainer;
import org.matsim.dsim.simulation.SimStepMessaging;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Event handler to collect data relevant for an agents backpack. At the moment, this includes data for experienced plans
 * as well as data used for scoring.
 * <p>
 * Each partition in a DSim has one event handler.
 * Agents arriving on a partition are registered with the handler, agents leaving are de-registered.
 * Each agent carries a backpack with data that is used for scoring and which contains state, such as in which
 * vehicle an agent is currently in.
 * <p>
 * DistributionMode MUST be PARTITION, and processing MUST be DIRECT, as each instance must be called only by the
 * partition it is responsible for, and it must be called right within the simulation timestep, because otherwise
 * agents might have left the partition already.
 */
@DistributedEventHandler(value = DistributedMode.PARTITION, processing = ProcessingMode.DIRECT)
public class BackpackDataCollector implements BasicEventHandler {

	private final Map<Id<Person>, BackPack> backpackByPerson = new HashMap<>();
	private final Map<Id<Vehicle>, Set<BackPack>> backpackByVehicle = new HashMap<>();
	private final Map<Id<Vehicle>, TransitInformation> transitInformation = new HashMap<>();
	private final Set<Id<Person>> ignoredAgents = new HashSet<>();

	private final SimStepMessaging simStepMessaging;
	private final Network network;
	private final Population population;

	private final AgentSourcesContainer asc;
	private final FinishedBackpackCollector backpackCollector;

	// transit schedule has to be optional, as not all scenarios have a transit schedule.
	@SuppressWarnings("FieldMayBeFinal")
	@Inject(optional = true)
	private TransitSchedule transitSchedule;

	@Inject
	public BackpackDataCollector(SimStepMessaging simStepMessaging, Network network, Population population, AgentSourcesContainer asc, FinishedBackpackCollector fbc) {
		this(simStepMessaging, network, population, null, asc, fbc);
	}

	/**
	 * Constructor for testing, which includes all dependencies
	 */
	BackpackDataCollector(SimStepMessaging simStepMessaging, Network network, Population population, TransitSchedule transitSchedule, AgentSourcesContainer asc, FinishedBackpackCollector fbc) {
		this.simStepMessaging = simStepMessaging;
		this.network = network;
		this.population = population;
		this.asc = asc;
		this.transitSchedule = transitSchedule;
		this.backpackCollector = fbc;
	}

	public void registerAgent(MobsimAgent agent) {

		// only persons which are part of the population are scored. Other agents such as transit drivers, or drt agents
		// can be ignored. If this assumption turns out to be incorect, we should probably mark agents as 'scorable' instead
		if (population.getPersons().containsKey(agent.getId())) {
			var startLink = agent.getCurrentLinkId();
			var startPartition = network.getPartitioning().getPartition(startLink);
			var backpack = new BackPack(agent.getId(), startPartition);
			this.backpackByPerson.put(agent.getId(), backpack);
		} else {
			ignoredAgents.add(agent.getId());
		}
	}

	public void process(SimStepMessage msg) {

		for (var backpack : msg.backPacks()) {
			this.backpackByPerson.put(backpack.personId(), backpack);
			if (backpack.isInVehicle()) {
				this.backpackByVehicle
					.computeIfAbsent(backpack.currentVehicle(), _ -> new HashSet<>())
					.add(backpack);
			}
		}

		// we tap into the vehicle messages to get the state of the transit vehicle. In particular, we need to connect the driver id
		// with the line and route information. This information is needed to recreate TransitPassengerRoutes.
		// It is a little dirty to do this, as the vehicle messages are kinda private to the NetworkTrafficEngine and we are creating
		// transit vehicles and drivers from the message in two places.
		// The alternative would have been to introduce new events for passengers entering and leaving pt and we decided not to do this
		// janek, marcel Dec' 2025
		for (VehicleContainer vehicle : msg.vehicles()) {
			var veh = asc.vehicleFromContainer(vehicle);

			// if the driver does not bring a backpack, we can ignore it.
			var driverId = veh.getDriver().getId();
			if (!backpackByPerson.containsKey(driverId)) {
				ignoredAgents.add(driverId);
			}
			if (veh instanceof TransitVehicle) {
				if (veh.getDriver() instanceof AbstractTransitDriverAgent td) {
					var route = td.getTransitRoute().getId();
					var line = td.getTransitLine().getId();
					transitInformation.put(veh.getId(), new TransitInformation(route, line));
				}
			}
		}

	}

	public void vehicleLeavesPartition(DistributedMobsimVehicle vehicle) {
		var driverId = vehicle.getDriver().getId();
		var targetPart = network.getPartitioning().getPartition(vehicle.getCurrentLinkId());

		// we don't want to send data for transit drivers, but we want to remove them from our bookkeeping
		if (ignoredAgents.contains(driverId)) {
			transitInformation.remove(driverId);
		} else {
			personLeavingPartition(driverId, targetPart);
		}

		for (var passenger : vehicle.getPassengers()) {
			personLeavingPartition(passenger.getId(), targetPart);
		}
	}

	public void teleportedPersonLeavesPartition(DistributedMobsimAgent agent) {
		var targetPart = network.getPartitioning().getPartition(agent.getDestinationLinkId());
		personLeavingPartition(agent.getId(), targetPart);
	}

	public void finishPerson(Id<Person> agentId) {

		if (!backpackByPerson.containsKey(agentId)) return;

		var backpack = backpackByPerson.remove(agentId);
		if (backpack.isInVehicle()) {
			backpackByVehicle.get(backpack.currentVehicle()).remove(backpack);
		}

		finishBackpack(backpack);
	}

	/**
	 * This method finishes all backpacks and passes the finished backpacks to the finished backpack collector.
	 * This is the terminating method. This collector will not clean up its state, as it is expected that a new
	 * instance of ScoringDataCollector will be created for the next iteration.
	 */
	public void finishAllPersons() {
		for (var backpack : backpackByPerson.values()) {
			finishBackpack(backpack);
		}
	}

	private void finishBackpack(BackPack backpack) {
		var finishedBackpack = backpack.finish(network, transitSchedule);
		backpackCollector.addBackpack(finishedBackpack);
	}

	private void personLeavingPartition(Id<Person> id, int toPart) {
		var backpack = backpackByPerson.remove(id);
		if (backpack.isInVehicle()) {
			var backpacksInVehicle = backpackByVehicle
				.get(backpack.currentVehicle());
			backpacksInVehicle.remove(backpack);
			if (backpacksInVehicle.isEmpty()) {
				backpackByVehicle.remove(backpack.currentVehicle());
			}
		}
		simStepMessaging.collectBackPack(backpack, toPart);
	}

	@Override
	public void handleEvent(Event e) {

		// short circuit on transit drivers and pass on special scoring events
		if (e instanceof HasPersonId hpi) {
			if (ignoredAgents.contains(hpi.getPersonId())) {
				return;
			}
			if (BackPack.isRelevantForScoring(e) && backpackByPerson.containsKey(hpi.getPersonId())) {
				var backpack = backpackByPerson.get(hpi.getPersonId());
				backpack.addSpecialScoringEvent(e);
			}
		}

		if (e instanceof TransitDriverStartsEvent tdse) {
			transitInformation.put(tdse.getVehicleId(), new TransitInformation(tdse.getTransitRouteId(), tdse.getTransitLineId()));
			ignoredAgents.add(tdse.getDriverId());
		} else if (e instanceof PersonContinuesInVehicleEvent pcive) {
			var backpacksInVehicle = backpackByVehicle.get(pcive.getVehicleId());
			var backpack = backpackByPerson.get(pcive.getPersonId());
			backpacksInVehicle.remove(backpack);
			backpackByVehicle.computeIfAbsent(pcive.getVehicleId(), _ -> new HashSet<>()).add(backpack);
			var transitInfo = transitInformation.get(pcive.getVehicleId());
			backpack.backpackPlan().startPtPart(transitInfo.line(), transitInfo.route());
			backpack.backpackPlan().handleEvent(pcive);

		} else if (e instanceof PersonEntersVehicleEvent peve) {
			var backpack = backpackByPerson.get(peve.getPersonId());

			backpackByVehicle
				.computeIfAbsent(peve.getVehicleId(), _ -> new HashSet<>())
				.add(backpack);

			var transitInfo = transitInformation.get(peve.getVehicleId());
			if (transitInfo != null) {
				backpack.backpackPlan().startPtPart(transitInfo.line(), transitInfo.route());
			}
			backpack.backpackPlan().handleEvent(peve);

		} else if (e instanceof PersonLeavesVehicleEvent plve) {
			var personId = plve.getPersonId();
			var backpack = backpackByPerson.get(personId);
			var backpacksInVehicle = backpackByVehicle.get(plve.getVehicleId());
			backpacksInVehicle.remove(backpack);
			if (backpacksInVehicle.isEmpty()) {
				backpackByVehicle.remove(plve.getVehicleId());
			}
		} else if (e instanceof LinkEnterEvent lee) {
			var backpacksInVehicle = backpackByVehicle.get(lee.getVehicleId());
			if (backpacksInVehicle != null) {
				for (var backpack : backpacksInVehicle) {
					backpack.backpackPlan().handleEvent(lee);
				}
			}
		} else if (e instanceof VehicleEntersTrafficEvent vete) {
			var backpacksInVehicle = backpackByVehicle.get(vete.getVehicleId());
			if (backpacksInVehicle != null) {
				for (var backpack : backpacksInVehicle) {
					backpack.backpackPlan().handleEvent(vete);
				}
			}
		} else if (e instanceof VehicleLeavesTrafficEvent vlte) {
			var backpacksInVehicle = backpackByVehicle.get(vlte.getVehicleId());
			if (backpacksInVehicle != null) {
				for (var backpack : backpacksInVehicle) {
					backpack.backpackPlan().handleEvent(vlte);
				}
			}
		} else if (e instanceof VehicleArrivesAtFacilityEvent vaafe) {
			var backpacksInVehicle = backpackByVehicle.get(vaafe.getVehicleId());
			if (backpacksInVehicle != null) {
				for (var backpack : backpacksInVehicle) {
					backpack.backpackPlan().handleEvent(vaafe);
				}
			}
		} else if (e instanceof VehicleDepartsAtFacilityEvent vdafe) {
			var backpacksInVehicle = backpackByVehicle.get(vdafe.getVehicleId());
			if (backpacksInVehicle != null) {
				for (var backpack : backpacksInVehicle) {
					backpack.backpackPlan().handleEvent(vdafe);
				}
			}
		} else if (e instanceof ActivityStartEvent ase) {
			var backpack = backpackByPerson.get(ase.getPersonId());
			backpack.backpackPlan().handleEvent(ase);
		} else if (e instanceof ActivityEndEvent aee) {
			var backpack = backpackByPerson.get(aee.getPersonId());
			backpack.backpackPlan().handleEvent(aee);
		} else if (e instanceof PersonDepartureEvent pde) {
			var backpack = backpackByPerson.get(pde.getPersonId());
			backpack.backpackPlan().handleEvent(pde);
		} else if (e instanceof PersonArrivalEvent pae) {
			var backpack = backpackByPerson.get(pae.getPersonId());
			backpack.backpackPlan().handleEvent(pae);
			backpack.backpackPlan().finishLeg(network, transitSchedule);
		} else if (e instanceof TeleportationArrivalEvent tae) {
			var backpack = backpackByPerson.get(tae.getPersonId());
			backpack.backpackPlan().handleEvent(tae);
		} else if (e instanceof PersonStuckEvent pse) {
			var backpack = backpackByPerson.get(pse.getPersonId());
			if (backpack != null) {
				backpack.backpackPlan().handleEvent(pse);
				finishPerson(pse.getPersonId());
			}
		}
	}

	private record TransitInformation(Id<TransitRoute> route, Id<TransitLine> line) {

	}
}
