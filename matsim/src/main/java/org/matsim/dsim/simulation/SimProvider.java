package org.matsim.dsim.simulation;

import com.google.inject.*;
import com.google.inject.Module;
import com.google.inject.name.Named;
import com.google.inject.util.Modules;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.LP;
import org.matsim.api.core.v01.LPProvider;
import org.matsim.api.core.v01.messages.ComputeNode;
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

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.util.*;

public class SimProvider implements LPProvider {

	private static final Logger log = LogManager.getLogger(SimProvider.class);

	private final Network network;
	private final Config config;
	private final ComputeNode node;
	private final IterationCounter iterationCounter;
	private final Collection<AbstractQSimModule> modules;
	private final List<AbstractQSimModule> overridingModules;
	private final Set<AbstractQSimModule> overridingModulesFromAbstractModule;
	private final QSimComponentsConfig components;

	private final Injector injector;

	public Injector getInjector() {
		return injector;
	}

	private final Set<MobsimListener> listeners = new HashSet<>();

	public Collection<? extends MobsimListener> getListeners() {
		return this.listeners;
	}

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
		this.node = injector.getInstance(ComputeNode.class);
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
		modules.forEach(m -> m.setComputeNode(node));

		// Remove modules that are known to be incompatible and are not needed
		modules.removeIf(m -> m instanceof QNetsimEngineModule);

		overridingModules.forEach(m -> m.setConfig(config));
		overridingModules.forEach(m -> m.setComputeNode(node));

		int iterationNumber = iterationCounter.getIterationNumber();
		modules.forEach(m -> m.setIterationNumber(iterationNumber));
		overridingModules.forEach(m -> m.setIterationNumber(iterationNumber));

		AbstractQSimModule qsimModule = AbstractQSimModule.overrideQSimModules(modules, Collections.emptyList());

		for (AbstractQSimModule override : overridingModulesFromAbstractModule) {
			override.setConfig(config);
			override.setComputeNode(node);
			qsimModule = AbstractQSimModule.overrideQSimModules(Collections.singleton(qsimModule), Collections.singletonList(override));
		}

		for (AbstractQSimModule override : overridingModules) {
			override.setConfig(config);
			override.setComputeNode(node);
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

		// Partitions other than the first one re-use the injector from the first partition for node singletons. See the end of this
		// method where the node singleton injector is assigend when this method is called for the first partition.
		if (!isFirstPartition(partition)) {
			Objects.requireNonNull(nodeSingletonInjector, "Node singleton injector must be present for non-first partitions");
			module = Modules.override(module).with(new NodeSingletonModule(nodeSingletonInjector));
		}

		Injector dsimInjector = injector.createChildInjector(module);

		SimProcess simProcess = (SimProcess) dsimInjector.getInstance(Netsim.class);

		AgentSourcesContainer agentSources = dsimInjector.getInstance(AgentSourcesContainer.class);

		for (Object activeComponent : components.getActiveComponents()) {

			Key<Collection<Provider<QSimComponent>>> activeComponentKey = getComponentKey(activeComponent);
			Collection<Provider<QSimComponent>> providers = getComponentProviders(activeComponentKey, activeComponent, dsimInjector);

			for (Provider<QSimComponent> provider : providers) {
				QSimComponent qSimComponent = provider.get();

				// Skip component that is a node singleton, but this process is not the first
				if (!isFirstPartition(partition) && isNodeSingleton(qSimComponent)) {
					continue;
				}

				simProcess.addMobsimComponent(qSimComponent);

				if (qSimComponent instanceof DistributedAgentSource as) {
					agentSources.addSource(as);
				}

				if (qSimComponent instanceof MobsimListener l) {
					addMobsimListener(partition, l, listeners, simProcess);
				}
			}
		}

		// Retrieve all mobsim listeners that are not mobsim components
		Set<MobsimListener> mobsimListeners = dsimInjector.getInstance(Key.get(new TypeLiteral<>() {}));
		for (var l : mobsimListeners) {
			addMobsimListener(partition, l, listeners, simProcess);
		}

		// We re-use the injector which was created on for the first partition of the compute node for all following partitions to provide
		// instances that are node singletons.
		if (isFirstPartition(partition)) {
			nodeSingletonInjector = dsimInjector;
		}

		return simProcess;
	}

	/**
	 * Helper to wire up a mobsim listener. By default, a listener is added to the sim process. Each sim process will call its instance of a listener.
	 * If the listener is annotated with {@link NodeSingleton}, it is added to the list of node wide listeners. That list is ultimately handled by
	 * {@link org.matsim.dsim.DSim}.
	 */
	private void addMobsimListener(NetworkPartition partition, MobsimListener l, Collection<MobsimListener> listeners, SimProcess simProcess) {
		if (isFirstPartition(partition) && isNodeSingleton(l)) {
			listeners.add(l);
		} else {
			simProcess.addQueueSimulationListeners(l);
		}
	}

	private boolean isFirstPartition(NetworkPartition partition) {
		return node.isFirstPartition(partition.getIndex());
	}

	/**
	 * Helper that tests whether the given object is annotated with {@link NodeSingleton} or any interface or superclass in the hierarchy.
	 */
	private static boolean isNodeSingleton(Object o) {
		var clazz = o.getClass();
		while (clazz != null) {
			if (clazz.isAnnotationPresent(NodeSingleton.class)) return true;
			for (var iface : clazz.getInterfaces()) {
				if (isNodeSingletonInterface(iface)) return true;
			}
			clazz = clazz.getSuperclass();
		}
		return false;
	}

	/**
	 * Helper that tests whether any interface in the hierarchy is annotated with {@link NodeSingleton}.
	 */
	private static boolean isNodeSingletonInterface(Class<?> iface) {
		if (iface.isAnnotationPresent(NodeSingleton.class)) return true;
		for (Class<?> parent : iface.getInterfaces()) {
			if (isNodeSingletonInterface(parent)) return true;
		}
		return false;
	}

	/**
	 * Helper that wraps the getInstance call to injector with a try-catch block.
	 */
	private static @Nonnull Collection<Provider<QSimComponent>> getComponentProviders(Key<Collection<Provider<QSimComponent>>> activeComponentKey, Object activeComponent, Injector dsimInjector) {
		try {
			return dsimInjector.getInstance(activeComponentKey);
		} catch (ProvisionException | ConfigurationException e) {
			log.warn("Failed to load component %s: ".formatted(activeComponent), e.getMessage());
			return List.of();
		}
	}

	/**
	 * Helper that figures out what key to use for the given component.
	 */
	private static @Nonnull Key<Collection<Provider<QSimComponent>>> getComponentKey(Object activeComponent) {
		if (activeComponent instanceof Annotation) {
			return Key.get(new TypeLiteral<>() {
			}, (Annotation) activeComponent);
		} else {
			return Key.get(new TypeLiteral<>() {
			}, (Class<? extends Annotation>) activeComponent);
		}
	}
}
