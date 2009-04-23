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

package playground.meisterk.org.matsim.run.ktiYear3;

import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.locationchoice.facilityload.FacilitiesLoadCalculator;

import playground.meisterk.org.matsim.controler.listeners.CalcLegTimesKTIListener;
import playground.meisterk.org.matsim.controler.listeners.KtiRouterListener;
import playground.meisterk.org.matsim.controler.listeners.ScoreElements;
import playground.meisterk.org.matsim.scoring.ktiYear3.KTIYear3ScoringFunctionFactory;

public class Controler {

	private org.matsim.core.controler.Controler controler;
	
	public Controler(String[] args) {
		this(Gbl.createConfig(args));
	}

	public Controler(Config config) {
		this.controler = new org.matsim.core.controler.Controler(config);
	}

	public void run() {
		
		KTIYear3ScoringFunctionFactory kTIYear3ScoringFunctionFactory = new KTIYear3ScoringFunctionFactory(
				Gbl.getConfig().charyparNagelScoring(), 
				controler.getFacilityPenalties()
				);
		controler.setScoringFunctionFactory(kTIYear3ScoringFunctionFactory);
		
		// the scoring function processes facility loads independent of whether a location choice module is used or not
		controler.addControlerListener(new FacilitiesLoadCalculator(controler.getFacilityPenalties()));
		controler.addControlerListener(new ScoreElements("scoreElementsAverages.txt"));
		controler.addControlerListener(new CalcLegTimesKTIListener("calcLegTimesKTI.txt"));
		controler.addControlerListener(new KtiRouterListener());
		
		controler.run();
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Controler(args).run();
	}

	public org.matsim.core.controler.Controler getControler() {
		return controler;
	}

}
