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
import java.util.Arrays;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
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
		
//		new DgConfigCleaner().cleanAndWriteConfig(configfile, configfile + ".xml");
		
		Config config = new Config();
		config.addCoreModules();
		ConfigReader confReader = new ConfigReader(config);
		confReader.readFile(configfile);
		
		
//		final int iteration = 0;
		final int iteration = 10;
		
		Controler controler = new Controler(config);
		if (useQSim){
		  controler.getConfig().controler().setSnapshotFormat(Arrays.asList("otfvis"));
		  controler.getConfig().controler().setWriteSnapshotsInterval(0);
		  controler.getConfig().qsim().setSnapshotPeriod(1.0);
		  controler.getConfig().qsim().setSnapshotStyle(SnapshotStyle.queue);
		}
		else {
		  ((SimulationConfigGroup) controler.getConfig().getModule(SimulationConfigGroup.GROUP_NAME)).setSnapshotPeriod(1.0);
		}

		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

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
