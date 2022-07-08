package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.google.inject.ProvisionException;
import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.List;

public class QSimComponentsTest{
	private final static String MY_NETSIM_ENGINE = "MyNetsimEngine";

	private static final Logger log = Logger.getLogger( QSimComponentsTest.class );
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test(expected = RuntimeException.class)
	public void testRemoveNetsimEngine() {

		Config config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" ) );
		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration( 0 );

		QSimComponentsConfigGroup componentsConfig = ConfigUtils.addOrGetModule( config, QSimComponentsConfigGroup.class );
		List<String> components = componentsConfig.getActiveComponents();
		components.remove( QNetsimEngineModule.COMPONENT_NAME );
		componentsConfig.setActiveComponents( components );


		Scenario scenario = ScenarioUtils.loadScenario( config );

		Controler controler = new Controler( scenario );

		controler.run();
	}

	@Test(expected= ProvisionException.class)
	public void testInstallQSimModule() {

		Config config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" ) );
		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration( 0 );

		QSimComponentsConfigGroup componentsConfig = ConfigUtils.addOrGetModule( config, QSimComponentsConfigGroup.class );
		List<String> components = componentsConfig.getActiveComponents();
		components.remove( QNetsimEngineModule.COMPONENT_NAME );
		components.add( MY_NETSIM_ENGINE );
		componentsConfig.setActiveComponents( components );


		Scenario scenario = ScenarioUtils.loadScenario( config );

		Controler controler = new Controler( scenario );

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				this.installQSimModule( new AbstractQSimModule(){
					@Override protected void configureQSim(){
						bind( QNetsimEngineI.class ).to( QNetsimEngineWithThreadpool.class ).asEagerSingleton();

						bind( VehicularDepartureHandler.class ).toProvider( QNetsimEngineDepartureHandlerProvider.class ).asEagerSingleton();

						bind( QNetworkFactory.class ).to( BrokenNetworkFactory.class );

						addQSimComponentBinding( MY_NETSIM_ENGINE ).to( VehicularDepartureHandler.class );
						addQSimComponentBinding( MY_NETSIM_ENGINE ).to( QNetsimEngineI.class );
					}
				} );
			}
		} );

		controler.run();
	}

	@Test(expected = RuntimeException.class)
	public void testOverridingQSimModule() {

		Config config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" ) );
		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration( 0 );

		Scenario scenario = ScenarioUtils.loadScenario( config );

		Controler controler = new Controler( scenario );

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				this.installOverridingQSimModule( new AbstractQSimModule(){
					@Override protected void configureQSim(){
						bind( QNetworkFactory.class ).to( BrokenNetworkFactory.class );
					}
				} );
			}
		} );

		controler.run();
	}


	private static class BrokenNetworkFactory implements QNetworkFactory {
		@Override public void initializeFactory( AgentCounter agentCounter, MobsimTimer mobsimTimer, QNetsimEngineI.NetsimInternalInterface simEngine1 ){
			throw new RuntimeException( "not implemented" );
		}
		@Override public QNodeI createNetsimNode( Node node ){
			throw new RuntimeException( "not implemented" );
		}
		@Override public QLinkI createNetsimLink( Link link, QNodeI queueNode ){
			throw new RuntimeException( "not implemented" );
		}
	}
}
