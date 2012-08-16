/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.droeder.southAfrica.testScenario;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.PtConstants;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFFileWriterFactory;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.helper.PScenarioImpl;
import playground.andreas.P2.hook.PQSimFactory;
import playground.andreas.P2.schedule.PTransitRouterImplFactory;
import playground.droeder.southAfrica.FixedPtSubModeControler;

/**
 * @author droeder
 *
 */
public class RunRsaMultiModalTest {
	
	
	private final static Logger log = Logger.getLogger(RunRsaMultiModalTest.class);

	public static void main(final String[] args) {
		
		if(args.length == 0){
			log.info("Arg 1: config.xml");
			System.exit(1);
		}
		sim(args[0]);
		
//		try {
//			playOutputConfig(args[0]);
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	
	private static void sim(String conf) {
		Config config = new Config();
		config.addModule(PConfigGroup.GROUP_NAME, new PConfigGroup());
		ConfigUtils.loadConfig(config, conf);
		
		PScenarioImpl scenario = new PScenarioImpl(config);
		ScenarioUtils.loadScenario(scenario);
		
//		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(conf));
	
		Controler controler = new FixedPtSubModeControler(scenario);
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(true);
		
		// manipulate config
		// add "pt interaction" cause controler.init() is called too late and in a protected way
		ActivityParams transitActivityParams = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		transitActivityParams.setTypicalDuration(120.0);
		scenario.getConfig().planCalcScore().addActivityParams(transitActivityParams);
		
		PTransitRouterImplFactory pFact = new PTransitRouterImplFactory(controler);
		controler.addControlerListener(pFact);		
		controler.setTransitRouterFactory(pFact);
		controler.setMobsimFactory(new PQSimFactory());
		controler.addControlerListener(new WriteSelectedPlansAfterIteration());
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());

		controler.run();
	}
	
	
	private static void playOutputConfig(String configfile) throws FileNotFoundException, IOException {
		log.info("using " + configfile + " ...");
//		String currentDirectory = configfile.substring(0, configfile.lastIndexOf("/") + 1);
		String currentDirectory = null;
		if (currentDirectory == null) {
			currentDirectory = configfile.substring(0, configfile.lastIndexOf("\\") + 1);
		}
		log.info("using " + currentDirectory + " as base directory...");
		String newConfigFile = currentDirectory + "lastItLiveConfig.xml";
		handleNoLongerSupportedParameters(configfile, newConfigFile);
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(newConfigFile);
		OutputDirectoryHierarchy oldConfControlerIO;
		if (config.controler().getRunId() != null) {
			oldConfControlerIO = new OutputDirectoryHierarchy(currentDirectory + config.controler().getRunId(), config.controler().getRunId(), true);
		}
		else {
			oldConfControlerIO = new OutputDirectoryHierarchy(currentDirectory + config.controler().getRunId(), true);
		}
		config.network().setInputFile(oldConfControlerIO.getOutputFilename(Controler.FILENAME_NETWORK));
		config.plans().setInputFile(oldConfControlerIO.getOutputFilename(Controler.FILENAME_POPULATION));
		config.transit().setTransitScheduleFile(oldConfControlerIO.getIterationFilename(config.controler().getLastIteration() - 1, "schedule.xml.gz"));
		config.transit().setVehiclesFile(oldConfControlerIO.getIterationFilename(config.controler().getLastIteration() - 1, "vehicles.xml.gz"));
		
		log.info("Complete config dump:");
		StringWriter writer = new StringWriter();
		new ConfigWriter(config).writeStream(new PrintWriter(writer));
		log.info("\n\n" + writer.getBuffer().toString());
		log.info("Complete config dump done.");

		if (config.getQSimConfigGroup() == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}
		// disable snapshot writing as the snapshot should not be overwritten
		config.getQSimConfigGroup().setSnapshotPeriod(0.0);

		Scenario sc = ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
		QSim qSim1 = (QSim) new QSimFactory().createMobsim(sc, events);
		
		ActivityEngine activityEngine = new ActivityEngine();
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);
		QNetsimEngine netsimEngine = new DefaultQSimEngineFactory().createQSimEngine(qSim1, MatsimRandom.getRandom());
		qSim1.addMobsimEngine(netsimEngine);
		qSim1.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim1.addMobsimEngine(teleportationEngine);
		QSim qSim = qSim1;
		AgentFactory agentFactory;
		agentFactory = new TransitAgentFactory(qSim);
		TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
		transitEngine.setUseUmlaeufe(true);
		transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
		qSim.addDepartureHandler(transitEngine);
		qSim.addAgentSource(transitEngine);
		qSim.addMobsimEngine(transitEngine);
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);
		QSim sim = qSim;
		transitEngine.setUseUmlaeufe(true);
		
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(config, sc, events, sim);
		OTFClientLive.run(config, server);
		sim.run();
	}

	private static void handleNoLongerSupportedParameters(String configfile, String liveConfFile)
			throws FileNotFoundException, IOException {
		BufferedReader reader = IOUtils.getBufferedReader(configfile);
		String line = reader.readLine();
		BufferedWriter writer = IOUtils.getBufferedWriter(liveConfFile);
		while (line != null) {
			if (line.contains("bikeSpeedFactor")) {
				line = line.replaceAll("bikeSpeedFactor", "bikeSpeed");
			}
			else if (line.contains("undefinedModeSpeedFactor")) {
				line = line.replaceAll("undefinedModeSpeedFactor", "undefinedModeSpeed");
			}
			else if (line.contains("walkSpeedFactor")) {
				line = line.replaceAll("walkSpeedFactor", "walkSpeed");
			}
			else if (line.contains("ptScaleFactor") ||
					line.contains("localDTDBase") ||
					line.contains("outputSample") ||
					line.contains("outputVersion") ||
					line.contains("evacuationTime") ||
					line.contains("snapshotfile")
				){
				line = reader.readLine();
				continue;
			}
			writer.write(line);
			line = reader.readLine();
		}
		reader.close();
		writer.close();
	}
}
