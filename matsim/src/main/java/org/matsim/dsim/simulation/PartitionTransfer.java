package org.matsim.dsim.simulation;

import com.google.inject.Inject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.MobsimMessageCollector;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.api.core.v01.network.NetworkPartitioning;
import org.matsim.dsim.MessageBroker;

import java.util.ArrayList;
import java.util.List;

public class PartitionTransfer implements MobsimMessageCollector {

	private final NetworkPartitioning networkPartitioning;
	private final NetworkPartition partition;
	private final MessageBroker messageBroker;

	private final Int2ObjectMap<Int2ObjectMap<List<Message>>> messages = new Int2ObjectOpenHashMap<>();

	@Inject
	public PartitionTransfer(Network network, NetworkPartition partition, MessageBroker messageBroker) {
		this.networkPartitioning = network.getPartitioning();
		this.partition = partition;
		this.messageBroker = messageBroker;
	}

	public void collect(Message message, Id<Link> targetLink) {
		var targetPartition = getPartitionIndex(targetLink);
		collect(message, targetPartition);
	}

	public void collect(Message message, int targetRank) {
		var rankMessages = messages.computeIfAbsent(targetRank, _ -> new Int2ObjectOpenHashMap<>());
		var messagesForType = rankMessages.computeIfAbsent(message.getType(), _ -> new ArrayList<>());
		messagesForType.add(message);
	}

	public void send(double now, IntSet neighborPartitions) {
		for (var partitions : messages.int2ObjectEntrySet()) {
			var toPartition = partitions.getIntKey();
			for (var types : partitions.getValue().int2ObjectEntrySet()) {
				var messageType = types.getIntKey();
				var messages = types.getValue();
				var simStepMessage = new SimStepMessage(now, messageType, messages);
				messageBroker.send(simStepMessage, toPartition);
			}
			// remove reference to the list of message for given type.
			partitions.getValue().clear();
		}

		// Ensure every neighbor partition receives at least a sync heartbeat this time step,
		// even if no data was collected for it. The broker will send EmptyMessage if no real
		// data was queued for that rank.
		for (int neighbor : neighborPartitions) {
			messageBroker.syncToPart(neighbor);
		}
	}

	public boolean isLocal(Id<Link> linkId) {return partition.containsLink(linkId);}

	public int getPartitionIndex(Id<Link> linkId) {return networkPartitioning.getPartition(linkId);}
}
