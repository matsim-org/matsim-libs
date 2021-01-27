/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

package playground.vsp.scoring;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.core.config.*;
import org.matsim.testcases.MatsimTestUtils;


import static playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters.PERSONAL_INCOME_ATTRIBUTE_NAME;

public class TestInstances {

	@Rule
	public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

	private static Population population;

	@Test
	public void testSameInstances(){

		final Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"),
				"config.xml"));
		config.controler().setLastIteration(2);
		config.controler().setOutputDirectory(matsimTestUtils.getOutputDirectory());

//		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = config.planCalcScore();
//
//		PlanCalcScoreConfigGroup.ScoringParameterSet personParams = planCalcScoreConfigGroup.getOrCreateScoringParameters("person");
//		personParams.setMarginalUtilityOfMoney(1);
//		personParams.setMarginalUtlOfWaitingPt_utils_hr(0.5 * 3600);
//
//		PlanCalcScoreConfigGroup.ScoringParameterSet freightParams = planCalcScoreConfigGroup.getOrCreateScoringParameters("freight");
//		freightParams.setMarginalUtilityOfMoney(444);
//		freightParams.setMarginalUtlOfWaitingPt_utils_hr(1d * 3600);
//
//		PlanCalcScoreConfigGroup.ScoringParameterSet defaultParams = planCalcScoreConfigGroup.getOrCreateScoringParameters("default");
//		defaultParams.setMarginalUtilityOfMoney(100000);
//		defaultParams.setMarginalUtlOfWaitingPt_utils_hr(5000d * 3600d);
//
		Scenario scenario = ScenarioUtils.loadScenario(config);

		scenario.getPopulation().getPersons().values().stream()
				.forEach(person -> PopulationUtils.putPersonAttribute(person, PERSONAL_INCOME_ATTRIBUTE_NAME, 100d));

//		population = scenario.getPopulation();
//
//		PopulationFactory factory = population.getFactory();
//
//		{ //fill population
//			Person negativeIncome = factory.createPerson(Id.createPersonId("negativeIncome"));
//			PopulationUtils.putSubpopulation(negativeIncome, "person");
//			PopulationUtils.putPersonAttribute(negativeIncome, PERSONAL_INCOME_ATTRIBUTE_NAME, -100d);
//			population.addPerson(negativeIncome);
//
//			Person zeroIncome = factory.createPerson(Id.createPersonId("zeroIncome"));
//			PopulationUtils.putSubpopulation(zeroIncome, "person");
//			PopulationUtils.putPersonAttribute(zeroIncome, PERSONAL_INCOME_ATTRIBUTE_NAME, 0d);
//			population.addPerson(zeroIncome);
//
//			Person lowIncome = factory.createPerson(Id.createPersonId("lowIncome"));
//			PopulationUtils.putSubpopulation(lowIncome, "person");
//			PopulationUtils.putPersonAttribute(lowIncome, PERSONAL_INCOME_ATTRIBUTE_NAME, 0.5d);
//			population.addPerson(lowIncome);
//
//			Person mediumIncome = factory.createPerson(Id.createPersonId("mediumIncome"));
//			PopulationUtils.putSubpopulation(mediumIncome, "person");
//			PopulationUtils.putPersonAttribute(mediumIncome, PERSONAL_INCOME_ATTRIBUTE_NAME, 1d);
//			population.addPerson(mediumIncome);
//
//			Person highIncome = factory.createPerson(Id.createPersonId("highIncome"));
//			PopulationUtils.putSubpopulation(highIncome, "person");
//			PopulationUtils.putPersonAttribute(highIncome, PERSONAL_INCOME_ATTRIBUTE_NAME, 1.5d);
//			population.addPerson(highIncome);
//
//			Person freight = factory.createPerson(Id.createPersonId("freight"));
//			PopulationUtils.putSubpopulation(freight, "freight");
//			population.addPerson(freight);
//
//			Person freightWithIncome1 = factory.createPerson(Id.createPersonId("freightWithIncome1"));
//			PopulationUtils.putSubpopulation(freightWithIncome1, "freight");
//			PopulationUtils.putPersonAttribute(freightWithIncome1, PERSONAL_INCOME_ATTRIBUTE_NAME, 1.5d);
//			population.addPerson(freightWithIncome1);
//
//			Person freightWithIncome2 = factory.createPerson(Id.createPersonId("freightWithIncome2"));
//			PopulationUtils.putSubpopulation(freightWithIncome2, "freight");
//			PopulationUtils.putPersonAttribute(freightWithIncome2, PERSONAL_INCOME_ATTRIBUTE_NAME, 0.5d);
//			population.addPerson(freightWithIncome2);
//		}

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class).in(Singleton.class);
				addControlerListenerBinding().toInstance(new InstanceHandler());
			}
		});

		controler.run();
	}


	class InstanceHandler implements IterationStartsListener{

		@Inject ScoringParametersForPerson scoringParametersForPerson;
		@Inject
		ScoringFunctionFactory scoringFunctionFactory;

		Logger logger = Logger.getLogger(InstanceHandler.class);
		private ScoringParametersForPerson firstIterationScoringParametersForPerson;
		private ScoringFunctionFactory firstIterationScoringFunctionFactory;


		@Override
		public void notifyIterationStarts(IterationStartsEvent event) {
//			ScoringFunctionFactory currentScoringFunctionFactory = event.getServices().getInjector().getInstance(ScoringFunctionFactory.class);
//			ScoringParametersForPerson currentIterationScoringParametersForPerson = event.getServices().getInjector().getInstance(ScoringParametersForPerson.class);

			ScoringFunctionFactory currentScoringFunctionFactory = scoringFunctionFactory;
			ScoringParametersForPerson currentIterationScoringParametersForPerson = scoringParametersForPerson;


			String message = "\n \n \n ###################################### \n";
			message += "factory:\n" + currentScoringFunctionFactory + "\n \n scoringParametersForPerson: \n" + currentIterationScoringParametersForPerson + "\n \n";
			message += "###########################";
			logger.fatal(message);

			if(event.getIteration() == 0){
				this.firstIterationScoringFunctionFactory = currentScoringFunctionFactory;
				this.firstIterationScoringParametersForPerson = currentIterationScoringParametersForPerson;
			} else 	if(! currentIterationScoringParametersForPerson.equals(firstIterationScoringParametersForPerson) ||
			! currentScoringFunctionFactory.equals(firstIterationScoringFunctionFactory)) {
				throw new RuntimeException("different instances for scoring. see above");
			}
		}
	}

}
