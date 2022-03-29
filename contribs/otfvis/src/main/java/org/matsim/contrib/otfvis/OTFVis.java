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

package org.matsim.contrib.otfvis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AgentTracker;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.MatsimFileTypeGuesser;
import org.matsim.core.utils.io.MatsimFileTypeGuesser.FileType;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vis.otfvis.OTFClientFile;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFEvent2MVI;
import org.matsim.vis.otfvis.OnTheFlyServer;
import org.matsim.vis.otfvis.OnTheFlyServer.NonPlanAgentQueryHelper;
import org.matsim.vis.otfvis.handler.FacilityDrawer;
import org.matsim.vis.snapshotwriters.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

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
		System.out.println("usage 2: OTFVis config-file");
		System.out.println("usage 3: OTFVis network-file");
		System.out.println("usage 4: OTFVis -(-)convert event-file network-file mvi-file [snapshot-period]");
		System.out.println();
		System.out.println("Usages 1-3: Starts the Visualizer");
		System.out.println("mvi-file:      A MATSim visualizer file that contains a pre-recorder state");
		System.out.println("               to be visualized (*.mvi).");
		System.out.println("config-file:   A complete MATSim config file to run a simulation. In that case,");
		System.out.println("               a QueueSimulation will be started and visualized in real-time, ");
		System.out.println("               allowing to interactively query the state of single agents");
		System.out.println("network-file:  A MATSim network file (*.xml).");
		System.out.println();
		System.out.println("Usage 4: Convert events into a mvi-file");
		System.out.println("snapshot-period:  Optional. Specify how often a snapshot should be taken when");
		System.out.println("                  reading the events, in seconds. Default: 600 seconds");
		System.out.println();
		System.out.println("---------------------");
		System.out.println("2008-2010, matsim.org");
		System.out.println();
	}

	public static void main(final String[] args) {
		if (args.length == 0) {
			OTFVisGUI.runDialog();
		} else if ( args[0].equalsIgnoreCase("-convert") || args[0].equalsIgnoreCase("--convert") ) {
			convert(args);
		} else if (args[0].equalsIgnoreCase("-help") || args[0].equalsIgnoreCase("--help") || args[0].equalsIgnoreCase("-?") ) {
			convert(args);
		} else if (args.length == 1) {
			String filename = args[0];
			play(filename);
		} else {
			printUsage();
		}

	}

	private static void play(String filename) {
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

	public static void playMVI(final String[] args) {
		playMVI(args[0]);
	}

	public static void playMVI(String file) {
		new OTFClientFile(file).run();
	}

	public static void playConfig(final String configFilename){
		playConfig(new String[]{configFilename});
	}

	public static void playConfig(final String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);
		MatsimRandom.reset(config.global().getRandomSeed());
		log.info("Complete config dump:");
		StringWriter writer = new StringWriter();
		new ConfigWriter(config).writeStream(new PrintWriter(writer));
		log.info("\n\n" + writer.getBuffer().toString());
		log.info("Complete config dump done.");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		playScenario(scenario);
	}
	
	public static void playScenario(Scenario scenario){
		EventsManager events = EventsUtils.createEventsManager();
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim qSim = new QSimBuilder(scenario.getConfig()) //
				.useDefaults() //
				.build(scenario, events);

		OnTheFlyServer server = startServerAndRegisterWithQSim(scenario.getConfig(),scenario, events, qSim);
		OTFClientLive.run(scenario.getConfig(), server);
		
		qSim.run();
	}

    public static OnTheFlyServer startServerAndRegisterWithQSim(Config config, Scenario scenario, EventsManager events, QSim qSim) {
        return startServerAndRegisterWithQSim(config, scenario, events, qSim, null);
    }
	
    public static OnTheFlyServer startServerAndRegisterWithQSim(Config config, Scenario scenario, EventsManager events, QSim qSim,
            NonPlanAgentQueryHelper nonPlanAgentQueryHelper) {
		OnTheFlyServer server = OnTheFlyServer.createInstance(scenario, events, qSim, nonPlanAgentQueryHelper);

		if (config.transit().isUseTransit()) {

			Network network = scenario.getNetwork();
			TransitSchedule transitSchedule = scenario.getTransitSchedule();

//			TransitQSimEngine transitEngine = qSim.getTransitEngine();
//			AgentTracker agentTracker = transitEngine.getAgentTracker();

//			AgentSnapshotInfoFactory snapshotInfoFactory = qSim.getVisNetwork().getagentsnapshotinfofactory();
			SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
			linkWidthCalculator.setLinkWidthForVis(config.qsim().getLinkWidthForVis());
			if (!Double.isNaN(network.getEffectiveLaneWidth())) {
				linkWidthCalculator.setLaneWidth(network.getEffectiveLaneWidth());
			}

			var positionInfoBuilder = new PositionInfo.LinkBasedBuilder().setLinkWidthCalculator(linkWidthCalculator);
			for (AgentTracker agentTracker : qSim.getAgentTrackers()) {
				FacilityDrawer.Writer facilityWriter = new FacilityDrawer.Writer(network, transitSchedule, agentTracker, positionInfoBuilder);
				server.addAdditionalElement(facilityWriter);
			}
		}

		server.pause();
		return server;
	}

	public static void playNetwork(final String filename) {
		Config config = ConfigUtils.createConfig();
		final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(filename);
		EventsManager events = EventsUtils.createEventsManager();
		final Map<Id<Link>, VisLink> visLinks = new HashMap<>();
		for (final Id<Link> linkId : scenario.getNetwork().getLinks().keySet()) {
			visLinks.put(linkId, new VisLink() {
				@Override
				public Link getLink() {
					return scenario.getNetwork().getLinks().get(linkId);
				}

				@Override
				public Collection<? extends VisVehicle> getAllVehicles() {
					return Collections.emptyList();
				}

				@Override
				public VisData getVisData() {
					return positions -> Collections.emptyList();
				}
			});
		}
		OnTheFlyServer server = OnTheFlyServer.createInstance(scenario, events, new VisMobsim() {
			@Override
			public void run() {

			}

			@Override
			public void addQueueSimulationListeners(MobsimListener listener) {

			}

			@Override
			public VisNetwork getVisNetwork() {
				return new VisNetwork() {
					@Override
					public Map<Id<Link>, ? extends VisLink> getVisLinks() {
						return visLinks;
					}

					@Override
					public Network getNetwork() {
						return scenario.getNetwork();
					}
				};
			}

			@Override
			public Map<Id<Person>, MobsimAgent> getAgents() {
				return Collections.emptyMap();
			}

			@Override
			public VisData getNonNetworkAgentSnapshots() {
				return positions -> Collections.emptyList();
			}
		});

		OTFClientLive.run(config, server);
	}

	public static void convert(final String[] args) {
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
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		OTFEvent2MVI.convert(scenario, eventFile, mviFile, snapshotPeriod);
	}

}
