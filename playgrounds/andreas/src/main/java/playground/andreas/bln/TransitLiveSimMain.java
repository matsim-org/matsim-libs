package playground.andreas.bln;

import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.ReconstructingUmlaufBuilder;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

public class TransitLiveSimMain {

	public static void main(String[] args) {

		String inputDir = "e:/_out/otflivetest/";

		String configFilename = inputDir + "config.xml";
		Config config = new Config();
		config.addCoreModules();
		new ConfigReader(config).readFile(configFilename);
		config.transit().setUseTransit(true);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);

		new TransitScheduleReaderV1(scenario.getTransitSchedule(), scenario.getNetwork()).readFile(inputDir + "transitSchedule.xml");
		new VehicleReaderV1(scenario.getTransitVehicles()).parse(inputDir + "vehicles.xml");

		ReconstructingUmlaufBuilder reconstructingUmlaufBuilder = new ReconstructingUmlaufBuilder(
				scenario.getNetwork(),
				scenario.getTransitSchedule().getTransitLines().values(),
				scenario.getTransitVehicles(),
				scenario.getConfig().planCalcScore());
		reconstructingUmlaufBuilder.build();

		EventsManager events = EventsUtils.createEventsManager();
		QSim qSim1 = new QSim(scenario, events);
		ActivityEngine activityEngine = new ActivityEngine(events, qSim1.getAgentCounter());
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);
        QNetsimEngineModule.configure(qSim1);
		TeleportationEngine teleportationEngine = new TeleportationEngine(scenario, events);
		qSim1.addMobsimEngine(teleportationEngine);
        QSim qSim = qSim1;
        AgentFactory agentFactory;
            agentFactory = new TransitAgentFactory(qSim);
            TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
            transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
            qSim.addDepartureHandler(transitEngine);
            qSim.addAgentSource(transitEngine);
            qSim.addMobsimEngine(transitEngine);
        PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qSim);
        qSim.addAgentSource(agentSource);
        QSim sim = qSim;
		
		
		
		
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(config,scenario, events, sim);
		OTFClientLive.run(config, server);


//		if(useHeadwayControler){
//			sim.getQSimTransitEngine().setAbstractTransitDriverFactory(new FixedHeadwayCycleUmlaufDriverFactory());
//			this.events.addHandler(new FixedHeadwayControler(sim));
//		}
//		this.events.addHandler(new LogOutputEventHandler());


		sim.run();

	}

}
