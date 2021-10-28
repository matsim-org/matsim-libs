package org.matsim.contrib.drt.extension.shifts.events;

import org.matsim.core.events.handler.EventHandler;

/**
 * @author nkuehnel / MOIA
 */
public interface DrtShiftBreakEndedEventHandler extends EventHandler {
    public void handleEvent (DrtShiftBreakEndedEvent event);
}
