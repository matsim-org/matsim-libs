/* *********************************************************************** *
 * project: org.matsim.*
 * HerbiePlanBasedLegScoringFunctionTest.java
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
package playground.thibautd.herbie;

import herbie.running.config.HerbieConfigGroup;
import herbie.running.scoring.HerbieScoringFunctionFactory;
import herbie.running.scoring.LegScoringFunction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalty;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.deprecated.scoring.ScoringFunctionAccumulator.BasicScoring;
import org.matsim.deprecated.scoring.ScoringFunctionAccumulator.LegScoring;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.pt.PtConstants;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import playground.thibautd.socnetsimusages.cliques.herbie.scoring.HerbieJointLegScoringFunction;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * @author thibautd
 */
@RunWith( value = Parameterized.class )
public class HerbiePlanBasedLegScoringFunctionTest {
	private final Plan plan;
	private Network network;
	private Config config;
	private ScoringParameters params;
	private HerbieConfigGroup ktiConfigGroup;
	private ActivityFacilitiesImpl facilities;

	// /////////////////////////////////////////////////////////////////////////
	// construction(s)
	// /////////////////////////////////////////////////////////////////////////
	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList( 
				new Object[] {getCarPlan()},
				new Object[] {getPtPlanNoTransfer()},
				new Object[] {getPtPlanTransfer()},
				new Object[] {getWalkPlan()});
	}

	private static Plan getCarPlan() {
		Person person = PopulationUtils.getFactory().createPerson(Id.create("jojo", Person.class));
		//Desires desires = person.createDesires( "bwarf" );
		//desires.putActivityDuration( "h" , 12 * 3600 );
		//desires.putActivityDuration( "w" , 12 * 3600 );
		Plan plan = PopulationUtils.createPlan(person);

		Activity act = PopulationUtils.createAndAddActivity(plan, "h");
		act.setEndTime( 10 );
		act.setFacilityId( Id.create( "h" , ActivityFacility.class ) );
		Leg l = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		NetworkRoute nRoute = new LinkNetworkRouteImpl( Id.create( 12 , Link.class ) , Id.create( 23 , Link.class ) );
		l.setRoute( nRoute );
		l.setDepartureTime( 123 );
		l.setTravelTime( 456 );

		act = PopulationUtils.createAndAddActivity(plan, "w");
		act.setStartTime( 104 );
		act.setEndTime( 294 );
		act.setFacilityId( Id.create( "w" , ActivityFacility.class ) );
		l = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		nRoute = new LinkNetworkRouteImpl( Id.create( 43 , Link.class ) , Id.create( 23 , Link.class ) );
		nRoute.setLinkIds(
				Id.create( 43 , Link.class ) ,
				Arrays.asList( Id.create(34, Link.class) , Id.create( 42, Link.class ) , Id.create( 23, Link.class ) , Id.create( 34, Link.class ) , Id.create( 42, Link.class ) ) ,
				Id.create( 23 , Link.class ) );
		l.setRoute( nRoute );
		l.setDepartureTime( 123 );
		l.setTravelTime( 456 );

		act = PopulationUtils.createAndAddActivity(plan, "h");
		act.setStartTime( 2000 );
		act.setFacilityId( Id.create( "h" , ActivityFacility.class) );

		return plan;
	}

	private static Plan getPtPlanNoTransfer() {
		Person person = PopulationUtils.getFactory().createPerson(Id.create("jojo", Person.class));
		//Desires desires = person.createDesires( "bwarf" );
		//desires.putActivityDuration( "h" , 12 * 3600 );
		//desires.putActivityDuration( "w" , 12 * 3600 );
		Plan plan = PopulationUtils.createPlan(person);

		Activity act = PopulationUtils.createAndAddActivity(plan, "h");
		act.setEndTime( 10 );
		act.setFacilityId( Id.create( "h" , ActivityFacility.class ) );
		Leg l = PopulationUtils.createAndAddLeg( plan, TransportMode.pt );
		ExperimentalTransitRoute tRoute =
			new ExperimentalTransitRoute( new FakeFacility( 23 ) , null , null , new FakeFacility( 43 ) );
		l.setRoute( tRoute );
		l.setDepartureTime( 1123 );
		l.setTravelTime( 3456 );

		PopulationUtils.createAndAddActivity(plan, "w");
		act.setStartTime( 100 );
		act.setEndTime( 109 );
		act.setFacilityId( Id.create( "w" , ActivityFacility.class ) );
		l = PopulationUtils.createAndAddLeg( plan, TransportMode.pt );
		tRoute =
			new ExperimentalTransitRoute( new FakeFacility( 13 ) , null , null , new FakeFacility( 42 ) );
		l.setRoute( tRoute );
		l.setDepartureTime( 1123 );
		l.setTravelTime( 3456 );
		
		PopulationUtils.createAndAddActivity(plan, "h");
		act.setStartTime( 2000 );
		act.setFacilityId( Id.create( "h", ActivityFacility.class ) );

		return plan;
	}

	private static Plan getPtPlanTransfer() {
		Person person = PopulationUtils.getFactory().createPerson(Id.create("jojo", Person.class));
		//Desires desires = person.createDesires( "bwarf" );
//		desires.putActivityDuration( "h" , 12 * 3600 );
//		desires.putActivityDuration( "w" , 12 * 3600 );
		Plan plan = PopulationUtils.createPlan(person);

		Activity act = PopulationUtils.createAndAddActivity(plan, "h");
		act.setEndTime( 100 );
		act.setFacilityId( Id.create( "h", ActivityFacility.class ) );
		Leg l = PopulationUtils.createAndAddLeg( plan, TransportMode.transit_walk );
		Route route = new GenericRouteImpl( Id.create( 12 , Link.class ) , Id.create( 23 , Link.class ) );
		l.setRoute( route );
		l.setDepartureTime( 123 );
		l.setTravelTime( 456 );

		act = PopulationUtils.createAndAddActivity(plan, PtConstants.TRANSIT_ACTIVITY_TYPE);
		act.setStartTime( 0 );
		act.setEndTime( 0 );
		l = PopulationUtils.createAndAddLeg( plan, TransportMode.pt );
		ExperimentalTransitRoute tRoute =
			new ExperimentalTransitRoute( new FakeFacility( 23 ) , null , null , new FakeFacility( 43 ) );
		l.setRoute( tRoute );
		l.setDepartureTime( 1123 );
		l.setTravelTime( 3456 );

		PopulationUtils.createAndAddActivity(plan, PtConstants.TRANSIT_ACTIVITY_TYPE);
		act.setStartTime( 0 );
		act.setEndTime( 0 );
		l = PopulationUtils.createAndAddLeg( plan, TransportMode.transit_walk );
		l.setRoute( new GenericRouteImpl( Id.create( 23 , Link.class ) , Id.create( 43 , Link.class ) ) );
		l.setDepartureTime( 1123 );
		l.setTravelTime( 3456 );

		PopulationUtils.createAndAddActivity(plan, "w");
		act.setStartTime( 2000 );
		act.setEndTime( 3000 );
		act.setFacilityId( Id.create( "w" , ActivityFacility.class ) );
		l = PopulationUtils.createAndAddLeg( plan, TransportMode.transit_walk );
		l.setRoute( new GenericRouteImpl( Id.create( 34 , Link.class ) , Id.create( 43 , Link.class ) ) );
		l.setDepartureTime( 1123 );
		l.setTravelTime( 346 );

		PopulationUtils.createAndAddActivity(plan, "h");
		act.setStartTime( 2000 );
		act.setFacilityId( Id.create( "h" , ActivityFacility.class ) );

		return plan;
	}

	private static Plan getWalkPlan() {
		Person person = PopulationUtils.getFactory().createPerson(Id.create("jojo", Person.class));
		//Desires desires = person.createDesires( "bwarf" );
		//desires.putActivityDuration( "h" , 12 * 3600 );
		//desires.putActivityDuration( "w" , 12 * 3600 );
		Plan plan = PopulationUtils.createPlan(person);

		for (double tt=60; tt <= 2*3600; tt *= 1.5) {
			Activity act = PopulationUtils.createAndAddActivity(plan, "w");
			act.setStartTime( 2000 );
			act.setEndTime( 3000 );
			act.setFacilityId( Id.create( "w" , ActivityFacility.class ) );
			Leg l = PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
			l.setRoute( new GenericRouteImpl( Id.create( 12 , Link.class ) , Id.create( 13 , Link.class ) ) );
			l.setDepartureTime( 1123 );
			l.setTravelTime( tt );
		}
		Activity act = PopulationUtils.createAndAddActivity(plan, "w");
		act.setStartTime( 2000 );
		act.setFacilityId( Id.create( "w" , ActivityFacility.class ) );

		return plan;
	}

	public HerbiePlanBasedLegScoringFunctionTest(final Plan plan) {
		this.plan = plan;
		// the factory now requires that we score the selected plan (mar. 2014)
		plan.getPerson().addPlan( plan );
		plan.getPerson().setSelectedPlan( plan );
	}

	// /////////////////////////////////////////////////////////////////////////
	// tests
	// /////////////////////////////////////////////////////////////////////////
	@Before
	public void init() {
		initConfig();
		initNetwork();
		//initPlans();
		initFacilities();
	}

	private void initFacilities() {
		facilities = new ActivityFacilitiesImpl();
		ActivityFacilityImpl fac = facilities.createAndAddFacility( Id.create( "h" , ActivityFacility.class ) , new Coord(0, 0));
		fac.createAndAddActivityOption( "h" ).addOpeningTime( new OpeningTimeImpl( 0 , 24 * 3600 ) );
		fac = facilities.createAndAddFacility( Id.create( "w" , ActivityFacility.class) , new Coord(0, 0));
		fac.createAndAddActivityOption( "w" ).addOpeningTime( new OpeningTimeImpl( 7 , 20 * 3600 ) );
	}

	private void initConfig() {
		config = ConfigUtils.createConfig();
		ktiConfigGroup = new HerbieConfigGroup();
		config.addModule( ktiConfigGroup );
		final double marginalUtlOfDistanceWalk = -5;
		config.planCalcScore().getModes().get(TransportMode.walk).setMarginalUtilityOfDistance(marginalUtlOfDistanceWalk);
		double marginalUtlOfDistanceOther = -9;
		config.planCalcScore().getModes().get(TransportMode.other).setMarginalUtilityOfDistance(marginalUtlOfDistanceOther);
		double monetaryDistanceRateCar = -1;
		config.planCalcScore().getModes().get(TransportMode.car).setMonetaryDistanceRate(monetaryDistanceRateCar);
		double monetaryDistanceRatePt = -2;
		config.planCalcScore().getModes().get(TransportMode.pt).setMonetaryDistanceRate(monetaryDistanceRatePt);
		params = new ScoringParameters.Builder(config.planCalcScore(), config.planCalcScore().getScoringParameters(null), config.scenario()).build();
	}

	//private void initPlans() {
	//	testPlans = new ArrayList<Plan>();

	//	testPlans.add( getCarPlan() );
	//	testPlans.add( getPtPlanTransfer() );
	//	testPlans.add( getPtPlanNoTransfer() );
	//	testPlans.add( getWalkPlan() );
	//}


	private void initNetwork() {
		Network nImpl = ScenarioUtils.createScenario( config ).getNetwork();
		Node n1 = NetworkUtils.createAndAddNode(nImpl, Id.create( 1, Node.class ), new Coord(0, 0));
		Node n2 = NetworkUtils.createAndAddNode(nImpl, Id.create( 2, Node.class ), new Coord(1, 0));
		Node n3 = NetworkUtils.createAndAddNode(nImpl, Id.create( 3, Node.class ), new Coord(1, 2));
		Node n4 = NetworkUtils.createAndAddNode(nImpl, Id.create( 4, Node.class ), new Coord(4, 2));
		final Node fromNode = n1;
		final Node toNode = n2;

		NetworkUtils.createAndAddLink(nImpl,Id.create( 12 , Link.class ), fromNode, toNode, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Node fromNode1 = n1;
		final Node toNode1 = n3;
		NetworkUtils.createAndAddLink(nImpl,Id.create( 13 , Link.class ), fromNode1, toNode1, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Node fromNode2 = n2;
		final Node toNode2 = n3;
		NetworkUtils.createAndAddLink(nImpl,Id.create( 23 , Link.class ), fromNode2, toNode2, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Node fromNode3 = n4;
		final Node toNode3 = n3;
		NetworkUtils.createAndAddLink(nImpl,Id.create( 43 , Link.class ), fromNode3, toNode3, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Node fromNode4 = n4;
		final Node toNode4 = n2;
		NetworkUtils.createAndAddLink(nImpl,Id.create( 42 , Link.class ), fromNode4, toNode4, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Node fromNode5 = n3;
		final Node toNode5 = n4;
		NetworkUtils.createAndAddLink(nImpl,Id.create( 34 , Link.class ), fromNode5, toNode5, (double) 1, (double) 1, (double) 1, (double) 1 );

		network = nImpl;
	}

	@Test
	@Ignore
	public void testLegScores() {
		Random random = new Random( 42 );
		double amplitude = 100;

		LegScoring planBased = new PlanBasedLegScoringFunction(
				plan,
				new HerbieJointLegScoringFunction(
					plan, 
					params,
					config,
					this.network,
					this.ktiConfigGroup));
		LegScoring base = new LegScoringFunction(
					plan, 
					params,
					config,
					this.network,
					this.ktiConfigGroup);
		
		int c = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				// use randomized start and end time, so that if one
				// of the scoring function uses the leg ones, it fails.
				double start = leg.getDepartureTime() + (random.nextDouble() * amplitude) - amplitude / 2;
				double end = start + leg.getTravelTime() + (random.nextDouble() * amplitude) - amplitude / 2;
				end = end > start ? end : 0d;
				c++;
				planBased.startLeg( start , leg );
				base.startLeg( start , leg );
				planBased.endLeg( end );
				base.endLeg( end );
			}
		}
		if (c==0) throw new RuntimeException( "nothing tested!" );

		((BasicScoring) planBased).finish();
		((BasicScoring) base).finish();

		Assert.assertEquals(
			((BasicScoring) base).getScore(),
			((BasicScoring) planBased).getScore(),
			MatsimTestUtils.EPSILON);
	}

	@Test
	@Ignore
	public void testPlanScores() {
		HerbiePlanBasedScoringFunctionFactory planBasedScoringFunctionFactory =
			new HerbiePlanBasedScoringFunctionFactory(
				config,
				ktiConfigGroup,
				new TreeMap<Id, FacilityPenalty>(),
				facilities,
				network);

		HerbieScoringFunctionFactory baseScoringFunctionFactory =
			new HerbieScoringFunctionFactory(
				config,
				ktiConfigGroup,
				new TreeMap<Id, FacilityPenalty>(),
				facilities,
				network);

		for (int until=1; until < plan.getPlanElements().size(); until++) {
			ScoringFunction planBased = planBasedScoringFunctionFactory.createNewScoringFunction( plan.getPerson() );
			ScoringFunction base = baseScoringFunctionFactory.createNewScoringFunction( plan.getPerson() );
	
			// test all "partial" scores, to track where it fails
			int c = 0;
			for (PlanElement pe : plan.getPlanElements().subList(0,until)) {
				c++;
				if (pe instanceof Leg) {
					planBased.handleLeg( (Leg) pe );
					base.handleLeg( (Leg) pe );
				}
				else {
					planBased.handleActivity( (Activity) pe );
					base.handleActivity( (Activity) pe );
				}
			}
			if (c==0) throw new RuntimeException( "nothing tested!" );

			planBased.finish();
			base.finish();

			Assert.assertEquals(
					"wrong score for "+plan.getPlanElements().subList(0,until),
					base.getScore(),
					planBased.getScore(),
					MatsimTestUtils.EPSILON);
		}
	}

	private static class FakeFacility implements TransitStopFacility {
		private final Id<TransitStopFacility> id;
		public FakeFacility(final int id) {
			this.id = Id.create( id , TransitStopFacility.class );
		}

		@Override
		public Id getLinkId() {
			return id;
		}

		@Override
		public Coord getCoord() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public void setCoord(Coord coord) {
			// TODO Auto-generated method stub
		}

		@Override
		public Id getId() {
			return id;
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean getIsBlockingLane() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void setLinkId(Id linkId) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setName(String name) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getStopPostAreaId() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setStopPostAreaId(String stopPostAreaId) {
			// TODO Auto-generated method stub
			
		}
	}
}

