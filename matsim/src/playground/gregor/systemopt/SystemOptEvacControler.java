/* *********************************************************************** *
 * project: org.matsim.*
 * SystemOptEvacControler.java
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

package playground.gregor.systemopt;

import org.matsim.controler.Controler;
import org.matsim.evacuation.EvacuationQSimControler;
import org.matsim.trafficmonitoring.PessimisticTravelTimeAggregator;
import org.matsim.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.trafficmonitoring.TravelTimeRoleHashMap;


public class SystemOptEvacControler extends EvacuationQSimControler{

	public SystemOptEvacControler(final String[] args) {
		super(args);
	}

	@Override
	protected void setup() {
		super.setup();
		
		
		TravelTimeCalculatorFactory factory = new TravelTimeCalculatorFactory();
		factory.setTravelTimeRolePrototype(TravelTimeRoleHashMap.class);
		factory.setTravelTimeAggregatorPrototype(PessimisticTravelTimeAggregator.class);
		double endTime = this.config.simulation().getEndTime() > 0 ? this.config.simulation().getEndTime() : 30*3600;
		TravelTimeAndSocialCostCalculator t = new TravelTimeAndSocialCostCalculator(this.network,this.config.controler().getTraveltimeBinSize(),(int)endTime,factory);
		this.travelTimeCalculator = t;
		this.travelCostCalculator = new MarginalTravelCostCalculator(t);
		this.events.removeHandler(this.travelTimeCalculator);
		this.events.addHandler(t);
		this.strategyManager = loadStrategyManager();
	}

	
	public static void main(final String[] args) {
		final Controler controler = new SystemOptEvacControler(args);
		controler.run();
		System.exit(0);
	}
}
