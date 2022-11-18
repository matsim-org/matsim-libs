/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.core.scoring.functions;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.testcases.MatsimTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * this class tests {@link PersonScoringParametersFromPersonAttributes}
 *
 * It checks whether the person specific income and mode constant (asc) is read from the person attributes.
 * The marginalUtilityOfMoney should be calculated as averageIncome/personSpecificIncome and not taken from the subpopulation-specific scoring params.
 * To check whether the remaining scoring params are subpopulation-specific, this class tests the person's marginalUtilityOfWaitingPt_s accordingly.
 *
 */
public class PersonScoringParametersFromPersonAttributesNoSubpopulationTest {

	@Rule
	public MatsimTestUtils utils;
	private PersonScoringParametersFromPersonAttributes personScoringParams;
	private Population population;

	@Before
	public void setUp() {
		TransitConfigGroup transitConfigGroup = new TransitConfigGroup();
		ScenarioConfigGroup scenarioConfigGroup = new ScenarioConfigGroup();
		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = new PlanCalcScoreConfigGroup();

		PlanCalcScoreConfigGroup.ScoringParameterSet personParams = planCalcScoreConfigGroup.getOrCreateScoringParameters(null);
		personParams.setMarginalUtilityOfMoney(1);
		personParams.setMarginalUtlOfWaitingPt_utils_hr(0.5 * 3600);

		PlanCalcScoreConfigGroup.ModeParams modeParamsCar = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.car);
		modeParamsCar.setConstant(-1.0);
		modeParamsCar.setMarginalUtilityOfTraveling(-0.001);
		modeParamsCar.setMarginalUtilityOfDistance(-0.002);
		modeParamsCar.setMonetaryDistanceRate(-0.003);
		modeParamsCar.setDailyMonetaryConstant(-7.5);
		modeParamsCar.setDailyUtilityConstant(-0.3);
		personParams.addModeParams(modeParamsCar);

		PlanCalcScoreConfigGroup.ModeParams modeParamsBike = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.bike);
		modeParamsBike.setConstant(-0.55);
		modeParamsBike.setMarginalUtilityOfTraveling(-0.05);
		modeParamsBike.setMarginalUtilityOfDistance(-0.003);
		modeParamsBike.setMonetaryDistanceRate(-0.002);
		personParams.addModeParams(modeParamsBike);

		population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		PopulationFactory factory = population.getFactory();

		{ //fill population
			Person negativeIncome = factory.createPerson(Id.createPersonId("negativeIncome"));
			PersonUtils.setIncome(negativeIncome, -100d);
			population.addPerson(negativeIncome);

			Person zeroIncome = factory.createPerson(Id.createPersonId("zeroIncome"));
			PersonUtils.setIncome(zeroIncome, 0d) ;
			population.addPerson(zeroIncome);

			Person lowIncomeLowCarAsc = factory.createPerson(Id.createPersonId("lowIncomeLowCarAsc"));
			PersonUtils.setIncome(lowIncomeLowCarAsc, 0.5d);
			Map<String, String> lowIncomeLowCarAscModeConstants = new HashMap<>();
			lowIncomeLowCarAscModeConstants.put(TransportMode.car, "-0.1");
			lowIncomeLowCarAscModeConstants.put(TransportMode.bike, "-100.0");
			PersonUtils.setModeConstants(lowIncomeLowCarAsc, lowIncomeLowCarAscModeConstants);
			population.addPerson(lowIncomeLowCarAsc);

			Person mediumIncomeHighCarAsc = factory.createPerson(Id.createPersonId("mediumIncomeHighCarAsc"));
			PersonUtils.setIncome(mediumIncomeHighCarAsc, 1d);
			Map<String, String> mediumIncomeHighCarAscModeConstants = new HashMap<>();
			mediumIncomeHighCarAscModeConstants.put(TransportMode.car, "-2.1");
			mediumIncomeHighCarAscModeConstants.put(TransportMode.bike, "-50.0");
			PersonUtils.setModeConstants(mediumIncomeHighCarAsc, mediumIncomeHighCarAscModeConstants);
			population.addPerson(mediumIncomeHighCarAsc);

			Person highIncomeLowCarAsc = factory.createPerson(Id.createPersonId("highIncomeLowCarAsc"));
			PersonUtils.setIncome(highIncomeLowCarAsc, 1.5d);
			Map<String, String> highIncomeLowCarAscModeConstants = new HashMap<>();
			highIncomeLowCarAscModeConstants.put(TransportMode.car, "-0.1");
			PersonUtils.setModeConstants(highIncomeLowCarAsc, highIncomeLowCarAscModeConstants);
			population.addPerson(highIncomeLowCarAsc);

		}
		personScoringParams = new PersonScoringParametersFromPersonAttributes(population,
				planCalcScoreConfigGroup,
				scenarioConfigGroup,
				transitConfigGroup);
	}

	@Test
	public void testPersonWithNegativeIncome(){
		Id<Person> id = Id.createPersonId("negativeIncome");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		//person's attribute says it has negative income which is considered invalid and therefore the subpopulation's mgnUtilityOfMoney is taken (which is 1)
		makeAssertMarginalUtilityOfMoneyAndPtWait(params, 1d, 0.5d);
	}

	@Test
	public void testPersonWithNoIncome(){
		Id<Person> id = Id.createPersonId("zeroIncome");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		//person's attribute says it has 0 income which is considered invalid and therefore the subpopulation's mgnUtilityOfMoney is taken (which is 1)
		makeAssertMarginalUtilityOfMoneyAndPtWait(params, 1d, 0.5d);
	}

	@Test
	public void testPersonWithLowIncomeLowCarAsc(){
		Id<Person> id = Id.createPersonId("lowIncomeLowCarAsc");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		makeAssertMarginalUtilityOfMoneyAndPtWait(params, 0.5d, 0.5d);
		Assert.assertEquals(-0.1d, params.modeParams.get(TransportMode.car).constant, MatsimTestUtils.EPSILON);
		Assert.assertEquals(-0.001d, params.modeParams.get(TransportMode.car).marginalUtilityOfTraveling_s, MatsimTestUtils.EPSILON);
	}

	@Test
	public void testPersonWithHighIncomeLowCarAsc(){
		Id<Person> id = Id.createPersonId("highIncomeLowCarAsc");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		makeAssertMarginalUtilityOfMoneyAndPtWait(params, 1.5d, 0.5d);
		Assert.assertEquals(-0.1d, params.modeParams.get(TransportMode.car).constant, MatsimTestUtils.EPSILON);
		Assert.assertEquals(-0.001d, params.modeParams.get(TransportMode.car).marginalUtilityOfTraveling_s, MatsimTestUtils.EPSILON);
	}

	@Test
	public void testPersonWithMediumIncomeHighCarAsc(){
		Id<Person> id = Id.createPersonId("mediumIncomeHighCarAsc");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		makeAssertMarginalUtilityOfMoneyAndPtWait(params, 1d, 0.5d);
		Assert.assertEquals(-2.1d, params.modeParams.get(TransportMode.car).constant, MatsimTestUtils.EPSILON);
		Assert.assertEquals(-50.0d, params.modeParams.get(TransportMode.bike).constant, MatsimTestUtils.EPSILON);
		Assert.assertEquals(-0.001d, params.modeParams.get(TransportMode.car).marginalUtilityOfTraveling_s, MatsimTestUtils.EPSILON);
	}

	@Test
	public void testMoneyScore(){
		ScoringParameters paramsRich = personScoringParams.getScoringParameters(population.getPersons().get(Id.createPersonId("highIncomeLowCarAsc")));
		CharyparNagelMoneyScoring moneyScoringRich = new CharyparNagelMoneyScoring(paramsRich);
		moneyScoringRich.addMoney(100);
		Assert.assertEquals("for the rich person, 100 money units should be equal to a score of 66.66", 1./1.5 * 100, moneyScoringRich.getScore(), MatsimTestUtils.EPSILON);

		ScoringParameters paramsPoor = personScoringParams.getScoringParameters(population.getPersons().get(Id.createPersonId("lowIncomeLowCarAsc")));
		CharyparNagelMoneyScoring moneyScoringPoor = new CharyparNagelMoneyScoring(paramsPoor);
		moneyScoringPoor.addMoney(100);
		Assert.assertEquals("for the poor person, 100 money units should be equal to a score of 200.00", 1./0.5 * 100, moneyScoringPoor.getScore(), MatsimTestUtils.EPSILON);

		Assert.assertTrue("100 money units should worth more for a poor person than for a rich person", moneyScoringPoor.getScore() > moneyScoringRich.getScore());
	}

	@Test
	public void testPersonSpecificAscScoring(){
		ScoringParameters paramsRich = personScoringParams.getScoringParameters(population.getPersons().get(Id.createPersonId("highIncomeLowCarAsc")));
		CharyparNagelLegScoring legScoringRichCarLeg = new CharyparNagelLegScoring(paramsRich, NetworkUtils.createNetwork(), Set.of(TransportMode.pt));
		Leg carLegZeroDistanceTenSeconds = createLeg(TransportMode.car,  0.0d, 10.0d );

		legScoringRichCarLeg.handleLeg(carLegZeroDistanceTenSeconds);
		Assert.assertEquals("for the rich person with low car asc, a 0 meter and 10s car trip should be equal to a score of ",
				-0.1d -0.001d * 10 -7.5*1./1.5 -0.3, legScoringRichCarLeg.getScore(), MatsimTestUtils.EPSILON);

		ScoringParameters paramsMediumIncomeHighCarAsc = personScoringParams.getScoringParameters(population.getPersons().get(Id.createPersonId("mediumIncomeHighCarAsc")));
		CharyparNagelLegScoring legScoringMediumIncomeHighCarAsc = new CharyparNagelLegScoring(paramsMediumIncomeHighCarAsc, NetworkUtils.createNetwork(), Set.of(TransportMode.pt));
		legScoringMediumIncomeHighCarAsc.handleLeg(carLegZeroDistanceTenSeconds);
		Assert.assertEquals("for the medium person with high car asc, a 0 meter and 10s car trip should be equal to a score of ",
				-2.1d -0.001d * 10 -7.5*1./1.0 -0.3, legScoringMediumIncomeHighCarAsc.getScore(), MatsimTestUtils.EPSILON);

		// bike has no person specific asc for high income person and is not affected
		CharyparNagelLegScoring legScoringRichBikeLeg = new CharyparNagelLegScoring(paramsRich, NetworkUtils.createNetwork(), Set.of(TransportMode.pt));
		Leg bikeLegZeroDistanceZeroSeconds = createLeg(TransportMode.bike,  0.0d, 0.0d );
		legScoringRichBikeLeg.handleLeg(bikeLegZeroDistanceZeroSeconds);
		Assert.assertEquals("for the rich person with low car asc, a 0 meter and 0s bike trip should be equal to a score of ",
				-0.55d, legScoringRichBikeLeg.getScore(), MatsimTestUtils.EPSILON);

		// bike has a person specific asc for the medium income person
		CharyparNagelLegScoring legScoringMediumIncomeBikeLeg = new CharyparNagelLegScoring(paramsMediumIncomeHighCarAsc, NetworkUtils.createNetwork(), Set.of(TransportMode.pt));
		legScoringMediumIncomeBikeLeg.handleLeg(bikeLegZeroDistanceZeroSeconds);
		Assert.assertEquals("for the medium income person with high car asc, a 0 meter and 0s bike trip should be equal to a score of ",
				-50.0d, legScoringMediumIncomeBikeLeg.getScore(), MatsimTestUtils.EPSILON);
	}

	private static Leg createLeg(String mode, double distance, double travelTime) {
		Leg leg = PopulationUtils.createLeg(mode);
		leg.setDepartureTime( 0.0d );
		Route carRouteZeroDistance = RouteUtils.createGenericRouteImpl(Id.createLinkId("dummyStart"), Id.createLinkId("dummyEnd"));
		carRouteZeroDistance.setDistance(distance);
		leg.setRoute(carRouteZeroDistance);
		leg.setTravelTime( travelTime );
		return leg;
	}

	private void makeAssertMarginalUtilityOfMoneyAndPtWait(ScoringParameters params, double income, double marginalUtilityOfWaitingPt_s){
		Assert.assertEquals("marginalUtilityOfMoney is wrong", 1 / income , params.marginalUtilityOfMoney, 0.);
		Assert.assertEquals("marginalUtilityOfWaitingPt_s is wrong", marginalUtilityOfWaitingPt_s , params.marginalUtilityOfWaitingPt_s, 0.);
	}


}