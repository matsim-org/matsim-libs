package org.matsim.vsp.ev.temperature;

import org.matsim.core.events.handler.EventHandler;

public interface TemperatureChangeEventHandler extends EventHandler {
    public void handleEvent(TemperatureChangeEvent event);

}
