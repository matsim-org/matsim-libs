/* *********************************************************************** *
 * project: org.matsim.*
 * MarginalCostControler.java
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

package playground.gregor.sims.run;

import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.evacuation.socialcost.MarginalTravelCostCalculatorII;
import org.matsim.evacuation.socialcost.SocialCostCalculatorSingleLink;



public class MarginalCostControlerRandFlowCap extends Controler{



	private final double c;

	public MarginalCostControlerRandFlowCap(final String[] args, double c) {
		super(args);
		this.c = c;
		
	}

	@Override
	protected void setUp() {
		super.setUp();
		
		
//		LinkFlowCapRandomizer lr = new LinkFlowCapRandomizer(this.network, this.c, 0.);
		
//		TravelTimeAggregatorFactory factory = new TravelTimeAggregatorFactory();
//		factory.setTravelTimeDataPrototype(TravelTimeDataHashMap.class);
//		factory.setTravelTimeAggregatorPrototype(PessimisticTravelTimeAggregator.class);
		final SocialCostCalculatorSingleLink sc = new SocialCostCalculatorSingleLink(this.network,this.config.travelTimeCalculator().getTraveltimeBinSize(), getEvents());
		
		this.events.addHandler(sc);
		// this.setTravelCostCalculator(new MarginalTravelCostCalculatorII(this.travelTimeCalculator,sc));
		this.setTravelCostCalculatorFactory(new TravelCostCalculatorFactory() {

			@Override
			public PersonalizableTravelCost createTravelCostCalculator(
					TravelTime timeCalculator,
					CharyparNagelScoringConfigGroup cnScoringGroup) {
				return new MarginalTravelCostCalculatorII(MarginalCostControlerRandFlowCap.this.getTravelTimeCalculator(),sc);
			}
			
		});
		this.strategyManager = loadStrategyManager();
		this.addControlerListener(sc);
//		this.addControlerListener(lr);
	}

	public static void main(final String[] args) {
		double c = Double.parseDouble(args[1]);
		final Controler controler = new MarginalCostControlerRandFlowCap(new String [] {args[0]}, c);
		controler.setOverwriteFiles(true);
		controler.run();
		System.exit(0);
	}
}