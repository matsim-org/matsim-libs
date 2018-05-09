package org.matsim.pt.router;

import org.matsim.core.events.handler.EventHandler;

public interface TransitScheduleChangedEventHandler extends EventHandler {

    void handleEvent(TransitScheduleChangedEvent event);

}
