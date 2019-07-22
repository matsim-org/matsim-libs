/**
 * 
 */
package org.matsim.codeexamples.programming.injectionBeforeControler;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.*;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;

import static org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists;

/**
 * @author kainagel
 */
public class RunInjectionWithoutControlerExample{

	public static void main(String[] args) {

		// create a config:
		Config config = ConfigUtils.createConfig() ;
		config.controler().setOverwriteFileSetting( deleteDirectoryIfExists );

		// create a scenario:
		final Scenario scenario = ScenarioUtils.createScenario(config);

		// create an injector with the matsim infrastructure:
		com.google.inject.Injector injector = org.matsim.core.controler.Injector.createInjector(config, new AbstractModule() {
			@Override
			public void install() {
				install(new NewControlerModule());
				install(new ControlerDefaultCoreListenersModule());
				install(new ControlerDefaultsModule());
				install(new ScenarioByInstanceModule(scenario));
			}
		});

		// retreive something from the matsim injection infrastructure and do something with it:
		TripRouter tripRouter = injector.getInstance( TripRouter.class ) ;
		tripRouter.getRoutingModule( "car" ) ;
		
	}

}
