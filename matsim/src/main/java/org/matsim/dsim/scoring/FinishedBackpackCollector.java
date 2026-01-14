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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class FinishedBackpackCollector implements ExperiencedPlansService {

	private static final Logger log = LogManager.getLogger(FinishedBackpackCollector.class);
	private final Set<FinishedBackpack> backpacks = ConcurrentHashMap.newKeySet();

	private final Config config;
	private final Population population;
	private final Network network;
	private final Communicator comm;
	private final ComputeNode selfNode;
	private final SerializationProvider serializer;
	private final Topology topology;

	private boolean finished = false;

	@Inject
	public FinishedBackpackCollector(Config config, Population population, Network network, Communicator comm, ComputeNode node, SerializationProvider serializer, Topology topology) {
		this.config = config;
		this.population = population;
		this.network = network;
		this.comm = comm;
		this.selfNode = node;
		this.serializer = serializer;
		this.topology = topology;
	}

	public void addBackpack(FinishedBackpack backpack) {
		backpacks.add(backpack);
	}

	public Collection<FinishedBackpack> backpacks() {
		return backpacks;
	}

	public void reset() {
		backpacks.clear();
	}

	public void exchangePlansForScoring() {

		var tag = 43;
		var backpacksByPartition = new Int2ObjectOpenHashMap<FinishedBackpackMsg>();
		// we must send a message to each node.
		for (ComputeNode node : topology) {
			backpacksByPartition.put(node.getRank(), new FinishedBackpackMsg(new HashSet<>()));
		}
		// sort backpacks by compute node
		for (var backpack : backpacks) {
			var partition = backpack.startingPartition();
			backpacksByPartition.get(partition)
				.add(backpack);
		}
		// send a message to each node
		for (var entry : backpacksByPartition.int2ObjectEntrySet()) {
			var targetRank = entry.getIntKey();
			if (targetRank != selfNode.getRank()) {
				comm.send(targetRank, entry.getValue(), tag, serializer);
			}
		}
		// all backpacks are send away
		backpacks.clear();

		// now receive a message from each othernode
		var received = recvFromOthers(tag);
		log.info("#{} Received {} backpacks from other nodes", selfNode.getRank(), received);
		var selfBackpacks = backpacksByPartition.get(selfNode.getRank()).backpacks();
		// add the received backpacks as well as our own backpacks.
		backpacks.addAll(received);
		backpacks.addAll(selfBackpacks);
	}

	private Collection<FinishedBackpack> recvFromOthers(int tag) {
		var recvCounter = new AtomicInteger();
		var receivedBackpacks = new HashSet<FinishedBackpack>();

		comm.recv(() -> recvCounter.get() < topology.getNodesCount() - 1, serializedMsg -> {
			int t = serializedMsg.getInt();
			if (tag != t)
				throw new IllegalStateException("Unexpected tag, got: %d, expected: %d".formatted(t, tag));
			serializedMsg.getInt(); // sender
			serializedMsg.getInt(); // receiver

			FinishedBackpackMsg msg = serializer.parse(serializedMsg);
			receivedBackpacks.addAll(msg.backpacks());
			recvCounter.incrementAndGet();
		});
		return receivedBackpacks;
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

	@Override
	public Map<Id<Person>, Plan> getExperiencedPlans() {
		return backpacks.stream()
			.map(b -> Tuple.of(b.personId(), b.experiencedPlan()))
			.collect(Collectors.toMap(Tuple::getFirst, Tuple::getSecond));
	}

	/**
	 * Collects all experienced plans on the head node, so that they can be written on that node.
	 */
	@Override
	public void finishIteration() {

		finished = true;
		var tag = 42; // select some tag

		// everyone except the head node sends to the head node
		if (selfNode.isHeadNode()) {
			// receive all the experienced plans except from ourselfs
			var received = recvFromOthers(tag);
			log.info("#{} recv on finish {}", selfNode.getRank(), received);
			this.backpacks.addAll(received);
		} else {
			// send the backpacks
			var msg = new FinishedBackpackMsg(backpacks);
			log.info("#{} sending on finish {}", selfNode.getRank(), msg);
			comm.send(0, msg, tag, serializer);
			backpacks.clear();
		}
	}

	record FinishedBackpackMsg(Collection<FinishedBackpack> backpacks) implements Message {

		void add(FinishedBackpack backpack) {
			backpacks.add(backpack);
		}
	}
}
