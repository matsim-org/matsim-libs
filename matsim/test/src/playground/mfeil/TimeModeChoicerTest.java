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
import org.matsim.population.algorithms.PlanAnalyzeSubtours;
import org.matsim.core.scoring.PlanScorer;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.scenario.ScenarioLoader;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.population.routes.NodeNetworkRouteImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NodeImpl;
import org.matsim.api.core.v01.network.Node;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.BasicPlanElement;



public class TimeModeChoicerTest extends MatsimTestCase{
	
	private static final Logger log = Logger.getLogger(TimeModeChoicerTest.class);
	private Initializer initializer;
	final String TEST_PERSON_ID = "1";
	private PlansCalcRoute router;
	private LegTravelTimeEstimator estimator;
	private TimeModeChoicer1 testee;
	private ScenarioImpl scenario_input;
	private PlanImpl plan;

	protected void setUp() throws Exception {

		super.setUp();
		
		this.initializer = new Initializer();
		this.initializer.init(this); 
		
		this.scenario_input = new ScenarioImpl();
		new MatsimNetworkReader(this.scenario_input.getNetwork()).readFile(this.initializer.getControler().getConfig().network().getInputFile());
		new MatsimFacilitiesReader(this.scenario_input.getActivityFacilities()).readFile(this.initializer.getControler().getConfig().facilities().getInputFile());
		new MatsimPopulationReader(this.scenario_input).readFile(this.initializer.getControler().getConfig().plans().getInputFile());
		
		/*ScenarioLoader loader = new ScenarioLoader(this.initializer.getControler().getConfig());
		loader.loadScenario();
		this.scenario_input = loader.getScenario();
		*/
		
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
		
		this.testee = new TimeModeChoicer1 (legTravelTimeEstimatorFactory, this.estimator, new PlanScorer(new JohScoringTestFunctionFactory()), this.router);
		
		PlanImpl basePlan = ((PersonImpl)(this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)))).getSelectedPlan();
		this.plan = new PlanImpl (basePlan.getPerson());
		this.plan.copyPlan(basePlan);
	}
	
	
	
	public void testRun (){
		
		log.info("Running main test comparing processed plan with expected output plan...");
		
		// Import plan of person 1, copy and delete original population
		this.scenario_input.getPopulation().getPersons().clear();
		
		// Process plan
		this.router.run(this.plan);
		this.testee.run(this.plan);
		
		// Import expected output plan into population
		new MatsimPopulationReader(this.scenario_input).readFile(this.getPackageInputDirectory()+"expected_output_person1.xml");
				
		// Compare the two plans; <1 because of double rounding errors
		for (int i=0;i<this.plan.getPlanElements().size();i++){
			if (i%2==0){
				assertEquals(Math.floor(((ActivityImpl)(this.plan.getPlanElements().get(i))).getStartTime()), Math.floor(((ActivityImpl)(scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan().getPlanElements().get(i))).getStartTime()));
				assertEquals(Math.floor(((ActivityImpl)(this.plan.getPlanElements().get(i))).getEndTime()), Math.floor(((ActivityImpl)(scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan().getPlanElements().get(i))).getEndTime()));
			}
			else {
				assertEquals(Math.floor(((LegImpl)(this.plan.getPlanElements().get(i))).getDepartureTime()), Math.floor(((LegImpl)(scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan().getPlanElements().get(i))).getDepartureTime()));
				assertEquals(Math.floor(((LegImpl)(this.plan.getPlanElements().get(i))).getArrivalTime()),  Math.floor(((LegImpl)(scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan().getPlanElements().get(i))).getArrivalTime()));
			}
		}	
		log.info("... done.");
	}

	
	public void testCopyActslegs (){
		
		PlanImpl newPlan = new PlanImpl (this.plan.getPerson());
		newPlan.copyPlan(this.plan);
		
		NodeNetworkRouteImpl route = new NodeNetworkRouteImpl();
		List<Node> nodes1 = new ArrayList<Node>();
		nodes1.add(new NodeImpl(new IdImpl("1")));
		nodes1.add(new NodeImpl(new IdImpl("2")));
		nodes1.add(new NodeImpl(new IdImpl("3")));
		route.setNodes(nodes1);
		((LegImpl)(newPlan.getPlanElements().get(1))).setRoute(route);
		
		double planActTime = ((ActivityImpl)(newPlan.getPlanElements().get(0))).getEndTime();
		
		List<? extends BasicPlanElement> newPlanActsLegs = this.testee.copyActsLegs(newPlan.getPlanElements());
		
		// deep copy of acts (complete act) and leg times (only times!) so that time change in newPlan does not affect plan
		((ActivityImpl)(newPlanActsLegs.get(0))).setEndTime(0.0);
		
		// but flat copy of leg routes so that change in plan does also affect newPlan
		List<Node> nodes2 = new ArrayList<Node>();
		nodes2.add(new NodeImpl(new IdImpl("3")));
		nodes2.add(new NodeImpl(new IdImpl("2")));
		nodes2.add(new NodeImpl(new IdImpl("1")));
		route.setNodes(nodes2);
		
		log.info("planActTime "+planActTime);
		log.info("newPlanActsLegs.get(0))).getEndTime() "+((ActivityImpl)(newPlanActsLegs.get(0))).getEndTime());
		
		assertEquals(((ActivityImpl)newPlan.getPlanElements().get(0)).getEndTime(), planActTime);
		assertEquals(((ActivityImpl)(newPlanActsLegs.get(0))).getEndTime(), 0.0);
		assertEquals(((LegImpl)(newPlan.getPlanElements().get(1))).getRoute(), route);
		assertEquals(((LegImpl)(newPlanActsLegs.get(1))).getRoute(), route);
	}
	
	
	public void testIncreaseTime (){
		
		// Import expected output plan into population
		this.scenario_input.getPopulation().getPersons().clear();
		new MatsimPopulationReader(this.scenario_input).readFile(this.getPackageInputDirectory()+"expected_output_person1.xml");
		
		// Use expected output plan as input plan for this test (since it includes everything we need for this test: routes, travel times, etc.)
		// Import expected output plan into population
		PlanomatXPlan newPlan = new PlanomatXPlan (this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan().getPerson());
		newPlan.copyPlan(this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan());
		
		/* Analysis of subtours */
		PlanAnalyzeSubtours planAnalyzeSubtours = new PlanAnalyzeSubtours();
		planAnalyzeSubtours.run(newPlan);
		
		/* Make sure that all subtours with distance = 0 are set to "walk" */
		int [] subtourDis = new int [planAnalyzeSubtours.getNumSubtours()];
		for (int i=0;i<subtourDis.length;i++) {
			subtourDis[i]=this.testee.checksubtourDistance2(newPlan.getPlanElements(), planAnalyzeSubtours, i);
		}
		for (int i=1;i<newPlan.getPlanElements().size();i=i+2){
			if (subtourDis[planAnalyzeSubtours.getSubtourIndexation()[(i-1)/2]]==0) {
				((LegImpl)(newPlan.getPlanElements().get(i))).setMode(TransportMode.walk);
			}
		}
		
		/* 1. Just the very normal case: increase an act, decrease another one */
		/* Copy planElements */
		List<? extends BasicPlanElement> alIn = this.testee.copyActsLegs(newPlan.getPlanElements()); 
		List<? extends BasicPlanElement> alCheck = this.testee.copyActsLegs(newPlan.getPlanElements()); 
		
		// Run testee
		this.testee.increaseTime(newPlan, alIn, 0, 2, planAnalyzeSubtours, subtourDis);
		
		// Assert
		assertEquals(Math.floor(((LegImpl)(alCheck.get(1))).getDepartureTime()+this.testee.OFFSET), Math.floor(((LegImpl)(alIn.get(1))).getDepartureTime()));
		assertEquals(Math.floor(((LegImpl)(alCheck.get(1))).getArrivalTime()+this.testee.OFFSET), Math.floor(((LegImpl)(alIn.get(1))).getArrivalTime()));		
		
		
		/* 2. Swap durations */
		/* Copy planElements */
		alIn = this.testee.copyActsLegs(this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan().getPlanElements()); 
		alCheck = this.testee.copyActsLegs(this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan().getPlanElements()); 
		
		// Run testee
		this.testee.increaseTime(newPlan, alIn, 2, 4, planAnalyzeSubtours, subtourDis);
		
		// Assert considering rounding errors
		assertTrue(Math.abs(((LegImpl)(alCheck.get(1))).getArrivalTime()+(((LegImpl)(alCheck.get(5))).getDepartureTime()-((LegImpl)(alCheck.get(3))).getArrivalTime())-((LegImpl)(alIn.get(3))).getDepartureTime())<2);
		assertTrue(Math.abs(((LegImpl)(alCheck.get(5))).getDepartureTime()-(((LegImpl)(alCheck.get(3))).getDepartureTime()-((LegImpl)(alCheck.get(1))).getArrivalTime())-((LegImpl)(alIn.get(3))).getArrivalTime())<2);
		
		
		/* 3. outer != 0 && inner == size()-1 && inner too short but long enough with 0 */
		/* Prepare actslegs */
		newPlan.setActsLegs(this.testee.copyActsLegs(this.scenario_input.getPopulation().getPersons().get(new IdImpl(this.TEST_PERSON_ID)).getSelectedPlan().getPlanElements()));
		double move = 9000;
		((ActivityImpl)newPlan.getPlanElements().get(4)).setEndTime(((ActivityImpl)newPlan.getPlanElements().get(4)).getEndTime()+move);
		((ActivityImpl)newPlan.getPlanElements().get(4)).setDuration(((ActivityImpl)newPlan.getPlanElements().get(4)).getDuration()+move);
		((LegImpl)(newPlan.getPlanElements().get(5))).setDepartureTime(((LegImpl)(newPlan.getPlanElements().get(5))).getDepartureTime()+move);
		((LegImpl)(newPlan.getPlanElements().get(5))).setArrivalTime(((LegImpl)(newPlan.getPlanElements().get(5))).getArrivalTime()+move);
		((ActivityImpl)newPlan.getPlanElements().get(6)).setStartTime(((ActivityImpl)newPlan.getPlanElements().get(6)).getStartTime()+move);
		((ActivityImpl)newPlan.getPlanElements().get(6)).setDuration(((ActivityImpl)newPlan.getPlanElements().get(6)).getDuration()-move);
		
		/* Copy planElements */
		alIn = this.testee.copyActsLegs(newPlan.getPlanElements()); 
		alCheck = this.testee.copyActsLegs(newPlan.getPlanElements()); 
		
		log.info("vor 3. testee.");
		for (int i=1;i<alIn.size();i++){
			if (i%2==1)	log.info("Departure time "+((LegImpl)(alIn.get(i))).getDepartureTime()+" travel time "+((LegImpl)(alIn.get(i))).getTravelTime()+" arrival time "+((LegImpl)(alIn.get(i))).getArrivalTime());
			else log.info("Start time "+((ActivityImpl)(alIn.get(i))).getStartTime()+" duration "+((ActivityImpl)(alIn.get(i))).getDuration()+" end time "+((ActivityImpl)(alIn.get(i))).getEndTime());
		}
		System.out.println();
		
		// Run testee
		this.testee.increaseTime(newPlan, alIn, 4, 6, planAnalyzeSubtours, subtourDis);
		
		// Assert
		assertEquals(Math.floor(((LegImpl)(alCheck.get(5))).getDepartureTime()+this.testee.OFFSET), Math.floor(((LegImpl)(alIn.get(5))).getDepartureTime()));
		assertEquals(Math.floor(((LegImpl)(alCheck.get(5))).getArrivalTime()+this.testee.OFFSET), Math.floor(((LegImpl)(alIn.get(5))).getArrivalTime()));
		
		
		/* 4. outer == 0 && inner == size()-1 */
		/* Copy planElements (take solution of 3.) */
		this.testee.cleanActs(newPlan.getPlanElements());
		alIn = this.testee.copyActsLegs(newPlan.getPlanElements()); 
		alCheck = this.testee.copyActsLegs(newPlan.getPlanElements()); 
		
		
		// Run testee
		this.testee.increaseTime(newPlan, alIn, 0, 6, planAnalyzeSubtours, subtourDis);
		
		/*
		log.info("nach 4. testee alCheck.");
		for (int i=1;i<alIn.size();i++){
			if (i%2==1)	log.info("Departure time "+((LegImpl)(alCheck.get(i))).getDepartureTime()+" travel time "+((LegImpl)(alCheck.get(i))).getTravelTime()+" arrival time "+((LegImpl)(alCheck.get(i))).getArrivalTime());
			else log.info("Start time "+((ActivityImpl)(alCheck.get(i))).getStartTime()+" duration "+((ActivityImpl)(alCheck.get(i))).getDuration()+" end time "+((ActivityImpl)(alCheck.get(i))).getEndTime());
		}*/
		
		// Assert
		assertEquals(Math.floor(((LegImpl)(alCheck.get(5))).getDepartureTime()+this.testee.OFFSET), Math.floor(((LegImpl)(alIn.get(5))).getDepartureTime()));
		assertEquals(Math.floor(((LegImpl)(alCheck.get(5))).getArrivalTime()+this.testee.OFFSET), Math.floor(((LegImpl)(alIn.get(5))).getArrivalTime()));
		
		
	}
	
	
	
	/*
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
	}*/
}
