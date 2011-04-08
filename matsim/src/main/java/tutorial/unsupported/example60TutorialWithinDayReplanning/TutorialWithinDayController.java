/* *********************************************************************** *
 * project: org.matsim.*
 * TutorialWithinDayController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package tutorial.unsupported.example60TutorialWithinDayReplanning;

import org.matsim.core.controler.Controler;
import org.matsim.evacuation.run.EvacuationQSimControler;

/**
 * This class should give an example what is needed to run
 * simulations with WithinDayReplanning.
 * 
 * The Path to a config file is needed as argument to run the
 * simulation.
 * 
 * By default
 * "./examples/evacuation-tutorial/withinDayEvacuationConf.xml"
 * should be ok. 
 * 
 * @author cdobler
 */
public class TutorialWithinDayController {
	
	/*
	 * ===================================================================
	 * main
	 * ===================================================================
	 */
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controller = new EvacuationQSimControler(args);
			controller.setOverwriteFiles(true);

			// perform only a single iteration
			controller.getConfig().controler().setLastIteration(0);
			
			/*
			 * Configure Controller for usage with WithinDay modules.
			 */
			WithinDayControllerListener withinDayControllerListener = new WithinDayControllerListener();
			withinDayControllerListener.setControllerParameters(controller);
			// yyyy this will register the listener with the control(l)er as a side effect. Is this what we want?
			// It seems to me that we had agreed to avoid side effects.  kai, apr'11
			// Adapted the code. cdobler, apr'11
			
			controller.run();
		}
		System.exit(0);
	}
	
}