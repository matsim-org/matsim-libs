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
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.dsim.SimStepMessage;
import org.matsim.core.mobsim.dsim.VehicleContainer;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.dsim.simulation.AgentSourcesContainer;
import org.matsim.dsim.simulation.PartitionTransfer;
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

	private final Map<Id<Person>, Backpack> backpackByPerson = new HashMap<>();
	private final Map<Id<Vehicle>, Set<Backpack>> backpackByVehicle = new HashMap<>();
	private final Set<Id<Person>> ignoredAgents = new HashSet<>();

	//private final SimStepMessaging simStepMessaging;
	private final PartitionTransfer partitionTransfer;
	private final Network network;
	private final Population population;

	private final AgentSourcesContainer asc;
	private final FinishedBackpackCollector backpackCollector;
	private final Map<String, BackpackRouteProvider> providers;

	@Inject
	BackpackDataCollector(PartitionTransfer partitionTransfer, Network network, Population population,
						  AgentSourcesContainer asc, FinishedBackpackCollector fbc, Map<String, BackpackRouteProvider> providers) {
		this.partitionTransfer = partitionTransfer;
		//this.simStepMessaging = simStepMessaging;
		this.network = network;
		this.population = population;
		this.asc = asc;
		this.backpackCollector = fbc;
		this.providers = providers;
	}

	public void registerAgent(MobsimAgent agent) {

		// only persons which are part of the population are scored. Other agents such as transit drivers, or drt agents
		// can be ignored. If this assumption turns out to be incorect, we should probably mark agents as 'scorable' instead
		if (population.getPersons().containsKey(agent.getId())) {
			var startLink = agent.getCurrentLinkId();
			var startPartition = network.getPartitioning().getPartition(startLink);
			var backpack = new Backpack(agent.getId(), startPartition, providers);
			this.backpackByPerson.put(agent.getId(), backpack);
		} else {
			ignoredAgents.add(agent.getId());
		}
	}

	public void process(SimStepMessage msg) {

		for (var backpackMsg : msg.backpacks()) {
			var backpack = new Backpack(backpackMsg, providers);
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
		}

	}

	public void vehicleLeavesPartition(DistributedMobsimVehicle vehicle) {
		var driverId = vehicle.getDriver().getId();
		var targetPart = network.getPartitioning().getPartition(vehicle.getCurrentLinkId());

		if (!ignoredAgents.contains(driverId)) {
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

	private void finishBackpack(Backpack backpack) {
		var finishedBackpack = backpack.finish();
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
		var message = backpack.toMessage();
		partitionTransfer.collect(message, toPart);
	}

	@Override
	public void handleEvent(Event e) {

		// 1. bookeeping which adds things
		if (e instanceof TransitDriverStartsEvent tdse) {
			ignoredAgents.add(tdse.getDriverId());
		} else if (e instanceof PersonEntersVehicleEvent peve) {
			bookkeepingPersonEntersVehicle(peve);
		} else if (e instanceof PersonContinuesInVehicleEvent pcive) {
			bookkeepingPersonContinuesInVehicle(pcive);
		}

		// 2. take care of all person-related events
		if (e instanceof HasPersonId hpi) {
			handlePersonEvent(e, hpi);
		}
		// 3. take care of all vehicle-related events. Dispatch event to all persons in that vehicle
		else if (e instanceof HasVehicleId hvi) {
			handleVehicleEvent(e, hvi);
		}

		// 4. bookkeeping which removes things
		if (e instanceof PersonLeavesVehicleEvent plve) {
			bookkeepingPersonLeavesVehicle(plve);
		} else if (e instanceof PersonStuckEvent pse) {
			handleStuckEvent(pse);
		}
	}

	private void bookkeepingPersonEntersVehicle(PersonEntersVehicleEvent peve) {
		if (ignoredAgents.contains(peve.getPersonId())) {
			return;
		}

		var backpack = backpackByPerson.get(peve.getPersonId());
		backpackByVehicle
			.computeIfAbsent(peve.getVehicleId(), _ -> new HashSet<>())
			.add(backpack);
	}

	private void bookkeepingPersonLeavesVehicle(PersonLeavesVehicleEvent plve) {
		if (ignoredAgents.contains(plve.getPersonId())) {
			return;
		}
		var personId = plve.getPersonId();
		var backpack = backpackByPerson.get(personId);
		var backpacksInVehicle = backpackByVehicle.get(plve.getVehicleId());
		backpacksInVehicle.remove(backpack);
		if (backpacksInVehicle.isEmpty()) {
			backpackByVehicle.remove(plve.getVehicleId());
		}
	}

	private void bookkeepingPersonContinuesInVehicle(PersonContinuesInVehicleEvent pcive) {
		var backpacksInVehicle = backpackByVehicle.get(pcive.getVehicleId());
		var backpack = backpackByPerson.get(pcive.getPersonId());
		backpacksInVehicle.remove(backpack);
		backpackByVehicle.computeIfAbsent(pcive.getVehicleId(), _ -> new HashSet<>()).add(backpack);
	}

	private void handlePersonEvent(Event e, HasPersonId hpi) {
		if (ignoredAgents.contains(hpi.getPersonId())) {
			return;
		}
		if (backpackByPerson.containsKey(hpi.getPersonId())) {
			// pass on the event to the bacpack plan in any case
			var backpack = backpackByPerson.get(hpi.getPersonId());
			backpack.backpackPlan().handleEvent(e);

			// if it is a special scoring event, also collect it separately
			if (Backpack.isRelevantForScoring(e)) {
				backpack.addSpecialScoringEvent(e);
			}
		}
	}

	private void handleVehicleEvent(Event e, HasVehicleId hvi) {
		var backpacksInVehicle = backpackByVehicle.get(hvi.getVehicleId());
		if (backpacksInVehicle != null) {
			for (var backpack : backpacksInVehicle) {
				backpack.backpackPlan().handleEvent(e);
			}
		}
	}

	private void handleStuckEvent(PersonStuckEvent pse) {
		var backpack = backpackByPerson.get(pse.getPersonId());
		if (backpack != null) {
			finishPerson(pse.getPersonId());
		}
	}
}
