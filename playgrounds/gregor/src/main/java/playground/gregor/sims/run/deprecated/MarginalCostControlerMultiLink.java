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

package playground.gregor.sims.run.deprecated;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;

import playground.gregor.sims.socialcostII.MarginalTravelCostCalculatorIII;
import playground.gregor.sims.socialcostII.SocialCostCalculatorMultiLink;

@Deprecated
public class MarginalCostControlerMultiLink extends Controler {

	public static double QUICKnDIRTY;

	public MarginalCostControlerMultiLink(final String[] args) {
		super(args);
	}

	@Override
	protected void setUp() {
		super.setUp();

		// TravelTimeAggregatorFactory factory = new
		// TravelTimeAggregatorFactory();
		// factory.setTravelTimeDataPrototype(TravelTimeDataHashMap.class);
		// factory.setTravelTimeAggregatorPrototype(PessimisticTravelTimeAggregator.class);
		final SocialCostCalculatorMultiLink sc = new SocialCostCalculatorMultiLink(this.network, this.config.travelTimeCalculator().getTraveltimeBinSize(), this.travelTimeCalculator, this.population, this.events);

		this.events.addHandler(sc);
		getQueueSimulationListener().add(sc);
		setTravelCostCalculatorFactory(new TravelCostCalculatorFactory() {

			@Override
			public PersonalizableTravelCost createTravelCostCalculator(PersonalizableTravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
				return new MarginalTravelCostCalculatorIII(MarginalCostControlerMultiLink.this.travelTimeCalculator, sc, MarginalCostControlerMultiLink.this.config.travelTimeCalculator().getTraveltimeBinSize());
			}

		});
		this.strategyManager = loadStrategyManager();
		addControlerListener(sc);
	}

	public static void main(final String[] args) {
		QUICKnDIRTY = Double.parseDouble(args[1]);
		System.out.println("DISCOUNT:" + QUICKnDIRTY);
		String[] args2 = { args[0] };
		final Controler controler = new MarginalCostControlerMultiLink(args2);
		controler.setOverwriteFiles(true);
		controler.run();
		System.exit(0);
	}
}