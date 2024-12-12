package org.matsim.dsim.events;

import it.unimi.dsi.fastutil.ints.IntSet;
import org.matsim.api.core.v01.Message;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.dsim.MessageBroker;

import java.util.function.Consumer;

/**
 * Interface to allow different message pattern for event handlers.
 *
 * @param <T> type of event handler
 */
public sealed interface EventMessagingPattern<T extends EventHandler> extends Consumer<Message> permits AggregateFromAll {

	/**
	 * Let the handler process received message.
	 */
	void process(T handler);

	/**
	 * Generate message and send them via the broker if necesarry. Called after the handler is done processing all messages.
	 */
	void communicate(MessageBroker broker, T handler);

	/**
	 * Determine for which other precessed should be waited.
	 */
	IntSet waitForOtherRanks(double time);

}
