package org.matsim.freight.logistics.events;

import org.matsim.core.events.handler.EventHandler;

/**
 * @author Kai Martins-Turner (kturner)
 */
public interface HandlingInHubStartedEventHandler extends EventHandler {

  void handleEvent(HandlingInHubStartsEvent event);
}
