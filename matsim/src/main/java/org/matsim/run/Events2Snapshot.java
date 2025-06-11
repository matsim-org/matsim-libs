/* *********************************************************************** *
 * project: org.matsim.*
 * Events2Snapshot.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
import java.util.Collection;
import java.util.Iterator;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.SnapshotGenerator;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ArgumentParser;
import org.matsim.vis.snapshotwriters.SnapshotWriter;
import org.matsim.vis.snapshotwriters.TransimsSnapshotWriter;

import static org.matsim.core.config.groups.ControllerConfigGroup.*;

/**
 * Converts  an events file to a snapshot file.
 *
 * @author mrieser
 * @author glaemmel
 */
public class Events2Snapshot {

	private Config config = null;
	private Network network = null;
	private EventsManager events = null;
	private SnapshotGenerator visualizer = null;
	private String configfile = null;
	private String eventsfile;
	private SnapshotWriter writer = null;

	/**
	 * Parses all arguments and sets the corresponding members.
	 *
	 * @param args
	 */
	private void parseArguments(final String[] args) {
		if (args.length == 0) {
			System.out.println("Too few arguments.");
			printUsage();
			System.exit(1);
		}
		Iterator<String> argIter = new ArgumentParser(args).iterator();
		while (argIter.hasNext()) {
			String arg = argIter.next();
			if (arg.equals("-h") || arg.equals("--help")) {
				printUsage();
				System.exit(0);
			} else {
				if (arg.contains(".xml"))
					this.configfile = arg;
				else if (arg.contains("events"))
					this.eventsfile = arg;
				else {
					System.out.println("Unrecognized file \"" + arg + "\"");
					printUsage();
					System.exit(1);
				}
			}
		}
	}

	private void printUsage() {
		System.out.println();
		System.out.println("Events2Snapshot");
		System.out.println("Converts an events file to a snapshot file.");
		System.out.println();
		System.out.println("usage: Events2Snapshot [OPTIONS] configfile eventsfile");
		System.out.println("       The snapshots are generated according to the snapshot-settings in the");
		System.out.println("       simulation-part of the configuration.");
		System.out.println();
		System.out.println("Options:");
		System.out.println("-h, --help:     Displays this message.");
		System.out.println();
		System.out.println("----------------");
		System.out.println("2007, matsim.org");
		System.out.println();
	}

	/** Starts the conversion of events into snapshot files. Stand-alone run-method.
	 *
	 * @param args command-line arguments
	 */
	public void run(final String[] args) {
		parseArguments(args);
		Scenario scenario;
		this.config = ConfigUtils.loadConfig(this.configfile);
		MatsimRandom.reset(this.config.global().getRandomSeed());
		scenario = ScenarioUtils.createScenario(this.config);

//		if (((SimulationConfigGroup) this.config.getModule(SimulationConfigGroup.GROUP_NAME)).getSnapshotPeriod() <= 0.0) {
//			System.out.println("The snapshotPeriod must be larger than 0 seconds.");
//			return;
//		}

		this.network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(this.config.network().getInputFile());
		prepare();

		System.out.println("reading events from " + this.eventsfile);
		File file = new File(this.eventsfile);
		String outputDir = file.getParent() + "/";

		loadSnapshotWriters(outputDir);

		new MatsimEventsReader(this.events).readFile(this.eventsfile);
		this.visualizer.finish();
		System.out.println("done.");
	}

	/** Starts the conversion of events into snapshot files. Alternative run-method
	 * to more easily integrate it into existing code where config, network etc.
	 * are already loaded.
	 *
	 * @param eventsFile
	 * @param config
	 * @param network
	 */
	public void run(final File eventsFile, final Config config, final Network network) {
		this.eventsfile = eventsFile.getAbsolutePath();
		this.config = config;

		if (this.config.qsim().getSnapshotPeriod() <= 0.0) {
			System.out.println("The snapshotPeriod must be larger than 0 seconds.");
			return;
		}

		this.network = network;

		prepare();

		System.out.println("reading events from " + this.eventsfile);
		File file = new File(this.eventsfile);
		String outputDir = file.getParent() + "/";

		loadSnapshotWriters(outputDir);

		try {
			new MatsimEventsReader(this.events).readFile(this.eventsfile);
		}
		catch (OutOfMemoryError e) {
			System.err.println("OutOfMemoryError while reading all events:");
			e.printStackTrace();
			System.err.println("Trying to close visualizer file up to this state, it may not be complete though.");
		}
		this.visualizer.finish();
		System.out.println("done.");
	}


	private void prepare() {
		// create events
		this.events = EventsUtils.createEventsManager();

		// create SnapshotGenerator
		this.visualizer = new SnapshotGenerator(this.network, this.config.qsim().getSnapshotPeriod(),
				this.config.qsim());
		this.events.addHandler(this.visualizer);
	}

	public void addExternalSnapshotWriter(final SnapshotWriter writer) {
		this.writer = writer;
	}

	private void loadSnapshotWriters(final String outputDir) {

		if (this.writer != null) {
			this.visualizer.addSnapshotWriter(this.writer);
		}

		Collection<SnapshotFormat> snapshotFormats = this.config.controller().getSnapshotFormat();

		for( SnapshotFormat snapshotFormat : snapshotFormats ){
			switch( snapshotFormat ){
				case transims: {
					String snapshotFile = outputDir + "T.veh";
					this.visualizer.addSnapshotWriter(new TransimsSnapshotWriter(snapshotFile));
					break; }
				case googleearth:
					// KML support removed, michalm, may'22
				case otfvis:
					// this was not filled in when I found it, but I think it should.  kai, feb'20
				case positionevents:
				default:
					throw new IllegalStateException( "Unexpected value: " + snapshotFormat );
			}
		}
	}

	public static void main(final String[] args) {
		new Events2Snapshot().run(args);
	}

}
