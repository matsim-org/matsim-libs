package org.matsim.contrib.drt.extension.shifts.events;

import org.matsim.core.events.handler.EventHandler;

/**
 * @author nkuehnel
 */
public interface DrtShiftBreakStartedEventHandler extends EventHandler {
    public void handleEvent (DrtShiftBreakStartedEvent event);
}
