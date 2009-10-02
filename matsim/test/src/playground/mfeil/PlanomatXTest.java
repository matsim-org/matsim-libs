/* *********************************************************************** *
 * project: org.matsim.*
 * TimeOptimizerTest.java
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

package playground.mfeil;

import org.matsim.testcases.MatsimTestCase;
import org.matsim.core.population.PersonImpl;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.core.population.PlanImpl;
import org.matsim.planomat.costestimators.*;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;
import org.matsim.core.scoring.PlanScorer;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.population.routes.NodeNetworkRouteImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NodeImpl;
import org.matsim.api.core.v01.network.Node;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.BasicPlanElement;

import playground.mfeil.MDSAM.ActivityTypeFinder;
import playground.mfeil.filesForTests.Initializer;



public class PlanomatXTest extends MatsimTestCase{
	
	private static final Logger log = Logger.getLogger(PlanomatXTest.class);
	private Initializer initializer;
	final String TEST_PERSON_ID = "1";
	private PlansCalcRoute router;
	//private LegTravelTimeEstimator estimator;
	private PlanomatX testee;
	private ScenarioImpl scenario_input;

	protected void setUp() throws Exception {

		super.setUp();
		
		this.initializer = new Initializer();
		this.initializer.init(this); 
		this.initializer.run();
		
		this.scenario_input = this.initializer.getControler().getScenarioData();

		// no events are used, hence an empty road network
		DepartureDelayAverageCalculator tDepDelayCalc = new DepartureDelayAverageCalculator(this.scenario_input.getNetwork(), 900);
       	
		LocationMutatorwChoiceSet locator = new LocationMutatorwChoiceSet (this.scenario_input.getNetwork(), this.initializer.getControler(), this.initializer.getControler().getScenarioData().getKnowledges());
		ActivityTypeFinder finder = new ActivityTypeFinder (this.initializer.getControler());
		
		this.testee = new PlanomatX (this.initializer.getControler(), locator, tDepDelayCalc, finder);
	}
	
	
	public void testRun (){
		log.info("Running PlX testRun...");
		
		PlanImpl plan = new PlanImpl (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)));
		plan.copyPlan(this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan());
		
		this.testee.run(plan);
		/*
		log.info("Writing plans...");
		new PopulationWriter(this.scenario_input.getPopulation(), "plans/test_plans.xml.gz").write();
		log.info("done.");
		*/
		// Import expected output plan into population
		this.scenario_input.getPopulation().getPersons().clear();
		new MatsimPopulationReader(this.scenario_input).readFile(this.getPackageInputDirectory()+"PLX_expected_output.xml");
				
		// Compare the two plans; <1 because of double rounding errors
		for (int i=0;i<plan.getPlanElements().size();i++){
			if (i%2==0){
				assertEquals(Math.floor(((ActivityImpl)(plan.getPlanElements().get(i))).getStartTime()), Math.floor(((ActivityImpl)(scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan().getPlanElements().get(i))).getStartTime()));
				assertEquals(Math.floor(((ActivityImpl)(plan.getPlanElements().get(i))).getEndTime()), Math.floor(((ActivityImpl)(scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan().getPlanElements().get(i))).getEndTime()));
			}
			else {
				assertEquals(Math.floor(((LegImpl)(plan.getPlanElements().get(i))).getDepartureTime()), Math.floor(((LegImpl)(scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan().getPlanElements().get(i))).getDepartureTime()));
				assertEquals(Math.floor(((LegImpl)(plan.getPlanElements().get(i))).getArrivalTime()),  Math.floor(((LegImpl)(scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan().getPlanElements().get(i))).getArrivalTime()));
			}
		}	
		log.info("... done.");
	}
	
	
	
}
