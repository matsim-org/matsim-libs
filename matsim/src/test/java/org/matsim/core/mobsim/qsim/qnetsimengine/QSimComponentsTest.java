package org.matsim.core.mobsim.qsim.qnetsimengine;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.inject.Inject;
import com.google.inject.ProvisionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
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

	private static final Logger log = LogManager.getLogger( QSimComponentsTest.class );
	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testRemoveNetsimEngine() {
		assertThrows(RuntimeException.class, () -> {
			// test removing a default qsim module by name

			// running MATSim should fail after removing the netsim engine, since some of the routes do not have a travel time, and in consequence cannot
			// be teleported if netsim engine is missing.  Thus, the RuntimeException confirms that removing the module worked.

			Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
			config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
			config.controller().setLastIteration(0);
			config.controller().setOutputDirectory(utils.getOutputDirectory());

			// remove the module:  (There is also syntax at some intermediate level for this, but I prefer the syntax at config level.  kai, oct'22)
			QSimComponentsConfigGroup componentsConfig = ConfigUtils.addOrGetModule(config, QSimComponentsConfigGroup.class);
			List<String> components = componentsConfig.getActiveComponents();
			components.remove(QNetsimEngineModule.COMPONENT_NAME);
			componentsConfig.setActiveComponents(components);

			Scenario scenario = ScenarioUtils.loadScenario(config);

			Controler controler = new Controler( scenario );

			controler.run();
		});
	}

	@Test
	void testReplaceQNetworkFactory() {
		assertThrows(ProvisionException.class, () -> {
			// here we try to replace the QNetworkFactory.  Complains that QNetworkFactory is bound multiple times.

			Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
			config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
			config.controller().setLastIteration(0);
			config.controller().setOutputDirectory(utils.getOutputDirectory());

			Scenario scenario = ScenarioUtils.loadScenario(config);

			Controler controler = new Controler( scenario );

			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					this.installQSimModule(new AbstractQSimModule(){
						@Override
						protected void configureQSim() {
							bind(QNetworkFactory.class).to(MyNetworkFactory.class);
						}
					});
				}
			});

			controler.run();
		});
	}

	@Test
	void testReplaceQNetworkFactory2() {
		// here we try to replace the QNetworkFactory at AbstractQSimModule.  This works.

		Config config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" ) );
		config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		config.controller().setLastIteration( 0 );
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		Scenario scenario = ScenarioUtils.loadScenario( config );

		Controler controler = new Controler( scenario );

		controler.addOverridingQSimModule( new AbstractQSimModule(){
			@Override public void configureQSim(){
				bind( QNetworkFactory.class ).to( MyNetworkFactory.class );
			}
		} );

		controler.run();
	}

	@Test
	void testOverridingQSimModule() {
		// use the newly implemented install _overriding_ qsim module.  With this, replacing the QNetworkFactory now works as part of AbstractModule.

		Config config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" ) );
		config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		config.controller().setLastIteration( 0 );
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		Scenario scenario = ScenarioUtils.loadScenario( config );

		Controler controler = new Controler( scenario );

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				this.installOverridingQSimModule( new AbstractQSimModule(){
					@Override protected void configureQSim(){
						bind( QNetworkFactory.class ).to( MyNetworkFactory.class );
					}
				} );
			}
		} );

		controler.run();
	}


	private static class MyNetworkFactory implements QNetworkFactory {
		ConfigurableQNetworkFactory delegate ;
		@Inject MyNetworkFactory( EventsManager events, Scenario scenario ) {
			delegate = new ConfigurableQNetworkFactory( events, scenario );
		}
		@Override public void initializeFactory( AgentCounter agentCounter, MobsimTimer mobsimTimer, QNetsimEngineI.NetsimInternalInterface simEngine1 ){
			delegate.initializeFactory( agentCounter, mobsimTimer, simEngine1 );
		}
		@Override public QNodeI createNetsimNode( Node node ){
			return delegate.createNetsimNode( node );
		}
		@Override public QLinkI createNetsimLink( Link link, QNodeI queueNode ){
			return delegate.createNetsimLink( link, queueNode);
		}
	}
}
