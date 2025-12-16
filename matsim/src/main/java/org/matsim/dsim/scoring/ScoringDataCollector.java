package org.matsim.dsim.scoring;


import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.handler.DistributedEventHandler;
import org.matsim.api.core.v01.events.handler.DistributedMode;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.dsim.BackPack;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.dsim.SimStepMessage;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.population.PopulationUtils;
import org.matsim.dsim.simulation.SimStepMessaging;

public class ScoringDataCollector {

	private static final Logger log = LogManager.getLogger(ScoringDataCollector.class);
	private final IdMap<Person, BackPack> backPacks = new IdMap<>(Person.class);
	private final SimStepMessaging simStepMessaging;
	private final NetworkPartition partition;

	@Inject
	public ScoringDataCollector(SimStepMessaging simStepMessaging, NetworkPartition partition, EventsManager em) {
		this.simStepMessaging = simStepMessaging;
		this.partition = partition;
		//em.addHandler(() -> this);
	}

	public void registerAgent(MobsimAgent agent) {
		var backpack = new BackPack(agent.getId(), PopulationUtils.createPlan());
		this.backPacks.put(agent.getId(), backpack);
	}

	public void process(SimStepMessage msg) {
		for (var backpack : msg.backPacks()) {
			log.info("Processing backpack for person {} at {} on partition: {}", backpack.personId(), msg.simstep(), partition.getIndex());
			this.backPacks.put(backpack.personId(), backpack);
		}
	}

	public void vehicleLeavesPartition(DistributedMobsimVehicle vehicle, int toPart) {
		var driverId = vehicle.getDriver().getId();
		personLeavingPartition(driverId, toPart);

		for (var passenger : vehicle.getPassengers()) {
			personLeavingPartition(passenger.getId(), toPart);
		}
	}

	public void teleportedPersonLeavesPartition(Id<Person> personId, int toPart) {
		personLeavingPartition(personId, toPart);
	}

	private void personLeavingPartition(Id<Person> id, int toPart) {
		var backPack = backPacks.remove(id);
		log.info("Person {} leaving partition {}, sending backpack at", id, partition.getIndex());
		simStepMessaging.collectBackPack(backPack, toPart);
	}
}
