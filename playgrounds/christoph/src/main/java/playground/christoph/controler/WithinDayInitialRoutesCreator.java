/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayInitialRoutesCreator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.controler;

import org.matsim.contrib.analysis.christoph.ActivitiesAnalyzer;
import org.matsim.contrib.analysis.christoph.TripsAnalyzer;
import org.matsim.core.controler.Controler;

/*
 * Replacement for WithinDayInitialRoutesController.
 */
public class WithinDayInitialRoutesCreator  {

	private static double duringLegReroutingShare = 0.10;
	
	private static boolean initialLegRerouting = true;
	private static boolean duringLegRerouting = false;
	
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: InitialRoutesCreator config-file [dtd-file]");
			System.out.println();
		} else {
			Controler controler = new Controler(args);
			
			/*
			 * Add some analysis modules to the controler.
			 */
			controler.addControlerListener(new TripsAnalyzer());
			controler.addControlerListener(new ActivitiesAnalyzer());
			
			WithinDayInitialRoutesControlerListener controlerListener = new WithinDayInitialRoutesControlerListener();
			controlerListener.setDuringLegReroutingEnabled(duringLegRerouting);
			controlerListener.setDuringLegReroutingShare(duringLegReroutingShare);
			controlerListener.setInitialLegReroutingEnabled(initialLegRerouting);
			controler.addControlerListener(controlerListener);
			
			controler.run();
		}
		System.exit(0);
	}
}