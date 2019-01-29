package org.matsim.contrib.ev.ev.charging;

import org.matsim.core.events.handler.EventHandler;

public interface ChargingEndEventHandler extends EventHandler {
    void handleEvent(ChargingEndEvent event);

}
