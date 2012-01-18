/* *********************************************************************** *
 * project: org.matsim.*
 * FitnessTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.planomat.basic;

import java.util.HashMap;
import java.util.Map;

import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.impl.StockRandomGenerator;
import org.jgap.InvalidConfigurationException;
import org.jgap.RandomGenerator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.OnlyTimeDependentScoringFunctionFactory;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.planomat.api.ActivityWhiteList;
import playground.thibautd.planomat.api.PlanomatFitnessFunction;
import playground.thibautd.planomat.config.Planomat2ConfigGroup;

/**
 * Tests the correct behaviour of PlanomatFitnessFunctionImpl AND
 * PlanomatFitnessFunctionFactoryImpl
 *
 * @author thibautd
 */
public class FitnessTest {
	private PlanomatFitnessFunctionFactoryImpl fitnessFactory;
	private Plan plan;

	@Before
	public void init() {
		ScoringFunctionFactory scoring =
			new OnlyTimeDependentScoringFunctionFactory();
		//Configuration jgapConf = new Configuration();
		Planomat2ConfigGroup confGroup = new Planomat2ConfigGroup();
		Config conf = ConfigUtils.createConfig() ;
		conf.addModule( Planomat2ConfigGroup.GROUP_NAME , confGroup );
		// is there a less dirty way to do it?
		Network net = ScenarioUtils.loadScenario( conf ).getNetwork();

		NetworkFactory netFact = net.getFactory();
		Node n1 = netFact.createNode( new IdImpl( "node1" ) , new CoordImpl( 0 , 0 ) );
		Node n2 = netFact.createNode( new IdImpl( "node2" ) , new CoordImpl( 0 , 0 ) );
		Link l1 = netFact.createLink( new IdImpl( "link" ) , n1 , n2 );
		Link l2 = netFact.createLink( new IdImpl( "knil" ) , n2 , n1 );
		net.addNode( n1 );
		net.addNode( n2 );
		net.addLink( l1 );
		net.addLink( l2 );

		PlanImpl plan = new PlanImpl( new PersonImpl( new IdImpl( "bouh" ) ) );
		plan.createAndAddActivity( "h" , l1.getId() ).setEndTime( 8 * 3600 );
		plan.createAndAddLeg( TransportMode.car );
		plan.createAndAddActivity( "w" , l2.getId() ).setEndTime( 10 * 3600 );
		plan.createAndAddLeg( TransportMode.car );
		plan.createAndAddActivity( "h" , l1.getId() ).setEndTime( 12 * 3600 );
		plan.createAndAddLeg( TransportMode.car );
		plan.createAndAddActivity( "w" , l2.getId() ).setEndTime( 19 * 3600 );
		plan.createAndAddLeg( TransportMode.car );
		plan.createAndAddActivity( "h" , l1.getId() ).setEndTime( 24 * 3600 );
		this.plan = plan;

		FreespeedTravelTimeCost travelTimeCost =
			new FreespeedTravelTimeCost( conf.planCalcScore() );
		PlansCalcRoute router =
			new PlansCalcRoute(
					conf.plansCalcRoute(),
					net,
					travelTimeCost,
					travelTimeCost,
					new DijkstraFactory(),
					new ModeRouteFactory());
		LegTravelTimeEstimatorFactory estFactory =
			new LegTravelTimeEstimatorFactory(
					travelTimeCost,
					new DepartureDelayAverageCalculator( net , 1 ) );

		fitnessFactory =
			new PlanomatFitnessFunctionFactoryImpl(
					scoring,
					confGroup,
					router,
					net,
					estFactory);
	}

	@Before
	public void jgapClean() {
		Configuration.reset();
	}

	@Test
	public void testFactory() {
		Configuration jgapConfig  = new Configuration();

		PlanomatFitnessFunction fit =
			fitnessFactory.createFitnessFunction(
					jgapConfig,
					plan,
					new PermissiveWhiteList());

		Assert.assertTrue(
				"unexpected fitness function class",
				fit instanceof PlanomatFitnessFunctionImpl);
	}

	@Test
	public void testModifyBackPlan() throws InvalidConfigurationException {
		modifyBackPlanFromRandomChrom( new PermissiveWhiteList() );

		boolean isFirstAct = true;
		double now = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity activity = (Activity) pe;
				// is it valid for the scoring function?
				if (!isFirstAct) {
					Assert.assertEquals(
							"unexpected activity start time",
							now,
							activity.getStartTime(),
							MatsimTestUtils.EPSILON);
				}
				else {
					isFirstAct = false;
					Assert.assertTrue(
							"first activity has negative end time",
							activity.getEndTime() >= 0);
				}
				Assert.assertTrue(
						"activity has negative duration",
						activity.getEndTime() >= activity.getStartTime());
				now = activity.getEndTime();
			}
			else {
				Leg leg = (Leg) pe;
				Assert.assertEquals(
						"unexpected leg departure time",
						now,
						leg.getDepartureTime(),
						MatsimTestUtils.EPSILON);
				Assert.assertTrue(
						"leg has negative travel time: "+leg.getTravelTime(),
						leg.getTravelTime() >= 0);
				now += leg.getTravelTime();
			}
		}
	}

	@Test
	public void testWhiteListBehaviour() throws InvalidConfigurationException {
		ActivityWhiteListImpl whiteList = new ActivityWhiteListImpl();
		whiteList.addType( "w" );

		// get the durations of untouchable activities
		Map<Activity, Double> enforcedDurations = new HashMap<Activity, Double>();
		double now = 0;
		int nUntouchable = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;

				if (!whiteList.isModifiableType( act.getType() )) {
					// duration includes leg(s)
					double dur =  act.getEndTime() - now;
					enforcedDurations.put( act , dur );
					nUntouchable++;
				}

				now = act.getEndTime();
			}
		}
		if (nUntouchable == 0) {
			throw new RuntimeException( "testWhiteListBehaviour operates without untouchable activities. Double check the test code!" );
		}

		modifyBackPlanFromRandomChrom( whiteList );

		now = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;

				if (!whiteList.isModifiableType( act.getType() )) {
					// duration includes leg(s)
					double dur =  act.getEndTime() - now;
					Assert.assertEquals(
							"untouchable duration were touched",
							enforcedDurations.get( act ),
							dur,
							MatsimTestUtils.EPSILON);
				}

				now = act.getEndTime();
			}
		}
	}

	private void modifyBackPlanFromRandomChrom(
			final ActivityWhiteList whiteList ) throws InvalidConfigurationException {
		Configuration jgapConfig  = new Configuration();
		StockRandomGenerator random = new StockRandomGenerator();
		random.setSeed( 10 );
		jgapConfig.setRandomGenerator( random );

		PlanomatFitnessFunction fit =
			fitnessFactory.createFitnessFunction(
					jgapConfig,
					plan,
					whiteList);
		IChromosome chrom = fit.getSampleChomosome();
		for (Gene gene : chrom.getGenes()) {
			gene.setToRandomValue( random );
		}
		fit.modifyBackPlan( chrom );
	}
}

