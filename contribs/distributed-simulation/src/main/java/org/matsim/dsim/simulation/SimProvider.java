package org.matsim.dsim.simulation;

import com.google.inject.Module;
import com.google.inject.*;
import com.google.inject.name.Named;
import com.google.inject.util.Modules;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.LP;
import org.matsim.api.LPProvider;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.core.config.Config;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.mobsim.dsim.DistributedAgentSource;
import org.matsim.core.mobsim.dsim.NodeSingleton;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.dsim.utils.NodeSingletonModule;

import java.lang.annotation.Annotation;
import java.util.*;

@Log4j2
public class SimProvider implements LPProvider {

	private final Network network;
	private final Config config;
	private final IterationCounter iterationCounter;
	private final Collection<AbstractQSimModule> modules;
	private final List<AbstractQSimModule> overridingModules;
	private final Set<AbstractQSimModule> overridingModulesFromAbstractModule;
	private final QSimComponentsConfig components;

	@Getter
	private final Injector injector;

	@Getter
	private final Set<MobsimListener> listeners = new HashSet<>();

	/**
	 * Injector containing the node singletons
	 */
	private Injector nodeSingletonInjector;

	@Inject
	SimProvider(Injector injector, Collection<AbstractQSimModule> modules,
					  @Named("overrides") List<AbstractQSimModule> overridingModules,
					  @Named("overridesFromAbstractModule") Set<AbstractQSimModule> overridingModulesFromAbstractModule) {
		this.injector = injector;
		this.modules = new ArrayList<>(modules);
		// (these are the implementations)
		this.config = injector.getInstance(Config.class);
		this.network = injector.getInstance(Network.class);
		this.iterationCounter = injector.getInstance(IterationCounter.class);
		this.components = injector.getInstance(QSimComponentsConfig.class);
		this.overridingModules = overridingModules;
		this.overridingModulesFromAbstractModule = overridingModulesFromAbstractModule;
	}

	@Override
	public LP create(int part) {
		NetworkPartition partition = network.getPartitioning().getPartition(part);
		return create(partition);
	}

	/**
	 * Initialize module with underlying netsim.
	 */
	public SimProcess create(NetworkPartition partition) {

		modules.forEach(m -> m.setConfig(config));

		// Remove modules that are known to be incompatible and are not needed
		modules.removeIf(m -> m instanceof QNetsimEngineModule);
		//modules.removeIf(m -> m instanceof TransitEngineModule);

		overridingModules.forEach(m -> m.setConfig(config));

		int iterationNumber = iterationCounter.getIterationNumber();
		modules.forEach(m -> m.setIterationNumber(iterationNumber));
		overridingModules.forEach(m -> m.setIterationNumber(iterationNumber));

		AbstractQSimModule qsimModule = AbstractQSimModule.overrideQSimModules(modules, Collections.emptyList());

		for (AbstractQSimModule override : overridingModulesFromAbstractModule) {
			override.setConfig(config);
			qsimModule = AbstractQSimModule.overrideQSimModules(Collections.singleton(qsimModule), Collections.singletonList(override));
		}

		for (AbstractQSimModule override : overridingModules) {
			override.setConfig(config);
			qsimModule = AbstractQSimModule.overrideQSimModules(Collections.singleton(qsimModule), Collections.singletonList(override));
		}

		final AbstractQSimModule finalQsimModule = qsimModule;

		Module module = new AbstractModule() {
			@Override
			protected void configure() {
				install(finalQsimModule);
				bind(NetworkPartition.class).toInstance(partition);
				bind(SimProcess.class).asEagerSingleton();
				bind(Netsim.class).to(SimProcess.class);
			}
		};

		if (nodeSingletonInjector != null) {
			module = Modules.override(module).with(new NodeSingletonModule(nodeSingletonInjector));
		}

		Injector dsimInjector = injector.createChildInjector(module);

		SimProcess simProcess = (SimProcess) dsimInjector.getInstance(Netsim.class);

		AgentSourcesContainer agentSources = dsimInjector.getInstance(AgentSourcesContainer.class);

		// Retrieve all mobsim listeners
		Set<MobsimListener> listener = dsimInjector.getInstance(Key.get(new TypeLiteral<>() {}));

		// Add all listener that are not node singletons, or this is the first instance
		listener.stream()
			.filter(l -> !l.getClass().isAnnotationPresent(NodeSingleton.class) || nodeSingletonInjector == null)
			.forEach(listeners::add);

		for (Object activeComponent : components.getActiveComponents()) {
			Key<Collection<Provider<QSimComponent>>> activeComponentKey;
			if (activeComponent instanceof Annotation) {
				activeComponentKey = Key.get(new TypeLiteral<Collection<Provider<QSimComponent>>>() {
				}, (Annotation) activeComponent);
			} else {
				activeComponentKey = Key.get(new TypeLiteral<Collection<Provider<QSimComponent>>>() {
				}, (Class<? extends Annotation>) activeComponent);
			}

			Collection<Provider<QSimComponent>> providers;
			try {
				providers = dsimInjector.getInstance(activeComponentKey);
			} catch (ProvisionException | ConfigurationException e) {
				log.warn("Failed to load component %s".formatted(activeComponent), e);
				continue;
			}

			for (Provider<QSimComponent> provider : providers) {
				QSimComponent qSimComponent = provider.get();
				simProcess.addMobsimComponent(qSimComponent);

				if (qSimComponent instanceof DistributedAgentSource as) {
					agentSources.addSource(as);
				}

				if (qSimComponent instanceof MobsimListener l) {
					listeners.add(l);
				}
			}
		}

		if (nodeSingletonInjector == null) {
			nodeSingletonInjector = dsimInjector;
		}

		return simProcess;
	}

}
