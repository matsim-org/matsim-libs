package lsp.controler;

import java.util.Collection;

import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.handler.EventHandler;

import lsp.functions.Info;
import lsp.LogisticsSolution;
import lsp.LogisticsSolutionElement;
import lsp.resources.Resource;

public interface SimulationTracker extends AfterMobsimListener {

	public Collection<EventHandler> getEventHandlers();
	public Collection<Info> getInfos();
	public void notifyAfterMobsim(AfterMobsimEvent event);
	public void reset();
}
