package org.matsim.core.router;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class FallbackRoutingModuleTest{
	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void calcRoute(){

		Config config = ConfigUtils.createConfig();
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		config.controller().setOutputDirectory( utils.getOutputDirectory() );
		config.controller().setLastIteration( 1 );

		ReplanningConfigGroup.StrategySettings sets = new ReplanningConfigGroup.StrategySettings();
		sets.setStrategyName( DefaultPlanStrategiesModule.DefaultStrategy.ReRoute );
		sets.setWeight( 1. );
		config.replanning().addStrategySettings( sets );

		Scenario scenario = ScenarioUtils.createScenario( config );

		Network network = scenario.getNetwork();
		NetworkFactory nf = network.getFactory();

		Node node1 = nf.createNode( Id.createNodeId( 1 ), new Coord( 0., 10. ) );
		network.addNode( node1 );
		Node node2 = nf.createNode( Id.createNodeId( 2 ), new Coord( 1000., 10. ) );
		network.addNode( node2 );
		Link link12 = nf.createLink( Id.createLinkId( "1-2" ), node1, node2 );
		network.addLink( link12 );

		PopulationFactory pf = scenario.getPopulation().getFactory();

		Person person = pf.createPerson( Id.createPersonId( "abc" ) );
		Plan plan = pf.createPlan();
		{
			Activity act = pf.createActivityFromCoord( "dummy", new Coord( 0., 0. ) );
			act.setEndTime(0);
			plan.addActivity( act );
		}
		Leg leg = pf.createLeg( "abcd" );
		plan.addLeg( leg );
		{
			Activity act = pf.createActivityFromCoord( "dummy", new Coord( 1000., 0. ) );
			plan.addActivity( act );
		}
		person.addPlan( plan );
		scenario.getPopulation().addPerson( person );


		Controler controler = new Controler( scenario );

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				this.addRoutingModuleBinding( "abcd" ).toInstance( new RoutingModule(){
					@Override
					public List<? extends PlanElement> calcRoute( RoutingRequest request ){
						return null;
					}
				} );
			}
		} );

		controler.run();

	}
}
