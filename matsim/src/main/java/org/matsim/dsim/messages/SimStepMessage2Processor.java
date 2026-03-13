package org.matsim.dsim.messages;

import org.matsim.api.core.v01.MessageProcessor;

/**
 * Processes a {@link SimStepMessage2}.
 */
public interface SimStepMessage2Processor extends MessageProcessor {

	void process(SimStepMessage2 message);

}
