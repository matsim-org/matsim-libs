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
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.core.population.PlanImpl;
import org.matsim.planomat.costestimators.*;
import org.matsim.core.scoring.PlanScorer;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.scenario.ScenarioLoader;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;



public class TimeModeChoicerTest extends MatsimTestCase{
	
	private static final Logger log = Logger.getLogger(TimeModeChoicerTest.class);
	private Initializer initializer;
	final String TEST_PERSON_ID = "1";
	private PlansCalcRoute router;
	private LegTravelTimeEstimator estimator;
	private TimeModeChoicer1 testee;
	private ScenarioImpl scenario_input;

	protected void setUp() throws Exception {

		super.setUp();
		
		this.initializer = new Initializer();
		this.initializer.init(this); 
		
		ScenarioLoader loader = new ScenarioLoader(this.initializer.getControler().getConfig());
		loader.loadScenario();
		this.scenario_input = loader.getScenario();
	
		// no events are used, hence an empty road network
		DepartureDelayAverageCalculator tDepDelayCalc = new DepartureDelayAverageCalculator(this.scenario_input.getNetwork(), 900);
       	
		TravelTimeCalculator linkTravelTimeEstimator = new TravelTimeCalculator(this.scenario_input.getNetwork(), this.initializer.getControler().getConfig().travelTimeCalculator());
		// Using charyparNagelScoring is okay since only travel values which are identical with JohScoring
		TravelCost linkTravelCostEstimator = new TravelTimeDistanceCostCalculator(linkTravelTimeEstimator, this.initializer.getControler().getConfig().charyparNagelScoring());

		this.router = new PlansCalcRoute(this.initializer.getControler().getConfig().plansCalcRoute(), this.scenario_input.getNetwork(), linkTravelCostEstimator, linkTravelTimeEstimator);

		LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory = new LegTravelTimeEstimatorFactory(linkTravelTimeEstimator, tDepDelayCalc);
		
		this.estimator = (FixedRouteLegTravelTimeEstimator) legTravelTimeEstimatorFactory.getLegTravelTimeEstimator(
				((PersonImpl)(this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)))).getSelectedPlan(),
				PlanomatConfigGroup.SimLegInterpretation.CetinCompatible,
				PlanomatConfigGroup.RoutingCapability.fixedRoute,
				this.router);
		
		this.testee = new TimeModeChoicer1 (legTravelTimeEstimatorFactory, new PlanScorer(new JohScoringTestFunctionFactory()), this.router);
	}
	
	
	
	public void testRun (){
		
		log.info("Running main test comparing processed plan with expected output plan...");
		
		// Import plan of person 1, copy and delete original population
		PlanImpl basePlan = ((PersonImpl)(this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)))).getSelectedPlan();
		PlanImpl plan = new PlanomatXPlan (basePlan.getPerson());
		plan.copyPlan(basePlan);
		this.scenario_input.getPopulation().getPersons().clear();
		
		// Process plan
		this.router.run(plan);
		this.testee.run(plan);
		
		// Import expected output plan into population
		new MatsimPopulationReader(scenario_input).readFile(this.getPackageInputDirectory()+"expected_output_person1.xml");
			
		// Compare the two plans; <1 because of double rounding errors
		for (int i=0;i<plan.getPlanElements().size();i++){
			if (i%2==0){
				assert(((ActivityImpl)(plan.getPlanElements().get(i))).getStartTime()-((ActivityImpl)(scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan().getPlanElements().get(i))).getStartTime()<1);
				assert(((ActivityImpl)(plan.getPlanElements().get(i))).getEndTime()-((ActivityImpl)(scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan().getPlanElements().get(i))).getEndTime()<1);
			}
			else {
				assert(((LegImpl)(plan.getPlanElements().get(i))).getDepartureTime()-((LegImpl)(scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan().getPlanElements().get(i))).getDepartureTime()<1);
				assert(((LegImpl)(plan.getPlanElements().get(i))).getArrivalTime()-((LegImpl)(scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan().getPlanElements().get(i))).getArrivalTime()<1);
			}
		}	
		log.info("... done.");
	}
}/*
	
	public void testCopyActslegs (){
		
		Plan plan = new Plan (population.getPerson(this.TEST_PERSON_ID));
		plan.copyPlan(population.getPerson(this.TEST_PERSON_ID).getPlans().get(0));
		
		NodeCarRoute route = new NodeCarRoute();
		route.setNodes("1 2 3");
		
		// but flat copy of leg routes so that change in plan does also affect newPlan
		((Leg)(plan.getActsLegs().get(1))).setRoute(route);
		
		double planActTime = ((Act)(plan.getActsLegs().get(0))).getEndTime();
		
		ArrayList<Object> newPlanActsLegs = this.testee.copyActsLegs(plan.getActsLegs()); 
		
		// deep copy of acts (complete act) and leg times (only times!) so that time change in newPlan does not affect plan
		((Act)(newPlanActsLegs.get(0))).setEndTime(0.0);
		
		// but flat copy of leg routes so that change in plan does also affect newPlan
		route.setNodes("3 2 1");
		
		assertEquals(((Act)plan.getActsLegs().get(0)).getEndTime(), planActTime);
		assertEquals(((Act)(newPlanActsLegs.get(0))).getEndTime(), 0.0);
		assertEquals(((Leg)(plan.getActsLegs().get(1))).getRoute(), route);
		assertEquals(((Leg)(newPlanActsLegs.get(1))).getRoute(), route);
	}
	
	public void testIncreaseTime (){
		
		PlanomatXPlan plan = new PlanomatXPlan (population.getPerson(this.TEST_PERSON_ID));
		plan.copyPlan(population.getPerson(this.TEST_PERSON_ID).getPlans().get(0));
		
		this.router.run(plan);	// conducts routing and sets travel times
		this.testee.cleanActs(plan.getActsLegs());	// adjusts the act durations according to travel times
		
		ArrayList<Object> alIn = this.testee.copyActsLegs(plan.getActsLegs()); 
		ArrayList<Object> alCheck = this.testee.copyActsLegs(plan.getActsLegs()); 
		
		this.testee.increaseTime(plan, alIn, 2, 4);
		this.testee.cleanActs(alIn);
		
		((Act)(alCheck.get(2))).setDuration(((Act)(alCheck.get(2))).getDuration()+this.testee.getOffset());
		((Act)(alCheck.get(2))).setEndTime(((Act)(alCheck.get(2))).getEndTime()+this.testee.getOffset());
		
		((Leg)(alCheck.get(3))).setTravelTime(this.ltte.getLegTravelTimeEstimation(plan.getPerson().getId(), ((Act)(alCheck.get(2))).getEndTime(), (Act)(alCheck.get(2)), (Act)(alCheck.get(4)), (Leg)(alCheck.get(3))));
		((Act)(alCheck.get(4))).setDuration(((Act)(alCheck.get(4))).getEndTime()-(((Act)(alCheck.get(2))).getEndTime()+((Leg)(alCheck.get(3))).getTravelTime()));
	
		for (int i=0;i<alIn.size();i+=2){
			assertEquals(((Act)(alIn.get(i))).getDuration(), ((Act)(alCheck.get(i))).getDuration());
		}
		
		
	}
	
	private void testDecreaseTime (){
		
		PlanomatXPlan plan = new PlanomatXPlan (population.getPerson(this.TEST_PERSON_ID));
		plan.copyPlan(population.getPerson(this.TEST_PERSON_ID).getPlans().get(0));
		
		this.router.run(plan);	// conducts routing and sets travel times
		this.testee.cleanActs(plan.getActsLegs());	// adjusts the act durations according to travel times
		
		ArrayList<Object> alIn = this.testee.copyActsLegs(plan.getActsLegs()); 
		ArrayList<Object> alCheck = this.testee.copyActsLegs(plan.getActsLegs()); 
		
		this.testee.decreaseTime(plan, alIn, 2, 4);
		this.testee.cleanActs(alIn);
		
		((Act)(alCheck.get(2))).setDuration(((Act)(alCheck.get(2))).getDuration()-this.testee.getOffset());
		((Act)(alCheck.get(2))).setEndTime(((Act)(alCheck.get(2))).getEndTime()-this.testee.getOffset());
		
		((Leg)(alCheck.get(3))).setTravelTime(this.ltte.getLegTravelTimeEstimation(plan.getPerson().getId(), ((Act)(alCheck.get(2))).getEndTime(), (Act)(alCheck.get(2)), (Act)(alCheck.get(4)), (Leg)(alCheck.get(3))));
		((Act)(alCheck.get(4))).setDuration(((Act)(alCheck.get(4))).getEndTime()-(((Act)(alCheck.get(2))).getEndTime()+((Leg)(alCheck.get(3))).getTravelTime()));
	
		for (int i=0;i<alIn.size();i+=2){
			log.info(((Act)(alCheck.get(i))).getDuration());
		}
		
		for (int i=0;i<alIn.size();i+=2){
			log.warn("Iteration "+i);
			assertEquals(((Act)(alIn.get(i))).getDuration(), ((Act)(alCheck.get(i))).getDuration());
		}
	}
	
	public void testRun (){
		Plan plan = new Plan (population.getPerson(this.TEST_PERSON_ID));
		plan.copyPlan(population.getPerson(this.TEST_PERSON_ID).getPlans().get(0));
		
		this.router.run(plan);
		
		Plan targetPlan = population.getPerson(this.TEST_PERSON_ID).getPlans().get(1);
		
		this.testee.run(plan);
		
		// Test whether differences are smaller than 10 sec because leg estimator causes minor time differences
		for (int i=0;i<plan.getActsLegs().size();i++){
			if (i%2==0){
				assert(java.lang.Math.abs(((Act)(plan.getActsLegs().get(i))).getDuration()-((Act)(targetPlan.getActsLegs().get(i))).getDuration())<1);
				assert(java.lang.Math.abs(((Act)(plan.getActsLegs().get(i))).getStartTime()-((Act)(targetPlan.getActsLegs().get(i))).getStartTime())<1);
				assert(java.lang.Math.abs(((Act)(plan.getActsLegs().get(i))).getEndTime()-((Act)(targetPlan.getActsLegs().get(i))).getEndTime())<1);
			}
			else {
				assert(java.lang.Math.abs(((Leg)(plan.getActsLegs().get(i))).getTravelTime()-((Leg)(targetPlan.getActsLegs().get(i))).getTravelTime())<1);
				assert(java.lang.Math.abs(((Leg)(plan.getActsLegs().get(i))).getDepartureTime()-((Leg)(targetPlan.getActsLegs().get(i))).getDepartureTime())<1);
				assert(java.lang.Math.abs(((Leg)(plan.getActsLegs().get(i))).getArrivalTime()-((Leg)(targetPlan.getActsLegs().get(i))).getArrivalTime())<1);
			}
		}
	}
	
	public void testCheck () {
		Plan plan = new Plan (population.getPerson(this.TEST_PERSON_ID));
		plan.copyPlan(population.getPerson(this.TEST_PERSON_ID).getPlans().get(0));
		for (int i=0;i<plan.getActsLegs().size();i++){
			if (i%2==1) {
				System.out.print(((Leg)(plan.getActsLegs().get(i))).getTravelTime()+" "); 
				System.out.print(((Leg)(plan.getActsLegs().get(i))).getRoute().getTravelTime()+" ");
				System.out.print(((Leg)(plan.getActsLegs().get(i))).getRoute().getDist()+" ");
				System.out.print(((Leg)(plan.getActsLegs().get(i))).getMode()+", "); 
				System.out.print(((Leg)(plan.getActsLegs().get(i))).getRoute().getStartLinkId()+", ");
				System.out.print(((Leg)(plan.getActsLegs().get(i))).getRoute().getEndLinkId()+", ");
				for (int j=0;j<((Leg)(plan.getActsLegs().get(i))).getRoute().getLinkIds().size();j++ ){
					System.out.print(((Leg)(plan.getActsLegs().get(i))).getRoute().getLinkIds().get(j)+" "); 
				}
				System.out.println();
			}
		}
	}
}*/
