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
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentsConfigurator;
import org.matsim.core.scenario.ScenarioByInstanceModule;

import com.google.inject.Injector;
import com.google.inject.TypeLiteral;

/**
 * Builds a new QSim. By default, the QSim is completely empty, i.e. there are
 * no plugins registered and there are not engines, handlers or agent sources
 * activated.
 * 
 * The default settings can be added via a couple of methods:
 * 
 * <ul>
 * <li>{@link #useDefaultPlugins()} loads the default QSim plugins for
 * MATSim</li>
 * <li>{@link #useDefaultComponents()} registers the default components for
 * MATSim</li>
 * <li>{@link #useDefaults()} adds both the default plugins and components</li>
 * </ul>
 * 
 * Usage example:
 * 
 * <pre>
 * QSim qsim = new QSimBuilder(config) //
 * 		.useDefaults() //
 * 		.addPlugin(new MyCustomQSimPlugin(config)) //
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

	private final List<AbstractModule> overridingModules = new LinkedList<>();
	private final Collection<AbstractQSimPlugin> plugins = new LinkedList<>();
	private final QSimComponents components = new QSimComponents();

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
		useDefaultPlugins();
		return this;
	}

	/**
	 * Adds a module that overrides existing bindings from MATSim (i.e. mainly those
	 * from the {@link StandaloneQSimModule} and derived stages).
	 */
	public QSimBuilder addOverridingModule(AbstractModule module) {
		overridingModules.add(module);
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
	public QSimBuilder configureComponents(Consumer<QSimComponents> configurator) {
		configurator.accept(components);
		return this;
	}

	/**
	 * Resets the registered plugins to the default ones provided by MATSim.
	 */
	public QSimBuilder useDefaultPlugins() {
		plugins.clear();
		plugins.addAll(QSimModule.getDefaultQSimPlugins(config));
		return this;
	}

	/**
	 * Configures the registered plugins via callback.
	 */
	public QSimBuilder configurePlugins(Consumer<Collection<AbstractQSimPlugin>> configurator) {
		configurator.accept(plugins);
		return this;
	}

	/**
	 * Adds a plugin for the QSim to the existing list of plugins.
	 */
	public QSimBuilder addPlugin(AbstractQSimPlugin plugin) {
		plugins.add(plugin);
		return this;
	}

	/**
	 * Removes a QSim plugin with a specific type form the list of registered
	 * plugins.
	 */
	public QSimBuilder removePlugin(Class<? extends AbstractQSimPlugin> pluginType) {
		plugins.removeIf(pluginType::isInstance);
		return this;
	}

	/**
	 * Builds a new QSim with the registered plugins and the defined active
	 * components.
	 */
	public QSim build(Scenario scenario, EventsManager eventsManager) {
		// First, load standard QSim module
		AbstractModule module = new StandaloneQSimModule(scenario, eventsManager);

		// Add all overrides
		for (AbstractModule override : overridingModules) {
			module = org.matsim.core.controler.AbstractModule.override(Collections.singleton(module), override);
		}

		// Overide plugins
		module = AbstractModule.override(Collections.singleton(module), new AbstractModule() {
			@Override
			public void install() {
				bind(new TypeLiteral<Collection<AbstractQSimPlugin>>() {
				}).toInstance(plugins);
			}
		});

		// Override components
		module = AbstractModule.override(Collections.singleton(module), new AbstractModule() {
			@Override
			public void install() {
				bind(QSimComponents.class).toInstance(components);
			}
		});

		// Build QSim
		Injector injector = org.matsim.core.controler.Injector.createInjector(config, module);
		return (QSim) injector.getInstance(Mobsim.class);
	}

	private static class StandaloneQSimModule extends org.matsim.core.controler.AbstractModule {
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
			install(new QSimModule());
		}
	}
}
