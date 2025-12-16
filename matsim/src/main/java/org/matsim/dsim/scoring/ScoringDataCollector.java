package org.matsim.dsim.scoring;


import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkPartitioning;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.dsim.BackPack;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;
import org.matsim.core.mobsim.dsim.SimStepMessage;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.population.PopulationUtils;
import org.matsim.dsim.simulation.SimStepMessaging;

public class ScoringDataCollector {

	private static final Logger log = LogManager.getLogger(ScoringDataCollector.class);
	private final IdMap<Person, BackPack> backPacks = new IdMap<>(Person.class);
	private final SimStepMessaging simStepMessaging;
	private final NetworkPartitioning partitioning;

	@Inject
	public ScoringDataCollector(SimStepMessaging simStepMessaging, Network network) {
		this.simStepMessaging = simStepMessaging;
		this.partitioning = network.getPartitioning();
	}

	public void registerAgent(MobsimAgent agent) {
		var backpack = new BackPack(agent.getId(), PopulationUtils.createPlan());
		this.backPacks.put(agent.getId(), backpack);
	}

	public void process(SimStepMessage msg) {
		for (var backpack : msg.backPacks()) {
			log.info("Processing backpack for person {} at {}", backpack.personId(), msg.simstep());
			this.backPacks.put(backpack.personId(), backpack);
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
		var backPack = backPacks.remove(id);
		log.info("Person {} leaving partition sending backpack at", id);
		simStepMessaging.collectBackPack(backPack, toPart);
	}
}
