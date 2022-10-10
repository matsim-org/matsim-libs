package org.matsim.contrib.drt.extension.operations.shifts.events;

import org.matsim.core.events.handler.EventHandler;

/**
 * @author nkuehnel / MOIA
 */
public interface DrtShiftStartedEventHandler extends EventHandler {
    public void handleEvent (DrtShiftStartedEvent event);
}
