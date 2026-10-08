package org.matsim.dsim.scoring;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.DistributedEventHandler;
import org.matsim.api.core.v01.events.handler.DistributedMode;
import org.matsim.api.core.v01.events.handler.ProcessingMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.dsim.DSimComponentsMessageProcessor;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.dsim.NotifyAgentInsertion;
import org.matsim.core.mobsim.dsim.NotifyAgentPartitionTransfer;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.core.mobsim.qsim.interfaces.AfterMobsim;
import org.matsim.dsim.simulation.PartitionTransfer;
import org.matsim.vehicles.Vehicle;

import java.util.*;

/**
 * Event handler to collect data relevant for an agents backpack. At the moment, this includes data for experienced plans as well as data used for
 * scoring.
 * <p>
 * Each partition in a DSim has one event handler. Agents inserted into the simulation are registered with the handler of their starting partition.
 * Agents arriving on a partition are registered with the handler, agents leaving are de-registered. Each agent carries a backpack with data that is
 * used for scoring and which contains state, such as in which vehicle an agent is currently in.
 * <p>
 * DistributionMode MUST be PARTITION, and processing MUST be DIRECT, as each instance must be called only by the partition it is responsible for, and
 * it must be called right within the simulation timestep, because otherwise agents might have left the partition already.
 */
@DistributedEventHandler(value = DistributedMode.PARTITION, processing = ProcessingMode.DIRECT)
public class BackpackDataCollector
	implements BasicEventHandler, MobsimScopeEventHandler, NotifyAgentInsertion, NotifyAgentPartitionTransfer, QSimComponent,
	DSimComponentsMessageProcessor, AfterMobsim {

	private final Map<Id<Person>, Backpack> backpackByPerson = new HashMap<>();
	private final Map<Id<Vehicle>, Set<Backpack>> backpackByVehicle = new HashMap<>();
	private final Set<Id<Person>> ignoredAgents = new HashSet<>();

	private final PartitionTransfer partitionTransfer;
	private final Network network;
	private final Population population;

	private final FinishedBackpackCollector backpackCollector;
	private final Map<String, BackpackRouteProvider> providers;

	@Inject
	BackpackDataCollector(PartitionTransfer partitionTransfer, Network network, Population population,
						  FinishedBackpackCollector fbc, Map<String, BackpackRouteProvider> providers) {
		this.partitionTransfer = partitionTransfer;
		this.network = network;
		this.population = population;
		this.backpackCollector = fbc;
		this.providers = providers;
	}

	@Override
	public void onAgentLeavesPartition(DistributedMobsimAgent agent, int toPartition) {
		var backpack = backpackByPerson.remove(agent.getId());

		if (backpack == null)
			return; // this agent is ingored. We don't need to send anything.

		if (backpack.isInVehicle()) {
			var backpacksInVehicle = backpackByVehicle
				.get(backpack.currentVehicle());
			backpacksInVehicle.remove(backpack);
			if (backpacksInVehicle.isEmpty()) {
				backpackByVehicle.remove(backpack.currentVehicle());
			}
		}
		var message = backpack.toMessage();
		partitionTransfer.collect(message, toPartition);
	}

	/**
	 * Creates a backpack for each person of the population, so that persons which never produce events are scored as well. Agents which are not part
	 * of the population, such as transit drivers, are ignored.
	 */
	@Override
	public void onAgentInserted(DistributedMobsimAgent agent) {

		var id = agent.getId();
		if (!population.getPersons().containsKey(id)) {
			ignoredAgents.add(id);
			return;
		}
		if (backpackByPerson.containsKey(id))
			return;

		var startPartition = network.getPartitioning().getPartition(agent.getCurrentLinkId());
		var backpack = new Backpack(id, startPartition, providers);
		if (agent instanceof PlanAgent pa && pa.getCurrentPlanElement() instanceof Activity act) {
			backpack.backpackPlan().startWithActivity(act);
		}
		backpackByPerson.put(id, backpack);
	}

	@Override
	public void onAgentEntersPartition(DistributedMobsimAgent agent) {

		// we don't want to do anything if we don't know the person
		if (!population.getPersons().containsKey(agent.getId())) {
			ignoredAgents.add(agent.getId());
		}
	}

	private void processBackpackMessages(List<Message> messages, double now) {
		for (var m : messages) {
			var backpackMsg = (Backpack.Msg) m;
			var backpack = new Backpack(backpackMsg, providers);
			this.backpackByPerson.put(backpack.personId(), backpack);
			if (backpack.isInVehicle()) {
				this.backpackByVehicle
					.computeIfAbsent(backpack.currentVehicle(), _ -> new HashSet<>())
					.add(backpack);
			}
		}
	}

	public Map<Class<? extends Message>, DSimComponentsMessageProcessor.MessageHandler> getMessageHandlers() {
		return Map.of(
			Backpack.Msg.class, this::processBackpackMessages
		);
	}

	/**
	 * This method finishes all backpacks and passes the finished backpacks to the finished backpack collector. This is the terminating method. It
	 * removes all backpacks from this collector, so that events arriving afterwards fail with an exception.
	 */
	@Override
	public void afterMobsim() {
		for (var backpack : backpackByPerson.values()) {
			var finishedBackpack = backpack.finish();
			backpackCollector.addBackpack(finishedBackpack);
		}
		backpackByPerson.clear();
		backpackByVehicle.clear();
	}

	@Override
	public void handleEvent(Event e) {

		// 0. check registration before any bookkeeping, so that a failing check leaves no inconsistent state behind
		if (e instanceof TransitDriverStartsEvent tdse) {
			ignoredAgents.add(tdse.getDriverId());
		}
		if (e instanceof HasPersonId hpi) {
			assertRegistered(e, hpi.getPersonId());
		}

		// 1. bookeeping before dispatching the event
		if (e instanceof PersonEntersVehicleEvent peve) {
			bookkeepingPersonEntersVehicle(peve);
		} else if (e instanceof PersonContinuesInVehicleEvent pcive) {
			bookkeepingPersonContinuesInVehicle(pcive);
		} else if (e instanceof PersonStuckEvent pse) {
			bookkeepingStuckEvent(pse); // handle bookkeeping of stuck event up here, before it is passed to the backpack further down
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
		}
	}

	/**
	 * Adds persons which are not part of the population to the ignored agents. Persons of the population must have a backpack on this partition, as
	 * their experienced plan and score would be silently incomplete otherwise.
	 *
	 * @throws IllegalStateException if a person of the population has no backpack on this partition.
	 */
	private void assertRegistered(Event e, Id<Person> id) {
		if (ignoredAgents.contains(id) || backpackByPerson.containsKey(id))
			return;

		if (!population.getPersons().containsKey(id)) {
			ignoredAgents.add(id);
			return;
		}

		throw new IllegalStateException("""
			Received event '%s' at time %s for person '%s', but this person has no backpack on this partition. \
			\
			Likely causes: (1) an agent source puts population agents into the mobsim without calling \
			InsertableMobsim#insertAgentIntoMobsim; (2) a component produces an event for this person on a partition where the person is \
			not currently located; (3) a component produces events after BackpackDataCollector#afterMobsim, e.g. because it is registered \
			after the collector."""
			.formatted(e.getEventType(), e.getTime(), id));
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
		var backpack = backpackByPerson.get(plve.getPersonId());
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

	private void bookkeepingStuckEvent(PersonStuckEvent pse) {
		var backpack = backpackByPerson.get(pse.getPersonId());

		if (backpack != null && backpack.isInVehicle()) {
			var backpacksInVehicle = backpackByVehicle.get(backpack.currentVehicle());
			backpacksInVehicle.remove(backpack);
			if (backpacksInVehicle.isEmpty()) {
				backpackByVehicle.remove(backpack.currentVehicle());
			}
		}

	}
}
