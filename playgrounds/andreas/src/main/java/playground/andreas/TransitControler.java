package playground.andreas;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;
import playground.andreas.fixedHeadway.FixedHeadwayControler;
import playground.andreas.fixedHeadway.FixedHeadwayCycleUmlaufDriverFactory;

/**
 * @author aneumann
 */
public class TransitControler extends Controler {

	private final static Logger log = Logger.getLogger(TransitControler.class);

	private boolean useOTFVis = true;
	private boolean useHeadwayControler = false;
	
	public TransitControler(Config config) {
		super(config);
	}
	
	@Override
	protected void runMobSim() {
		
		log.info("Overriding runMobSim()");

        Scenario sc = this.getScenario();EventsManager eventsManager = this.getEvents();

        QSimConfigGroup conf = sc.getConfig().qsim();
        if (conf == null) {
            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
        }

		QSim qSim1 = new QSim(sc, eventsManager);
		ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim1.getAgentCounter());
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);
        QNetsimEngineModule.configure(qSim1);
		TeleportationEngine teleportationEngine = new TeleportationEngine(sc, eventsManager);
		qSim1.addMobsimEngine(teleportationEngine);
        QSim qSim = qSim1;
        AgentFactory agentFactory = new TransitAgentFactory(qSim);
        TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
        transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
        qSim.addDepartureHandler(transitEngine);
        qSim.addAgentSource(transitEngine);


        PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
        qSim.addAgentSource(agentSource);

        transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
//		this.events.addHandler(new LogOutputEventHandler());

		

		if(this.useHeadwayControler){
			transitEngine.setAbstractTransitDriverFactory(new FixedHeadwayCycleUmlaufDriverFactory(sc, eventsManager, transitEngine.getInternalInterface(), transitEngine.getAgentTracker()));
			this.getEvents().addHandler(new FixedHeadwayControler(qSim, transitEngine));
		}

        for (MobsimListener l : this.getMobsimListeners()) {
            qSim.addQueueSimulationListeners(l);
        }
        if (this.useOTFVis) {
			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(getConfig(),getScenario(), getEvents(), qSim);
			OTFClientLive.run(getConfig(), server);
		}
		qSim.run();
	}	
	
	void setUseOTFVis(boolean useOTFVis) {
		this.useOTFVis = useOTFVis;
	}

	public static void main(final String[] args) {
		Config config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).readFile(args[0]);
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		
				
		TransitControler tc = new TransitControler(config);
		if(args.length > 1 && args[1].equalsIgnoreCase("true")){
			tc.setUseOTFVis(true);
		}
		tc.setOverwriteFiles(true);
//		tc.setCreateGraphs(false);
		tc.run();
	}
}
