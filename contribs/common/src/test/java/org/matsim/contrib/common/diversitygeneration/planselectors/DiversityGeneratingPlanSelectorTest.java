/**
 * 
 */
package org.matsim.contrib.common.diversitygeneration.planselectors;

import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author kainagel
 *
 */
public class DiversityGeneratingPlanSelectorTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void testLegOverlap() {
		Fixture f = new Fixture() ;
		List<Leg> legs1 = PopulationUtils.getLegs(f.plan1) ;
		List<Leg> legs2 = PopulationUtils.getLegs(f.plan2);
		List<Leg> legs3 = PopulationUtils.getLegs(f.plan3);
		
		LegsSimilarityCalculator1.Builder builder = new LegsSimilarityCalculator1.Builder( null ) ;
		// yyyy I cannot say at this point why it is working with network=null.  kai, sep'17
		
		builder.setSameModeReward(1.);
		builder.setSameRouteReward(1.);
		LegsSimilarityCalculator lsc = builder.build() ;
		
		Assert.assertEquals( 2., lsc.calculateSimilarity( legs1, legs2 ) , 0.001 ) ;
		Assert.assertEquals( 1., lsc.calculateSimilarity( legs1, legs3 ) , 0.001 ) ;
	}
	
	@Test
	public void testActivityOverlap() {
		Fixture f = new Fixture() ;
		List<Activity> acts1 = PopulationUtils.getActivities(f.plan1, null ) ;
		List<Activity> acts2 = PopulationUtils.getActivities(f.plan2, null ) ;
		List<Activity> acts3 = PopulationUtils.getActivities(f.plan3, null ) ;
		
		ActivitiesSimilarityCalculator1.Builder builder = new ActivitiesSimilarityCalculator1.Builder() ;
		builder.setActTypeWeight(1.);
		builder.setLocationWeight(1.);
		builder.setActTimeWeight(0.);
		ActivitiesSimilarityCalculator asc = builder.build() ;
		
		Assert.assertEquals( 6., asc.calculateSimilarity( acts1, acts2 ) , 0.001 ) ;
		Assert.assertEquals( 5., asc.calculateSimilarity( acts1, acts3 ) , 0.001 ) ;
	}
	
	private static class Fixture {
		Plan plan1, plan2, plan3 ;
		Fixture() {
			Config config = ConfigUtils.createConfig() ;
			Scenario scenario = ScenarioUtils.createScenario(config) ;
			Population pop = scenario.getPopulation() ;
			PopulationFactory pf = pop.getFactory() ;
			
			{
				Plan plan = pf.createPlan() ;

				Activity act1 = pf.createActivityFromCoord("h", new Coord(0., 0.)) ;
				plan.addActivity(act1);
				
				Leg leg1 = pf.createLeg( TransportMode.car ) ;
				plan.addLeg( leg1 ) ;

				Activity act2 = pf.createActivityFromCoord("w", new Coord(1000., 0.)) ;
				plan.addActivity(act2) ;

				Leg leg2 = pf.createLeg( TransportMode.car ) ;
				plan.addLeg( leg2 ) ;

				Activity act3 = pf.createActivityFromCoord("h", new Coord(0., 0.)) ;
				plan.addActivity(act3) ;
				
				plan1 = plan ;
			}
			{
				Plan plan = pf.createPlan() ;

				Activity act1 = pf.createActivityFromCoord("h", new Coord(0., 0.)) ;
				plan.addActivity(act1);
				
				Leg leg1 = pf.createLeg( TransportMode.car ) ;
				plan.addLeg( leg1 ) ;

				Activity act2 = pf.createActivityFromCoord("w", new Coord(1000., 0.)) ;
				plan.addActivity(act2) ;

				Leg leg2 = pf.createLeg( TransportMode.car ) ;
				plan.addLeg( leg2 ) ;

				Activity act3 = pf.createActivityFromCoord("h", new Coord(0., 0.)) ;
				plan.addActivity(act3) ;
				
				plan2 = plan ;
			}
			{
				Plan plan = pf.createPlan() ;

				Activity act1 = pf.createActivityFromCoord("h", new Coord(0., 0.)) ;
				plan.addActivity(act1);
				
				Leg leg1 = pf.createLeg( TransportMode.car ) ;
				plan.addLeg( leg1 ) ;

				Activity act2 = pf.createActivityFromCoord("s", new Coord(1000., 0.)) ;
				plan.addActivity(act2) ;

				Leg leg2 = pf.createLeg( TransportMode.bike ) ;
				plan.addLeg( leg2 ) ;

				Activity act3 = pf.createActivityFromCoord("h", new Coord(0., 0.)) ;
				plan.addActivity(act3) ;
				
				plan3 = plan ;
			}
		}
	}

}
