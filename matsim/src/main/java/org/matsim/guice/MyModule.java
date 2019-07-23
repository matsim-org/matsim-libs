package org.matsim.guice;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;


class MyModule extends AbstractModule {
	@Override
    public void install() {
        install(new NewControlerModule());
        install(new ControlerDefaultCoreListenersModule());
        install(new ControlerDefaultsModule());
        install(new ScenarioByInstanceModule(ScenarioUtils.createScenario(ConfigUtils.createConfig())));
    }
}
