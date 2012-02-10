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
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.MatsimFileTypeGuesser;
import org.matsim.core.utils.io.MatsimFileTypeGuesser.FileType;
import org.matsim.lanes.data.v20.LaneDefinitions;
import org.matsim.lanes.otfvis.io.OTFLaneWriter;
import org.matsim.pt.otfvis.FacilityDrawer;
import org.matsim.pt.qsim.TransitQSimEngine;
import org.matsim.pt.qsim.TransitStopAgentTracker;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QSimFactory;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.signalgroups.v20.SignalGroupsData;
import org.matsim.signalsystems.data.signalsystems.v20.SignalSystemsData;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.signalsystems.otfvis.io.OTFSignalWriter;
import org.matsim.signalsystems.otfvis.io.SignalGroupStateChangeTracker;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.OTFClientFile;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFEvent2MVI;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;
import org.matsim.vis.otfvis.OnTheFlyServer;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;

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
		System.out.println("usage 1: OTFVis mvi-file");
		System.out.println("usage 2: OTFVis mvi-file1 mvi-file2");
		System.out.println("usage 3: OTFVis config-file");
		System.out.println("usage 4: OTFVis network-file");
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
		String [] args2 = args;
		if (args2.length == 0) {
			String filename = chooseFile();
			play(filename);
		} else if (args2[0].equalsIgnoreCase("-convert")) {
			convert(args2);
		} else if (args2.length == 1) {
			String filename = args2[0];
			play(filename);
		} else {
			printUsage();
		}

	}

	private static final void play(String filename) {
		String lowerCaseFilename = filename.toLowerCase(Locale.ROOT);
		if (lowerCaseFilename.endsWith(".mvi")) {
			playMVI(filename);
		} else if ((lowerCaseFilename.endsWith(".xml") || lowerCaseFilename.endsWith(".xml.gz"))) {
			FileType type;
			type = new MatsimFileTypeGuesser(filename).getGuessedFileType();
			if (FileType.Config.equals(type)) {
				playConfig(filename);
			} else if (FileType.Network.equals(type)) {
				playNetwork(filename);
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

	public static final void playConfig(final String configFilename){
		playConfig(new String[]{configFilename});
	}

	public static final void playConfig(final String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);
		MatsimRandom.reset(config.global().getRandomSeed());
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
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
		QSim qSim = (QSim) new QSimFactory().createMobsim(scenario, events);
		if (scenario.getConfig().scenario().isUseSignalSystems()){
			SignalEngine engine = new QSimSignalEngine(new FromDataBuilder(scenario.getScenarioElement(SignalsData.class), events).createAndInitializeSignalSystemsManager());
			qSim.addQueueSimulationListeners(engine);
		}

		OnTheFlyServer server = startServerAndRegisterWithQSim(config,scenario, events, qSim);
		OTFClientLive.run(config, server);

		qSim.run();
	}

    public static OnTheFlyServer startServerAndRegisterWithQSim(Config config, Scenario scenario, EventsManager events, QSim qSim) {
		OnTheFlyServer server = OnTheFlyServer.createInstance(scenario, events);
		OTFVisMobsimFeature queueSimulationFeature = new OTFVisMobsimFeature(scenario, server, qSim);
		qSim.addQueueSimulationListeners(queueSimulationFeature);
		qSim.getEventsManager().addHandler(queueSimulationFeature) ;
		queueSimulationFeature.setVisualizeTeleportedAgents(config.otfVis().isShowTeleportedAgents());
		server.setSimulation(queueSimulationFeature);
		
		if (config.scenario().isUseTransit()) {
			
			Network network = scenario.getNetwork();
			TransitSchedule transitSchedule = ((ScenarioImpl) scenario).getTransitSchedule();
			TransitQSimEngine transitEngine = qSim.getTransitEngine();
			TransitStopAgentTracker agentTracker = transitEngine.getAgentTracker();
			AgentSnapshotInfoFactory snapshotInfoFactory = qSim.getVisNetwork().getAgentSnapshotInfoFactory();
			FacilityDrawer.Writer facilityWriter = new FacilityDrawer.Writer(network, transitSchedule, agentTracker, snapshotInfoFactory);
			server.addAdditionalElement(facilityWriter);
		}

		if (config.scenario().isUseLanes() && (!config.scenario().isUseSignalSystems())) {
			config.otfVis().setScaleQuadTreeRect(true);
			OTFLaneWriter otfLaneWriter = new OTFLaneWriter(qSim.getVisNetwork(), ((ScenarioImpl) scenario).getLaneDefinitions());
			server.addAdditionalElement(otfLaneWriter);
		} else if (config.scenario().isUseSignalSystems()) {
			config.otfVis().setScaleQuadTreeRect(true);
			SignalGroupStateChangeTracker signalTracker = new SignalGroupStateChangeTracker();
			events.addHandler(signalTracker);
			SignalsData signalsData = scenario.getScenarioElement(SignalsData.class);
			LaneDefinitions laneDefs = ((ScenarioImpl)scenario).getLaneDefinitions();
			SignalSystemsData systemsData = signalsData.getSignalSystemsData();
			SignalGroupsData groupsData = signalsData.getSignalGroupsData();
			OTFSignalWriter otfSignalWriter = new OTFSignalWriter(qSim.getVisNetwork(), laneDefs, systemsData, groupsData , signalTracker);
			server.addAdditionalElement(otfSignalWriter);
		}
		server.pause();
		return server;
	}

	public static final void playNetwork(final String filename) {
		Config config = ConfigUtils.createConfig();
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile(filename);
		EventsManager events = EventsUtils.createEventsManager();
		OnTheFlyServer server = OnTheFlyServer.createInstance(scenario, events);
		OTFClientLive.run(config, server);
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
		OTFClientControl.getInstance().setOTFVisConfig(scenario.getConfig().otfVis());
		new MatsimNetworkReader(scenario).readFile(networkFile);
		OTFEvent2MVI.convert(new QSimConfigGroup(), scenario.getNetwork(), eventFile, mviFile, snapshotPeriod);
	}

}
