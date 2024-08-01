package org.matsim.contrib.common.diversitygeneration.planselectors;

import com.google.common.primitives.Doubles;
import gnu.trove.map.TMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DiversityGeneratingPlansRemoverTest {
	private static final Logger log = LogManager.getLogger( DiversityGeneratingPlansRemoverTest.class ) ;

	private final Id<Node> node0 = Id.createNodeId( "node0" ) ;
	private final Id<Node> node1 = Id.createNodeId( "node1" ) ;
	private final Id<Node> node2 = Id.createNodeId( "node2" ) ;
	private final Id<Node> node3 = Id.createNodeId( "node3" ) ;
	private final Id<Link> link0_1 = Id.createLinkId( "dummy0-1" );
	private final Id<Link> link1_2 = Id.createLinkId( "dummy1-2" );
	private final Id<Link> link2_3 = Id.createLinkId( "dummyN" );

	@Test
	void calcWeights() {
		// yy This is not really a strong test.  Rather something I wrote for debugging.  Would be a good
		// starting point for a fuller test.  kai, jul'18

		Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() ) ;
		{
			Network net = scenario.getNetwork();
			NetworkFactory nf = net.getFactory();
			Node theNode0 = nf.createNode( node0, new Coord( 0., 0. ) );
			net.addNode( theNode0 );
			Node theNode1 = nf.createNode( node1, new Coord( 1000., 0. ) );
			net.addNode( theNode1 );
			Node theNode2 = nf.createNode( node2, new Coord( 2000., 0. ) );
			net.addNode( theNode2 );
			Node theNode3 = nf.createNode( node3, new Coord( 3000., 0. ) );
			net.addNode( theNode3 );
			{
				Link theLink = nf.createLink(link0_1, theNode0, theNode1 ) ;
				theLink.setLength( 1000. );
				theLink.setFreespeed( 100./3.6 );
				theLink.setNumberOfLanes( 1. );
				net.addLink( theLink );
			}
			{
				Link theLink = nf.createLink(link1_2, theNode1, theNode2 ) ;
				theLink.setLength( 1000. );
				theLink.setFreespeed( 100./3.6 );
				theLink.setNumberOfLanes( 1. );
				net.addLink( theLink );
			}
			{
				Link theLink = nf.createLink(link2_3, theNode2, theNode3 ) ;
				theLink.setLength( 1000. );
				theLink.setFreespeed( 100./3.6 );
				theLink.setNumberOfLanes( 1. );
				net.addLink( theLink );
			}
		}
		// ---
		Population pop = scenario.getPopulation() ;
		PopulationFactory pf = pop.getFactory() ;
		Map<String,Plan> plans = new LinkedHashMap<>( ) ;
		// ---
		{
			Person person = pf.createPerson( Id.createPersonId( 0 ) ) ;
			{
				Plan plan = createHwhPlan( pf );
				person.addPlan( plan ) ;
				plans.put("hwh_car", plan) ;
			}
			{
				Plan plan = createHwhPlan( pf );
				final List<Leg> legs = TripStructureUtils.getLegs( plan );
				{
					Leg leg = legs.get(0) ;
					NetworkRoute route = pf.getRouteFactories().createRoute( NetworkRoute.class, link0_1, link2_3 ) ;
					List<Id<Link>> linkIds = new ArrayList<>() ;
//					linkIds.add( link1_2 ) ;
					route.setLinkIds( link0_1, linkIds, link2_3 );
					leg.setRoute( route );
				}
				person.addPlan( plan ) ;
				plans.put("hwh_car_oneOtherRoute", plan) ;
			}
			{
				Plan plan = createHwhPlan( pf );
				final List<Leg> legs = TripStructureUtils.getLegs( plan );
				{
					Leg leg = legs.get(0) ;
					leg.setMode( TransportMode.pt );
					TripStructureUtils.setRoutingMode(leg, TransportMode.pt );
					leg.setRoute( pf.getRouteFactories().createRoute( GenericRouteImpl.class , link0_1, link2_3 ) ) ;
				}
				{
					Leg leg = legs.get(1) ;
					leg.setMode( TransportMode.pt );
					TripStructureUtils.setRoutingMode(leg, TransportMode.pt );
					leg.setRoute( pf.getRouteFactories().createRoute( GenericRouteImpl.class , link2_3, link0_1 ) ) ;
				}
				person.addPlan( plan ) ;
				plans.put("hwh_car_otherMode",plan) ;
			}
			pop.addPerson( person );

			DiversityGeneratingPlansRemover.Builder builder = new DiversityGeneratingPlansRemover.Builder() ;
			builder.setNetwork( scenario.getNetwork() ) ;
			final DiversityGeneratingPlansRemover remover = builder.get();

			for ( Map.Entry<String,Plan> entry : plans.entrySet() ) {
				log.info( "similarity " + entry.getKey() + " to self is " + remover.similarity( entry.getValue(), entry.getValue() ) );
				log.info("") ;
				for ( Map.Entry<String,Plan> entry2 : plans.entrySet() ) {
					if ( ! ( entry.getKey().equals( entry2.getKey() ) ) ) {
						log.info( "similarity " + entry.getKey() + " to " + entry2.getKey() + " is " + remover.similarity( entry.getValue(), entry2.getValue() ) );
					}
				}
				log.info("") ;
			}

//			{
//				final double similarity = remover.similarity( person.getPlans().get( 0 ), person.getPlans().get( 1 ) );
//				log.info( "similarity 0 to 1: " + similarity );
//				Assert.assertEquals( 12.0, similarity, 10.*Double.MIN_VALUE );
//			}
//			{
//				final double similarity = remover.similarity( person.getPlans().get( 1 ), person.getPlans().get( 0 ) );
//				log.info( "similarity 1 to 0: " + similarity );
//				Assert.assertEquals( 12.0, similarity, 10.*Double.MIN_VALUE );
//			}
//			{
//				final double similarity = remover.similarity( person.getPlans().get( 0 ), person.getPlans().get( 2 ) );
//				log.info( "similarity 0 to 2: " + similarity );
//				Assert.assertEquals( 12.0, similarity, 10.*Double.MIN_VALUE );
//			}

			final Map<Plan, Double> retVal = remover.calcWeights( person.getPlans() );
			log.info("") ;
			for ( Map.Entry<Plan,Double> entry : retVal.entrySet() ) {
				log.info( "weight= " + entry.getValue() + "; plan=" + entry.getKey() ) ;
				for ( PlanElement pe : entry.getKey().getPlanElements() ) {
					log.info( pe.toString() ) ;
				}
				log.info("") ;
			}

			double[] expecteds = new double[]{1.0,0.0} ;

//			Assert.assertArrayEquals( expecteds, Doubles.toArray( retVal.values() ) , 10.*Double.MIN_VALUE );
		}


	}

	private Plan createHwhPlan( final PopulationFactory pf ) {
		Plan plan = pf.createPlan() ;
		{
			Activity act = pf.createActivityFromCoord( "home", new Coord(0.,0.) ) ;
			act.setEndTime( 7.*3600. );
			plan.addActivity( act );
		}
		{
			Leg leg = pf.createLeg( TransportMode.car ) ;
			{
				NetworkRoute route = pf.getRouteFactories().createRoute( NetworkRoute.class, link0_1, link2_3 ) ;
				List<Id<Link>> linkIds = new ArrayList<>() ;
				linkIds.add( link1_2 ) ;
				route.setLinkIds( link0_1, linkIds, link2_3 );
				leg.setRoute( route );
			}
			plan.addLeg( leg ) ;
		}
		{
			Activity act = pf.createActivityFromCoord( "work", new Coord(10000.,0.) ) ;
			act.setEndTime( 16.*3600. );
			plan.addActivity( act );
		}
		{
			Leg leg = pf.createLeg( TransportMode.car ) ;
			{
				NetworkRoute route = pf.getRouteFactories().createRoute( NetworkRoute.class, link2_3, link0_1 ) ;
				List<Id<Link>> linkIds = new ArrayList<>() ;
				linkIds.add( link1_2 ) ;
				route.setLinkIds( link2_3, linkIds, link0_1 );
				leg.setRoute( route );
			}
			plan.addLeg( leg ) ;
		}
		{
			Activity act = pf.createActivityFromCoord( "home", new Coord(0.,0.) ) ;
			plan.addActivity( act );
		}
		plan.setScore(90.) ;
		return plan;
	}

	@Test
	void selectPlan() {
	}
}
