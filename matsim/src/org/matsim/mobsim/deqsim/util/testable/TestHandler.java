package org.matsim.mobsim.deqsim.util.testable;

import org.matsim.events.handler.EventHandler;

public interface TestHandler extends EventHandler {

	abstract public void checkAssertions();
};
