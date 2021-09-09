package org.matsim.contrib.shifts.events;

import org.matsim.core.events.handler.EventHandler;

/**
 * @author nkuehnel
 */
public interface DrtShiftAssignedEventHandler extends EventHandler {
    public void handleEvent (DrtShiftAssignedEvent event);
}
