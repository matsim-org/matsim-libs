package playground.benjamin.otfvis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.*;

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
public class BkVisLiveFromLastIteration {
	private static final Logger log = Logger.getLogger(BkVisLiveFromLastIteration.class);

//	private final static String runNumber = "981";
//	private final static String runDirectory = "../../runs-svn/run" + runNumber + "/";
//	private final static String configFile = runDirectory + runNumber + ".output_config.xml.gz";
	
	private final static String runDirectory = "../../runs-svn/detEval/latsis/output/output_baseCase_ctd_newCode/";
	private final static String configFile = runDirectory + "output_config.xml.gz";
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		args = new String[1];
		args[0] = configFile;
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
			new BkVisLiveFromLastIteration().playOutputConfig(configfile);
		}
	}

	private void playOutputConfig(String configfile) throws FileNotFoundException, IOException {
		String currentDirectory = configfile.substring(0, configfile.lastIndexOf("/") + 1);
		if (currentDirectory == null) {
			currentDirectory = configfile.substring(0, configfile.lastIndexOf("\\") + 1);
		}
		log.info("using " + currentDirectory + " as base directory...");
		String newConfigFile = currentDirectory + "lastItLiveConfig.xml";
		this.handleNoLongerSupportedParameters(configfile, newConfigFile);
		Config config = new Config();
		config.addCoreModules();
		ConfigReader configReader = new ConfigReader(config);
		configReader.readFile(newConfigFile);
		OutputDirectoryHierarchy oldConfControlerIO;
		if (config.controler().getRunId() != null) {
			oldConfControlerIO = new OutputDirectoryHierarchy(
					currentDirectory,
					config.controler().getRunId(),
							false ? OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles : OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		}
		else {
			oldConfControlerIO = new OutputDirectoryHierarchy(
					currentDirectory,
							true ? OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles : OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		}
		config.network().setInputFile(oldConfControlerIO.getOutputFilename(Controler.FILENAME_NETWORK));
		config.plans()
				.setInputFile(oldConfControlerIO.getOutputFilename(Controler.FILENAME_POPULATION));
		if ( config.network().getLaneDefinitionsFile()!=null || config.qsim().isUseLanes()) {
			config.network().setLaneDefinitionsFile(
					oldConfControlerIO.getOutputFilename(Controler.FILENAME_LANES));
		}

		log.info("Complete config dump:");
		StringWriter writer = new StringWriter();
		new ConfigWriter(config).writeStream(new PrintWriter(writer));
		log.info("\n\n" + writer.getBuffer().toString());
		log.info("Complete config dump done.");

//		if (config.qsim() == null) {
//			config.setQSimConfigGroup(new QSimConfigGroup());
//			config.qsim().setFlowCapFactor(((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).getFlowCapFactor());
//			config.qsim().setStorageCapFactor(((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).getStorageCapFactor());
//			config.qsim().setRemoveStuckVehicles(
//					((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).isRemoveStuckVehicles());
//			config.qsim().setStuckTime(((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).getStuckTime());
//			config.qsim().setSnapshotStyle(((SimulationConfigGroup) config.getModule(SimulationConfigGroup.GROUP_NAME)).getSnapshotStyle());
//		}
		// disable snapshot writing as the snapshot should not be overwritten
		config.qsim().setSnapshotPeriod(0.0);

		Scenario sc = ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
		OutputDirectoryHierarchy controlerIO = new OutputDirectoryHierarchy(
				sc.getConfig().controler().getOutputDirectory(),
						true ? OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles : OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		QSim otfVisQSim = QSimUtils.createDefaultQSim(sc, events);
		
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(config, sc, events, otfVisQSim);
		OTFClientLive.run(config, server);
		otfVisQSim.run();
	}

	private void handleNoLongerSupportedParameters(String configfile, String liveConfFile)
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
}
