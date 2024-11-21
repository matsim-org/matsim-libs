package org.matsim.dsim;

import com.google.inject.*;
import com.google.inject.name.Named;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.core.v01.Message;
import org.matsim.core.config.Config;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.mobsim.framework.DistributedAgentSource;
import org.matsim.core.mobsim.framework.DistributedMobsimAgent;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.interfaces.*;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.dsim.messages.VehicleContainer;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Loads qsim components for providing these modules the distributed simulation.
 */
@Log4j2
public final class QSimCompatibility {

	private final Config config;
	private final IterationCounter iterationCounter;
	private final Collection<AbstractQSimModule> modules;
	private final List<AbstractQSimModule> overridingModules;
	private final Set<AbstractQSimModule> overridingModulesFromAbstractModule;
	private final QSimComponentsConfig components;

	private final Map<Class<? extends DistributedMobsimAgent>, DistributedAgentSource> agentTypes = new HashMap<>();
	private final Map<Class<? extends DistributedMobsimVehicle>, DistributedAgentSource> vehicleTypes = new HashMap<>();

	@Getter
	private final Injector injector;

	@Getter
	private final List<DistributedAgentSource> agentSources = new ArrayList<>();

	@Getter
	private final List<MobsimEngine> engines = new ArrayList<>();

	@Getter
	private final List<ActivityEngine> activityEngines = new ArrayList<>();

	@Getter
	private final List<DepartureHandler> departureHandlers = new ArrayList<>();

	private Injector qsimInjector;

	private Netsim netsim;

	@Inject
	QSimCompatibility(Injector injector, Config config, IterationCounter iterationCounter,
					  Collection<AbstractQSimModule> modules, QSimComponentsConfig components,
					  @Named("overrides") List<AbstractQSimModule> overridingModules,
					  @Named("overridesFromAbstractModule") Set<AbstractQSimModule> overridingModulesFromAbstractModule) {
		this.injector = injector;
		this.modules = new ArrayList<>(modules);
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

		this.netsim = netsim;

		modules.forEach(m -> m.setConfig(config));

		// Remove modules that are known to be incompatible and are not needed
		modules.removeIf(m -> m instanceof QNetsimEngineModule);

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
				log.warn("Failed to load component %s".formatted(activeComponent), e);
				continue;
			}

			for (Provider<QSimComponent> provider : providers) {
				QSimComponent qSimComponent = provider.get();

				int n = engines.size() + activityEngines.size() + departureHandlers.size() + agentSources.size();

				if (qSimComponent instanceof DistributedMobsimEngine m) {
					engines.add(m);
				}

				if (qSimComponent instanceof DistributedActivityEngine ah) {
					activityEngines.add(ah);
				}

				if (qSimComponent instanceof DistributedDepartureHandler dh) {
					departureHandlers.add(dh);
				}

				if (qSimComponent instanceof DistributedAgentSource as) {
					agentSources.add(as);
					for (Class<? extends DistributedMobsimAgent> agentClass : as.getAgentClasses()) {
						// TODO: Need other type system
						if (agentTypes.containsKey(agentClass))
							throw new IllegalStateException("Duplicate agent provider found for %s".formatted(agentClass));

						agentTypes.put(agentClass, as);
					}

					for (Class<? extends DistributedMobsimVehicle> vehicleClass : as.getVehicleClasses()) {
						if (vehicleTypes.containsKey(vehicleClass))
							throw new IllegalStateException("Duplicate vehicle provider found for %s".formatted(vehicleClass));

						vehicleTypes.put(vehicleClass, as);
					}
				}

				int m = engines.size() + activityEngines.size() + departureHandlers.size() + agentSources.size();
				if (n == m) {
					log.warn("Ignored component not compatible with distributed simulation: {}", qSimComponent);
				}

			}
		}
	}

	/**
	 * Create a vehicle container, which includes all the occupants.
	 */
	public VehicleContainer vehicleToContainer(DistributedMobsimVehicle vehicle) {
		return VehicleContainer.builder()
			.setVehicleType(vehicle.getClass())
			.setVehicle(vehicle.toMessage())
			.setDriver(new VehicleContainer.Occupant((DistributedMobsimAgent) vehicle.getDriver()))
			.setPassengers(vehicle.getPassengers().stream().map(p -> new VehicleContainer.Occupant((DistributedMobsimAgent) p)).toList())
			.build();
	}

	/**
	 * Create a vehicle and its occupants from received container.
	 */
	public DistributedMobsimVehicle vehicleFromContainer(VehicleContainer container) {

		DistributedMobsimVehicle vehicle = vehicleFromMessage(container.getVehicleType(), container.getVehicle());
		DriverAgent driver = (DriverAgent) agentFromMessage(container.getDriver().type(), container.getDriver().occupant());
		vehicle.setDriver(driver);
		driver.setVehicle(vehicle);

		for (VehicleContainer.Occupant occ : container.getPassengers()) {
			PassengerAgent p = (PassengerAgent) agentFromMessage(occ.type(), occ.occupant());
			vehicle.addPassenger(p);
			p.setVehicle(vehicle);
		}

		return vehicle;
	}

	/**
	 * Create a vehicle from a received message. This should normally not be used directly.
	 *
	 * @see #vehicleFromContainer(VehicleContainer)
	 */
	private DistributedMobsimVehicle vehicleFromMessage(Class<? extends DistributedMobsimVehicle> type, Message m) {
		DistributedAgentSource source = vehicleTypes.get(type);
		if (source == null) {
			throw new RuntimeException("No vehicle provider found for %s".formatted(type));
		}

		return source.vehicleFromMessage(type, m);
	}

	/**
	 * Create an agent from a received message.
	 */
	@SuppressWarnings("unchecked")
	public <T extends DistributedMobsimAgent> T agentFromMessage(Class<T> type, Message m) {
		DistributedAgentSource source = agentTypes.get(type);
		if (source == null) {
			throw new RuntimeException("No agent provider found for %s".formatted(type));
		}

		return (T) source.agentFromMessage(type, m);
	}

}
