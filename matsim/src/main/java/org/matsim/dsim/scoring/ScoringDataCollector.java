package org.matsim.dsim.scoring;


import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.DistributedEventHandler;
import org.matsim.api.core.v01.events.handler.DistributedMode;
import org.matsim.api.core.v01.events.handler.ProcessingMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkPartitioning;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.dsim.SimStepMessage;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.dsim.simulation.SimStepMessaging;
import org.matsim.vehicles.Vehicle;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Event handler to collect data relevant for scoring. Each partition in a DSim has one event handler.
 * Agents arriving on a partition are registered with the handler, agents leaving are de-registered.
 * Each agent carries a backpack with data that is used for scoring and which contains state, such as in which
 * vehicle an agent is currently in.
 * <p>
 * DistributionMode MUST be PARTITION, and processing MUST be DIRECT, as each instance must be called only by the
 * partition it is responsible for, and it must be called right within the simulation timestep, because otherwise
 * agents might have left the partition already.
 */
@DistributedEventHandler(value = DistributedMode.PARTITION, processing = ProcessingMode.DIRECT)
public class ScoringDataCollector implements BasicEventHandler {

	private static final Logger log = LogManager.getLogger(ScoringDataCollector.class);

	private final IdMap<Person, BackPack> backpackByPerson = new IdMap<>(Person.class);
	private final IdMap<Vehicle, Set<BackPack>> backpackByVehicle = new IdMap<>(Vehicle.class);
	private final SimStepMessaging simStepMessaging;
	private final NetworkPartitioning partitioning;
	private final Network network;

	@Inject
	public ScoringDataCollector(SimStepMessaging simStepMessaging, Network network) {
		this.simStepMessaging = simStepMessaging;
		this.partitioning = network.getPartitioning();
		this.network = network;
	}

	public void registerAgent(MobsimAgent agent) {
		var backpack = new BackPack(agent.getId(), network);
		this.backpackByPerson.put(agent.getId(), backpack);
	}

	public void process(SimStepMessage msg) {
		for (var backpack : msg.backPacks()) {
			log.info("Processing backpack for person {} at {}", backpack.personId(), msg.simstep());
			this.backpackByPerson.put(backpack.personId(), backpack);
			if (backpack.isInVehicle()) {
				this.backpackByVehicle
					.computeIfAbsent(backpack.currentVehicle(), _ -> new HashSet<>())
					.add(backpack);
			}
		}
	}

	public void vehicleLeavesPartition(DistributedMobsimVehicle vehicle) {
		var driverId = vehicle.getDriver().getId();
		var targetPart = partitioning.getPartition(vehicle.getCurrentLinkId());
		personLeavingPartition(driverId, targetPart);

		for (var passenger : vehicle.getPassengers()) {
			personLeavingPartition(passenger.getId(), targetPart);
		}
	}

	public void teleportedPersonLeavesPartition(DistributedMobsimAgent agent) {

		var targetPart = partitioning.getPartition(agent.getDestinationLinkId());
		personLeavingPartition(agent.getId(), targetPart);
	}

	private void personLeavingPartition(Id<Person> id, int toPart) {
		var backPack = backpackByPerson.remove(id);
		if (backPack.isInVehicle()) {
			var backpacksInVehicle = backpackByVehicle
				.get(backPack.currentVehicle());
			backpacksInVehicle.remove(backPack);
			if (backpacksInVehicle.isEmpty()) {
				backpackByVehicle.remove(backPack.currentVehicle());
			}
		}
		log.info("Person {} leaving partition sending backpack to {}", id, toPart);
		simStepMessaging.collectBackPack(backPack, toPart);
	}

	@Override
	public void handleEvent(Event e) {
		log.info("Received event {}", e);

		if (BackPack.isRelevantForScoring(e) && e instanceof HasPersonId hpid) {
			getBackPack(e)
				.ifPresent(b -> b.addSpecialScoringEvent(e));
		}

		if (e instanceof PersonEntersVehicleEvent peve) {
			var backpack = backpackByPerson.get(peve.getPersonId());
			backpack.setCurrentVehicle(peve.getVehicleId());
			backpack.backpackPlan().handleEvent(peve);
			backpackByVehicle
				.computeIfAbsent(peve.getVehicleId(), _ -> new HashSet<>())
				.add(backpack);
		} else if (e instanceof PersonLeavesVehicleEvent plve) {
			var personId = plve.getPersonId();
			var backpack = backpackByPerson.get(personId);
			backpack.setCurrentVehicle(null);
			var backpacksInVehicle = backpackByVehicle.get(plve.getVehicleId());
			backpacksInVehicle.remove(backpack);
			if (backpacksInVehicle.isEmpty()) {
				backpackByVehicle.remove(plve.getVehicleId());
			}
		} else if (e instanceof LinkEnterEvent lee) {
			if (backpackByVehicle.containsKey(lee.getVehicleId())) {
				for (var backpack : backpackByVehicle.get(lee.getVehicleId())) {
					// TODO update the backpack with the link information
					backpack.backpackPlan().handleEvent(lee);
				}
			}
		}
//		else if (e instanceof LinkLeaveEvent lle) {
//			if (backpackByVehicle.containsKey(lle.getVehicleId())) {
//				for (var backpack : backpackByVehicle.get(lle.getVehicleId())) {
//					// TODO update backpack with link information
//				}
//			}
//		}
		else if (e instanceof ActivityStartEvent ase) {
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
		} else if (e instanceof TeleportationArrivalEvent tae) {
			var backpack = backpackByPerson.get(tae.getPersonId());
			backpack.backpackPlan().handleEvent(tae);
		} else if (e instanceof VehicleEntersTrafficEvent vete) {
			var backpackInVehicle = backpackByVehicle.get(vete.getVehicleId());
			for (var backpack : backpackInVehicle) {
				backpack.backpackPlan().handleEvent(vete);
			}
		} else if (e instanceof VehicleLeavesTrafficEvent vlte) {
			var backpackInVehicle = backpackByVehicle.get(vlte.getVehicleId());
			for (var backpack : backpackInVehicle) {
				backpack.backpackPlan().handleEvent(vlte);
			}
		}
	}

	private Optional<BackPack> getBackPack(Event e) {
		if (e instanceof HasPersonId hpid) {
			return Optional.ofNullable(backpackByPerson.get(hpid.getPersonId()));
		}
		return Optional.empty();
	}
}
