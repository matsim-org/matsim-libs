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

package playground.benjamin.scoring.income;

/**
 * @author dgrether
 *
 */

//public class IncomeScoringFunctionFactory implements ScoringFunctionFactory {
//
//	private Config config;
//	private PlanCalcScoreConfigGroup configGroup;
//	private CharyparNagelScoringParameters params;
//	private PersonHouseholdMapping hhdb;
//	private final Network network;
//
//	public IncomeScoringFunctionFactory(Config config, PersonHouseholdMapping hhmapping, Network network) {
//		this.config = config;
//		this.configGroup = config.planCalcScore();
//		this.params = CharyparNagelScoringParameters.getBuilder(configGroup, config.scenario()).create();
//		this.hhdb = hhmapping;
//		this.network = network;
//	}
//
//	@Override
//	public ScoringFunction createNewScoringFunction(Person person) {
//
//		double householdIncomePerDay = getHouseholdIncomePerDay(person, hhdb);
//
//		//summing up all relevant ulitlites
//		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
//
//		//utility earned from daily income
//		scoringFunctionAccumulator.addScoringFunction(new ScoringFromDailyIncome(householdIncomePerDay));
//
//		//utility earned from activities
//		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params));
//
//		//utility spend for traveling (in this case: travel time and distance costs)
//		scoringFunctionAccumulator.addScoringFunction(new ScoringFromLeg(person.getSelectedPlan(), params, this.network, householdIncomePerDay ));
//
//		//utility spend for traveling (toll costs) if there is a toll
////        if(ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).isUsingRoadpricing()){
//		Logger.getLogger(this.getClass()).fatal("the above functionality does no longer exist; pls tlk to me if you need it. kai, sep'14") ;
//		System.exit(-1) ;
//		if ( true ) {
//			scoringFunctionAccumulator.addScoringFunction(new ScoringFromToll(params, householdIncomePerDay));
//		}
//
//		//utility spend for being stuck
//		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
//
//		return scoringFunctionAccumulator;
//
//	}
//
//	private double getHouseholdIncomePerDay(Person person, PersonHouseholdMapping hhdb) {
//		Income income = hhdb.getHousehold(person.getId()).getIncome();
//		double incomePerDay = this.calculateIncomePerDay(income);
//		if (Double.isNaN(incomePerDay)){
//			throw new IllegalStateException("cannot calculate income for person: " + person.getId());
//		}
//		return incomePerDay;
//	}
//
//	private double calculateIncomePerDay(Income income) {
//		if (income.getIncomePeriod().equals(IncomePeriod.year)) {
//			double incomePerDay = income.getIncome() / 240;
//			return incomePerDay;
//		} else {
//			throw new UnsupportedOperationException("Can't calculate income per day");
//		}
//	}
//
//}
