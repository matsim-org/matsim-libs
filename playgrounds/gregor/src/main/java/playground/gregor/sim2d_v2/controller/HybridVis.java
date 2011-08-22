package playground.gregor.sim2d_v2.controller;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.mobsim.SignalEngine;

import playground.gregor.pedvis.OTFVisMobsimFeature;
import playground.gregor.pedvis.PedVisPeekABot;
import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.scenario.ScenarioLoader2DImpl;
import playground.gregor.sim2d_v2.simulation.HybridQ2DMobsimFactory;

public class HybridVis {

	private static final Logger log = Logger.getLogger(HybridVis.class);

	public static void main (String [] args) {
		Config config = ConfigUtils.loadConfig(args[0]);


		Module module = config.getModule("sim2d");
		Sim2DConfigGroup s = null;
		if (module == null) {
			s = new Sim2DConfigGroup();
		} else {
			s = new Sim2DConfigGroup(module);
		}
		config.getModules().put("sim2d", s);

		MatsimRandom.reset(config.global().getRandomSeed());
		Scenario scenario = ScenarioUtils.createScenario(config);

		((NetworkImpl)scenario.getNetwork()).getFactory().setRouteFactory("walk2d", new LinkNetworkRouteFactory());
		ScenarioUtils.loadScenario(scenario);

		ScenarioLoader2DImpl loader = new ScenarioLoader2DImpl(scenario);
		loader.load2DScenario();

		log.info("Complete config dump:");
		StringWriter writer = new StringWriter();
		new ConfigWriter(config).writeStream(new PrintWriter(writer));
		log.info("\n\n" + writer.getBuffer().toString());
		log.info("Complete config dump done.");
		if (config.getQSimConfigGroup() == null){
			log.error("Cannot play live config without config module for QSim (in Java QSimConfigGroup). " +
					"Fixing this by adding default config module for QSim. " +
					"Please check if default values fit your needs, otherwise correct them in " +
			"the config given as parameter to get a valid visualization!");
			config.addQSimConfigGroup(new QSimConfigGroup());
		}

		//		ScenarioLoader2DImpl loader2 = new ScenarioLoader2DImpl(this.s/cenarioData);
		//		loader2.loadScenario();


		EventsManager events = EventsUtils.createEventsManager();

		//		PedVisPeekABot vis = new PedVisPeekABot(5,scenario);
		//		vis.setOffsets(386128,5820182);
		//		vis.setFloorShapeFile(s.getFloorShapeFile());
		//		vis.drawNetwork(scenario.getNetwork());
		//		events.addHandler(vis);

		ControlerIO controlerIO = new ControlerIO(config.controler().getOutputDirectory());
		QSim qSim = (QSim) new HybridQ2DMobsimFactory().createMobsim(scenario, events);
		if (scenario.getConfig().scenario().isUseSignalSystems()){
			SignalEngine engine = new QSimSignalEngine(new FromDataBuilder(scenario.getScenarioElement(SignalsData.class), events).createAndInitializeSignalSystemsManager());
			qSim.addQueueSimulationListeners(engine);
		}
		OTFVisMobsimFeature queueSimulationFeature = new OTFVisMobsimFeature(qSim);
		qSim.addQueueSimulationListeners(queueSimulationFeature);
		qSim.getEventsManager().addHandler(queueSimulationFeature) ;
		//		queueSimulationFeature.setVisualizeTeleportedAgents(scenario.getConfig().otfVis().isShowTeleportedAgents());
		qSim.run();
	}
}
