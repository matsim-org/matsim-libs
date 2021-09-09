package org.matsim.contrib.shifts.events;

import org.matsim.core.events.handler.EventHandler;

/**
 * @author nkuehnel
 */
public interface DrtShiftBreakStartedEventHandler extends EventHandler {
    public void handleEvent (DrtShiftBreakStartedEvent event);
}
