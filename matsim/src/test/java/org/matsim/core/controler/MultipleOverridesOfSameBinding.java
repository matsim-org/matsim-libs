package org.matsim.core.controler;

import com.google.inject.CreationException;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class MultipleOverridesOfSameBinding{
	private static final Logger log = Logger.getLogger( MultipleOverridesOfSameBinding.class ) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	private static int mobsimVersion = 0 ;

	@Test
	public void testSecondOverridesFirst() {
		Controler controler = getControler();

		controler.addOverridingModule( new Mobsim1Module() ) ;
		controler.addOverridingModule( new Mobsim2Module() ) ;

		controler.run() ;

		Assert.assertEquals( 2, mobsimVersion );

	}
	@Test
	public void testInstallDoesNotOverride() {
		Controler controler = getControler();

		try{
			controler.addOverridingModule( new AbstractModule(){
				@Override public void install(){
					install( new Mobsim1Module() );
					install( new Mobsim2Module() );
				}
			} );
		} catch ( Exception ee ) {
			Assert.assertTrue( ee instanceof CreationException );
		}

	}
	@Test
	public void testNewAddModueMethod() {
		Controler controler = getControler();

		try{
			controler.addModule( new Mobsim1Module() );
			controler.addModule( new Mobsim2Module() );
		} catch ( Exception ee ) {
			Assert.assertTrue( ee instanceof CreationException );
		}
	}

	private Controler getControler(){
		mobsimVersion = 0 ;

		Config config = ConfigUtils.createConfig() ;
		config.controler().setOutputDirectory( utils.getOutputDirectory() );
		config.controler().setLastIteration( 0 );

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		return new Controler( scenario );
	}

	private static class Mobsim1Module extends AbstractModule{
		@Override public void install(){
			this.bindMobsim().toInstance( new Mobsim(){
				@Override public void run(){
					log.warn("running mobsim1") ;
					mobsimVersion = 1 ;
				}
			} );
		}
	}
	private static class Mobsim2Module extends AbstractModule{
		@Override public void install(){
			this.bindMobsim().toInstance( new Mobsim(){
				@Override public void run(){
					log.warn("running mobsim2") ;
					mobsimVersion = 2 ;
				}
			} );
		}
	}
}
