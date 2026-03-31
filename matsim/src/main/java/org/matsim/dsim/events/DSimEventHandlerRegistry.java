package org.matsim.dsim.events;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.dsim.DistributedEventsManager;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

@Singleton
public class DSimEventHandlerRegistry implements AfterMobsimListener {

	private final Collection<MobsimScopeEventHandler> handlers = new ConcurrentLinkedQueue<>();
	private final DistributedEventsManager em;

	@Inject
	public DSimEventHandlerRegistry(DistributedEventsManager em) {
		this.em = em;
	}

	public void trackHandler(MobsimScopeEventHandler handler) {
		handlers.add(handler);
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		handlers.forEach(em::removeHandler);
		handlers.forEach(h -> h.cleanupAfterMobsim(event.getIteration()));
		handlers.clear();
	}
}
