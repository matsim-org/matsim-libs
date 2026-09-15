package org.matsim.core.mobsim.dsim;

import org.matsim.api.core.v01.LP;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.MessageProcessor;

import java.util.List;
import java.util.Map;

/**
 * This interface can be implemented if components which are part of the DSim want to process messages. The
 * {@link org.matsim.dsim.simulation.SimProcess} acts as general {@link MessageProcessor} to receive messages
 * from other partitions. It then dispatches messages to {@link DSimComponentsMessageProcessor}s which have
 * subscribed to the received message types.
 * <p>
 * This interface is distinct from {@link MessageProcessor} which is used by the {@link MessageProcessor}. Because we,
 * historically, have engines as part of a {@link org.matsim.dsim.simulation.SimProcess}/{@link org.matsim.core.mobsim.qsim.QSim}, engines
 * cannot act as {@link LP}s directly, but we have to have this second layer of message dispatching.
 */
public interface DSimComponentsMessageProcessor {

	/**
	 * Returns a map of message type IDs to handlers that process batches of incoming messages of that type.
	 * Called once during engine registration in {@link org.matsim.dsim.simulation.SimProcess}.
	 * Engines that receive no inter-partition messages can leave this as the default empty map.
	 */
	Map<Class<? extends Message>, DistributedMobsimEngine.MessageHandler> getMessageHandlers();

	@FunctionalInterface
	interface MessageHandler {
		void handle(List<Message> messages, double now);
	}
}
