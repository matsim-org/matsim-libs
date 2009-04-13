/* *********************************************************************** *
 * project: org.matsim.*
 * KTIYear3Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.org.matsim.controler;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.locationchoice.facilityload.FacilitiesLoadCalculator;

public class KTIYear3Controler {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Config config = Gbl.createConfig(args);
		Controler controler = new Controler(config);
		
		// the scoring function processes facility loads independent of whether a location choice module is used or not
		controler.addControlerListener(new FacilitiesLoadCalculator(controler.getFacilityPenalties()));
		// standard controler listeners for KTI Year 3 runs
		controler.setScoringFunctionFactory(new playground.meisterk.org.matsim.scoring.ktiYear3.KTIYear3ScoringFunctionFactory(
				Gbl.getConfig().charyparNagelScoring(), 
				controler.getFacilityPenalties()));
		
		controler.run();

	}

}
