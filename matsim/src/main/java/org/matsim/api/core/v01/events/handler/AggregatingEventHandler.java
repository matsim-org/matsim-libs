package org.matsim.api.core.v01.events.handler;


import org.matsim.api.core.v01.Message;
import org.matsim.core.events.handler.EventHandler;

import java.util.List;

/**
 * Interface for a distributed event handler that broadcasts messages to all other partitions and receives messages from
 * other partitions at a fixed time step.
 * Note that an event handler needs to function properly, even if {@link #send()} and {@link #receive(List)} is never executed,
 * in case the event handler is used in a non-distributed system.
 *
 * @param <T> message used for synchronization.
 */
public interface AggregatingEventHandler<T extends Message> extends EventHandler {

	/**
	 * Produce message sent to other partitions.
	 */
	T send();

	/**
	 * Receive all messages from other partitions. The own message will not be included.
	 *
	 * @param messages received messages from other handlers.
	 */
	void receive(List<T> messages);

}
