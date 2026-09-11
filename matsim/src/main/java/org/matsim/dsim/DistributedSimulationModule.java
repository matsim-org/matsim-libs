package org.matsim.dsim;

import com.google.inject.Binder;
import com.google.inject.Singleton;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import org.matsim.api.core.v01.LPProvider;
import org.matsim.api.core.v01.population.PopulationPartition;
import org.matsim.core.communication.Communicator;
import org.matsim.core.communication.NullCommunicator;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.serialization.MessageTypeRegistry;
import org.matsim.core.serialization.NoopSerializationProvider;
import org.matsim.core.serialization.SerializationProvider;
import org.matsim.dsim.events.DSimEventHandlingModule;
import org.matsim.dsim.executors.LPExecutor;
import org.matsim.dsim.executors.PoolExecutor;
import org.matsim.dsim.executors.SingleExecutor;
import org.matsim.dsim.scoring.BackpackScoringModule;

public class DistributedSimulationModule extends AbstractModule {


	@Override
	public void install() {

		ExecutionContext ctx = getSimulationContext();
		DistributedContext dtx;

		// Use distributed config
		if (ctx instanceof DistributedContext o) {
			dtx = o;
		} else {
			// No distributed context was supplied, so build a single-node one.
			dtx = DistributedContext.createLocal(new NullCommunicator(), getSimulationContext().getTopology());
			ctx = dtx;
		}

		bind(Communicator.class).toInstance(dtx.getComm());
		bind(MessageTypeRegistry.class).toInstance(MessageTypeRegistry.getInstance());

		// A multi-node run supplies its own wire codec via DistributedContext.create; a single-node run gets the
		// no-op provider, since its messages never leave the JVM.
		if (dtx.getSerializer() != null) {
			bind(SerializationProvider.class).toInstance(dtx.getSerializer());
		} else {
			bind(SerializationProvider.class).to(NoopSerializationProvider.class).in(Singleton.class);
		}

		bind(MessageBroker.class).in(Singleton.class);
		bind(DistributedEventsManager.class).in(Singleton.class);
		bindEventsManager().to(DistributedEventsManager.class).in(Singleton.class);
		addControllerListenerBinding().to(DSimControllerListener.class).in(Singleton.class);

		// Optional single threaded execution
		if (getConfig().dsim().getThreads() > 1) {
			bind(LPExecutor.class).to(PoolExecutor.class).in(Singleton.class);
		} else {
			bind(LPExecutor.class).to(SingleExecutor.class).in(Singleton.class);
		}

		// If there are multiple nodes, we need to partition the population
		if (ctx.isDistributed()) {

			bind(PopulationPartition.class).toInstance(new LazyPopulationPartition(dtx.getComm().getRank()));
			//TODO think about whether we still need something similar to consolidate experienced plans in the end
			//addControllerListenerBinding().to(DistributedScoringListener.class).in(Singleton.class);
		}

		// Need to define the set binder, in case no other module uses it
		Multibinder.newSetBinder(binder(), LPProvider.class);

		install(new DSimModule());
		install(new DSimEventHandlingModule());
		install(new BackpackScoringModule());
	}

	/**
	 * Helper method to define bindings for {@link LPProvider} in other modules.
	 */
	public static LinkedBindingBuilder<LPProvider> bindSimulationProcess(Binder binder) {
		Multibinder<LPProvider> lps = Multibinder.newSetBinder(binder, LPProvider.class);
		return lps.addBinding();
	}
}
