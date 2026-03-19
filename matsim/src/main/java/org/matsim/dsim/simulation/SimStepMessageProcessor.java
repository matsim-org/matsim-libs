package org.matsim.dsim.simulation;

import org.matsim.api.core.v01.MessageProcessor;

/**
 * Processes a {@link SimStepMessage}.
 */
public interface SimStepMessageProcessor extends MessageProcessor {

	void process(SimStepMessage message);

}
