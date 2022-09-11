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
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * As an alternative to {@link RunQSimComponentRequestInConfigExample}, one can also request the new component at the {@link
 * Controler} level.
 * <br/>
 * Personally, I don't like this very much, since it introduces an additional level.  E.g. stuff that is configured at the config
 * level can be reconfigured at the controler level.  And it is an additional level that exists for the qsim plugins, but not for
 * the controler plugins.  My recommendation would be to ignore this until you have mastered the approach of {@link
 * RunQSimComponentRequestInConfigExample}, but unfortunately you will find lots of code where {@link QSim} is configured at the
 * controler level, as shown here.  kn, mar'19
 */
class RunQSimComponentRequestInControlerExample{
	private static final Logger log = LogManager.getLogger( RunQSimComponentRequestInControlerExample.class ) ;

	public static void main( String[] args ){

		Config config = ConfigUtils.createConfig() ;
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration( 2 );
		config.qsim().setNumberOfThreads( 1 );

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		Controler controler = new Controler( scenario ) ;

		// --> The following is the piece of code that requests the qsim compoent "abc" at controler level:
		controler.configureQSimComponents( new QSimComponentsConfigurator(){
			@Override public void configure( QSimComponentsConfig components ){
				components.addNamedComponent( "abc" );
				log.info("=== after ...") ;
				for( Object component : components.getActiveComponents() ){
					log.info( component.toString() ) ;
				}
			}
		} ) ;

//		// yyyy if you run the following, the components are reset to their defaults:
//		controler.configureQSimComponents( new QSimComponentsConfigurator(){
//			@Override public void configure( QSimComponentsConfig components ){
//				log.info("=== check ...") ;
//				for( Object component : components.getActiveComponents() ){
//					log.info( component.toString() ) ;
//				}
//			}
//		} ) ;

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
