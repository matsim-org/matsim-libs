package org.matsim.contrib.freight.events;

import org.matsim.core.events.handler.EventHandler;

public interface ContractCanceledEventHandler extends EventHandler{
	public void handleEvent(ContractCanceledEvent event);
}
