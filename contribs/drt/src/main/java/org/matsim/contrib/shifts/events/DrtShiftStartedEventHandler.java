package org.matsim.contrib.shifts.events;

import org.matsim.core.events.handler.EventHandler;

/**
 * @author nkuehnel
 */
public interface DrtShiftStartedEventHandler extends EventHandler {
    public void handleEvent (DrtShiftStartedEvent event);
}
