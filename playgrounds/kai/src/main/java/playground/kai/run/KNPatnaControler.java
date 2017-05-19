package playground.kai.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;

class KNPatnaControler {
	
	public static void main ( String[] args ) {
		Logger.getLogger("blabla").warn("here") ;
		
		// prepare the config:
		Config config = ConfigUtils.loadConfig( args[0] ) ;
		config.plans().setRemovingUnneccessaryPlanAttributes(true) ;
		
		// prepare the scenario
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		// prepare the control(l)er:
		Controler controler = new Controler( scenario ) ;
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.addControlerListener(new KaiAnalysisListener()) ;
		controler.addOverridingModule(new OTFVisFileWriterModule());
//		controler.setMobsimFactory(new OldMobsimFactory()) ;

		// run everything:
		controler.run();
	
	}
	
//	static class OldMobsimFactory implements MobsimFactory {
//		@Override
//		public Mobsim createMobsim( Scenario sc, EventsManager eventsManager ) {
//			return new QueueSimulation(sc, eventsManager);
//		}
//	}
	
	static class PatnaMobsimFactory implements MobsimFactory {
		private boolean useOTFVis = false ;

		@Override
		public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
			
	        QSimConfigGroup conf = sc.getConfig().qsim();
	        if (conf == null) {
	            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
	        }

	        // construct the QSim:
			QSim qSim = new QSim(sc, eventsManager);

			// add the actsim engine:
			ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
			qSim.addMobsimEngine(activityEngine);
			qSim.addActivityHandler(activityEngine);

			// add the netsim engine:

			QNetsimEngine netsimEngine = new QNetsimEngine(qSim) ;
//			QNetsimEngine netsimEngine = netsimEngFactory.createQSimEngine(qSim, MatsimRandom.getRandom());
			qSim.addMobsimEngine(netsimEngine);
			qSim.addDepartureHandler(netsimEngine.getDepartureHandler());

			TeleportationEngine teleportationEngine = new TeleportationEngine(sc, eventsManager);
			qSim.addMobsimEngine(teleportationEngine);
	        
			AgentFactory agentFactory;
//	        if (sc.getConfig().transit().isUseTransit()) {
//	            agentFactory = new TransitAgentFactory(qSim);
//	            TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
//	            transitEngine.setUseUmlaeufe(true);
//	            transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
//	            qSim.addDepartureHandler(transitEngine);
//	            qSim.addAgentSource(transitEngine);
//	            qSim.addMobsimEngine(transitEngine);
//	        } else {
			agentFactory = new DefaultAgentFactory(qSim);
//	        }
//	        if (sc.getConfig().network().isTimeVariantNetwork()) {
//				qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
//			}

	        PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);

	        //	        setter for ModeVehicleTypes to agent source is gone now. Amit May'17
//	        Map<String, VehicleType> modeVehicleTypes = new HashMap<String, VehicleType>();
//	        VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car", VehicleType.class));
//	        car.setMaximumVelocity(60.0/3.6);
//	        car.setPcuEquivalents(1.0);
//	        modeVehicleTypes.put("car", car);
//
//	        VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create("bike", VehicleType.class));
//	        bike.setMaximumVelocity(60.0/3.6);
//	        bike.setPcuEquivalents(0.25);
//	        modeVehicleTypes.put("bike", bike);
//
//	        VehicleType bicycles = VehicleUtils.getFactory().createVehicleType(Id.create("bicycle", VehicleType.class));
//	        bicycles.setMaximumVelocity(15.0/3.6);
//	        bicycles.setPcuEquivalents(0.05);
//	        modeVehicleTypes.put("bicycle", bicycles);
//
//	        VehicleType walks = VehicleUtils.getFactory().createVehicleType(Id.create("walk", VehicleType.class));
//	        walks.setMaximumVelocity(1.5);
//	        walks.setPcuEquivalents(0.10);  			// assumed pcu for walks is 0.1
//	        modeVehicleTypes.put("walk", walks);
//
//			agentSource.setModeVehicleTypes(modeVehicleTypes);

	        qSim.addAgentSource(agentSource);
			
			if ( useOTFVis ) {
				// otfvis configuration.  There is more you can do here than via file!
				final OTFVisConfigGroup otfVisConfig = ConfigUtils.addOrGetModule(qSim.getScenario().getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
				otfVisConfig.setDrawTransitFacilities(false) ; // this DOES work
				//				otfVisConfig.setShowParking(true) ; // this does not really work

				OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, eventsManager, qSim);
				OTFClientLive.run(sc.getConfig(), server);
			}
			return qSim ;
		}
	}
}
