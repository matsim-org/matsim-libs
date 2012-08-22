/* *********************************************************************** *
 * project: org.matsim.*
 * SoController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor;

import org.matsim.contrib.evacuation.config.EvacuationConfigGroup;
import org.matsim.contrib.evacuation.socialcost.SocialCostCalculatorSingleLinkII;
import org.matsim.contrib.evacuation.travelcosts.PluggableTravelCostCalculator;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class SoController extends Controler {
	
	PluggableTravelCostCalculator pluggableTravelCost = null;

	public SoController(String[] args) {
		super(args);
		// TODO Auto-generated constructor stub
	}
	
	

	@Override
	protected void setUp() {
		super.setUp();
		initSocialCostOptimization();
	}
	
	private void initSocialCostOptimization() {
		
		Module m = new Module("evacuation");
		EvacuationConfigGroup ecg = new EvacuationConfigGroup(m);
		this.config.addModule("evacuation", ecg);
		ecg.setMSAOffset("20");
		
		initPluggableTravelCostCalculator();
		SocialCostCalculatorSingleLinkII sc = new SocialCostCalculatorSingleLinkII(this.scenarioData, getEvents());
		this.pluggableTravelCost.addTravelCost(sc);
		this.events.addHandler(sc);
		this.strategyManager = loadStrategyManager();
		addControlerListener(sc);
	}
	private void initPluggableTravelCostCalculator() {
		if (this.pluggableTravelCost == null) {
			if (this.travelTimeCalculator == null) {
				this.travelTimeCalculator = getTravelTimeCalculatorFactory().createTravelTimeCalculator(this.network, this.config.travelTimeCalculator());
			}
			this.pluggableTravelCost = new PluggableTravelCostCalculator(this.travelTimeCalculator);
			setTravelDisutilityFactory(new TravelDisutilityFactory() {

				// This is thread-safe because pluggableTravelCost is
				// thread-safe.

				@Override
				public TravelDisutility createTravelDisutility(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
					return SoController.this.pluggableTravelCost;
				}

			});
		}
	}

	public static void main(String [] args) {
		final Controler controler = new SoController(args);
		controler.run();
		System.exit(0);
	}
}
