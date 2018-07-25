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

public class QSimBuilder {
	private final Config config;

	private final List<AbstractModule> overridingModules = new LinkedList<>();
	private final Collection<AbstractQSimPlugin> plugins = new LinkedList<>();
	private final QSimComponents components = new QSimComponents();

	public QSimBuilder(Config config) {
		this.config = config;
	}

	public QSimBuilder useDefaults() {
		useDefaultComponents();
		useDefaultPlugins();
		return this;
	}

	public QSimBuilder addOverridingModule(AbstractModule module) {
		this.overridingModules.add(module);
		return this;
	}

	public QSimBuilder addOverridingModules(List<AbstractModule> modules) {
		modules.forEach(this.overridingModules::add);
		return this;
	}

	public QSimBuilder useDefaultComponents() {
		components.activeActivityHandlers.clear();
		components.activeAgentSources.clear();
		components.activeDepartureHandlers.clear();
		components.activeMobsimEngines.clear();

		new StandardQSimComponentsConfigurator(config).configure(components);

		return this;
	}

	public QSimBuilder configureComponents(Consumer<QSimComponents> configurator) {
		configurator.accept(components);
		return this;
	}

	public QSimBuilder useDefaultPlugins() {
		plugins.clear();
		plugins.addAll(QSimModule.getDefaultQSimPlugins(config));
		return this;
	}

	public QSimBuilder configurePlugins(Consumer<Collection<AbstractQSimPlugin>> configurator) {
		configurator.accept(plugins);
		return this;
	}

	public QSimBuilder addPlugin(AbstractQSimPlugin plugin) {
		this.plugins.add(plugin);
		return this;
	}

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
