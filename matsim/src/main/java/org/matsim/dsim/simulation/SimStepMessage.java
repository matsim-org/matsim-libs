package org.matsim.dsim.simulation;

import org.matsim.api.core.v01.Message;

import java.util.List;

/**
 * A batch of messages of one type, exchanged between two partitions for one time step.
 *
 * @param sender      index of the partition that sent this batch. The receiver needs it to process the batches of a time step in the same
 *                    order in every run, see {@code LPTask#run()}: the batches are collected by the senders' threads, so their arrival
 *                    order is thread timing, and the order in which they are handed to the handlers decides, for instance, in which order
 *                    two vehicles coming from two partitions enter the queue of the link they share.
 * @param timeStep    simulation time this batch belongs to
 * @param messageType type of the messages in the batch
 * @param messages    the batched messages, in the order the sender collected them
 */
public record SimStepMessage(int sender, double timeStep, int messageType, List<Message> messages) implements Message {
// this is intended to be used with one message type. Should we make it generic, to enforce it at compile time?

	public void add(Message message) {
		if (message.getType() != messageType)
			throw new IllegalArgumentException("Only messages of type " + messageType + " can be added to this message.");
		messages.add(message);
	}
}
