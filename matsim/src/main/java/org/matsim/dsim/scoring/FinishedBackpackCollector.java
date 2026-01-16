package org.matsim.dsim.scoring;

import com.google.inject.Inject;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.Topology;
import org.matsim.api.core.v01.messages.ComputeNode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.communication.Communicator;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.ExperiencedPlansService;
import org.matsim.core.serialization.SerializationProvider;
import org.matsim.core.utils.collections.Tuple;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This class holds all finished backpacks of a simulation. There should be one instance per compute node. All partitions running on that node
 * can add their backpacks to this collector, as it is thread-safe.
 * <p>
 * The collector has two methods for synchronizing with other compute nodes:
 * 1. {@link #exchangePlansForScoring()} sends backpacks to the starting compute node of agents, as only that node is expected to have the corresponding
 * Person in its population
 * 2. {@link #finishIteration()} sends all backpacks to the head node. This way the head node can write all experienced plans to disk.
 * <p>
 * A collector instance may be re-used between iterations but must be {@link #reset()} before the next iteration starts.
 */
public class FinishedBackpackCollector implements ExperiencedPlansService {

	private static final Logger log = LogManager.getLogger(FinishedBackpackCollector.class);

	private final Config config;
	private final Population population;
	private final Network network;
	private final ComputeNode selfNode;
	private final Topology topology;
	private final Communicator comm;
	private final SerializationProvider serializer;

	private Set<FinishedBackpack> backpacks = ConcurrentHashMap.newKeySet();
	private boolean finished = false;

	@Inject
	public FinishedBackpackCollector(Config config, Population population, Network network, ComputeNode node, Topology topology, Communicator comm, SerializationProvider serializer) {
		this.config = config;
		this.population = population;
		this.network = network;
		this.selfNode = node;
		this.topology = topology;
		this.comm = comm;
		this.serializer = serializer;
	}

	/**
	 * Add a backpack to the collector. This method is thread-safe.
	 *
	 * @throws IllegalStateException if the collector is finished. The collector must be {@link #reset()} after {@link #finishIteration()}
	 *                               before it accepts backpacks again.
	 */

	public void addBackpack(FinishedBackpack backpack) {

		if (finished) {
			throw new IllegalStateException("FinishedBackpackCollector is finished. Call reset() (possibly, during iteration start), before attempting to adding backpacks.");
		}

		backpacks.add(backpack);
	}

	/**
	 * @return reference to the collected backpack collection.
	 */
	public Collection<FinishedBackpack> backpacks() {
		return backpacks;
	}

	/**
	 * Deletes collected backpacks and resets the collector state.
	 */
	public void reset() {
		backpacks.clear();
		finished = false;
	}

	/**
	 * All backpacks are sent to the starting compute node of agents. This method must be called before handing experienced plans from
	 * collected backpacks to the scoring, as agents are supposed to be scored on their starting compute node.
	 * <p>
	 * This method is a barrier, as it only proceeds after all compute nodes have exchanged backpacks.
	 */
	public void exchangePlansForScoring() {

		var backpacksByRank = new Int2ObjectOpenHashMap<FinishedBackpackMsg>();
		var ownBackpacks = new HashSet<FinishedBackpack>();

		// we must send a message to each node.
		for (ComputeNode node : topology) {
			if (node.getRank() != selfNode.getRank()) {
				backpacksByRank.put(node.getRank(), new FinishedBackpackMsg(new HashSet<>()));
			}
		}
		// sort backpacks by compute node
		for (var backpack : backpacks) {
			var partition = backpack.startingPartition();
			var targetNode = topology.getNodeByPartition(partition);
			if (targetNode.getRank() == selfNode.getRank()) {
				ownBackpacks.add(backpack);
			} else {
				backpacksByRank.get(targetNode.getRank())
					.add(backpack);
			}
		}
		// send and receive backpacks
		var receivedMsgs = comm.allToAll(backpacksByRank, serializer);
		var receivedBackpacks = receivedMsgs.stream()
			.flatMap(msg -> msg.backpacks().stream())
			.collect(Collectors.toSet());
		// only store the backpacks we own.
		this.backpacks = ownBackpacks;
		this.backpacks.addAll(receivedBackpacks);

		log.trace("#{} Received {} backpacks from other nodes. Total number of backpacks to score is: {}", selfNode.getRank(), receivedBackpacks.size(), backpacks.size());
	}

	@Override
	public void writeExperiencedPlans(String filename) {

		// only the head node should write the plans.
		if (!selfNode.isHeadNode()) return;

		if (!finished) {
			throw new IllegalStateException("writeExperiencedPlans() called before finishIteration()");
		}

		var tmpPop = PopulationUtils.createPopulation(config, network);
		for (var backpack : backpacks) {
			var person = population.getFactory().createPerson(backpack.personId());
			var origPerson = population.getPersons().get(backpack.personId());

			if (origPerson == null) {
				throw new IllegalStateException("Person %s not found in population".formatted(backpack.personId()));
			}
			for (var attrEntry : origPerson.getAttributes().getAsMap().entrySet()) {
				person.getAttributes().putAttribute(attrEntry.getKey(), attrEntry.getValue());
				// note that this is not a completely deep copy.  Should not be a problem since we only write to file, but in the end we never know.  kai, oct'25
			}
			var experiencedPlan = backpack.experiencedPlan();
			experiencedPlan.setScore(origPerson.getSelectedPlan().getScore());
			// yyyy this is somewhat dangerous ... since there is no guarantee that this is indeed the correct plan.
			// ... up to here.
			// There is EquilTwoAgentsTest, where I switched on the experienced plans writing in the scoring config.
			// W/o the code lines above, the person attributes are not written.  W/ the code lines, they are written.
			// This is, evidently, not a true regression test, but at least I had a look if the functionality works at all. kai, oct'25

			person.addPlan(experiencedPlan);
			tmpPop.addPerson(person);
		}
		new PopulationWriter(tmpPop, null).write(filename);
	}

	/**
	 * Returns a map of experienced plans, indexed by person ID.
	 * <p>
	 * NOTE: The map is computed every time this method is called.
	 */
	@Override
	public Map<Id<Person>, Plan> getExperiencedPlans() {
		return backpacks.stream()
			.map(b -> Tuple.of(b.personId(), b.experiencedPlan()))
			.collect(Collectors.toMap(Tuple::getFirst, Tuple::getSecond));
	}

	/**
	 * Collects all experienced plans on the head node so that they can be written to disk on that node.
	 * <p>
	 * This method is a barrier. Compute nodes can only proceed after the head node has received a message from all other nodes.
	 */
	@Override
	public void finishIteration() {

		finished = true;

		// everyone except the head node sends to the head node
		if (selfNode.isHeadNode()) {
			try {
				log.info("Waiting for 1 second on the head node to simulate imbalance");
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			// receive all the experienced plans except from ourselfs
			var received = comm.gatherFromAll(FinishedBackpackMsg.class, serializer);
			log.trace(() -> traceMsg(received));

			for (var msg : received) {
				this.backpacks.addAll(msg.backpacks());
			}
		} else {
			// send the backpacks
			var msg = new FinishedBackpackMsg(backpacks);
			log.trace("#{} sending {} backpacks on finish", selfNode.getRank(), backpacks.size());
			comm.gatherTo(0, msg, serializer);
			backpacks.clear();
		}
	}

	private String traceMsg(List<FinishedBackpackMsg> msgs) {
		var backpackCount = msgs.stream()
			.mapToInt(msg -> msg.backpacks().size())
			.sum();
		return "#" + selfNode.getRank() + " received " + backpackCount + " backpacks on finish";
	}

	/**
	 * Must be public, so the serialization provider can pick it up automatically
	 *
	 * @param backpacks
	 */
	public record FinishedBackpackMsg(Collection<FinishedBackpack> backpacks) implements Message {

		void add(FinishedBackpack backpack) {
			backpacks.add(backpack);
		}
	}
}
