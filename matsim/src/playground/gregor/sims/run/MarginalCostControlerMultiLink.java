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

import playground.gregor.sims.socialcost.MarginalTravelCostCalculatorII;
import playground.gregor.sims.socialcost.MarginalTravelCostCalculatorIII;
import playground.gregor.sims.socialcost.SocialCostCalculator;
import playground.gregor.sims.socialcost.SocialCostCalculatorMultiLink;
import playground.gregor.sims.socialcost.SocialCostCalculatorMultiLinkII;

public class MarginalCostControlerMultiLink extends Controler{

	public static double QUICKnDIRTY;

	public MarginalCostControlerMultiLink(final String[] args) {
		super(args);
	}

	@Override
	protected void setUp() {
		super.setUp();
		
		
		TravelTimeAggregatorFactory factory = new TravelTimeAggregatorFactory();
		factory.setTravelTimeDataPrototype(TravelTimeDataHashMap.class);
		factory.setTravelTimeAggregatorPrototype(PessimisticTravelTimeAggregator.class);
		SocialCostCalculator sc = new SocialCostCalculatorMultiLinkII(this.network,this.config.travelTimeCalculator().getTraveltimeBinSize(), this.travelTimeCalculator, this.population);
		
		this.events.addHandler(sc);
		this.getQueueSimulationListener().add(sc);
		this.travelCostCalculator = new MarginalTravelCostCalculatorIII(this.travelTimeCalculator,sc,this.config.travelTimeCalculator().getTraveltimeBinSize());
		this.strategyManager = loadStrategyManager();
		this.addControlerListener(sc);
	}

	public static void main(final String[] args) {
		QUICKnDIRTY = Double.parseDouble(args[1]);
		System.out.println("DISCOUNT:" + QUICKnDIRTY);
		String [] args2 = {args[0]}; 
		final Controler controler = new MarginalCostControlerMultiLink(args2);
		controler.run();
		System.exit(0);
	}
}