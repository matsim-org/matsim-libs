package org.matsim.core.mobsim.qsim;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentsConfigurator;
import org.matsim.core.scenario.ScenarioByInstanceModule;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

/**
 * Builds a new QSim. By default, the QSim is completely empty, i.e. there are
 * no modules registered and there are not engines, handlers or agent sources
 * activated.
 * 
 * The default settings can be added via a couple of methods:
 * 
 * <ul>
 * <li>{@link #useDefaultModules()} loads the default QSim modules for
 * MATSim</li>
 * <li>{@link #useDefaultComponents()} registers the default components for
 * MATSim</li>
 * <li>{@link #useDefaults()} adds both the default modules and components</li>
 * </ul>
 * 
 * Usage example:
 * 
 * <pre>
 * QSim qsim = new QSimBuilder(config) //
 * 		.useDefaults() //
 * 		.addQSimModule(new MyCustomQSimModule()) //
 * 		.configureComponents(components -> {
 * 			components.activeMobsimEngines.add("MyCustomMobsimEngine");
 * 		}) //
 * 		.build(scenario, eventsManager);
 * </pre>
 * 
 * @author Sebastian Hörl <sebastian.hoerl@ivt.baug.ethz.ch>
 */
public class QSimBuilder {
	private final Config config;

	private final Collection<AbstractQSimModule> qsimModules = new LinkedList<>();
	private final QSimComponents components = new QSimComponents();

	private final List<AbstractModule> overridingControllerModules = new LinkedList<>();
	private final List<AbstractQSimModule> overridingQSimModules = new LinkedList<>();

	public QSimBuilder(Config config) {
		this.config = config;
	}

	/**
	 * Adds the default plugins and components to the QSim.
	 * 
	 * @see {@link #useDefaultComponents()} and {@link #useDefaultPlugins()}
	 */
	public QSimBuilder useDefaults() {
		useDefaultComponents();
		useDefaultQSimModules();
		return this;
	}

	/**
	 * Adds a module that overrides existing bindings from MATSim (i.e. mainly those
	 * from the {@link StandaloneQSimModule} and derived stages).
	 */
	public QSimBuilder addOverridingControllerModule(AbstractModule module) {
		overridingControllerModules.add(module);
		return this;
	}

	/**
	 * Adds a QSim module.
	 */
	public QSimBuilder addQSimModule(AbstractQSimModule module) {
		this.qsimModules.add(module);
		return this;
	}

	/**
	 * Adds a QSim module that overrides previously defined elements.
	 */
	public QSimBuilder addOverridingQSimModule(AbstractQSimModule module) {
		this.overridingQSimModules.add(module);
		return this;
	}

	/**
	 * Resets the active QSim components to the standard ones defined by MATSim.
	 */
	public QSimBuilder useDefaultComponents() {
		components.activeActivityHandlers.clear();
		components.activeAgentSources.clear();
		components.activeDepartureHandlers.clear();
		components.activeMobsimEngines.clear();

		new StandardQSimComponentsConfigurator(config).configure(components);

		return this;
	}

	/**
	 * Configures the current active QSim components.
	 */
	public QSimBuilder configureComponents(QSimComponentsConfigurator configurator) {
		configurator.configure(components);
		return this;
	}

	/**
	 * Resets the registered QSim modules to the default ones provided by MATSim.
	 */
	public QSimBuilder useDefaultQSimModules() {
		qsimModules.clear();
		qsimModules.addAll(QSimModule.getDefaultQSimModules());
		return this;
	}

	/**
	 * Configures the registered modules via callback.
	 */
	public QSimBuilder configureModules(Consumer<Collection<AbstractQSimModule>> configurator) {
		configurator.accept(qsimModules);
		return this;
	}

	/**
	 * Removes a QSim module with a specific type form the list of registered
	 * modules.
	 */
	public QSimBuilder removeModule(Class<? extends AbstractQSimModule> moduleType) {
		qsimModules.removeIf(moduleType::isInstance);
		return this;
	}

	/**
	 * Builds a new QSim with the registered plugins and the defined active
	 * components.
	 */
	public QSim build(Scenario scenario, EventsManager eventsManager) {
		// First, load standard QSim module
		AbstractModule controllerModule = new StandaloneQSimModule(scenario, eventsManager);

		// Add all overrides
		for (AbstractModule override : overridingControllerModules) {
			controllerModule = AbstractModule.override(Collections.singleton(controllerModule), override);
		}

		// Override components and modules
		controllerModule = AbstractModule.override(Collections.singleton(controllerModule), new AbstractModule() {
			@Override
			public void install() {
				bind(QSimComponents.class).toInstance(components);
				qsimModules.forEach(this::installQSimModule);
				bind(Key.get(new TypeLiteral<List<AbstractQSimModule>>() {
				}, Names.named("overrides"))).toInstance(overridingQSimModules);
			}
		});

		// Build QSim
		Injector injector = org.matsim.core.controler.Injector.createInjector(config, controllerModule);
		return (QSim) injector.getInstance(Mobsim.class);
	}

	private static class StandaloneQSimModule extends AbstractModule {
		private final Scenario scenario;
		private final EventsManager eventsManager;

		public StandaloneQSimModule(Scenario scenario, EventsManager eventsManager) {
			this.scenario = scenario;
			this.eventsManager = eventsManager;
		}

		@Override
		public void install() {
			install(new ScenarioByInstanceModule(scenario));
			bind(EventsManager.class).toInstance(eventsManager);
			install(new QSimModule(false));
		}
	}
}
