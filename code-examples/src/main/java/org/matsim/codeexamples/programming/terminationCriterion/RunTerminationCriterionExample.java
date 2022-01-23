package org.matsim.codeexamples.programming.terminationCriterion;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.scenario.ScenarioUtils;

class RunTerminationCriterionExample{

	public static void main( String[] args ){

		Config config = ConfigUtils.createConfig();
		config.controler().setLastIteration( 100 );
		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );

		Scenario scenario = ScenarioUtils.loadScenario( config );

		Controler controler = new Controler( scenario );

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				this.bind( TerminationCriterion.class ).to( MyTerminationCriterion.class);
			}
		} );

		controler.run();

	}

	private static class MyTerminationCriterion implements TerminationCriterion {
//		@Inject Something something;
		@Override public boolean mayTerminateAfterIteration( int iteration ){
			if ( iteration==10 ) {
				return true;
			}
			return false;
		}
		@Override public boolean doTerminate( int iteration ){
			return true;
		}
	}
}
