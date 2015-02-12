/* *********************************************************************** *
 * project: org.matsim.*
 * PathSizeLogitSelectorTest
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package air.pathsize;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.testcases.MatsimTestUtils;


/**
 * @author dgrether
 *
 */
public class PathSizeLogitSelectorTest {

	
	private static final Logger log = Logger.getLogger(PathSizeLogitSelectorTest.class);
	
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	private Scenario loadAndPrepareScenario(){
//		String netfile = utils.getClassInputDirectory() + "output_network.xml";
		String popfile = utils.getClassInputDirectory() + "output_plans.xml";
		Config config = ConfigUtils.createConfig();
//		config.network().setInputFile(netfile);
		config.plans().setInputFile(popfile);

		{
			ModeRoutingParams params = new ModeRoutingParams() ;
			params.setMode("pt") ;
			config.plansCalcRoute().addModeRoutingParams(params);
		}
		{
			ModeRoutingParams params = new ModeRoutingParams() ;
			params.setMode("transit_walk") ;
			config.plansCalcRoute().addModeRoutingParams(params);
		}
		{
			ModeRoutingParams params = new ModeRoutingParams() ;
			params.setMode("train") ;
			config.plansCalcRoute().addModeRoutingParams(params);
		}

		Scenario sc = ScenarioUtils.loadScenario(config);
		createExperimentalTransitRoutes(sc);
		
		return sc;
	}
	
	
	@Test
	public final void testSelectPlan() {
		Scenario sc = this.loadAndPrepareScenario();
		Set<String> mainModes = new HashSet<String>();
		mainModes.add("pt");
		mainModes.add("train");
		
		//Two plans with exactly same structure but a big difference in the scores
		PathSizeLogitSelector psls = new PathSizeLogitSelector(1, 2, mainModes);
		Plan plan = psls.selectPlan(sc.getPopulation().getPersons().get(Id.create("555555", Person.class)));
		System.out.println(plan);
		Assert.assertNotNull(plan);
		Assert.assertEquals(10.0, plan.getScore(), MatsimTestUtils.EPSILON);

		//Two plans with exactly same structure no difference in the scores
		psls = new PathSizeLogitSelector(50, 2, mainModes);
		plan = psls.selectPlan(sc.getPopulation().getPersons().get(Id.create("666666", Person.class)));
		System.out.println(plan);
		Assert.assertNotNull(plan); //can't test more both are exactly equal

		//Two exactly equal plans (structure + score) with a third pt plan that is not similar and worse 
		plan = psls.selectPlan(sc.getPopulation().getPersons().get(Id.create("777777", Person.class)));
		System.out.println(plan);
		Assert.assertNotNull(plan);
		Leg leg = (Leg) plan.getPlanElements().get(3);
		Assert.assertEquals("PT1===TXL===TXL_MUC_SBA===TXL_MUC_SBA23===MUC", ((GenericRoute)leg.getRoute()).getRouteDescription());

		//Two equal pt plans and one train plan
		plan = psls.selectPlan(sc.getPopulation().getPersons().get(Id.create("888888", Person.class)));
		System.out.println(plan);
		Assert.assertNotNull(plan);
		leg = (Leg) plan.getPlanElements().get(3);
		Assert.assertEquals("PT1===TXL===TXL_MUC_SBA===TXL_MUC_SBA23===MUC", ((GenericRoute)leg.getRoute()).getRouteDescription());
		
		//Two equal train plans and one pt plan with less score
		psls = new PathSizeLogitSelector(20, 2, mainModes);
		plan = psls.selectPlan(sc.getPopulation().getPersons().get(Id.create("999999", Person.class)));
		System.out.println(plan);
		Assert.assertNotNull(plan);
		leg = (Leg) plan.getPlanElements().get(1);
		Assert.assertEquals("train", leg.getMode());

		//Same as last test but with lest ps logit beta
		psls = new PathSizeLogitSelector(10, 2, mainModes);
		plan = psls.selectPlan(sc.getPopulation().getPersons().get(Id.create("999999", Person.class)));
		System.out.println(plan);
		Assert.assertNotNull(plan);
		leg = (Leg) plan.getPlanElements().get(1);
		Assert.assertEquals("transit_walk", leg.getMode());
		
	}

	/**
	 * Why?  dg 09-2013
	 */
	private void createExperimentalTransitRoutes(Scenario sc) {
		for (Person person : sc.getPopulation().getPersons().values()){
			for (Plan plan : person.getPlans()) {
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Leg){
						Leg leg = (Leg) pe;
						if (leg.getMode().equals("pt")) {
							GenericRoute route = (GenericRoute) leg.getRoute();
							ExperimentalTransitRoute tr = (ExperimentalTransitRoute) new ExperimentalTransitRouteFactory().createRoute(null, null);
							leg.setRoute(tr);
							tr.setRouteDescription(route.getStartLinkId(), route.getRouteDescription(), route.getEndLinkId());
							tr.setDistance(route.getDistance());
						}
					}
				}
			}
		}
	}

}
