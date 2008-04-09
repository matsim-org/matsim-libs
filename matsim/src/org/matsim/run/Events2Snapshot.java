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
import java.util.Iterator;

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.events.algorithms.SnapshotGenerator;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.transformations.TransformationFactory;
import org.matsim.utils.misc.ArgumentParser;
import org.matsim.utils.vis.netvis.VisConfig;
import org.matsim.utils.vis.snapshots.writers.KmlSnapshotWriter;
import org.matsim.utils.vis.snapshots.writers.PlansFileSnapshotWriter;
import org.matsim.utils.vis.snapshots.writers.SnapshotWriterI;
import org.matsim.utils.vis.snapshots.writers.TransimsSnapshotWriter;

/**
 * Converts  an events file to a snapshot file.
 *
 * @author mrieser
 * @author glaemmel
 */
public class Events2Snapshot {

	private Config config;
	private NetworkLayer network = null;
	private Events events = null;
	private SnapshotGenerator visualizer = null;
	private String configfile = null;
	private String dtdfile = null;
	private String eventsfile;
	private SnapshotWriterI writer = null;

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
				else if (arg.endsWith(".dtd"))
					this.dtdfile = arg;
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
		System.out.println("usage: Events2Snapshot [OPTIONS] configfile [config-dtdfile] [eventsfile]");
		System.out.println("       If no eventsfile is given, the in-events-file specified in the config-");
		System.out.println("       file will be used.");
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
		this.config = Gbl.createConfig(new String[]{this.configfile, this.dtdfile});

		if (this.config.simulation().getSnapshotPeriod() <= 0.0) {
			System.out.println("The snapshotPeriod must be larger than 0 seconds.");
			return;
		}

		prepare();

		if (this.eventsfile == null) {
			this.eventsfile = this.config.getParam("events", "inputFile");
		}
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
	public void run(final File eventsFile, final Config config, final NetworkLayer network) {
		this.eventsfile = eventsFile.getAbsolutePath();
		this.config = config;

		if (this.config.simulation().getSnapshotPeriod() <= 0.0) {
			System.out.println("The snapshotPeriod must be larger than 0 seconds.");
			return;
		}

		this.network = network;

		prepare();

		if (this.eventsfile == null) {
			this.eventsfile = this.config.getParam("events", "inputFile");
		}
		System.out.println("reading events from " + this.eventsfile);
		File file = new File(this.eventsfile);
		String outputDir = file.getParent() + "/";

		loadSnapshotWriters(outputDir);

		new MatsimEventsReader(this.events).readFile(this.eventsfile);
		this.visualizer.finish();
		System.out.println("done.");
	}


	private void prepare() {
		if (this.network == null) {
			// read network
			this.network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
			new MatsimNetworkReader(this.network).readFile(this.config.network().getInputFile());
		}

		// create events
		this.events = new Events();

		// create SnapshotGenerator
		this.visualizer = new SnapshotGenerator(this.network, this.config.simulation().getSnapshotPeriod());
		this.events.addHandler(this.visualizer);
	}

	public void addExternalSnapshotWriter(final SnapshotWriterI writer) {
		this.writer = writer;
	}

	private void loadSnapshotWriters(final String outputDir) {

		if (this.writer != null) this.visualizer.addSnapshotWriter(this.writer);

		String snapshotFormat = this.config.simulation().getSnapshotFormat();

		if (snapshotFormat.contains("plansfile")) {
			String snapshotFilePrefix = outputDir + "/positionInfoPlansFile";
			String snapshotFileSuffix = "xml";
			this.visualizer.addSnapshotWriter(new PlansFileSnapshotWriter(snapshotFilePrefix, snapshotFileSuffix));
		}
		if (snapshotFormat.contains("transims")) {
			String snapshotFile = outputDir + "T.veh";
			this.visualizer.addSnapshotWriter(new TransimsSnapshotWriter(snapshotFile));
		}
		if (snapshotFormat.contains("googleearth")) {
			String snapshotFile = outputDir + "googleearth.kmz";
			String coordSystem = this.config.global().getCoordinateSystem();
			this.visualizer.addSnapshotWriter(new KmlSnapshotWriter(snapshotFile,
					TransformationFactory.getCoordinateTransformation(coordSystem, TransformationFactory.WGS84)));
		}
		if (snapshotFormat.contains("netvis")) {
			String snapshotFile = outputDir + "/Snapshot";
			File networkFile = new File(this.config.network().getInputFile());
			VisConfig visConfig = VisConfig.newDefaultConfig();
			String[] params = {VisConfig.LOGO, VisConfig.DELAY, VisConfig.LINK_WIDTH_FACTOR, VisConfig.SHOW_NODE_LABELS, VisConfig.SHOW_LINK_LABELS};
			for (String param : params) {
				String value = this.config.findParam("vis", param);
				if (value != null) {
					visConfig.set(param, value);
				}
			}
			int buffers = this.network.getLinks().size();
			buffers = Math.max(5, Math.min(5000000/buffers, 1000));
			this.visualizer.addNetStateWriter(this.network, networkFile.getAbsolutePath(), visConfig, snapshotFile, (int)this.config.simulation().getSnapshotPeriod(), buffers);
		}
	}

	/**
	 * description
	 *
	 * @param args
	 */
	public static void main(final String[] args) {
		Events2Snapshot app = new Events2Snapshot();
		app.run(args);
	}

}
