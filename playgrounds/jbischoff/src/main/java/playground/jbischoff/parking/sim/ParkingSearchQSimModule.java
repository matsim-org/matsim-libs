package playground.jbischoff.parking.sim;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.ActivityEnginePlugin;
import org.matsim.core.mobsim.qsim.QSimProvider;
import org.matsim.core.mobsim.qsim.TeleportationPlugin;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsPlugin;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueuePlugin;
import org.matsim.core.mobsim.qsim.pt.TransitEnginePlugin;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEnginePlugin;
import org.matsim.pt.config.TransitConfigGroup;

import com.google.inject.Module;
import com.google.inject.Provides;


class ParkingSearchQSimModule extends com.google.inject.AbstractModule {
	@Override 
	protected void configure() {
		bind(Mobsim.class).toProvider(QSimProvider.class);
	}
	@SuppressWarnings("static-method")
	@Provides 
	Collection<AbstractQSimPlugin> provideQSimPlugins(TransitConfigGroup transitConfigGroup, NetworkConfigGroup networkConfigGroup, Config config) {
		final Collection<AbstractQSimPlugin> plugins = new ArrayList<>();
		plugins.add(new MessageQueuePlugin(config));
		plugins.add(new ActivityEnginePlugin(config));
		plugins.add(new QNetsimEnginePlugin(config));
		if (networkConfigGroup.isTimeVariantNetwork()) {
			plugins.add(new NetworkChangeEventsPlugin(config));
		}
		if (transitConfigGroup.isUseTransit()) {
			plugins.add(new TransitEnginePlugin(config));
		}
		plugins.add(new TeleportationPlugin(config));
		plugins.add(new ParkingSearchPopulationPlugin(config));
		return plugins;
	}
	
	private static class ParkingSearchPopulationPlugin extends AbstractQSimPlugin {
		public ParkingSearchPopulationPlugin(Config config) { super(config); }
		@Override 
		public Collection<? extends Module> modules() {
			Collection<Module> result = new ArrayList<>();
			result.add(new com.google.inject.AbstractModule() {
				@Override
				protected void configure() {
					bind(PopulationAgentSource.class).asEagerSingleton();
					if (getConfig().transit().isUseTransit()) {
						throw new RuntimeException("parking search together with transit is not implemented (should not be difficult)") ;
					} else {
						bind(AgentFactory.class).to(ParkingAgentFactory.class).asEagerSingleton(); // (**)
					}
				}
			});
			return result;
		}
		@Override 
		public Collection<Class<? extends AgentSource>> agentSources() {
			Collection<Class<? extends AgentSource>> result = new ArrayList<>();
			result.add(PopulationAgentSource.class);
			return result;
		}
	}
	
}