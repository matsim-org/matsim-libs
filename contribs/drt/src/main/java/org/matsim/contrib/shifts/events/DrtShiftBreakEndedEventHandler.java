package org.matsim.contrib.shifts.events;

import org.matsim.core.events.handler.EventHandler;

/**
 * @author nkuehnel
 */
public interface DrtShiftBreakEndedEventHandler extends EventHandler {
    public void handleEvent (DrtShiftBreakEndedEvent event);
}
