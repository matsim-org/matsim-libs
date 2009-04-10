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
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.locationchoice.facilityload.FacilitiesLoadCalculator;

import playground.meisterk.org.matsim.analysis.CalcTripDurations;

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
		controler.addControlerListener(new KTIYear3ControlerListener());
		controler.setScoringFunctionFactory(new playground.meisterk.org.matsim.scoring.ktiYear3.KTIYear3ScoringFunctionFactory(
				Gbl.getConfig().charyparNagelScoring(), 
				controler.getFacilityPenalties()));
		
		controler.run();

	}

	protected static class KTIYear3ControlerListener implements StartupListener, AfterMobsimListener {

		private CalcTripDurations calcTripDurations;

		public void notifyStartup(StartupEvent event) {
			this.calcTripDurations = new CalcTripDurations();
			Controler c = event.getControler();
			c.getEvents().addHandler(this.calcTripDurations);
		}

		public void notifyAfterMobsim(AfterMobsimEvent event) {
			if (this.calcTripDurations != null) {
				this.calcTripDurations.writeStats(Controler.getIterationFilename("tripDurations.txt"));
			}
		}
		
	}
	
}
