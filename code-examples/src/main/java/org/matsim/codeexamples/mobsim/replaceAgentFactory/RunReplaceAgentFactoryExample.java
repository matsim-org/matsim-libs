package org.matsim.codeexamples.mobsim.replaceAgentFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.QSimModule;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

class RunReplaceAgentFactoryExample {
	
	private Config config ;
	private Scenario scenario ;
	private Controler controler ;
	
	public static void main ( String [] args ) {
		final RunReplaceAgentFactoryExample matsim = new RunReplaceAgentFactoryExample();;
		matsim.run() ;
	}
	
	Controler prepareControler() {
		if ( scenario==null ) {
			prepareScenario() ;
		}
		controler = new Controler( scenario );
		
		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(AgentFactory.class).to(MyAgentFactory.class);
			}
		});
		
		return controler;
	}
	
	void run() {
		if ( controler==null) {
			prepareControler() ;
		}
		controler.run() ;
	}
	
	Scenario prepareScenario( ) {
		if ( config==null ) {
			prepareConfig() ;
		}
		scenario = ScenarioUtils.loadScenario( config );
		return scenario ;
	}
	
	Config prepareConfig() {
//		config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "siouxfalls-2014" ), "config_default.xml") ) ;
		config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml") ) ;
		return config;
	}
	
}
