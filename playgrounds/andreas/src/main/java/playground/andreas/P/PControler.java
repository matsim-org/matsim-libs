package playground.andreas.P;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.ObservableMobsim;
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
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;
import playground.andreas.P.init.CreateInitialTimeSchedule;
import playground.andreas.P.init.PConfigGroup;
import playground.andreas.P.replan.ReplanTimeSchedule;

import java.io.File;

/**
 * @author aneumann
 */
@Deprecated
public class PControler extends Controler {

	private final static Logger log = Logger.getLogger(PControler.class);

	private boolean useOTFVis = false;
	
	public PControler(ScenarioImpl scenario) {
		super(scenario);
		// TODO Auto-generated constructor stub
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
		ActivityEngine activityEngine = new ActivityEngine();
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);
        QNetsimEngineModule.configure(qSim1);
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim1.addMobsimEngine(teleportationEngine);
        QSim qSim = qSim1;
        AgentFactory agentFactory;
            agentFactory = new TransitAgentFactory(qSim);
                TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
                transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
                qSim.addDepartureHandler(transitEngine);
                qSim.addAgentSource(transitEngine);

        PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
        qSim.addAgentSource(agentSource);

        QSim simulation = (QSim) qSim;

		transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
//		this.events.addHandler(new LogOutputEventHandler());


		if (simulation instanceof ObservableMobsim){
			for (MobsimListener l : this.getMobsimListeners()) {
				((ObservableMobsim)simulation).addQueueSimulationListeners(l);
			}
		}
		if (this.useOTFVis) {
			OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(getConfig(),getScenario(), events, simulation);
			OTFClientLive.run(getConfig(), server);
		}
		simulation.run();
	}	
	
	void setUseOTFVis(boolean useOTFVis) {
		this.useOTFVis = useOTFVis;
	}

	public static void main(final String[] args) {
		
		String configFile = args[0];
		Config config;
		
		for (int i = 0; i < Integer.parseInt(args[1]); i++) {
			
			// reading the config file:
			config = ConfigUtils.loadConfig(configFile);
			
			String currentOutputBase = config.getParam("controler", "outputDirectory") + "it." + i + "/";
			String nextOutputBase = config.getParam("controler", "outputDirectory") + "it." + (i+1) + "/";
			
			PConfigGroup pConfig = new PConfigGroup(config);
			pConfig.setCurrentOutPutBase(currentOutputBase);
			pConfig.setNextOutPutBase(nextOutputBase);				
			
			String transitScheduleOutFile = pConfig.getCurrentOutputBase() + "transitSchedule.xml";
			config.setParam("transit", "transitScheduleFile", transitScheduleOutFile);				
			config.setParam("controler", "outputDirectory", currentOutputBase);
			String vehiclesOutFile  = pConfig.getCurrentOutputBase() + "transitVehicles.xml";
			config.setParam("transit", "vehiclesFile", vehiclesOutFile);
			
			if(i == 0) {
				File currentOutDir = new File(currentOutputBase);
				currentOutDir.mkdir();
				CreateInitialTimeSchedule.createInitialTimeSchedule(pConfig);
			}
			
			// reading the scenario (based on the config):
			ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.loadScenario(config);
			
			PControler tc = new PControler(sc);

//				if(args.length > 1 && args[1].equalsIgnoreCase("true")){
//					tc.setUseOTFVis(true);
//				}
			tc.setOverwriteFiles(true);
//				tc.setCreateGraphs(false);
			tc.run();
			
			File nextOutDir = new File(pConfig.getNextOutputBase());
			nextOutDir.mkdir();
			ReplanTimeSchedule replanTS = new ReplanTimeSchedule();
			replanTS.replan(pConfig, sc.getNetwork());
			
			new VehicleWriterV1(sc.getTransitVehicles()).writeFile(pConfig.getNextOutputBase() + "transitVehicles.xml");
		}
		
	}
}
