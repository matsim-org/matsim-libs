package org.matsim.core.mobsim.jdeqsim.util.testable;

import org.matsim.core.events.handler.EventHandler;

public interface TestHandler extends EventHandler {

	abstract public void checkAssertions();
};
