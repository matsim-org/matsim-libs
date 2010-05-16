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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.io.MatsimFileTypeGuesser;
import org.matsim.core.utils.io.MatsimFileTypeGuesser.FileType;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.vis.otfvis.OTFClientFile;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFClientSwing;
import org.matsim.vis.otfvis.OTFDoubleMVI;
import org.matsim.vis.otfvis.OTFVisQSim;
import org.matsim.vis.otfvis.data.DefaultConnectionManagerFactory;
import org.matsim.vis.otfvis.executables.OTFEvent2MVI;
import org.matsim.vis.snapshots.writers.VisMobsim;

/**
 * A generic starter for the OnTheFly Visualizer that supports
 * MATSim Visualizer Files (MVI), TRANSIMS vehicle Files (VEH),
 * and MATSim Config Files for the live simulation and visualization.
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
		System.out.println("usage 3: OTFVis veh-file network-file");
		System.out.println("usage 4: OTFVis config-file");
		System.out.println("usage 5: OTFVis [-swing] network-file");
		System.out.println("usage 6: OTFVis -convert event-file network-file mvi-file [snapshot-period]");
		System.out.println();
		System.out.println("Usages 1-5: Starts the Visualizer");
		System.out.println("mvi-file:      A MATSim visualizer file that contains a pre-recorder state");
		System.out.println("               to be visualized (*.mvi).");
		System.out.println("mvi-file1,2:   Loads two mvi-files in parallel and shows them next to each");
		System.out.println("               other. Good way to compare results from similar scenarios.");
		System.out.println("veh-file:      A TRANSIMS vehicle file to be visualized (*.veh).");
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
		String [] args2 = args;

		if (args.length == 0) {
			args2 = chooseFile(args2);
		}
		if (args2.length == 0) {
			printUsage();
			return;
		}
		String arg0l = args2[0].toLowerCase(Locale.ROOT);

		boolean useSwing = false;
		if ("-swing".equals(arg0l)) {
			useSwing = true;
			// pop that argument from the list
			String[] tmp = new String[args2.length - 1];
			System.arraycopy(args, 1, tmp, 0, tmp.length);
			args2 = tmp;
			// start over, kind of
			arg0l = args2[0].toLowerCase(Locale.ROOT);
		}

		if (arg0l.endsWith(".veh.gz") || arg0l.endsWith(".veh")) {
			playVEH(args2);
		} else if (arg0l.endsWith(".mvi")) {
			if (args2.length > 1) {
				String arg1l = args2[1].toLowerCase(Locale.ROOT);
				if (arg1l.endsWith(".mvi")) {
					playDoubleMVI(args2[0], args2[1]);
				} else {
					System.out.println("unrecognized input: " + args2[1]);
					printUsage();
				}
			} else {
				if (useSwing) {
					playMVI_Swing(args2[0]);
				} else {
					playMVI(args2);
				}
			}
		} else if ((arg0l.endsWith(".xml") || arg0l.endsWith(".xml.gz"))) {
			FileType type;
			try {
				type = new MatsimFileTypeGuesser(args2[0]).getGuessedFileType();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			if (FileType.Config.equals(type)) {
				playConfig(args2);
			} else if (FileType.Network.equals(type)) {
				if (useSwing) {
					playNetwork_Swing(args2[0]);
				} else {
					playNetwork(args2);
				}
			} else {
				printUsage();
			}
		} else if (arg0l.equals("-convert")) {
			convert(args2);
		} else {
			printUsage();
		}
	}

	public static final String[] chooseFile(final String[] args) {
		JFileChooser fc = new JFileChooser();

	    fc.setFileFilter( new FileFilter() {
	      @Override public boolean accept( File f ) {
	        return f.isDirectory() || f.getName().toLowerCase().endsWith( ".xml" );
	      }
	      @Override public String getDescription() { return "MATSim net or config file (*.xml)"; }
	    } );

	    fc.setFileFilter( new FileFilter() {
	      @Override public boolean accept( File f ) {
	        return f.isDirectory() || f.getName().toLowerCase().endsWith( ".mvi" );
	      }
	      @Override public String getDescription() { return "OTFVis movie file (*.mvi)"; }
	    } );

	    int state = fc.showOpenDialog( null );
	    if ( state == JFileChooser.APPROVE_OPTION ) {
	    	String [] args_new = {fc.getSelectedFile().getAbsolutePath()};
	    	return args_new;
	    }
      System.out.println( "No file selected." );
      return args;
	}

	public static final void playMVI(final String[] args) {
		playMVI(args[0]);
	}

	public static void playMVI(String file) {
		new OTFClientFile(file).run();
	}

	public static void playDoubleMVI(final String file1, final String file2) {
		new OTFDoubleMVI(file1, file2).run();
	}

	public static void playMVI_Swing(String file) {
		new OTFClientSwing("file:" + file).run();
	}

	/* @deprecated this currently does not work; may be fixed if needed.  kai, jan'10 */
	@Deprecated // this currently does not work; may be fixed if needed.  kai, jan'10
	public static final void playVEH(final String[] args) {
		log.error("this currently does not work; may be fixed if needed.  kai, jan'10" );
		log.error("At least OTFTVeh2MVI is now working again.  kai, may'10" ) ;
		System.exit(-1) ;
		// we should have the network as second argument
		String vehFileName = args[0];
		if (args.length < 2) {
			printUsage();
			return;
		}
		if (args[1].toLowerCase().endsWith(".xml") || args[1].toLowerCase().endsWith(".xml.gz")) {
			String netFileName = args[1];
//  seeing that Gbl.getConfig() is nowhere used anymore in org.matsim.vis.otfvis, I assume
//  it should now work without the following lines. marcel/6apr2010
//			if (Gbl.getConfig() == null) {
//				Gbl.createConfig(null); // is this really required?
//			}
//			Gbl.getConfig().setQSimConfigGroup(new QSimConfigGroup());
			OTFClientLive client = new OTFClientLive("tveh:"+vehFileName + "@" + netFileName, new DefaultConnectionManagerFactory().createConnectionManager());
			client.run();
		}
	}

	public static final void playConfig(final String configFilename){
		playConfig(new String[]{configFilename});
	}

	public static final void playConfig(final String[] args) {
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(args[0]);
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
		  loader.getScenario().getConfig().setQSimConfigGroup(new QSimConfigGroup());
		}
		loader.loadScenario();
		ScenarioImpl scenario = loader.getScenario();
		EventsManagerImpl events = new EventsManagerImpl();
		ControlerIO controlerIO = new ControlerIO(scenario.getConfig().controler().getOutputDirectory());
		VisMobsim queueSimulation = new OTFVisQSim(scenario, events);

		// replacing above line by following line runs this with core.mobsim.queuesimulation instead of QSim.
		// There are, however, things that don't work, for example:
		// - parked vehicles are not shown (I would assume that they are simply not included into the core.mobsim.queuesim.QLink.Visdata)
		// - it "catches" the wrong vehicles when you click on the vehicles (one time step forward seems to fix this)
//		VisMobsim queueSimulation = new OTFVisQueueSimulation( scenario, events ) ;

		queueSimulation.setControlerIO(controlerIO);
		queueSimulation.setIterationNumber(scenario.getConfig().controler().getLastIteration());
		queueSimulation.run();
	}

	public static final void playNetwork(final String[] args) {
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		new MatsimNetworkReader(scenario).readFile(args[0]);
		EventsManagerImpl events = new EventsManagerImpl();
		OTFVisQSim queueSimulation = new OTFVisQSim(scenario, events);
		queueSimulation.run();
	}

	public static final void playNetwork_Swing(final String filename) {
		new OTFClientSwing("net:" + filename).run();
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
		Scenario scenario = new ScenarioImpl();
		Gbl.setConfig(scenario.getConfig());
		scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		new MatsimNetworkReader(scenario).readFile(networkFile);
		QSim sim = new QSim(scenario, new EventsManagerImpl());
		OTFEvent2MVI converter = new OTFEvent2MVI(sim.getQNetwork(), eventFile, mviFile, snapshotPeriod);
		converter.convert(scenario.getConfig());
	}

}
