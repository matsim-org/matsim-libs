package lsp;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

import lsp.controler.LSPSimulationTracker;

/**
 *
 *
 */
public interface LSPResource {

	Id<LSPResource> getId();
	
	Id<Link> getStartLinkId();
	
	Class<?> getClassOfResource();
	// yyyyyy is it really necessary to use reflection in a code that we fully own?  kai, may'18
	//One could also leave this method signature out tm, august'18
	
	Id<Link> getEndLinkId();
	
    Collection <LogisticsSolutionElement> getClientElements();
    
    void schedule(int bufferTime);
    
    Collection <EventHandler> getEventHandlers();
    
    Collection <LSPInfo> getInfos();
    
    void addSimulationTracker(LSPSimulationTracker tracker);
    
    Collection<LSPSimulationTracker> getSimulationTrackers();
    
    void setEventsManager(EventsManager eventsManager);
   
}
