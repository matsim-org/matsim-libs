package org.matsim.dsim.simulation;

import com.google.inject.Inject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkPartitioning;
import org.matsim.dsim.MessageBroker;
import org.matsim.dsim.messages.SimStepMessage2;

import java.util.ArrayList;
import java.util.List;

public class PartitionTransfer {

	private final NetworkPartitioning networkPartitioning;
	private final MessageBroker messageBroker;

	private final Int2ObjectMap<Int2ObjectMap<List<Message>>> messages = new Int2ObjectOpenHashMap<>();

	@Inject
	public PartitionTransfer(Network network, MessageBroker messageBroker) {
		this.networkPartitioning = network.getPartitioning();
		this.messageBroker = messageBroker;
	}

	public void collect(Message message, Id<Link> targetLink) {
		var targetPartition = getRank(targetLink);
		messageBroker.send(message, targetPartition);
	}

	public void collect(Message message, int targetRank) {
		var rankMessages = messages.computeIfAbsent(targetRank, _ -> new Int2ObjectOpenHashMap<>());
		var messagesForType = rankMessages.computeIfAbsent(message.getType(), _ -> new ArrayList<>());
		messagesForType.add(message);
	}

	public void send(double now) {
		for (var partitions : messages.int2ObjectEntrySet()) {
			var toPartition = partitions.getIntKey();
			for (var types : partitions.getValue().int2ObjectEntrySet()) {
				var messageType = types.getIntKey();
				var messages = types.getValue();
				var simStepMessage = new SimStepMessage2(now, messageType, messages);
				messageBroker.send(simStepMessage, toPartition);
			}
			// remove reference to the list of message for given type.
			partitions.getValue().clear();
		}
	}

	public boolean isLocal(Id<Link> linkId) {return networkPartitioning.getPartition(messageBroker.getRank()).containsLink(linkId);}

	public int getRank(Id<Link> linkId) {return networkPartitioning.getPartition(linkId);}
}
