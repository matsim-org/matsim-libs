package playground.andreas;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.Gbl;

/**
 * @author aneumann
 */
public class TransitControler extends Controler {
	private final static Logger log = Logger.getLogger(TransitControler.class);

	private boolean useOTFVis = true;
	private boolean useHeadwayControler = false;
	
	public TransitControler(Config config) {
		super(config);
		throw new RuntimeException(Gbl.RUN_MOB_SIM_NO_LONGER_POSSIBLE ) ;
	}
	
//	@Override
//	protected void runMobSim() {
//		
//		log.info("Overriding runMobSim()");
//
//        Scenario sc = this.getScenario();EventsManager eventsManager = this.getEvents();
//
//        QSimConfigGroup conf = sc.getConfig().qsim();
//        if (conf == null) {
//            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
//        }
//
//		QSim qSim1 = new QSim(sc, eventsManager);
//		ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim1.getAgentCounter());
//		qSim1.addMobsimEngine(activityEngine);
//		qSim1.addActivityHandler(activityEngine);
//        QNetsimEngineModule.configure(qSim1);
//		TeleportationEngine teleportationEngine = new TeleportationEngine(sc, eventsManager);
//		qSim1.addMobsimEngine(teleportationEngine);
//        QSim qSim = qSim1;
//        AgentFactory agentFactory = new TransitAgentFactory(qSim);
//        TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
//        transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
//        qSim.addDepartureHandler(transitEngine);
//        qSim.addAgentSource(transitEngine);
//
//
//        PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
//        qSim.addAgentSource(agentSource);
//
//        transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
////		this.events.addHandler(new LogOutputEventHandler());
//
//		
//
//		if(this.useHeadwayControler){
//			transitEngine.setAbstractTransitDriverFactory(new FixedHeadwayCycleUmlaufDriverFactory(sc, eventsManager, transitEngine.getInternalInterface(), transitEngine.getAgentTracker()));
//			this.getEvents().addHandler(new FixedHeadwayControler(qSim, transitEngine));
//		}
//
//        for (MobsimListener l : this.getMobsimListeners()) {
//            qSim.addQueueSimulationListeners(l);
//        }
//        if (this.useOTFVis) {
//			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(getConfig(),getScenario(), getEvents(), qSim);
//			OTFClientLive.run(getConfig(), server);
//		}
//		qSim.run();
//	}	
	
	void setUseOTFVis(boolean useOTFVis) {
		this.useOTFVis = useOTFVis;
	}

	public static void main(final String[] args) {
		Config config = new Config();
		config.addCoreModules();
		new ConfigReader(config).readFile(args[0]);
		config.transit().setUseTransit(true);
				
		TransitControler tc = new TransitControler(config);
		if(args.length > 1 && args[1].equalsIgnoreCase("true")){
			tc.setUseOTFVis(true);
		}
		tc.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		//		tc.setCreateGraphs(false);
		tc.run();
	}
}
