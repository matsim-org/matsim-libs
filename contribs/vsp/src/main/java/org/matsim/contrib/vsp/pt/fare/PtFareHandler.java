package org.matsim.contrib.vsp.pt.fare;

import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;

public interface PtFareHandler extends ActivityStartEventHandler, AgentWaitingForPtEventHandler {
	//other methods for informed mode choice
}
