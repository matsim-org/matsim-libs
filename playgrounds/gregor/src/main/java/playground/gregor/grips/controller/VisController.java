package playground.gregor.grips.controller;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QSimFactory;
import org.matsim.run.OTFVis;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.mzilske.osm.JXMapOTFVisClient;

public class VisController {

	private static final Logger log = Logger.getLogger(VisController.class);

	public static void main (String [] args) {
		Config config = ConfigUtils.loadConfig(args[0]);

		MatsimRandom.reset(config.global().getRandomSeed());
		Scenario scenario = ScenarioUtils.createScenario(config);

		ScenarioUtils.loadScenario(scenario);

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


		EventsManager events = EventsUtils.createEventsManager();


		ControlerIO controlerIO = new ControlerIO(config.controler().getOutputDirectory());
		QSim qSim = (QSim) new QSimFactory().createMobsim(scenario, events);
		if (scenario.getConfig().scenario().isUseSignalSystems()){
			SignalEngine engine = new QSimSignalEngine(new FromDataBuilder(scenario.getScenarioElement(SignalsData.class), events).createAndInitializeSignalSystemsManager());
			qSim.addQueueSimulationListeners(engine);
		}


		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, qSim);
		//		WMSService wms = new WMSService("http://localhost:8080/geoserver/wms?service=WMS&","hh");
		//
		//		JXMapOTFVisClient.run(scenario.getConfig(), server, wms);
		JXMapOTFVisClient.run(scenario.getConfig(), server);

		qSim.run();
	}
}
