package org.matsim.contrib.shifts.events;

import org.matsim.core.events.handler.EventHandler;

/**
 * @author nkuehnel
 */
public interface DrtShiftEndedEventHandler extends EventHandler {
    public void handleEvent (DrtShiftEndedEvent event);
}
