/* *********************************************************************** *
 * project: org.matsim.*
 * TelAvivControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.telaviv.controler;

//import org.matsim.core.controler.Controler;
//import org.matsim.core.scoring.functions.CharyparNagelOpenTimesScoringFunctionFactory;
import org.matsim.roadpricing.RoadPricing;
import playground.telaviv.core.mobsim.qsim.TTAQSimFactory;
import playground.telaviv.locationchoice.matsimdc.DCControler;

public final class TelAvivControler {
		
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: TelAvivControler config-file [dtd-file]");
			System.out.println();
		} else {
			final DCControler controler = new DCControler(args);
			
			// add road pricing contrib
			// controler.addControlerListener(new RoadPricing());
			
			// use an adapted MobsimFactory
			controler.setMobsimFactory(new TTAQSimFactory());
			
			controler.addControlerListener(new TelAvivControlerListener());
			
			/*
			 * We use a Scoring Function that get the Facility Opening Times from
			 * the Facilities instead of the Config File.
			 */
//			controler.setScoringFunctionFactory(
//					new CharyparNagelOpenTimesScoringFunctionFactory(controler.getConfig().planCalcScore(), controler.getScenario())
//					) ;

			controler.run();
		}
		System.exit(0);
	}

}