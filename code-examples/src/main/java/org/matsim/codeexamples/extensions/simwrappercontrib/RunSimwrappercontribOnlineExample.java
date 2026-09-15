package org.matsim.codeexamples.extensions.simwrappercontrib;

import com.google.inject.multibindings.Multibinder;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperModule;

import java.net.URL;

/**
 * example class that I started writing during CR's class.  Some of it worked, but I cannot remember if the final version worked.  kai, jan'23
 */
class RunSimwrappercontribOnlineExample{

	public static void main( String[] args ){

		URL url = IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" );

		Config config = ConfigUtils.loadConfig( url );

		config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		config.controller().setLastIteration( 1 );


		Scenario scenario = ScenarioUtils.loadScenario( config );

		Controler controler = new Controler( scenario );

		controler.addOverridingModule( new SimWrapperModule() );

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){

				// Bind dashboard via guice
				SimWrapper.addDashboardBinding(binder()).to( RunSimwrappercontribOfflineExample.MyDashboard.class );

			}
		} );

		controler.run();

	}

}
