package lsp.controler;

import java.util.Collection;

import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.handler.EventHandler;

import lsp.functions.LSPInfo;

public interface LSPSimulationTracker extends AfterMobsimListener {

	public Collection<EventHandler> getEventHandlers();
	public Collection<LSPInfo> getInfos();
	public void reset();
}
