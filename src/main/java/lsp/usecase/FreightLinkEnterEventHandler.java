package lsp.usecase;

import org.matsim.core.events.handler.EventHandler;

import lsp.events.FreightLinkEnterEvent;

public interface FreightLinkEnterEventHandler extends EventHandler{

	public void handleEvent(FreightLinkEnterEvent event);

}
