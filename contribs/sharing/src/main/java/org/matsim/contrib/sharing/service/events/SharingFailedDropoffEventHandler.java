package org.matsim.contrib.sharing.service.events;

import org.matsim.core.events.handler.EventHandler;

public interface SharingFailedDropoffEventHandler extends EventHandler {
	void handleEvent(SharingFailedDropoffEvent event);
}
