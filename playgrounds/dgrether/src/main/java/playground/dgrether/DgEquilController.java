/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.dgrether;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;


/**
 * @author dgrether
 *
 */
public class DgEquilController {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		
	  final boolean useQSim = true;
	  
		final String configfile = DgPaths.EXAMPLEBASE + "equil/configPlans100.xml";
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader confReader = new MatsimConfigReader(config);
		confReader.readFile(configfile);
		
		
//		final int iteration = 0;
		final int iteration = 0;
		
		Controler controler = new Controler(config);
		if (useQSim){
		  controler.getConfig().addQSimConfigGroup(new QSimConfigGroup());
		  controler.getConfig().getQSimConfigGroup().setSnapshotFormat("otfvis");
		  controler.getConfig().getQSimConfigGroup().setSnapshotPeriod(1.0);
		  controler.getConfig().getQSimConfigGroup().setSnapshotStyle("queue");
		}
		else {
		  controler.getConfig().simulation().setSnapshotPeriod(1.0);
		}
		
		controler.setOverwriteFiles(true);
		
		controler.addControlerListener(new StartupListener(){
			public void notifyStartup(final StartupEvent event) {
				event.getControler().getConfig().controler().setLastIteration(iteration);
			}
		});
		
		controler.addControlerListener(new DgOTFVisConfigWriter());
		controler.run();
		
		String outdir = controler.getConfig().controler().getOutputDirectory();
		String filename = outdir + "/ITERS/it."+iteration+"/" + iteration + ".otfvis.mvi";
		System.out.println(filename);
		DgOTFVisReplayLastIteration.main(new String[]{outdir + "/" + Controler.FILENAME_CONFIG});
//		OTFVis.playConfig(filename + "/" + DgOTFVisConfigWriter.OTFVIS_LAST_ITERATION_CONFIG);
//		OTFVis.main(new String[] {filename});
//		new OTFClientSwing(filename).start();
		
	}

}
