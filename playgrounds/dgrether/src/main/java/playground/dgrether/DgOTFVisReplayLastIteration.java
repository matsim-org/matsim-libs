package playground.dgrether;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.run.OTFVis;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioWriter;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.dgrether.utils.DgConfigCleaner;

/* *********************************************************************** *
 * project: org.matsim.*
 * DgOTFVisReplayLastIteration
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 * @author dgrether
 * 
 */
public class DgOTFVisReplayLastIteration {

	private static final Logger log = Logger.getLogger(DgOTFVisReplayLastIteration.class);

	private void playOutputConfig(String configfile) throws FileNotFoundException, IOException {
		String currentDirectory = configfile.substring(0, configfile.lastIndexOf("/") + 1);
		if (currentDirectory == null) {
			currentDirectory = configfile.substring(0, configfile.lastIndexOf("\\") + 1);
		}
		log.info("using " + currentDirectory + " as base directory...");
		String newConfigFile = currentDirectory + "lastItLiveConfig.xml";
		new DgConfigCleaner().cleanAndWriteConfig(configfile, newConfigFile);
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(newConfigFile);
		ControlerIO oldConfControlerIO;
		if (config.controler().getRunId() != null) {
			oldConfControlerIO = new ControlerIO(currentDirectory, new IdImpl(config.controler()
					.getRunId()));
		}
		else {
			oldConfControlerIO = new ControlerIO(currentDirectory);
		}
		config.network().setInputFile(oldConfControlerIO.getOutputFilename(Controler.FILENAME_NETWORK));
		config.plans()
				.setInputFile(oldConfControlerIO.getOutputFilename(Controler.FILENAME_POPULATION));
		if (config.scenario().isUseLanes()) {
			config.network().setLaneDefinitionsFile(
					oldConfControlerIO.getOutputFilename(Controler.FILENAME_LANES));
		}
		if (config.scenario().isUseSignalSystems()) {
			config.signalSystems().setSignalSystemFile(
					oldConfControlerIO.getOutputFilename(SignalsScenarioWriter.FILENAME_SIGNAL_SYSTEMS));
			config.signalSystems().setSignalGroupsFile(
					oldConfControlerIO.getOutputFilename(SignalsScenarioWriter.FILENAME_SIGNAL_GROUPS));
			config.signalSystems().setSignalControlFile(
					oldConfControlerIO.getOutputFilename(SignalsScenarioWriter.FILENAME_SIGNAL_CONTROL));
			config.signalSystems().setAmberTimesFile(
					oldConfControlerIO.getOutputFilename(SignalsScenarioWriter.FILENAME_AMBER_TIMES));
		}

		log.info("Complete config dump:");
		StringWriter writer = new StringWriter();
		new ConfigWriter(config).writeStream(new PrintWriter(writer));
		log.info("\n\n" + writer.getBuffer().toString());
		log.info("Complete config dump done.");

		if (config.getQSimConfigGroup() == null) {
			config.addQSimConfigGroup(new QSimConfigGroup());
			config.getQSimConfigGroup().setFlowCapFactor(config.simulation().getFlowCapFactor());
			config.getQSimConfigGroup().setStorageCapFactor(config.simulation().getStorageCapFactor());
			config.getQSimConfigGroup().setRemoveStuckVehicles(
					config.simulation().isRemoveStuckVehicles());
			config.getQSimConfigGroup().setStuckTime(config.simulation().getStuckTime());
			config.getQSimConfigGroup().setSnapshotStyle(config.simulation().getSnapshotStyle());
		}
		// disable snapshot writing as the snapshot should not be overwritten
		config.getQSimConfigGroup().setSnapshotPeriod(0.0);

		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(config);
		Scenario sc = loader.loadScenario();
		EventsManager events = EventsUtils.createEventsManager();
		ControlerIO controlerIO = new ControlerIO(sc.getConfig().controler().getOutputDirectory());
		QSim otfVisQSim = new QSim(sc, events);
		if (sc.getConfig().scenario().isUseSignalSystems()) {
			SignalEngine engine = new QSimSignalEngine(
					new FromDataBuilder(sc.getScenarioElement(SignalsData.class), events)
							.createAndInitializeSignalSystemsManager());
			otfVisQSim.addQueueSimulationListeners(engine);
		}

		QSim queueSimulation = otfVisQSim;
		queueSimulation.setControlerIO(controlerIO);
		queueSimulation.setIterationNumber(sc.getConfig().controler().getLastIteration());
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, events, queueSimulation);
		OTFClientLive.run(sc.getConfig(), server);
		queueSimulation.run();
	}

	public static final String chooseFile() {
		JFileChooser fc = new JFileChooser();

		fc.setFileFilter(new FileFilter() {

			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml");
			}

			@Override
			public String getDescription() {
				return "MATSim config file (*.xml)";
			}
		});

		fc.setFileFilter(new FileFilter() {

			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml.gz");
			}

			@Override
			public String getDescription() {
				return "MATSim zipped config file (*.xml.gz)";
			}
		});

		int state = fc.showOpenDialog(null);
		if (state == JFileChooser.APPROVE_OPTION) {
			String args_new = fc.getSelectedFile().getAbsolutePath();
			return args_new;
		}
		System.out.println("No file selected.");
		return null;
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		// args = new String[1];
		// args[0] = "/home/dgrether/data/work/matsimOutput/equil/output_config.xml.gz";
		// args[0] = "/home/dgrether/runs-svn/run749/749.output_config.xml.gz";
		String configfile = null;
		if (args.length == 0) {
			configfile = chooseFile();
		}
		else if (args.length == 1) {
			configfile = args[0];
		}
		else {
			log.error("not the correct arguments");
		}
		if (configfile != null) {
			new DgOTFVisReplayLastIteration().playOutputConfig(configfile);
		}
	}

}
