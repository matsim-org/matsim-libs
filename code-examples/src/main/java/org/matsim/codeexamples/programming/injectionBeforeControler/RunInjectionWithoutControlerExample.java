/**
 * 
 */
package org.matsim.codeexamples.programming.injectionBeforeControler;

import com.google.inject.Module;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

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
		com.google.inject.Injector injector = createMinimalMatsimInjector( config, scenario );

		// retreive something from the matsim injection infrastructure and do something with it:
		TripRouter tripRouter = injector.getInstance( TripRouter.class ) ;
		tripRouter.getRoutingModule( "car" ) ;
		
	}

	static com.google.inject.Injector createMinimalMatsimInjector( Config config, Scenario scenario, Module... modules ){
		// yyyyyy TODO move into org.matsim.core.Injector

		final Collection<Module> theModules = new ArrayList<>();
		theModules.add( new AbstractModule(){
			@Override
			public void install(){
				install( new NewControlerModule() );
				install( new ControlerDefaultCoreListenersModule() );
				install( new ControlerDefaultsModule() );
				install( new ScenarioByInstanceModule( scenario ) );
			}
		});
		theModules.addAll( Arrays.asList( modules ) );

		return Injector.createInjector( config, theModules.toArray(new Module[0]) );
	}

}
