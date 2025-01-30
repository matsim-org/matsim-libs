package org.matsim.dsim;

import com.google.inject.Binder;
import com.google.inject.Singleton;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.core.v01.LPProvider;
import org.matsim.api.core.v01.Topology;
import org.matsim.api.core.v01.messages.SimulationNode;
import org.matsim.api.core.v01.population.PopulationPartition;
import org.matsim.core.communication.Communicator;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.scoring.DistributedScoringListener;
import org.matsim.core.serialization.SerializationProvider;
import org.matsim.dsim.executors.LPExecutor;
import org.matsim.dsim.executors.PoolExecutor;
import org.matsim.dsim.executors.SingleExecutor;

@Log4j2
public class DistributedSimulationModule extends AbstractModule {


	@SneakyThrows
	@Override
	public void install() {

		DistributedContext ctx = getSimulationContext();

		bind(Communicator.class).toInstance(ctx.getComm());
		bind(Topology.class).toInstance(ctx.getTopology());
		bind(MessageBroker.class).in(Singleton.class);
		bind(SerializationProvider.class).toInstance(ctx.getSerializer());
		bind(SimulationNode.class).toInstance(ctx.getNode());

		bindEventsManager().to(DistributedEventsManager.class).in(Singleton.class);

		addControlerListenerBinding().to(DSimControllerListener.class).in(Singleton.class);

		// Optional single threaded execution
		if (ctx.getNode().getCores() > 1) {
			bind(LPExecutor.class).to(PoolExecutor.class).in(Singleton.class);
		} else {
			bind(LPExecutor.class).to(SingleExecutor.class).in(Singleton.class);
		}

		// If there are multiple nodes, we need to partition the population
		if (ctx.getTopology().getNodesCount() > 1) {
			bind(PopulationPartition.class).toInstance(new LazyPopulationPartition(ctx.getComm().getRank()));

			addControlerListenerBinding().to(DistributedScoringListener.class).in(Singleton.class);
		}

		// Need to define the set binder, in case no other module uses it
		Multibinder.newSetBinder(binder(), LPProvider.class);

		install(new DSimModule());
	}

	/**
	 * Helper method to define bindings for {@link LPProvider} in other modules.
	 */
	public static LinkedBindingBuilder<LPProvider> bindSimulationProcess(Binder binder) {
		Multibinder<LPProvider> lps = Multibinder.newSetBinder(binder, LPProvider.class);
		return lps.addBinding();
	}
}
