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

import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.run.OTFVis;


/**
 * @author dgrether
 *
 */
public class DgEquilController {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		final String config = DgPaths.EXAMPLEBASE + "equil/configPlans100.xml";
		
//		final int iteration = 0;
		final int iteration = 0;
		
		Controler controler = new Controler(config);
		controler.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		controler.setOverwriteFiles(true);
		
		controler.addControlerListener(new StartupListener(){
			public void notifyStartup(final StartupEvent event) {
				event.getControler().getConfig().controler().setLastIteration(iteration);
			}
		});
		
		controler.run();
		
		
		
		String filename = controler.getConfig().controler().getOutputDirectory();
		
		filename += "/ITERS/it."+iteration+"/" + iteration + ".otfvis.mvi";
		System.out.println(filename);
		OTFVis.main(new String[] {filename});
		
		
	}

}
