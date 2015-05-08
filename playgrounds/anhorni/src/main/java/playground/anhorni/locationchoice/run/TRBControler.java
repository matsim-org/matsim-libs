/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.locationchoice.run;

import org.matsim.contrib.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalties;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;

import playground.anhorni.locationchoice.analysis.PrintShopAndLeisureLocations;
import playground.anhorni.locationchoice.analysis.TravelDistanceDistribution;
import playground.anhorni.locationchoice.analysis.events.CalcLegDistancesListenerDetailed;
import playground.anhorni.locationchoice.analysis.events.CalcLegTimesListenerDetailed;
import playground.anhorni.locationchoice.analysis.plans.CalculatePlanTravelStats;
import playground.anhorni.locationchoice.run.scoring.ScoreElements;
import playground.anhorni.locationchoice.run.scoring.TRBScoringFunctionFactory;

public class TRBControler {
	Controler cc ;

	public TRBControler(String[] args) {
//		super(args);
		cc = new Controler( args ) ;
		throw new RuntimeException( Gbl.SET_UP_IS_NOW_FINAL + Gbl.RETROFIT_CONTROLER ) ;
	}

	public TRBControler(Config config) {
//		super(config);
		cc = new Controler( config );
		throw new RuntimeException( Gbl.SET_UP_IS_NOW_FINAL + Gbl.RETROFIT_CONTROLER ) ;
	}

	public static void main(String[] args) {
		new TRBControler(args).run();
	}

	private void run() {
		cc.run();
	}
	
//	 protected void setUp() {
//	      super.setUp();
//	      
//	      TRBScoringFunctionFactory trbScoringFunctionFactory =
//	  			new TRBScoringFunctionFactory(this.getConfig().planCalcScore(), this);
//	  		this.setScoringFunctionFactory(trbScoringFunctionFactory);
//
//	  		this.addControlerListener(new FacilitiesLoadCalculator(((FacilityPenalties) this.getScenario().getScenarioElement(FacilityPenalties.ELEMENT_NAME)).getFacilityPenalties()));
//	  		this.addControlerListener(new ScoreElements("scoreElementsAverages.txt"));
//	  		this.addControlerListener(new CalcLegTimesListenerDetailed("calcLegTimes.txt", false));
//	  		this.addControlerListener(new CalcLegTimesListenerDetailed("calcLegTimes_wayThere.txt", true));
//	  		this.addControlerListener(new CalcLegDistancesListenerDetailed("CalcLegDistances_wayThere.txt"));
//	  		this.addControlerListener(new CalculatePlanTravelStats(true));
//	  		this.addControlerListener(new PrintShopAndLeisureLocations());
//	  		this.addControlerListener(new TravelDistanceDistribution());
//	  		super.run();
//	      
//	 }
}
