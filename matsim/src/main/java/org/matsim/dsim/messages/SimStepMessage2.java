package org.matsim.dsim.messages;

import org.matsim.api.core.v01.Message;

import java.util.List;

public record SimStepMessage2(double timeStep, long messageType, List<Message> messages) implements Message {
// this is intended to be used with one message type. Should we make it generic, to enforce it at compile time?

	public void add(Message message) {
		if (message.getType() != messageType)
			throw new IllegalArgumentException("Only messages of type " + messageType + " can be added to this message.");
		messages.add(message);
	}
}
