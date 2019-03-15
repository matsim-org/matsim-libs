package org.matsim.codeexamples.mobsim.mobsimPassingVehicleQ;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.codeexamples.config.RunFromConfigfileExample;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.DefaultTeleportationEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import javax.inject.Inject;

/**
 * Example to show how the standard queue can be replaced by something else.  Search for PassingVehicleQ in the code below.
 * <p></p>
 * I have a version of this that was running about a year ago in my playground.  This "tutorial" version has never been tested (but please
 * feel free to test, report back, and fix).
 * 
 * @author nagel
 *
 */
final class RunMobsimWithMultipleModeVehiclesExample {

	private final String[] args;
	private Config config;

	public static void main( String[] args ) {
		new RunMobsimWithMultipleModeVehiclesExample( args ).run();

	}

	RunMobsimWithMultipleModeVehiclesExample( String [] args ) {
		this.args = args ;
	}

	Config prepareConfig() {
		config = ConfigUtils.loadConfig( IOUtils.newUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" ) );
		config.qsim().setLinkDynamics( QSimConfigGroup.LinkDynamics.PassingQ );
		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
		config.plans().setRemovingUnneccessaryPlanAttributes(true) ;
		return config ;
	}

	void run(){

		// prepare the scenario
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		VehicleType car = VehicleUtils.getFactory().createVehicleType( Id.create("car", VehicleType.class ) );
		car.setMaximumVelocity(60.0/3.6);
		car.setPcuEquivalents(1.0);
		scenario.getVehicles().addVehicleType(car);

		VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create("bike", VehicleType.class));
		bike.setMaximumVelocity(60.0/3.6);
		bike.setPcuEquivalents(0.25);
		scenario.getVehicles().addVehicleType(bike);

		VehicleType bicycles = VehicleUtils.getFactory().createVehicleType(Id.create("bicycle", VehicleType.class));
		bicycles.setMaximumVelocity(15.0/3.6);
		bicycles.setPcuEquivalents(0.05);
		scenario.getVehicles().addVehicleType(bicycles);

		VehicleType walks = VehicleUtils.getFactory().createVehicleType(Id.create("walk", VehicleType.class));
		walks.setMaximumVelocity(1.5);
		walks.setPcuEquivalents(0.10);  			// assumed pcu for walks is 0.1
		scenario.getVehicles().addVehicleType(walks);

		// prepare the control(l)er:
		Controler controler = new Controler( scenario ) ;
		//		controler.addOverridingModule(new AbstractModule() {
		//			@Override
		//			public void install() {
		//				bindMobsim().toProvider(MultipleModeVehiclesQSimFactory.class);
		//			}
		//		});
		// I don't think that this is still needed.  kai, feb'19

//		controler.addOverridingModule( new OTFVisLiveModule() ) ;

		// run everything:
		controler.run();
	}

	//	/**
//	 * Look into {@link org.matsim.core.mobsim.qsim.QSimUtils} for the default matsim qsim factory.  This is copy and paste (and somewhat reduced).
//	 *
//	 * @author nagel
//	 *
//	 */
//	static class MultipleModeVehiclesQSimFactory implements Provider<Mobsim> {
//
//		@Inject Scenario scenario;
//		@Inject EventsManager eventsManager;
//
//		@Override
//		public Mobsim get() {
//
//			QSimConfigGroup conf = scenario.getConfig().qsim();
//			if (conf == null) {
//				throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
//			}
//
//
//
//			return new QSimBuilder(scenario.getConfig()) //
//					.useDefaults() //
//					.build(scenario, eventsManager);
//
//			/*// construct the QSim:
//			QSim qSim = new QSim(scenario, eventsManager);
//
//			// add the activity engine:
//			ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
//			qSim.addMobsimEngine(activityEngine);
//			qSim.addActivityHandler(activityEngine);
//
//			// add the netsim engine:
//			QNetsimEngine netsimEngine = new QNetsimEngine(qSim) ;
//			qSim.addMobsimEngine(netsimEngine);
//			qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
//
//			TeleportationEngine teleportationEngine = new DefaultTeleportationEngine(scenario, eventsManager);
//			qSim.addMobsimEngine(teleportationEngine);
//
//			AgentFactory agentFactory = new DefaultAgentFactory(qSim);
//
//			PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qSim);
//			qSim.addAgentSource(agentSource);
//
//			return qSim ;*/
//		}
//	}


}
