package org.matsim.codeexamples.mobsim.addRemoveReplaceQSimCompoments;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.List;

class RunQSimComponentRequestInConfigExample{
	private static final Logger log = LogManager.getLogger( RunQSimComponentRequestInConfigExample.class ) ;

	public static void main( String[] args ){

		Config config = ConfigUtils.createConfig() ;
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration( 2 );

		QSimComponentsConfigGroup qsimComponentsConfig = ConfigUtils.addOrGetModule( config, QSimComponentsConfigGroup.class );

		// the following requests that a component registered under the name "abc" will be used:
		List<String> cmps = qsimComponentsConfig.getActiveComponents();
		cmps.add( "abc") ;
		qsimComponentsConfig.setActiveComponents( cmps );

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		Controler controler = new Controler( scenario ) ;

		// the following registers a component under the name "abc".  This will guice-bind the component, but from this alone the new component will not be used.
		controler.addOverridingQSimModule( new AbstractQSimModule(){
			@Override protected void configureQSim(){
				this.addQSimComponentBinding( "abc" ).to( MyQSimComponent.class ) ;
			}
		} ) ;

		controler.run() ;

	}

	/**
	 * This is our new {@link QSimComponent}.  It needs to implement at least one of the downstream interfaces of {@link
	 * QSimComponent}, e.g. {@link MobsimEngine} or {@link org.matsim.core.mobsim.qsim.interfaces.DepartureHandler} in order to be
	 * used.  The mechanics will register it separately for each such interface that it implements.
	 */
	private static class MyQSimComponent implements MobsimEngine {

		@Override
		public void onPrepareSim(){
			log.info("calling onPrepareSim") ;
		}

		@Override
		public void afterSim(){
			log.info("calling afterSim") ;
		}

		@Override
		public void setInternalInterface( InternalInterface internalInterface ){
			log.info("calling setInternalInterface") ;
		}

		@Override
		public void doSimStep( double time ){
		}
	}
}
