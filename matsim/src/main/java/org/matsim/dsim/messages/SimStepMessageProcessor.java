package org.matsim.dsim.messages;

import org.matsim.api.core.v01.MessageProcessor;

/**
 * Processes a {@link SimStepMessage}.
 */
public interface SimStepMessageProcessor extends MessageProcessor {

	void process(SimStepMessage message);

}
