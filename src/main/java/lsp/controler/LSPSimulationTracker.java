package lsp.controler;

import java.util.Collection;

import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.handler.EventHandler;

import lsp.functions.LSPInfo;

public interface LSPSimulationTracker extends AfterMobsimListener {

	Collection<EventHandler> getEventHandlers();
	Collection<LSPInfo> getInfos();
	void reset();
}
