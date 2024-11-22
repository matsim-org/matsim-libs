package org.matsim.dsim.messages;


import org.matsim.api.core.v01.MessageProcessor;

public interface SimStepMessageProcessor extends MessageProcessor {

	void process(org.matsim.core.mobsim.disim.SimStepMessage message);

}
