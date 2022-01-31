package lsp;

import java.util.Collection;

import org.matsim.core.events.handler.EventHandler;

public interface HasEventHandlers {

	Collection<EventHandler>getEventHandlers();
}
