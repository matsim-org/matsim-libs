/**
 * 
 */
package tutorial.programming.injectionBeforeControler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.ControlerI;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author kainagel
 * @see {@link NewControlerTest}
 */
public class RunInjectionBeforeControlerExample {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig() ;
		config.controler().setLastIteration(1);
		final Scenario scenario = ScenarioUtils.createScenario(config);
		com.google.inject.Injector injector = Injector.createInjector(config, new AbstractModule() {
			@Override
			public void install() {
				install(new NewControlerModule());
				install(new ControlerDefaultCoreListenersModule());
				install(new ControlerDefaultsModule());
				install(new ScenarioByInstanceModule(scenario));
			}
		});
		
		ControlerI controler = injector.getInstance(ControlerI.class);
		// So the trick is, other then with config and scenario, not to pass the injector into the controler.  But to get the controler out of the injector.
		//
		// Also note that what we have here is ControlerI, not the full Controler.
		
		controler.run();

	}

}
