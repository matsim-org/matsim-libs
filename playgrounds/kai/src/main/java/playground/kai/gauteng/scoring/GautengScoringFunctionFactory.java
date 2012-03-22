/* *********************************************************************** *
 * project: org.matsim.*												   *
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

package playground.kai.gauteng.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;
import org.matsim.core.scoring.interfaces.BasicScoring;
import org.matsim.core.scoring.interfaces.MoneyScoring;

import playground.kai.gauteng.roadpricingscheme.SanralTollFactor;

/**
 * @author nagel after
 * @author kickhoefer after
 * @author dgrether
 */
public class GautengScoringFunctionFactory implements ScoringFunctionFactory {

	private Config config;
	private PlanCalcScoreConfigGroup configGroup;
	private CharyparNagelScoringParameters params;
	private final Network network;

	public GautengScoringFunctionFactory(Config config, Network network) {
		this.config = config;
		this.configGroup = config.planCalcScore();
		this.params = new CharyparNagelScoringParameters(configGroup);
		this.network = network;
	}

	public ScoringFunction createNewScoringFunction(Plan plan) {
		// Design comment: This is the only place where the person is available (via plan.getPerson()).  Thus, all 
		// person-specific scoring actions need to be injected from here. kai, mar'12

		//summing up all relevant ulitlites
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		//utility earned from activities
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.charyparNagel.ActivityScoringFunction(params));

		//utility spend for traveling (in this case: travel time and distance costs)
		scoringFunctionAccumulator.addScoringFunction(new LegScoringFunction(params, network));
		// yy careful: The standard leg scoring function assumes uniform utilities of time.  If this does not hold, the leg
		// scoring function needs to be replaced.  (But then, presumably, also the activity scoring function
		// needs to be replaced.)  kai, mar'12

		//utility spend for traveling (toll costs) if there is a toll
		scoringFunctionAccumulator.addScoringFunction(new ScoringFromToll( plan.getPerson().getId(), this.configGroup ) ) ;
		
		//utility spend for being stuck
		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction(params));

		return scoringFunctionAccumulator;

	}

}

/**
 * Notes:<ul>
 * <li> This class is only supposed to work as expected if there are NO OTHER money events than those from road pricing!
 * [[I don't see why that should be so.  Only problem: The logging statement would be wrong. kai, mar'12]]
 * <li> One may be tempted to include this into the standard Charypar Nagel scoring function.  However, it is currently 
 * not possible/not useful to have person-specific values of money in that scoring function.  So it is better to leave it here.
 * kai, mar'12
 * </ul>
 * 
 * @see http://www.matsim.org/node/263
 * @author bkick and michaz after rashid_waraich
 */
class ScoringFromToll implements MoneyScoring, BasicScoring {
	final static private Logger log = Logger.getLogger(ScoringFromToll.class);

	private double score = 0.0;
	private Id vehicleId ;
	private PlanCalcScoreConfigGroup cnScoringGroup ;
	
	ScoringFromToll( Id vehicleId, PlanCalcScoreConfigGroup cnScoringGroup ) {
		this.vehicleId = vehicleId ;
		this.cnScoringGroup = cnScoringGroup ;
	}
	
	@Override
	public void reset() {
		
	}

	@Override
	public void addMoney(final double amount) {
		
		this.score += - SanralTollFactor.getUtilityOfMoney(vehicleId, cnScoringGroup) * amount;
		log.info("toll paid: " + amount + "; resulting accumulated toll utility: " + this.score );
		log.error("utility of money == one for everybody; needs to be fixed ...") ;

	}

	@Override
	public void finish() {

	}
	
	@Override
	public double getScore() {
		return this.score;
	}

}

