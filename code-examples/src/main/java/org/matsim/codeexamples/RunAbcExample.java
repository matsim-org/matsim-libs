package org.matsim.codeexamples;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import java.net.URL;
import java.util.Collection;

public final class RunAbcExample{

	private final String[] args;
	private Config config = null ;
	private Scenario scenario = null ;

	public static void main ( String [] args ) {
		new RunAbcExample( args ).run() ;
	}

	public RunAbcExample( String [] args ) {
		this.args = args ;
	}

	public final Config prepareConfig() {
		URL url = ExamplesUtils.getTestScenarioURL( "equil" ) ;
		URL configUrl = IOUtils.newUrl( url, "config.xml" );;
		config = ConfigUtils.loadConfig( configUrl ) ;
		return config ;
	}

	public final Scenario prepareScenario() {
		if ( config==null ) {
			prepareConfig() ;
		}
		scenario = ScenarioUtils.loadScenario( config ) ;
		return scenario ;
	}

	public final void prepareAndRunControler( Collection<AbstractModule> controlerOverrides, Collection<AbstractQSimModule> qsimOverrides ) {
		if ( scenario==null ) {
			prepareScenario() ;
		}

		Controler controler = new Controler( scenario );;

		if ( controlerOverrides!=null ) {
			for( AbstractModule controlerOverride : controlerOverrides ){
				controler.addOverridingModule( controlerOverride ) ;
			}
		}
		if ( qsimOverrides!=null ) {
			for( AbstractQSimModule qsimOverride : qsimOverrides ){
				controler.addOverridingQSimModule( qsimOverride ) ;
			}
		}

	}

	void run() {
		prepareAndRunControler( null, null );
	}

}
