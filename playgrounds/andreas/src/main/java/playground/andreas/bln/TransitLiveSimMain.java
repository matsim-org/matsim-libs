package playground.andreas.bln;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.ReconstructingUmlaufBuilder;
import org.matsim.pt.qsim.ComplexTransitStopHandlerFactory;
import org.matsim.pt.qsim.TransitQSimEngine;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.AgentFactory;
import org.matsim.ptproject.qsim.agents.PopulationAgentSource;
import org.matsim.ptproject.qsim.agents.TransitAgentFactory;
import org.matsim.ptproject.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.run.OTFVis;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

public class TransitLiveSimMain {

	public static void main(String[] args) {

		String inputDir = "e:/_out/otflivetest/";

		String configFilename = inputDir + "config.xml";
		Config config = new Config();
		config.addCoreModules();
		new MatsimConfigReader(config).readFile(configFilename);
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		scenario = (ScenarioImpl) new ScenarioLoaderImpl(scenario).loadScenario();

		new TransitScheduleReaderV1(scenario.getTransitSchedule(), scenario.getNetwork(), scenario).readFile(inputDir + "transitSchedule.xml");
		new VehicleReaderV1(scenario.getVehicles()).parse(inputDir + "vehicles.xml");

		ReconstructingUmlaufBuilder reconstructingUmlaufBuilder = new ReconstructingUmlaufBuilder(
				scenario.getNetwork(),
				scenario.getTransitSchedule().getTransitLines().values(),
				scenario.getVehicles(),
				scenario.getConfig().planCalcScore());
		reconstructingUmlaufBuilder.build();

		EventsManager events = EventsUtils.createEventsManager();
        QSim qSim = new QSim(scenario, events, new DefaultQSimEngineFactory());
        AgentFactory agentFactory;
            agentFactory = new TransitAgentFactory(qSim);
            TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
            transitEngine.setUseUmlaeufe(true);
            transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
            qSim.addDepartureHandler(transitEngine);
            qSim.addAgentSource(transitEngine);
            qSim.addMobsimEngine(transitEngine);
        PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qSim);
        qSim.addAgentSource(agentSource);
        QSim sim = qSim;
		transitEngine.setUseUmlaeufe(true);
		
		
		
		
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
