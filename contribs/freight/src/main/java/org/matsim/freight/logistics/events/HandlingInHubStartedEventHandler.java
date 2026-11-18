package org.matsim.freight.logistics.events;

import org.matsim.core.events.handler.EventHandler;

/**
 * {@link EventHandler} that is notified whenever a {@link HandlingInHubStartsEvent} is emitted,
 * e.g. when the handling of a shipment inside a hub starts.
 *
 * @author Kai Martins-Turner (kturner)
 */
public interface HandlingInHubStartedEventHandler extends EventHandler {

  /**
   * Handle a {@link HandlingInHubStartsEvent}.
   *
   * @param event the emitted {@link HandlingInHubStartsEvent}
   */
  void handleEvent(HandlingInHubStartsEvent event);
}
