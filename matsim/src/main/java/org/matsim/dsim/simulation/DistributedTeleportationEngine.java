package org.matsim.dsim.simulation;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.mobsim.dsim.DistributedDepartureHandler;
import org.matsim.core.mobsim.dsim.DistributedMobsimAgent;
import org.matsim.core.mobsim.dsim.DistributedMobsimEngine;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;

import java.util.*;

public class DistributedTeleportationEngine implements DistributedDepartureHandler, DistributedMobsimEngine, TeleportationEngine {

	private static final Logger log = LogManager.getLogger(DistributedTeleportationEngine.class);

	/**
	 * Diagnostic for the one-step shifts seen in teleported arrivals between two otherwise identical runs. A teleported leg is sent to the
	 * partition of its destination link, which may be any partition, while {@code SimProcess.waitForOtherParts} only synchronises with the
	 * network neighbours - so for a target that is not a neighbour, nothing pins down the step in which the message is applied. That only
	 * changes an arrival when the leg is due within about one step of being handed over, which is what {@code tightSlack} counts.
	 * <p>
	 * Counts are logged per partition and iteration, the first few non-neighbour hand-overs in full. Remove once the question is settled.
	 */
	private int teleportsLocal;
	private int teleportsToNeighbor;
	private int teleportsToNonNeighbor;
	private int teleportsToNonNeighborTightSlack;
	private int samplesLogged;

	private static final int MAX_SAMPLES = 10;
	private static final double TIGHT_SLACK = 2.0;

	/**
	 * Diagnostic that splits the two possible origins of a teleported arrival landing one step apart in two otherwise identical runs: the
	 * exit time the departure was given, which comes out of the plan, or the step the queue released it in. The departure checksum covers
	 * {@code (person, exitTime)} of every teleported departure this partition handled, the release checksum {@code (person, arrivalStep)}
	 * of every agent it released. Both are sums, so they do not depend on the order the entries were added in, which is the point: they
	 * compare two runs, not two orders.
	 * <p>
	 * Equal departure checksums with differing release checksums put the fault in the release path; differing departure checksums put it
	 * upstream of the mobsim, in the plan the router wrote. Two longs and two counts per partition and iteration, logged in
	 * {@link #afterMobsim()}. Remove once the question is settled.
	 */
	private long departureChecksum;
	private long releaseChecksum;
	private int departuresCounted;
	private int releasesCounted;

	private final EventsManager em;
	/**
	 * Ordered by exit time, ties broken by the person id. Without the tie-break a {@link PriorityQueue} releases agents that are due in the
	 * same time step in an order that follows its insertion history rather than the agents, and that history need not be the same in two
	 * runs: teleportation hand-overs to a partition that is not a network neighbour are not covered by the per-step barrier, so they can be
	 * applied in a different step. That does not move an arrival, because such a leg is due hours later, but it does change the shape of
	 * this queue - and the order in which agents are released here decides the order of the departures that follow.
	 */
	private final Queue<TeleportationEntry> personsTeleporting = new PriorityQueue<>(
		Comparator.comparingDouble(TeleportationEntry::exitTime)
			.thenComparing(entry -> entry.person().getId())
	);
	private final PartitionTransfer partitionTransfer;
	private final AgentSourcesContainer asc;

	private InternalInterface internalInterface;
	// store the current time, so we can use it for after sim.
	private double now;

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	@Inject
	DistributedTeleportationEngine(EventsManager em, PartitionTransfer partitionTransfer, AgentSourcesContainer asc) {
		this.partitionTransfer = partitionTransfer;
		this.asc = asc;
		this.em = em;
	}

	@Override
	public double priority() {
		return Double.NEGATIVE_INFINITY;
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {

		if (!(agent instanceof DistributedMobsimAgent person)) {
			throw new IllegalStateException("Teleported agent must implement DistributedMobsimAgent");
		}

		// the original engine has a travel time check. The default config parameter (qsim.usingTravelTimeCheckInTeleportation)
		// is 'false' and is not settable from xml. This is usually an indicator that a feature is rarely used.
		// calculate travel time
		if (!person.getExpectedTravelTime().isDefined()) {
			throw new IllegalStateException(String.format("Expected travel time is undefined for %s", agent.getId()));
		}

		var travelTime = person.getExpectedTravelTime().seconds();
		var exitTime = now + travelTime;

		departureChecksum += checksumOf(person.getId(), exitTime);
		departuresCounted++;

		if (partitionTransfer.isLocal(person.getDestinationLinkId())) {
			teleportsLocal++;
			personsTeleporting.add(new TeleportationEntry(person, exitTime));
		} else {
			var toPartition = partitionTransfer.getPartitionIndex(person.getDestinationLinkId());
			countHandover(person, toPartition, now, exitTime);
			internalInterface.notifyAgentLeavesPartition(person, toPartition);
			partitionTransfer.collect(new TeleportationMessage(person.getClass(), person.toMessage(), exitTime), toPartition);
		}

		return true;
	}

	@Override
	public Map<Class<? extends Message>, MessageHandler> getMessageHandlers() {
		return Map.of(
			TeleportationMessage.class,
			this::processTeleportationMessages
		);
	}

	private void processTeleportationMessages(List<Message> messages, double now) {
		for (var m : messages) {
			var msg = (TeleportationMessage) m;
			if (msg.exitTime() < now) {
				throw new IllegalStateException("Teleportation message was received too late. Exit time is supposed to be " +
					msg.exitTime() + " but simulation time is already at: " + now + ". This might happen, if partitions " +
					"diverge in simulation time. We don't really have a solution to this problem yet. However, this " +
					"error might be an indicator, that the speed of the teleported leg is too fast.");
			}
			var agent = asc.agentFromMessage(msg.type(), msg.agent());
			personsTeleporting.add(new TeleportationEntry(agent, msg.exitTime()));
			internalInterface.notifyAgentEntersPartition(agent);
		}
	}

	@Override
	public void doSimStep(double now) {
		this.now = now;

		while (firstPersonReady(now)) {
			TeleportationEntry entry = personsTeleporting.remove();
			DistributedMobsimAgent person = entry.person();

			releaseChecksum += checksumOf(person.getId(), now);
			releasesCounted++;

			person.notifyArrivalOnLinkByNonNetworkMode(person.getDestinationLinkId());

			String mode = person.getMode();
			Double distance = person.getExpectedTravelDistance();
			em.processEvent(new TeleportationArrivalEvent(now, person.getId(), distance, mode));

			person.endLegAndComputeNextState(now);
			internalInterface.arrangeNextAgentState(person);
		}
	}

	/**
	 * See {@link #teleportsToNonNeighbor}.
	 */
	private void countHandover(DistributedMobsimAgent person, int toPartition, double now, double exitTime) {
		boolean neighbor = partitionTransfer.isNeighbor(toPartition);
		double slack = exitTime - now;

		if (neighbor) {
			teleportsToNeighbor++;
			return;
		}

		teleportsToNonNeighbor++;
		if (slack <= TIGHT_SLACK)
			teleportsToNonNeighborTightSlack++;

		if (samplesLogged < MAX_SAMPLES) {
			samplesLogged++;
			log.info("teleport hand-over to a non-neighbour partition: agent={} mode={} part {}->{} now={} exitTime={} slack={}s",
				person.getId(), person.getMode(), partitionTransfer.getIndex(), toPartition, now, exitTime, slack);
		}
	}

	/**
	 * See {@link #departureChecksum}. The person id goes in as the hash of its string form rather than as
	 * {@link org.matsim.api.core.v01.Id#index()}, so that the value does not depend on the order the ids were created in, and the double
	 * goes in by its bits, so that it is exact. The finalizer is the one from SplitMix64: without it the sum of the terms would lose the
	 * high bits of what distinguishes two entries.
	 */
	private static long checksumOf(Id<Person> person, double value) {
		long h = (person.toString().hashCode() & 0xFFFFFFFFL) * 0x9E3779B97F4A7C15L + Double.doubleToLongBits(value);
		h = (h ^ (h >>> 33)) * 0xFF51AFD7ED558CCDL;
		h = (h ^ (h >>> 33)) * 0xC4CEB9FE1A85EC53L;
		return h ^ (h >>> 33);
	}

	/**
	 * See {@link #departureChecksum}. Package-private for the test.
	 */
	long departureChecksum() {return departureChecksum;}

	/**
	 * See {@link #departureChecksum}. Package-private for the test.
	 */
	long releaseChecksum() {return releaseChecksum;}

	@Override
	public void afterMobsim() {
		log.info("teleport hand-overs in partition {}: local={} toNeighbour={} toNonNeighbour={} (of which due within {}s: {})",
			partitionTransfer.getIndex(), teleportsLocal, teleportsToNeighbor, teleportsToNonNeighbor, TIGHT_SLACK, teleportsToNonNeighborTightSlack);
		log.info("teleport checksums in partition {}: departures={} over {} entries, releases={} over {} entries",
			partitionTransfer.getIndex(), departureChecksum, departuresCounted, releaseChecksum, releasesCounted);
		teleportsLocal = 0;
		teleportsToNeighbor = 0;
		teleportsToNonNeighbor = 0;
		teleportsToNonNeighborTightSlack = 0;
		samplesLogged = 0;
		departureChecksum = 0;
		releaseChecksum = 0;
		departuresCounted = 0;
		releasesCounted = 0;

		for (var entry : personsTeleporting) {
			var person = entry.person();
			em.processEvent(new PersonStuckEvent(now, person.getId(), person.getDestinationLinkId(), person.getMode()));
		}
		personsTeleporting.clear();
	}

	@Override
	public double getEnginePriority() {
		return -10.; // this engine should handle things last.
	}

	private boolean firstPersonReady(double now) {
		return !personsTeleporting.isEmpty() && personsTeleporting.peek().exitTime() <= now;
	}

	@Override
	public Collection<AgentSnapshotInfo> addAgentSnapshotInfo(Collection<AgentSnapshotInfo> positions) {
		throw new RuntimeException("Snapshot Positions are not implemented for Distributed Teleportation Engine. This method is only here because the 'TeleportationInterface' requires it.");
	}

	private record TeleportationEntry(DistributedMobsimAgent person, double exitTime) {
	}

	// intentionally public, so that the SerializationProvider can pick up this class
	public record TeleportationMessage(Class<? extends DistributedMobsimAgent> type, Message agent, double exitTime) implements Message {}
}
