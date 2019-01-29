package org.matsim.contrib.ev.ev.temperature;

import org.matsim.core.events.handler.EventHandler;

public interface TemperatureChangeEventHandler extends EventHandler {
    public void handleEvent(TemperatureChangeEvent event);

}
