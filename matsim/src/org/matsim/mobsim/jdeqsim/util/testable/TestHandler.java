package org.matsim.mobsim.jdeqsim.util.testable;

import org.matsim.events.handler.EventHandler;

public interface TestHandler extends EventHandler {

	abstract public void checkAssertions();
};
