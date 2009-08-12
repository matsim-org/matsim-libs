/* project: org.matsim.*
 * LCControler.java
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

package playground.ciarif.retailers;

import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.Facility;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.marcel.kti.router.PlansCalcRouteKti;
import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;
import playground.meisterk.kti.scoring.KTIYear3ScoringFunctionFactory;
import playground.meisterk.org.matsim.controler.listeners.CalcLegTimesKTIListener;
import playground.meisterk.org.matsim.controler.listeners.SaveRevisionInfo;
import playground.meisterk.org.matsim.controler.listeners.ScoreElements;

public class RetailersControler extends Controler {
	
	protected static final String SVN_INFO_FILE_NAME = "svninfo.txt";
	protected static final String SCORE_ELEMENTS_FILE_NAME = "scoreElementsAverages.txt";
	protected static final String CALC_LEG_TIMES_KTI_FILE_NAME = "calcLegTimesKTI.txt";
	private PlansCalcRouteKtiInfo plansCalcRouteKtiInfo = null;
	private final KtiConfigGroup ktiConfigGroup;
	
	public RetailersControler(String[] args) {
		super(args);
		this.ktiConfigGroup = new KtiConfigGroup();
		super.config.addModule(KtiConfigGroup.GROUP_NAME, this.ktiConfigGroup);
	}

	@Override
	protected void shutdown(final boolean unexpected) {
		super.shutdown(unexpected);
	}
	
	/*@Override
	protected void setUp() {

		KTIYear3ScoringFunctionFactory kTIYear3ScoringFunctionFactory = new KTIYear3ScoringFunctionFactory(
				super.config.charyparNagelScoring(), 
				this.getFacilityPenalties(),
				this.ktiConfigGroup);
		this.setScoringFunctionFactory(kTIYear3ScoringFunctionFactory);

		if (this.ktiConfigGroup.isUsePlansCalcRouteKti()) {
			this.plansCalcRouteKtiInfo = new PlansCalcRouteKtiInfo();
			this.plansCalcRouteKtiInfo.prepare(this.ktiConfigGroup, this.getNetwork());
		}
		
		super.setUp();
	}*/

	@Override
	protected void loadControlerListeners() {
		
		super.loadControlerListeners();
		
		// the scoring function processes facility loads
		/*this.addControlerListener(new FacilitiesLoadCalculator(this.getFacilityPenalties()));
		this.addControlerListener(new ScoreElements(SCORE_ELEMENTS_FILE_NAME));
		this.addControlerListener(new CalcLegTimesKTIListener(CALC_LEG_TIMES_KTI_FILE_NAME));
		this.addControlerListener(new SaveRevisionInfo(SVN_INFO_FILE_NAME));*/
		this.addControlerListener(new RetailersLocationListener());
		
	}
	
	/*@Override
	public PlanAlgorithm getRoutingAlgorithm(final TravelCost travelCosts, final TravelTime travelTimes) {

		PlanAlgorithm router = null;

		if (!this.ktiConfigGroup.isUsePlansCalcRouteKti()) {
			router = super.getRoutingAlgorithm(travelCosts, travelTimes);
		} else {

			router = new PlansCalcRouteKti(
					super.getConfig().plansCalcRoute(), 
					super.network, 
					travelCosts, 
					travelTimes, 
					super.getLeastCostPathCalculatorFactory(), 
					this.plansCalcRouteKtiInfo);

		}

		return router;
	}*/
	
	
    public static void main (final String[] args) { 
    	Controler controler = new RetailersControler(args);
    	//controler.addControlerListener(new RetailersLocationListener());
    	//controler.addControlerListener(new RetailersParallelLocationListener());
    	//controler.addControlerListener(new RetailersSequentialLocationListener()); 

		//controler.addControlerListener(new ScoreElements(SCORE_ELEMENTS_FILE_NAME));
    	//controler.addControlerListener(new FacilitiesLoadCalculator(controler.getFacilityPenalties()));
    	controler.run();
    }
}
