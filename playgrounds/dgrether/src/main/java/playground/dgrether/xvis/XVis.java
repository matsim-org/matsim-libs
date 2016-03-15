/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.dgrether.xvis;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.MatsimFileTypeGuesser;


/**
 * Initializes XVis with some data and starts the visualizer. 
 * @author dgrether
 */
public class XVis {
	
	private static final Logger log = Logger.getLogger(XVis.class);
	
	private XVisStartup startup;

	static {
		try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        log.debug("Setting look and feel to " + UIManager.getSystemLookAndFeelClassName() + " name: " + UIManager.getLookAndFeel().getName());
		} 
    catch (UnsupportedLookAndFeelException e) {
    	e.printStackTrace();
    }
    catch (ClassNotFoundException e) {
    	e.printStackTrace();
    }
    catch (InstantiationException e) {
    	e.printStackTrace();
    }
    catch (IllegalAccessException e) {
    	e.printStackTrace();
    }
    
		if ("GTK look and feel".equals(UIManager.getLookAndFeel().getName())){
			UIManager.put("FileChooserUI", "eu.kostia.gtkjfilechooser.ui.GtkFileChooserUI");
		}
//		printClasspath();
		testResourceLoading();
	}
	
	private static void printClasspath() {
		final java.lang.String list = java.lang.System.getProperty("java.class.path");
		log.debug("Classpath: " + list);
		for (final java.lang.String path : list.split(":")) {
			final java.io.File object = new java.io.File(path);
			if (object.isDirectory())
				for (java.lang.String entry : object.list()) {
					final java.io.File thing = new java.io.File(entry);
					if (thing.isFile())
						java.lang.System.out.println(thing);
				}
			else if (object.isFile())
				java.lang.System.out.println(object);
		}
	}
	
	
	private static void testResourceLoading() {
		String filename = "jlfgr-1_0/toolbarButtonGraphics/general/ZoomIn24.gif";
//		filename = "jlfgr-1_0/jlfgr-1_0.jar";
		log.debug(filename);
		URL url = XVis.class.getClassLoader().getResource(filename);
//	URL r = ControlButtonPanel.class.getClassLoader().getResource(filename);
		log.debug("url to resource file: " + url);
	}


	private XVis(){
		this.startup = new XVisStartup();
	}
	

	private void startXVis(String filename) throws IOException {
		MatsimFileTypeGuesser guesser = new MatsimFileTypeGuesser(filename);
		Scenario scenario =  null;
		if (MatsimFileTypeGuesser.FileType.Config.equals(guesser.getGuessedFileType())) {
			Config config = ConfigUtils.loadConfig(filename);
			scenario = ScenarioUtils.loadScenario(config);
			
			// add missing scenario elements
			SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
					SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
			if (signalsConfigGroup.isUseSignalSystems()) {
				scenario.addScenarioElement(SignalsData.ELEMENT_NAME,
						new SignalsScenarioLoader(signalsConfigGroup).loadSignalsData());
			}
		}
		else 	if (MatsimFileTypeGuesser.FileType.Network.equals(guesser.getGuessedFileType())){
			Config config = ConfigUtils.createConfig();
			config.network().setInputFile(filename);
			scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		}
		else if (MatsimFileTypeGuesser.FileType.SignalControl.equals(guesser.getGuessedFileType())){
			Config config = ConfigUtils.createConfig();
			ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setUseSignalSystems(true);
			ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setSignalControlFile(filename);
			scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		}
		
		if (scenario != null){
			this.startup.runVisualizer(scenario);
		}
		else {
			printHelp();
		}
	}

	private static void printHelp() {
		System.out.println("Start with some MATSim file as argument and see what's happening ;-)");
	}
	
	public static final String chooseFile() {
		JFileChooser fc = new JFileChooser();
			fc.setFileFilter(new FileFilter() {

			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml") || f.getName().toLowerCase().endsWith(".xml.gz");
			}

			@Override
			public String getDescription() {
				return "MATSim file (*.xml , *.xml.gz)";
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
	 */
	public static void main(String[] args) throws IOException {
		XVis xv = new XVis();
		if (args == null || args.length == 0){
			String filename = chooseFile();
			if (filename == null){
				printHelp();
			}
			else {
				xv.startXVis(filename);
			}
		}
		else if (args.length == 1){
			String filename = args[0];
			xv.startXVis(filename);
		}
	}



}
