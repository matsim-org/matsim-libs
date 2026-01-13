package org.matsim.dsim.events;

import it.unimi.dsi.fastutil.ints.IntSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.LP;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.handler.AggregatingEventHandler;
import org.matsim.core.communication.Communicator;
import org.matsim.dsim.MessageBroker;

import java.util.ArrayList;
import java.util.List;

/**
 * Supports {@link AggregatingEventHandler} by receiving messages from all partitions.
 */
public final class AggregateFromAll<T extends Message> implements EventMessagingPattern<AggregatingEventHandler<T>> {

	private static final Logger log = LogManager.getLogger(AggregateFromAll.class);
	private final List<T> messages = new ArrayList<>();

	@Override
	@SuppressWarnings("unchecked")
	public void accept(Message message) {
		log.info("accepting {}", message);
		messages.add((T) message);
	}

	@Override
	public void process(AggregatingEventHandler<T> handler) {
		log.info("Processing messages {}", messages);
		handler.receive(messages);
		messages.clear();
	}

	@Override
	public void communicate(MessageBroker broker, AggregatingEventHandler<T> handler) {
		T msg = handler.send();
		log.info("Communicate {}", msg);
		broker.send(msg, Communicator.BROADCAST_TO_ALL);
	}

	@Override
	public IntSet waitForOtherRanks(double time) {
		return LP.ALL_NODES_BROADCAST;
	}
}
