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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
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
public class PersonScoringParametersFromPersonAttributesTest {

	private PersonScoringParametersFromPersonAttributes personScoringParams;
	private Population population;

	@BeforeEach
	public void setUp() {
		TransitConfigGroup transitConfigGroup = new TransitConfigGroup();
		ScenarioConfigGroup scenarioConfigGroup = new ScenarioConfigGroup();
		ScoringConfigGroup scoringConfigGroup = new ScoringConfigGroup();

		ScoringConfigGroup.ScoringParameterSet personParams = scoringConfigGroup.getOrCreateScoringParameters("person");
		personParams.setMarginalUtilityOfMoney(1);
		personParams.setMarginalUtlOfWaitingPt_utils_hr(0.5 * 3600);

		ScoringConfigGroup.ModeParams modeParamsCar = new ScoringConfigGroup.ModeParams(TransportMode.car);
		modeParamsCar.setConstant(-1.0);
		modeParamsCar.setMarginalUtilityOfTraveling(-0.001);
		modeParamsCar.setMarginalUtilityOfDistance(-0.002);
		modeParamsCar.setMonetaryDistanceRate(-0.003);
		modeParamsCar.setDailyMonetaryConstant(-7.5);
		modeParamsCar.setDailyUtilityConstant(-0.3);
		personParams.addModeParams(modeParamsCar);

		ScoringConfigGroup.ModeParams modeParamsBike = new ScoringConfigGroup.ModeParams(TransportMode.bike);
		modeParamsBike.setConstant(-0.55);
		modeParamsBike.setMarginalUtilityOfTraveling(-0.05);
		modeParamsBike.setMarginalUtilityOfDistance(-0.003);
		modeParamsBike.setMonetaryDistanceRate(-0.002);
		personParams.addModeParams(modeParamsBike);

		ScoringConfigGroup.ScoringParameterSet freightParams = scoringConfigGroup.getOrCreateScoringParameters("freight");
		freightParams.setMarginalUtilityOfMoney(444);
		freightParams.setMarginalUtlOfWaitingPt_utils_hr(1d * 3600);

		population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		PopulationFactory factory = population.getFactory();

		{ //fill population
			Person negativeIncome = factory.createPerson(Id.createPersonId("negativeIncome"));
			PopulationUtils.putSubpopulation(negativeIncome, "person");
			PersonUtils.setIncome(negativeIncome, -100d);
			population.addPerson(negativeIncome);

			Person zeroIncome = factory.createPerson(Id.createPersonId("zeroIncome"));
			PopulationUtils.putSubpopulation(zeroIncome, "person");
			PersonUtils.setIncome(zeroIncome, 0d) ;
			population.addPerson(zeroIncome);

			Person lowIncomeLowCarAsc = factory.createPerson(Id.createPersonId("lowIncomeLowCarAsc"));
			PopulationUtils.putSubpopulation(lowIncomeLowCarAsc, "person");
			PersonUtils.setIncome(lowIncomeLowCarAsc, 0.5d);
			Map<String, String> lowIncomeLowCarAscModeConstants = new HashMap<>();
			lowIncomeLowCarAscModeConstants.put(TransportMode.car, "-0.1");
			lowIncomeLowCarAscModeConstants.put(TransportMode.bike, "-100.0");
			PersonUtils.setModeConstants(lowIncomeLowCarAsc, lowIncomeLowCarAscModeConstants);
			population.addPerson(lowIncomeLowCarAsc);

			Person mediumIncomeHighCarAsc = factory.createPerson(Id.createPersonId("mediumIncomeHighCarAsc"));
			PopulationUtils.putSubpopulation(mediumIncomeHighCarAsc, "person");
			PersonUtils.setIncome(mediumIncomeHighCarAsc, 1d);
			Map<String, String> mediumIncomeHighCarAscModeConstants = new HashMap<>();
			mediumIncomeHighCarAscModeConstants.put(TransportMode.car, "-2.1");
			mediumIncomeHighCarAscModeConstants.put(TransportMode.bike, "-50.0");
			PersonUtils.setModeConstants(mediumIncomeHighCarAsc, mediumIncomeHighCarAscModeConstants);
			population.addPerson(mediumIncomeHighCarAsc);

			Person highIncomeLowCarAsc = factory.createPerson(Id.createPersonId("highIncomeLowCarAsc"));
			PopulationUtils.putSubpopulation(highIncomeLowCarAsc, "person");
			PersonUtils.setIncome(highIncomeLowCarAsc, 1.5d);
			Map<String, String> highIncomeLowCarAscModeConstants = new HashMap<>();
			highIncomeLowCarAscModeConstants.put(TransportMode.car, "-0.1");
			PersonUtils.setModeConstants(highIncomeLowCarAsc, highIncomeLowCarAscModeConstants);
			population.addPerson(highIncomeLowCarAsc);

			Person freight = factory.createPerson(Id.createPersonId("freight"));
			PopulationUtils.putSubpopulation(freight, "freight");
			population.addPerson(freight);

			Person freightWithIncome1 = factory.createPerson(Id.createPersonId("freightWithIncome1"));
			PopulationUtils.putSubpopulation(freightWithIncome1, "freight");
			PersonUtils.setIncome(freightWithIncome1, 1.5d);
			population.addPerson(freightWithIncome1);

			Person freightWithIncome2 = factory.createPerson(Id.createPersonId("freightWithIncome2"));
			PopulationUtils.putSubpopulation(freightWithIncome2, "freight");
			PersonUtils.setIncome(freightWithIncome2, 0.5d);
			population.addPerson(freightWithIncome2);
		}
		personScoringParams = new PersonScoringParametersFromPersonAttributes(population,
			scoringConfigGroup,
				scenarioConfigGroup,
				transitConfigGroup);
	}

	@Test
	void testPersonWithNegativeIncome(){
		Id<Person> id = Id.createPersonId("negativeIncome");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		//person's attribute says it has negative income which is considered invalid and therefore the subpopulation's mgnUtilityOfMoney is taken (which is 1)
		makeAssertMarginalUtilityOfMoneyAndPtWait(params, 1d, 0.5d);
	}

	@Test
	void testPersonWithNoIncome(){
		Id<Person> id = Id.createPersonId("zeroIncome");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		//person's attribute says it has 0 income which is considered invalid and therefore the subpopulation's mgnUtilityOfMoney is taken (which is 1)
		makeAssertMarginalUtilityOfMoneyAndPtWait(params, 1d, 0.5d);
	}

	@Test
	void testPersonWithLowIncomeLowCarAsc(){
		Id<Person> id = Id.createPersonId("lowIncomeLowCarAsc");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		makeAssertMarginalUtilityOfMoneyAndPtWait(params, 0.5d, 0.5d);
		Assertions.assertEquals(-1.0d -0.1d, params.modeParams.get(TransportMode.car).constant, MatsimTestUtils.EPSILON);
		Assertions.assertEquals(-0.001d, params.modeParams.get(TransportMode.car).marginalUtilityOfTraveling_s, MatsimTestUtils.EPSILON);
	}

	@Test
	void testPersonWithHighIncomeLowCarAsc(){
		Id<Person> id = Id.createPersonId("highIncomeLowCarAsc");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		makeAssertMarginalUtilityOfMoneyAndPtWait(params, 1.5d, 0.5d);
		Assertions.assertEquals(-1.0d -0.1d, params.modeParams.get(TransportMode.car).constant, MatsimTestUtils.EPSILON);
		Assertions.assertEquals(-0.001d, params.modeParams.get(TransportMode.car).marginalUtilityOfTraveling_s, MatsimTestUtils.EPSILON);
	}

	@Test
	void testPersonWithMediumIncomeHighCarAsc(){
		Id<Person> id = Id.createPersonId("mediumIncomeHighCarAsc");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		makeAssertMarginalUtilityOfMoneyAndPtWait(params, 1d, 0.5d);
		Assertions.assertEquals(-1.0d -2.1d, params.modeParams.get(TransportMode.car).constant, MatsimTestUtils.EPSILON);
		Assertions.assertEquals(-0.55d -50.0d, params.modeParams.get(TransportMode.bike).constant, MatsimTestUtils.EPSILON);
		Assertions.assertEquals(-0.001d, params.modeParams.get(TransportMode.car).marginalUtilityOfTraveling_s, MatsimTestUtils.EPSILON);
	}

	@Test
	void testPersonFreight(){
		Id<Person> id = Id.createPersonId("freight");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		//freight agent has no income attribute set, so it should use the marginal utility of money that is set in its subpopulation scoring parameters!
		makeAssertMarginalUtilityOfMoneyAndPtWait(params, 1d/444d, 1d);
	}

	@Test
	void testFreightWithIncome(){
		Id<Person> id = Id.createPersonId("freightWithIncome1");
		ScoringParameters params = personScoringParams.getScoringParameters(population.getPersons().get(id));
		makeAssertMarginalUtilityOfMoneyAndPtWait(params, 1.5/444d, 1d);
		Id<Person> id2 = Id.createPersonId("freightWithIncome2");
		ScoringParameters params2 = personScoringParams.getScoringParameters(population.getPersons().get(id2));
		makeAssertMarginalUtilityOfMoneyAndPtWait(params2, 0.5/444d, 1d);
	}

	@Test
	void testMoneyScore(){
		ScoringParameters paramsRich = personScoringParams.getScoringParameters(population.getPersons().get(Id.createPersonId("highIncomeLowCarAsc")));
		CharyparNagelMoneyScoring moneyScoringRich = new CharyparNagelMoneyScoring(paramsRich);
		moneyScoringRich.addMoney(100);
		Assertions.assertEquals(1./1.5 * 100, moneyScoringRich.getScore(), MatsimTestUtils.EPSILON, "for the rich person, 100 money units should be equal to a score of 66.66");

		ScoringParameters paramsPoor = personScoringParams.getScoringParameters(population.getPersons().get(Id.createPersonId("lowIncomeLowCarAsc")));
		CharyparNagelMoneyScoring moneyScoringPoor = new CharyparNagelMoneyScoring(paramsPoor);
		moneyScoringPoor.addMoney(100);
		Assertions.assertEquals(1./0.5 * 100, moneyScoringPoor.getScore(), MatsimTestUtils.EPSILON, "for the poor person, 100 money units should be equal to a score of 200.00");

		Assertions.assertTrue(moneyScoringPoor.getScore() > moneyScoringRich.getScore(), "100 money units should worth more for a poor person than for a rich person");
	}

	@Test
	void testPersonSpecificAscScoring(){
		ScoringParameters paramsRich = personScoringParams.getScoringParameters(population.getPersons().get(Id.createPersonId("highIncomeLowCarAsc")));
		CharyparNagelLegScoring legScoringRichCarLeg = new CharyparNagelLegScoring(paramsRich, NetworkUtils.createNetwork(), Set.of(TransportMode.pt));
		Leg carLegZeroDistanceTenSeconds = createLeg(TransportMode.car,  0.0d, 10.0d );

		legScoringRichCarLeg.handleLeg(carLegZeroDistanceTenSeconds);
		Assertions.assertEquals(-1.0d -0.1d -0.001d * 10 -7.5*1./1.5 -0.3, legScoringRichCarLeg.getScore(), MatsimTestUtils.EPSILON, "for the rich person with low car asc, a 0 meter and 10s car trip should be equal to a score of ");

		ScoringParameters paramsMediumIncomeHighCarAsc = personScoringParams.getScoringParameters(population.getPersons().get(Id.createPersonId("mediumIncomeHighCarAsc")));
		CharyparNagelLegScoring legScoringMediumIncomeHighCarAsc = new CharyparNagelLegScoring(paramsMediumIncomeHighCarAsc, NetworkUtils.createNetwork(), Set.of(TransportMode.pt));
		legScoringMediumIncomeHighCarAsc.handleLeg(carLegZeroDistanceTenSeconds);
		Assertions.assertEquals(-1.0d -2.1d -0.001d * 10 -7.5*1./1.0 -0.3, legScoringMediumIncomeHighCarAsc.getScore(), MatsimTestUtils.EPSILON, "for the medium person with high car asc, a 0 meter and 10s car trip should be equal to a score of ");

		// bike has no person specific asc for high income person and is not affected
		CharyparNagelLegScoring legScoringRichBikeLeg = new CharyparNagelLegScoring(paramsRich, NetworkUtils.createNetwork(), Set.of(TransportMode.pt));
		Leg bikeLegZeroDistanceZeroSeconds = createLeg(TransportMode.bike,  0.0d, 0.0d );
		legScoringRichBikeLeg.handleLeg(bikeLegZeroDistanceZeroSeconds);
		Assertions.assertEquals(-0.55d, legScoringRichBikeLeg.getScore(), MatsimTestUtils.EPSILON, "for the rich person with low car asc, a 0 meter and 0s bike trip should be equal to a score of ");

		// bike has a person specific asc for the medium income person
		CharyparNagelLegScoring legScoringMediumIncomeBikeLeg = new CharyparNagelLegScoring(paramsMediumIncomeHighCarAsc, NetworkUtils.createNetwork(), Set.of(TransportMode.pt));
		legScoringMediumIncomeBikeLeg.handleLeg(bikeLegZeroDistanceZeroSeconds);
		Assertions.assertEquals(-0.55d -50.0d, legScoringMediumIncomeBikeLeg.getScore(), MatsimTestUtils.EPSILON, "for the medium income person with high car asc, a 0 meter and 0s bike trip should be equal to a score of ");
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
		Assertions.assertEquals(1 / income , params.marginalUtilityOfMoney, 0., "marginalUtilityOfMoney is wrong");
		Assertions.assertEquals(marginalUtilityOfWaitingPt_s , params.marginalUtilityOfWaitingPt_s, 0., "marginalUtilityOfWaitingPt_s is wrong");
	}


}
