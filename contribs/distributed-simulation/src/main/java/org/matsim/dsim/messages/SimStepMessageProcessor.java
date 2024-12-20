package org.matsim.dsim.messages;


import org.matsim.api.core.v01.MessageProcessor;
import org.matsim.core.mobsim.dsim.SimStepMessage;

public interface SimStepMessageProcessor extends MessageProcessor {

	void process(SimStepMessage message);

}
