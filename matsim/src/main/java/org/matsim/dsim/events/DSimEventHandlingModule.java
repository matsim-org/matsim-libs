package org.matsim.dsim.events;

import com.google.inject.Inject;
import com.google.inject.multibindings.Multibinder;
import org.matsim.api.core.v01.events.handler.DistributedEventHandler;
import org.matsim.api.core.v01.events.handler.DistributedMode;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.dsim.DistributedEventsManager;

import java.util.Set;

/**
 * DSim replacement for {@link org.matsim.core.events.MobsimScopeEventHandlingModule}.
 * <p>
 * Reuses the {@link org.matsim.core.mobsim.qsim.AbstractQSimModule#addMobsimScopeEventHandlerBinding()} API
 * unchanged. Additionally handles partition-scoped handlers by reading the {@link DistributedEventHandler}
 * annotation at registration time and calling
 * {@link DistributedEventsManager#addHandler(org.matsim.core.events.handler.EventHandler, int)} for those.
 * <p>
 * {@link org.matsim.core.mobsim.DefaultMobsimModule} skips
 * {@link org.matsim.core.events.MobsimScopeEventHandlingModule} for DSim so that only this module
 * handles handler registration and cleanup.
 */
public class DSimEventHandlingModule extends AbstractModule {

	@Override
	public void install() {
		bind(DSimEventHandlerRegistry.class).asEagerSingleton();
		addControllerListenerBinding().to(DSimEventHandlerRegistry.class);

		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				Multibinder.newSetBinder(binder(), MobsimScopeEventHandler.class);
				bind(DSimEventHandlerRegistrator.class).asEagerSingleton();
			}
		});
	}

	static class DSimEventHandlerRegistrator {
		@Inject
		DSimEventHandlerRegistrator(
			DSimEventHandlerRegistry tracking, Set<MobsimScopeEventHandler> handlers, NetworkPartition partition, DistributedEventsManager em) {
			for (var handler : handlers) {
				var annotation = handler.getClass().getAnnotation(DistributedEventHandler.class);
				if (annotation != null && annotation.value() == DistributedMode.PARTITION) {
					em.addHandler(handler, partition.getIndex());
				} else {
					em.addHandler(handler);
				}
				tracking.trackHandler(handler);
			}
		}
	}
}
