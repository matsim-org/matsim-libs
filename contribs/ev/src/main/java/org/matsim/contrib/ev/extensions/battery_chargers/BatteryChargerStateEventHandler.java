package org.matsim.contrib.ev.extensions.battery_chargers;

import org.matsim.core.events.handler.EventHandler;

/**
 * Handles state events for battery-based chargers.
 * 
 * @author sebhoerl
 */
public interface BatteryChargerStateEventHandler extends EventHandler {
    void handleEvent(BatteryChargerStateEvent event);
}
