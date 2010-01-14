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

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.io.MatsimFileTypeGuesser;
import org.matsim.core.utils.io.MatsimFileTypeGuesser.FileType;
import org.matsim.ptproject.qsim.QueueNetwork;
import org.matsim.vis.otfvis.OTFClientFile;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisQueueSim;
import org.matsim.vis.otfvis.data.DefaultConnectionManagerFactory;
import org.matsim.vis.otfvis.executables.OTFEvent2MVI;

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
		System.out.println("usage 1: OTFVis mvi-file");
		System.out.println("usage 2: OTFVis veh-file network-file");
		System.out.println("usage 3: OTFVis config-file");
		System.out.println("usage 4: OTFVis network-file");
		System.out.println("usage 5: OTFVis -convert event-file network-file mvi-file [snapshot-period]");
		System.out.println();
		System.out.println("Usages 1-4: Starts the Visualizer");
		System.out.println("mvi-file:      A MATSim visualizer file that contains a pre-recorder state");
		System.out.println("               to be visualized (*.mvi).");
		System.out.println("veh-file:      A TRANSIMS vehicle file to be visualized (*.veh).");
		System.out.println("network-file:  A MATSim network file (*.xml).");
		System.out.println("config-file:   A complete MATSim config file to run a simulation. In that case,");
		System.out.println("               a QueueSimulation will be started and visualized in real-time, ");
		System.out.println("               allowing to interactively query the state of single agents");
		System.out.println();
		System.out.println("Usage 5: Convert events into a mvi-file");
		System.out.println("snapshot-period:  Optional. Specify how often a snapshot should be taken when");
		System.out.println("                  reading the events, in seconds. Default: 600 seconds");
		System.out.println();
		System.out.println("---------------------");
		System.out.println("2008-2009, matsim.org");
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
		String arg0l = args2[0].toLowerCase();
		
		if (arg0l.endsWith(".veh.gz") || arg0l.toLowerCase().endsWith(".veh")) {
			playVEH(args2);
		} else if (arg0l.endsWith(".mvi")) {
			playMVI(args2);
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
				playNetwork(args2);
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
//		new OTFClientSwing(args[0]).start();
		new OTFClientFile(args[0]).start();
	}

	/* @deprecated this currently does not work; may be fixed if needed.  kai, jan'10 */
	@Deprecated // this currently does not work; may be fixed if needed.  kai, jan'10
	public static final void playVEH(final String[] args) {
		log.error("this currently does not work; may be fixed if needed.  kai, jan'10" ) ;
		// we should have the network as second argument
		String vehFileName = args[0];
		if (args.length < 2) {
			printUsage();
			return;
		}
		if (args[1].toLowerCase().endsWith(".xml") || args[1].toLowerCase().endsWith(".xml.gz")) {
			String netFileName = args[1];
			if (Gbl.getConfig() == null) {
				Gbl.createConfig(null); // is this really required?
			}
			Gbl.getConfig().setQSimConfigGroup(new QSimConfigGroup());
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
		OTFVisQueueSim queueSimulation = new OTFVisQueueSim(scenario, events);
		queueSimulation.setControlerIO(controlerIO);
		queueSimulation.setIterationNumber(scenario.getConfig().controler().getLastIteration());
		queueSimulation.run();
	}
	
	public static final void playNetwork(final String[] args) {
		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(args[0]);
		EventsManagerImpl events = new EventsManagerImpl();
		OTFVisQueueSim queueSimulation = new OTFVisQueueSim(scenario, events);
		queueSimulation.run();
	}

	public static final void convert(final String[] args) {
		if ((args.length < 4) || (args.length > 5)) {
			printUsage();
			return;
		}
		Gbl.createConfig(null);
		String eventFile = args[1];
		String networkFile = args[2];
		String mviFile = args[3];
		int snapshotPeriod = 600;
		if (args.length == 5) {
			snapshotPeriod = Integer.parseInt(args[4]);
		}
		NetworkLayer net = new NetworkLayer();
		new MatsimNetworkReader(net).readFile(networkFile);
		OTFEvent2MVI converter = new OTFEvent2MVI(new QueueNetwork(net), eventFile, mviFile, snapshotPeriod);
		converter.convert();
	}
	
}
