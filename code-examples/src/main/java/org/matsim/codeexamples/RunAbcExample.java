package org.matsim.codeexamples;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.AllowsConfiguration;
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
	private Controler controler = null ;

	public static void main ( String [] args ) {
		new RunAbcExample( args ).run() ;
	}

	public RunAbcExample( String [] args ) {
		this.args = args ;
	}

	public final Config prepareConfig() {
		if ( args!=null && args.length > 0 ) {
			config = ConfigUtils.loadConfig( args[0] ) ;
		} else{
			throw new RuntimeException("need to provide path to config file. aborting ...") ;
		}
		return config ;
	}

	public final Scenario prepareScenario() {
		if ( config==null ) {
			prepareConfig() ;
		}
		scenario = ScenarioUtils.loadScenario( config ) ;
		return scenario ;
	}

	public final void addOverridingModule( AbstractModule controlerModule ) {
		if ( controler==null ){
			prepareControler();
		}
		controler.addOverridingModule( controlerModule ) ;
	}
	public final void addOverridingQSimModule( AbstractQSimModule qSimModule ) {
		if ( controler==null ){
			prepareControler();
		}
		controler.addOverridingQSimModule( qSimModule ) ;
	}

	public final void run() {
		if ( controler==null ) {
			prepareControler() ;
		}
		controler.run() ;
	}

	public final Controler prepareControler() {
		if ( scenario==null ) {
			prepareScenario() ;
		}
		controler = new Controler( scenario ) ;
		return controler ;
	}

}
