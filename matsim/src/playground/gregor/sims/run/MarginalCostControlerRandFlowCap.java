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

import org.matsim.core.controler.Controler;
import org.matsim.core.trafficmonitoring.PessimisticTravelTimeAggregator;
import org.matsim.core.trafficmonitoring.TravelTimeAggregatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeDataHashMap;

import playground.gregor.sims.socialcost.LinkRandomizer;
import playground.gregor.sims.socialcost.MarginalTravelCostCalculatorII;
import playground.gregor.sims.socialcost.SocialCostCalculator;
import playground.gregor.sims.socialcost.SocialCostCalculatorSingleLink;

public class MarginalCostControlerRandFlowCap extends Controler{



	public MarginalCostControlerRandFlowCap(final String[] args) {
		super(args);
		
	}

	@Override
	protected void setUp() {
		super.setUp();
		
		
		LinkRandomizer lr = new LinkRandomizer(this.network);
		
		TravelTimeAggregatorFactory factory = new TravelTimeAggregatorFactory();
		factory.setTravelTimeDataPrototype(TravelTimeDataHashMap.class);
		factory.setTravelTimeAggregatorPrototype(PessimisticTravelTimeAggregator.class);
		SocialCostCalculator sc = new SocialCostCalculatorSingleLink(this.network,this.config.travelTimeCalculator().getTraveltimeBinSize());
		
		this.events.addHandler(sc);
		this.travelCostCalculator = new MarginalTravelCostCalculatorII(this.travelTimeCalculator,sc);
		this.strategyManager = loadStrategyManager();
		this.addControlerListener(sc);
		this.addControlerListener(lr);
	}

	public static void main(final String[] args) {
		final Controler controler = new MarginalCostControlerRandFlowCap(args);
		controler.setOverwriteFiles(true);
		controler.run();
		System.exit(0);
	}
}