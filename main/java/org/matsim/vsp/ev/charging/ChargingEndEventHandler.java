package org.matsim.vsp.ev.charging;

import org.matsim.core.events.handler.EventHandler;

public interface ChargingEndEventHandler extends EventHandler {
    public void handleEvent(ChargingEndEvent event);

}
