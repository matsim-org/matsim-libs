package org.matsim.codeexamples.mobsim.mobsimListener;

import com.google.inject.Singleton;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.scenario.ScenarioUtils;

class TestMobsimListenerScope{
	private static final Logger log = LogManager.getLogger( TestMobsimListenerScope.class );

	private static class MyMobsimListener implements MobsimInitializedListener {
		MyMobsimListener() {
			log.warn("calling arg-free ctor ...");
		}
		@Override public void notifyMobsimInitialized( MobsimInitializedEvent e ){
			log.warn( "calling notifyMobsimInitialized ...");
		}
	}
	public static void main( String[] args ){

		Config config = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration( 2 );

		Scenario scenario = ScenarioUtils.createScenario( config );

		Controler controler = new Controler( scenario );

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				// if this is NOT a singleton, it is constructed in every iteration:
				this.addMobsimListenerBinding().to( MyMobsimListener.class );

				// if it IS a singleton, it is constructed only once:
//				this.addMobsimListenerBinding().to( MyMobsimListener.class ).in( Singleton.class );
			}
		} );

		controler.run();
	}

}
