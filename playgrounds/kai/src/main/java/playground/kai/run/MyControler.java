package playground.kai.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.OTFVisConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.ParallelQNetsimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFFileWriterFactory;
import org.matsim.vis.otfvis.OnTheFlyServer;

class MyControler {
	
	public static void main ( String[] args ) {
		Logger.getLogger("blabla").warn("here") ;

		Controler controler = new Controler( args ) ;

		controler.setOverwriteFiles(true) ;
		
		controler.addControlerListener(new KaiAnalysisListener()) ;
		
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		
		controler.run();
	
	}
	
	static class MyMobsimFactory implements MobsimFactory {
		private boolean useOTFVis = true ;

		@Override
		public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {

			QSimConfigGroup conf = sc.getConfig().getQSimConfigGroup();
			if (conf == null) {
				throw new NullPointerException("There is no configuration set for the QSim. Add the module 'qsim' to your config file.");
			}
			if (conf.getNumberOfThreads() > 1) {
				eventsManager = new SynchronizedEventsManagerImpl(eventsManager);
			}
			QSim qSim = new QSim(sc, eventsManager);
			
			ActivityEngine activityEngine = new ActivityEngine();
			qSim.addMobsimEngine(activityEngine);
			qSim.addActivityHandler(activityEngine);
			
			QNetsimEngineFactory netsimEngFactory;
			if (conf.getNumberOfThreads() > 1) {
				netsimEngFactory = new ParallelQNetsimEngineFactory();
			} else {
				netsimEngFactory = new DefaultQSimEngineFactory();
			}
			QNetsimEngine netsimEngine = netsimEngFactory.createQSimEngine(qSim, MatsimRandom.getRandom());
			qSim.addMobsimEngine(netsimEngine);
			qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
			
			qSim.addMobsimEngine(new TeleportationEngine());
			

			if ( useOTFVis ) {
				// otfvis configuration.  There is more you can do here than via file!
				final OTFVisConfigGroup otfVisConfig = qSim.getScenario().getConfig().otfVis();
				otfVisConfig.setDrawTransitFacilities(false) ; // this DOES work
				//				otfVisConfig.setShowParking(true) ; // this does not really work

				OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, eventsManager, qSim);
				OTFClientLive.run(sc.getConfig(), server);
			}
			return qSim ;
		}
	}
}
