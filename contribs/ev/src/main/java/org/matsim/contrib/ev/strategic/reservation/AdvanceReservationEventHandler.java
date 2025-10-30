package org.matsim.contrib.ev.strategic.reservation;

import org.matsim.core.events.handler.EventHandler;

public interface AdvanceReservationEventHandler extends EventHandler {
	void handleEvent(AdvanceReservationEvent event);
}
