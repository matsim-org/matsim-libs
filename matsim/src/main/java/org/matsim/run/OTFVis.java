/* *********************************************************************** *
 * project: org.matsim.*
 * OTFVis.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008, 2009 by the members listed in the COPYING,  *
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

package org.matsim.run;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.mobsim.queuesim.QueueSimulationFactory;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.MatsimFileTypeGuesser;
import org.matsim.core.utils.io.MatsimFileTypeGuesser.FileType;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QSimFactory;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.vis.otfvis.OTFClientFile;
import org.matsim.vis.otfvis.OTFClientSwing;
import org.matsim.vis.otfvis.OTFEvent2MVI;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;
import org.matsim.vis.otfvis.gui.OTFHostConnectionManager;
import org.matsim.vis.otfvis2.OTFVisClient;
import org.matsim.vis.otfvis2.OTFVisLiveServer;

/**
 * A generic starter for the OnTheFly Visualizer that supports
 * MATSim Visualizer Files (MVI) and MATSim Config Files for the live simulation and visualization.
 *
 * @author mrieser
 */
public class OTFVis {
	private static final Logger log = Logger.getLogger(OTFVis.class);

	private static void printUsage() {
		System.out.println();
		System.out.println("OTFVis");
		System.out.println("Starts the MATSim OnTheFly-Visualizer.");
		System.out.println();
		System.out.println("usage 1: OTFVis [-swing] mvi-file");
		System.out.println("usage 2: OTFVis mvi-file1 mvi-file2");
		System.out.println("usage 3: OTFVis config-file");
		System.out.println("usage 4: OTFVis [-swing] network-file");
		System.out.println("usage 5: OTFVis -convert event-file network-file mvi-file [snapshot-period]");
		System.out.println();
		System.out.println("Usages 1-4: Starts the Visualizer");
		System.out.println("mvi-file:      A MATSim visualizer file that contains a pre-recorder state");
		System.out.println("               to be visualized (*.mvi).");
		System.out.println("mvi-file1,2:   Loads two mvi-files in parallel and shows them next to each");
		System.out.println("               other. Good way to compare results from similar scenarios.");
		System.out.println("network-file:  A MATSim network file (*.xml).");
		System.out.println("config-file:   A complete MATSim config file to run a simulation. In that case,");
		System.out.println("               a QueueSimulation will be started and visualized in real-time, ");
		System.out.println("               allowing to interactively query the state of single agents");
		System.out.println("-swing         Use an alternative GUI built with Swing. This will be slower,");
		System.out.println("               not support all featuers, and not be able to visualize large");
		System.out.println("               scenarios.");
		System.out.println();
		System.out.println("Usage 6: Convert events into a mvi-file");
		System.out.println("snapshot-period:  Optional. Specify how often a snapshot should be taken when");
		System.out.println("                  reading the events, in seconds. Default: 600 seconds");
		System.out.println();
		System.out.println("---------------------");
		System.out.println("2008-2010, matsim.org");
		System.out.println();
	}

	public static void main(final String[] args) {
		boolean useSwing = false;
		String [] args2 = args;

		if (args2.length != 0 && "-swing".equalsIgnoreCase(args2[0])) {
			useSwing = true;
			// pop that argument from the list
			String[] tmp = new String[args2.length - 1];
			System.arraycopy(args, 1, tmp, 0, tmp.length);
			args2 = tmp;
		}

		if (args2.length == 0) {
			String filename = chooseFile();
			play(filename, useSwing);
		} else if (args2[0].equalsIgnoreCase("-convert")) {
			convert(args2);
		} else if (args2.length == 1) {
			String filename = args2[0];
			play(filename, useSwing);
		} else {
			printUsage();
		}

	}

	private static final void play(String filename, boolean useSwing) {
		String lowerCaseFilename = filename.toLowerCase();
		if (lowerCaseFilename.endsWith(".mvi")) {
			if (useSwing) {
				playMVI_Swing(filename);
			} else {
				playMVI(filename);
			}
		} else if ((lowerCaseFilename.endsWith(".xml") || lowerCaseFilename.endsWith(".xml.gz"))) {
			FileType type;
			type = new MatsimFileTypeGuesser(filename).getGuessedFileType();
			if (FileType.Config.equals(type)) {
				if (useSwing) {
					playConfig_Swing(filename);
				} else {
					playConfig(filename);
				}
			} else if (FileType.Network.equals(type)) {
				if (useSwing) {
					playNetwork_Swing(filename);
				} else {
					playNetwork(filename);
				}
			} else {
				printUsage();
			}
		}
	}

	private static final String chooseFile() {
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter( new FileFilter() {
			@Override public boolean accept( File f ) {
				return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".xml" );
			}
			@Override public String getDescription() { return "MATSim net or config file (*.xml)"; }
		} );

		fc.setFileFilter( new FileFilter() {
			@Override public boolean accept( File f ) {
				return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith( ".mvi" );
			}
			@Override public String getDescription() { return "OTFVis movie file (*.mvi)"; }
		} );

		int state = fc.showOpenDialog( null );
		if ( state == JFileChooser.APPROVE_OPTION ) {
			String filename = fc.getSelectedFile().getAbsolutePath();
			return filename;
		}
		System.out.println( "No file selected." );
		return null;
	}

	public static final void playMVI(final String[] args) {
		playMVI(args[0]);
	}

	public static final void playMVI(String file) {
		new OTFClientFile(file).run();
	}

	public static final void playMVI_Swing(String file) {
		new OTFClientSwing(file).run();
	}

	public static final void playConfig(final String configFilename){
		playConfig(new String[]{configFilename});
	}

	public static final void playConfig_Swing(String configFileName) {
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFileName));
		EventsManager events = EventsUtils.createEventsManager();
		OTFVisLiveServer server = new OTFVisLiveServer(scenario, events);
		QueueSimulation queueSimulation = (QueueSimulation) new QueueSimulationFactory().createMobsim(scenario, events);
		queueSimulation.addSnapshotWriter(server.getSnapshotReceiver());
		OTFHostConnectionManager hostConnectionManager = new OTFHostConnectionManager(configFileName, server);
		OTFVisClient client = new OTFVisClient();
		client.setHostConnectionManager(hostConnectionManager);
		client.setSwing(true);
		client.run();
		queueSimulation.run();
	}

	public static final void playConfig(final String[] args) {
		Scenario scenario1;
		Config config = ConfigUtils.loadConfig(args[0]);
		MatsimRandom.reset(config.global().getRandomSeed());
		scenario1 = ScenarioUtils.createScenario(config);
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario1);
		log.info("Complete config dump:");
		StringWriter writer = new StringWriter();
		new ConfigWriter(loader.getScenario().getConfig()).writeStream(new PrintWriter(writer));
		log.info("\n\n" + writer.getBuffer().toString());
		log.info("Complete config dump done.");
		if (loader.getScenario().getConfig().getQSimConfigGroup() == null){
			log.error("Cannot play live config without config module for QSim (in Java QSimConfigGroup). " +
					"Fixing this by adding default config module for QSim. " +
					"Please check if default values fit your needs, otherwise correct them in " +
			"the config given as parameter to get a valid visualization!");
			loader.getScenario().getConfig().addQSimConfigGroup(new QSimConfigGroup());
		}
		loader.loadScenario();
		Scenario scenario = loader.getScenario();
		EventsManager events = EventsUtils.createEventsManager();
		ControlerIO controlerIO = new ControlerIO(scenario.getConfig().controler().getOutputDirectory());
		QSim qSim = (QSim) new QSimFactory().createMobsim(scenario, events);
		if (scenario.getConfig().scenario().isUseSignalSystems()){
			SignalEngine engine = new QSimSignalEngine(new FromDataBuilder(scenario.getScenarioElement(SignalsData.class), events).createAndInitializeSignalSystemsManager());
			qSim.addQueueSimulationListeners(engine);
		}
		OTFVisMobsimFeature queueSimulationFeature = new OTFVisMobsimFeature(qSim);
		qSim.addFeature(queueSimulationFeature);
		queueSimulationFeature.setVisualizeTeleportedAgents(scenario.getConfig().otfVis().isShowTeleportedAgents());
		qSim.setControlerIO(controlerIO);
		qSim.setIterationNumber(scenario.getConfig().controler().getLastIteration());
		qSim.run();
	}

	public static final void playNetwork(final String filename) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(filename);
		EventsManager events = EventsUtils.createEventsManager();
		OTFVisLiveServer server = new OTFVisLiveServer(scenario, events);
		OTFHostConnectionManager hostConnectionManager = new OTFHostConnectionManager(filename, server);
		OTFVisClient client = new OTFVisClient();
		client.setHostConnectionManager(hostConnectionManager);
		client.setSwing(false);
		client.run();
		server.getSnapshotReceiver().finish();
	}

	public static final void playNetwork_Swing(final String filename) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(filename);
		EventsManager events = EventsUtils.createEventsManager();
		OTFVisLiveServer server = new OTFVisLiveServer(scenario, events);
		OTFHostConnectionManager hostConnectionManager = new OTFHostConnectionManager(filename, server);
		OTFVisClient client = new OTFVisClient();
		client.setHostConnectionManager(hostConnectionManager);
		client.setSwing(true);
		client.run();
		server.getSnapshotReceiver().finish();
	}

	public static final void convert(final String[] args) {
		if ((args.length < 4) || (args.length > 5)) {
			printUsage();
			return;
		}
		String eventFile = args[1];
		String networkFile = args[2];
		String mviFile = args[3];
		int snapshotPeriod = 600;
		if (args.length == 5) {
			snapshotPeriod = Integer.parseInt(args[4]);
		}
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFile);
		OTFEvent2MVI.convert(new QSimConfigGroup(), scenario.getNetwork(), eventFile, mviFile, snapshotPeriod);
	}

}
