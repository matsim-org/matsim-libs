package org.matsim.dsim;

import com.google.inject.*;
import com.google.inject.name.Named;
import lombok.Getter;
import org.matsim.api.core.v01.Message;
import org.matsim.core.config.Config;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.mobsim.framework.DistributedAgentSource;
import org.matsim.core.mobsim.framework.DistributedMobsimAgent;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.interfaces.*;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Loads qsim components for providing these modules the distributed simulation.
 */
public final class QSimCompatibility {

	private final Config config;
	private final IterationCounter iterationCounter;
	private final Collection<AbstractQSimModule> modules;
	private final List<AbstractQSimModule> overridingModules;
	private final Set<AbstractQSimModule> overridingModulesFromAbstractModule;
	private final QSimComponentsConfig components;

	private final Map<Class<? extends Message>, DistributedAgentSource> agentTypes = new HashMap<>();
	private final Map<Class<? extends Message>, DistributedAgentSource> vehicleSources = new HashMap<>();

	@Getter
	private final Injector injector;

	@Getter
	private final List<DistributedAgentSource> agentSources = new ArrayList<>();

	@Getter
	private final List<MobsimEngine> engines = new ArrayList<>();

	@Getter
	private final List<ActivityHandler> activityHandlers = new ArrayList<>();

	@Getter
	private final List<DepartureHandler> departureHandlers = new ArrayList<>();

	private Injector qsimInjector;

	@Inject
	QSimCompatibility(Injector injector, Config config, IterationCounter iterationCounter,
					  Collection<AbstractQSimModule> modules, QSimComponentsConfig components,
					  @Named("overrides") List<AbstractQSimModule> overridingModules,
					  @Named("overridesFromAbstractModule") Set<AbstractQSimModule> overridingModulesFromAbstractModule) {
		this.injector = injector;
		this.modules = modules;
		// (these are the implementations)
		this.config = config;
		this.iterationCounter = iterationCounter;
		this.components = components;
		this.overridingModules = overridingModules;
		this.overridingModulesFromAbstractModule = overridingModulesFromAbstractModule;
	}

	/**
	 * Initialize module with underlying netsim.
	 */
	public void init(Netsim netsim) {
		if (qsimInjector != null) {
			return;
		}

		modules.forEach(m -> m.setConfig(config));
		overridingModules.forEach(m -> m.setConfig(config));

		int iterationNumber = iterationCounter.getIterationNumber();
		modules.forEach(m -> m.setIterationNumber(iterationNumber));
		overridingModules.forEach(m -> m.setIterationNumber(iterationNumber));

		AbstractQSimModule qsimModule = AbstractQSimModule.overrideQSimModules(modules, Collections.emptyList());

		for (AbstractQSimModule override : overridingModulesFromAbstractModule) {
			qsimModule = AbstractQSimModule.overrideQSimModules(Collections.singleton(qsimModule), Collections.singletonList(override));
		}

		for (AbstractQSimModule override : overridingModules) {
			qsimModule = AbstractQSimModule.overrideQSimModules(Collections.singleton(qsimModule), Collections.singletonList(override));
		}

		final AbstractQSimModule finalQsimModule = qsimModule;

		AbstractModule module = new AbstractModule() {
			@Override
			protected void configure() {
				install(finalQsimModule);
				bind(Netsim.class).toInstance(netsim);
			}
		};

		qsimInjector = injector.createChildInjector(module);

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
				providers = qsimInjector.getInstance(activeComponentKey);
			} catch (ProvisionException | ConfigurationException e) {
				// ignore
				continue;
			}

			for (Provider<QSimComponent> provider : providers) {
				QSimComponent qSimComponent = provider.get();
				if (qSimComponent instanceof MobsimEngine m) {
					engines.add(m);
				}

				if (qSimComponent instanceof ActivityHandler ah) {
					activityHandlers.add(ah);
				}

				if (qSimComponent instanceof DepartureHandler dh) {
					departureHandlers.add(dh);
				}

				if (qSimComponent instanceof DistributedAgentSource as) {
					agentSources.add(as);

					Class<? extends Message> aClass = as.getAgentClass();
					if (aClass != null)
						agentTypes.put(aClass, as);

					Class<? extends Message> vClass = as.getVehicleClass();
					if (vClass != null)
						vehicleSources.put(vClass, as);
				}
			}
		}
	}

	/**
	 * Create a vehicle from a received message.
	 */
	public DistributedMobsimVehicle vehicleFromMessage(Message m) {
		DistributedAgentSource source = vehicleSources.get(m.getClass());
		if (source == null) {
			throw new RuntimeException("No vehicle provider found for %s".formatted(m.getClass()));
		}

		return source.vehicleFromMessage(m);
	}

	/**
	 * Create an agent from a received message.
	 */
	public DistributedMobsimAgent agentFromMessage(Message m) {
		DistributedAgentSource source = agentTypes.get(m.getClass());
		if (source == null) {
			throw new RuntimeException("No agent provider found for %s".formatted(m.getClass()));
		}


		return source.agentFromMessage(m);
	}

}
