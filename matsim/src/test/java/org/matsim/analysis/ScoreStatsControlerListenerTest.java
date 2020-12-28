/**
 * 
 */
package org.matsim.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ListIterator;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.CompressionType;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author Aravind
 *
 */
public class ScoreStatsControlerListenerTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	private static int avgexecuted;
	private static int avgworst;
	private static int avgaverage;
	private static int avgbest;
	
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private Population population = scenario.getPopulation();
	
	@Test
	public void testScoreStatsControlerListner() {

		/************************************
		 * Person - creating person 1
		 ************************************/
		Person person1 = PopulationUtils.getFactory().createPerson(Id.create("1", Person.class));
		final Plan person1_plan1 = PopulationUtils
				.createPlan(person1);
        
		final Id<Link> link1 = Id.create(10723, Link.class);
		final Id<Link> link2 = Id.create(123160, Link.class);
		final Id<Link> link3 = Id.create(130181, Link.class);
		final Id<Link> link4 = Id.create(139117, Link.class);
		final Id<Link> link5 = Id.create(139100, Link.class);

		Activity act1_1_1 = PopulationUtils.createActivityFromLinkId("home", link1);
		person1_plan1.addActivity(act1_1_1);
		Leg leg1_1_1 = PopulationUtils.createLeg(TransportMode.walk);
		Route route1_1_1 = RouteUtils.createGenericRouteImpl(link1, link1);
		route1_1_1.setDistance(100);
		leg1_1_1.setRoute(route1_1_1);
		person1_plan1.addLeg(leg1_1_1);
		Activity act1_1_2 = PopulationUtils.createActivityFromLinkId("car interaction", link1);
		person1_plan1.addActivity(act1_1_2);
		Leg leg1_1_2 = PopulationUtils.createLeg(TransportMode.car);
		Route route1_1_2 = RouteUtils.createGenericRouteImpl(link1, link2);
		route1_1_2.setDistance(5000);
		leg1_1_2.setRoute(route1_1_2);
		person1_plan1.addLeg(leg1_1_2);
		Activity act1_1_3 = PopulationUtils.createActivityFromLinkId("car interaction", link2);
		person1_plan1.addActivity(act1_1_3);
		Leg leg1_1_3 = PopulationUtils.createLeg(TransportMode.walk);
		Route route1_1_3 = RouteUtils.createGenericRouteImpl(link2, link2);
		route1_1_3.setDistance(300);
		leg1_1_3.setRoute(route1_1_3);
		person1_plan1.addLeg(leg1_1_3);	
		Activity act1_1_4 = PopulationUtils.createActivityFromLinkId("work", link2);
		person1_plan1.addActivity(act1_1_4);
		Leg leg1_1_4 = PopulationUtils.createLeg(TransportMode.walk);
		Route route1_1_4 = RouteUtils.createGenericRouteImpl(link2, link2);
		route1_1_4.setDistance(300);
		leg1_1_4.setRoute(route1_1_4);
		person1_plan1.addLeg(leg1_1_4);
		Activity act1_1_5 = PopulationUtils.createActivityFromLinkId("car interaction", link2);
		person1_plan1.addActivity(act1_1_5);
		Leg leg1_1_5 = PopulationUtils.createLeg(TransportMode.car);
		Route route1_1_5 = RouteUtils.createGenericRouteImpl(link4, link1);
		route1_1_5.setDistance(6000);
		leg1_1_5.setRoute(route1_1_5);
		person1_plan1.addLeg(leg1_1_5);
		Activity act1_1_6 = PopulationUtils.createActivityFromLinkId("car interaction", link1);
		person1_plan1.addActivity(act1_1_6);
		Leg leg1_1_6 = PopulationUtils.createLeg(TransportMode.walk);
		leg1_1_6.setRoute(route1_1_1);
		person1_plan1.addLeg(leg1_1_6);
		Activity act1_1_7 = PopulationUtils.createActivityFromLinkId("home", link1);
		person1_plan1.addActivity(act1_1_7);
		
		person1_plan1.setScore(123.0);
		person1.addPlan(person1_plan1);
		
		/************************************
		 * Second plan of person 1
		 ************************************/
		final Plan person1_plan2 = PopulationUtils
				.createPlan(person1);
		Activity act1_2_1 = PopulationUtils.createActivityFromLinkId("home", link1);
		person1_plan2.addActivity(act1_2_1);
		Leg leg1_2_1 = PopulationUtils.createLeg(TransportMode.walk);
		Route route1_2_1 = RouteUtils.createGenericRouteImpl(link1, link1);
		route1_2_1.setDistance(100);
		leg1_2_1.setRoute(route1_2_1);
		person1_plan2.addLeg(leg1_2_1);
		Activity act1_2_2 = PopulationUtils.createActivityFromLinkId("car interaction", link1);
		person1_plan2.addActivity(act1_2_2);
		Leg leg1_2_2 = PopulationUtils.createLeg(TransportMode.car);
		Route route1_2_2 = RouteUtils.createGenericRouteImpl(link1, link5);
		route1_2_2.setDistance(5000);
		leg1_1_2.setRoute(route1_2_2);
		person1_plan2.addLeg(leg1_2_2);
		Activity act1_2_3 = PopulationUtils.createActivityFromLinkId("car interaction", link5);
		person1_plan2.addActivity(act1_2_3);
		Leg leg1_2_3 = PopulationUtils.createLeg(TransportMode.walk);
		Route route1_2_3 = RouteUtils.createGenericRouteImpl(link5, link2);
		route1_2_3.setDistance(300);
		leg1_1_3.setRoute(route1_2_3);
		person1_plan2.addLeg(leg1_2_3);	
		Activity act1_2_4 = PopulationUtils.createActivityFromLinkId("work", link2);
		person1_plan2.addActivity(act1_2_4);
		Leg leg1_2_4 = PopulationUtils.createLeg(TransportMode.walk);
		Route route1_2_4 = RouteUtils.createGenericRouteImpl(link2, link2);
		route1_2_4.setDistance(300);
		leg1_1_4.setRoute(route1_2_4);
		person1_plan2.addLeg(leg1_2_4);
		Activity act1_2_5 = PopulationUtils.createActivityFromLinkId("car interaction", link2);
		person1_plan2.addActivity(act1_2_5);
		Leg leg1_2_5 = PopulationUtils.createLeg(TransportMode.car);
		Route route1_2_5 = RouteUtils.createGenericRouteImpl(link2, link1);
		route1_2_5.setDistance(6000);
		leg1_2_5.setRoute(route1_2_5);
		person1_plan2.addLeg(leg1_2_5);
		Activity act1_2_6 = PopulationUtils.createActivityFromLinkId("car interaction", link1);
		person1_plan2.addActivity(act1_2_6);
		Leg leg1_2_6 = PopulationUtils.createLeg(TransportMode.walk);
		leg1_2_6.setRoute(route1_2_1);
		person1_plan2.addLeg(leg1_2_6);
		Activity act1_2_7 = PopulationUtils.createActivityFromLinkId("home", link1);
		person1_plan2.addActivity(act1_2_7);
		
		person1_plan2.setScore(105.0);
		person1.addPlan(person1_plan2);

		population.addPerson(person1);
		

		/********************************
		 * Person 2 - creating person 2
		 ********************************/
		Person person2 = PopulationUtils.getFactory().createPerson(Id.create("2", Person.class));
		final Plan person2_plan1 = PopulationUtils
				.createPlan(person2);

		Activity act2_1_1= PopulationUtils.createActivityFromLinkId("home", link1);
		person2_plan1.addActivity(act2_1_1);
		Leg leg2_1_1 = PopulationUtils.createLeg(TransportMode.walk);
		Route route2_1_1 = RouteUtils.createGenericRouteImpl(link1, link1);
		route2_1_1.setDistance(100);
		leg2_1_1.setRoute(route2_1_1);
		person2_plan1.addLeg(leg2_1_1);
		Activity act2_1_2 = PopulationUtils.createActivityFromLinkId("car interaction", link1);
		person2_plan1.addActivity(act2_1_2);
		Leg leg2_1_2 = PopulationUtils.createLeg(TransportMode.car);
		Route route2_1_2 = RouteUtils.createGenericRouteImpl(link1, link4);
		route2_1_2.setDistance(6000);
		leg2_1_2.setRoute(route2_1_2);
		person2_plan1.addLeg(leg2_1_2);
		Activity act2_1_3 = PopulationUtils.createActivityFromLinkId("car interaction", link4);
		person2_plan1.addActivity(act2_1_3);
		Leg leg2_1_3 = PopulationUtils.createLeg(TransportMode.walk);
		Route route2_1_3 = RouteUtils.createGenericRouteImpl(link4, link4);
		route2_1_3.setDistance(200);
		leg2_1_3.setRoute(route2_1_3);
		person2_plan1.addLeg(leg2_1_3);
		Activity act2_1_4 = PopulationUtils.createActivityFromLinkId("shopping", link4);
		person2_plan1.addActivity(act2_1_4);
		Leg leg2_1_4 = PopulationUtils.createLeg(TransportMode.walk);
		Route route2_1_4 = RouteUtils.createGenericRouteImpl(link4, link4);
		route2_1_4.setDistance(250);
		leg2_1_4.setRoute(route2_1_4);
		person2_plan1.addLeg(leg2_1_4);
		Activity act2_1_5 = PopulationUtils.createActivityFromLinkId("car interaction", link4);
		person2_plan1.addActivity(act2_1_5);
		Leg leg2_1_5 = PopulationUtils.createLeg(TransportMode.car);
		leg2_1_5.setRoute(route2_1_2);
		person2_plan1.addLeg(leg2_1_5);
		Activity act2_1_6 = PopulationUtils.createActivityFromLinkId("car interaction", link1);
		person2_plan1.addActivity(act2_1_6);
		Leg leg2_1_6 = PopulationUtils.createLeg(TransportMode.walk);
		leg2_1_6.setRoute(route2_1_1);
		person2_plan1.addLeg(leg2_1_6);
		Activity act2_1_7 = PopulationUtils.createActivityFromLinkId("home", link1);
		person2_plan1.addActivity(act2_1_7);

		person2_plan1.setScore(135.0);
		person2.addPlan(person2_plan1);
		
		final Plan person2_plan2 = PopulationUtils
				.createPlan(person2);

		Activity act2_2_1= PopulationUtils.createActivityFromLinkId("home", link1);
		person2_plan2.addActivity(act2_2_1);
		Leg leg2_2_1 = PopulationUtils.createLeg(TransportMode.walk);
		Route route2_2_1 = RouteUtils.createGenericRouteImpl(link1, link1);
		route2_2_1.setDistance(100);
		leg2_2_1.setRoute(route2_2_1);
		person2_plan2.addLeg(leg2_2_1);
		Activity act2_2_2 = PopulationUtils.createActivityFromLinkId("car interaction", link1);
		person2_plan2.addActivity(act2_2_2);
		Leg leg2_2_2 = PopulationUtils.createLeg(TransportMode.car);
		Route route2_2_2 = RouteUtils.createGenericRouteImpl(link1, link4);
		route2_2_2.setDistance(6000);
		leg2_2_2.setRoute(route2_2_2);
		person2_plan2.addLeg(leg2_2_2);
		Activity act2_2_3 = PopulationUtils.createActivityFromLinkId("car interaction", link5);
		person2_plan2.addActivity(act2_2_3);
		Leg leg2_2_3 = PopulationUtils.createLeg(TransportMode.walk);
		Route route2_2_3 = RouteUtils.createGenericRouteImpl(link5, link4);
		route2_2_3.setDistance(2000);
		leg2_2_3.setRoute(route2_2_3);
		person2_plan2.addLeg(leg2_2_3);
		Activity act2_2_4 = PopulationUtils.createActivityFromLinkId("shopping", link4);
		person2_plan2.addActivity(act2_2_4);
		Leg leg2_2_4 = PopulationUtils.createLeg(TransportMode.walk);
		Route route2_2_4 = RouteUtils.createGenericRouteImpl(link4, link4);
		route2_2_4.setDistance(250);
		leg2_2_4.setRoute(route2_2_4);
		person2_plan2.addLeg(leg2_2_4);
		Activity act2_2_5 = PopulationUtils.createActivityFromLinkId("car interaction", link4);
		person2_plan2.addActivity(act2_2_5);
		Leg leg2_2_5 = PopulationUtils.createLeg(TransportMode.car);
		leg2_2_5.setRoute(route2_2_2);
		person2_plan2.addLeg(leg2_2_5);
		Activity act2_2_6 = PopulationUtils.createActivityFromLinkId("car interaction", link1);
		person2_plan2.addActivity(act2_2_6);
		Leg leg2_2_6 = PopulationUtils.createLeg(TransportMode.walk);
		leg2_2_6.setRoute(route2_2_1);
		person2_plan2.addLeg(leg2_2_6);
		Activity act2_2_7 = PopulationUtils.createActivityFromLinkId("home", link1);
		person2_plan2.addActivity(act2_2_7);

		person2_plan2.setScore(101.0);
		person2.addPlan(person2_plan2);
		
		population.addPerson(person2);
		

		/************************************
		 * Person 3 - creating person 3
		 ************************************/
		Person person3 = PopulationUtils.getFactory().createPerson(Id.create("3", Person.class));
		final Plan person3_plan1 = PopulationUtils
				.createPlan(person3);

		Activity act3_1_1 = PopulationUtils.createActivityFromLinkId("home", link1);
		person3_plan1.addActivity(act3_1_1);
		Leg leg3_1_1 = PopulationUtils.createLeg(TransportMode.walk);
		Route route3_1_1 = RouteUtils.createGenericRouteImpl(link1, link1);
		route3_1_1.setDistance(100);
		leg3_1_1.setRoute(route3_1_1);
		person3_plan1.addLeg(leg3_1_1);
		Activity act3_1_2 = PopulationUtils.createActivityFromLinkId("car interaction", link1);
		person3_plan1.addActivity(act3_1_2);
		Leg leg3_1_2 = PopulationUtils.createLeg(TransportMode.car);
		Route route3_1_2 = RouteUtils.createGenericRouteImpl(link1, link5);
		route3_1_2.setDistance(8000);
		leg3_1_2.setRoute(route3_1_2);
		person3_plan1.addLeg(leg3_1_2);
		Activity act3_1_3 = PopulationUtils.createActivityFromLinkId("car interaction", link5);
		person3_plan1.addActivity(act3_1_3);
		Leg leg3_1_3 = PopulationUtils.createLeg(TransportMode.walk);
		Route route3_1_3 = RouteUtils.createGenericRouteImpl(link5, link5);
		route3_1_3.setDistance(300);
		leg3_1_3.setRoute(route3_1_3);
		person3_plan1.addLeg(leg3_1_3);
		Activity act3_1_4 = PopulationUtils.createActivityFromLinkId("shopping", link5);// main mode car
		person3_plan1.addActivity(act3_1_4);
		Leg leg3_1_4 = PopulationUtils.createLeg(TransportMode.walk);
		leg3_1_4.setRoute(route3_1_3);
		person3_plan1.addLeg(leg3_1_4);
		Activity act3_1_5 = PopulationUtils.createActivityFromLinkId("car interaction", link5);
		person3_plan1.addActivity(act3_1_5);
		Leg leg3_1_5 = PopulationUtils.createLeg(TransportMode.car);
		leg3_1_5.setRoute(route3_1_2);
		person3_plan1.addLeg(leg3_1_5);
		Activity act3_1_6 = PopulationUtils.createActivityFromLinkId("car interaction", link1);
		person3_plan1.addActivity(act3_1_6);
		Leg leg3_1_6 = PopulationUtils.createLeg(TransportMode.walk);
		leg3_1_6.setRoute(route3_1_1);
		person3_plan1.addLeg(leg3_1_6);
		Activity act3_1_7 = PopulationUtils.createActivityFromLinkId("home", link1);// main mode car
		person3_plan1.addActivity(act3_1_7);

		person3_plan1.setScore(111.00);
		person3.addPlan(person3_plan1);
		
		
		final Plan person3_plan2 = PopulationUtils
				.createPlan(person3);

		Activity act3_2_1 = PopulationUtils.createActivityFromLinkId("home", link1);
		person3_plan2.addActivity(act3_2_1);
		Leg leg3_2_1 = PopulationUtils.createLeg(TransportMode.walk);
		Route route3_2_1 = RouteUtils.createGenericRouteImpl(link1, link1);
		route3_2_1.setDistance(100);
		leg3_2_1.setRoute(route3_2_1);
		person3_plan2.addLeg(leg3_2_1);
		Activity act3_2_2 = PopulationUtils.createActivityFromLinkId("pt interaction", link1);
		person3_plan2.addActivity(act3_2_2);
		Leg leg3_2_2 = PopulationUtils.createLeg(TransportMode.pt);
		Route route3_2_2 = RouteUtils.createGenericRouteImpl(link1, link5);
		route3_2_2.setDistance(8000);
		leg3_2_2.setRoute(route3_2_2);
		person3_plan2.addLeg(leg3_2_2);
		Activity act3_2_3 = PopulationUtils.createActivityFromLinkId("pt interaction", link5);
		person3_plan2.addActivity(act3_2_3);
		Leg leg3_2_3 = PopulationUtils.createLeg(TransportMode.walk);
		Route route3_2_3 = RouteUtils.createGenericRouteImpl(link5, link5);
		route3_2_3.setDistance(300);
		leg3_2_3.setRoute(route3_2_3);
		person3_plan2.addLeg(leg3_2_3);
		Activity act3_2_4 = PopulationUtils.createActivityFromLinkId("shopping", link5);// main mode car
		person3_plan2.addActivity(act3_2_4);
		Leg leg3_2_4 = PopulationUtils.createLeg(TransportMode.walk);
		leg3_2_4.setRoute(route3_2_3);
		person3_plan2.addLeg(leg3_2_4);
		Activity act3_2_5 = PopulationUtils.createActivityFromLinkId("pt interaction", link5);
		person3_plan2.addActivity(act3_2_5);
		Leg leg3_2_5 = PopulationUtils.createLeg(TransportMode.pt);
		leg3_2_5.setRoute(route3_2_2);
		person3_plan2.addLeg(leg3_2_5);
		Activity act3_2_6 = PopulationUtils.createActivityFromLinkId("pt interaction", link1);
		person3_plan2.addActivity(act3_2_6);
		Leg leg3_2_6 = PopulationUtils.createLeg(TransportMode.walk);
		leg3_2_6.setRoute(route3_2_1);
		person3_plan2.addLeg(leg3_2_6);
		Activity act3_2_7 = PopulationUtils.createActivityFromLinkId("home", link1);// main mode car
		person3_plan2.addActivity(act3_2_7);

		person3_plan2.setScore(165.00);
		person3.addPlan(person3_plan2);
		
		population.addPerson(person3);
		

		/************************
		 * Person 4-----creating person 4
		 **************************************/
		Person person4 = PopulationUtils.getFactory().createPerson(Id.create("4", Person.class));
		final Plan person4_plan1 = PopulationUtils
				.createPlan(person4);

		Activity act4_1_1 = PopulationUtils.createActivityFromLinkId("home", link1);
		person4_plan1.addActivity(act4_1_1);
		Leg leg4_1_1 = PopulationUtils.createLeg(TransportMode.walk);
		Route route4_1_1 = RouteUtils.createGenericRouteImpl(link1, link1);
		route4_1_1.setDistance(350);
		leg4_1_1.setRoute(route4_1_1);
		person4_plan1.addLeg(leg4_1_1);
		Activity act4_1_2 = PopulationUtils.createActivityFromLinkId("pt interaction", link1);
		person4_plan1.addActivity(act4_1_2);
		Leg leg4_1_2 = PopulationUtils.createLeg(TransportMode.pt);
		Route route4_1_2 = RouteUtils.createGenericRouteImpl(link1, link3);
		route4_1_2.setDistance(6500);
		leg4_1_2.setRoute(route4_1_2);
		person4_plan1.addLeg(leg4_1_2);
		Activity act4_1_3 = PopulationUtils.createActivityFromLinkId("pt interaction", link3);
		person4_plan1.addActivity(act4_1_3);
		Leg leg4_1_3 = PopulationUtils.createLeg(TransportMode.walk);
		Route route4_1_3 = RouteUtils.createGenericRouteImpl(link3, link3);
		route4_1_3.setDistance(250);
		leg4_1_3.setRoute(route4_1_3);
		person4_plan1.addLeg(leg4_1_3);
		Activity act4_1_4 = PopulationUtils.createActivityFromLinkId("shopping", link3);// main mode pt
		person4_plan1.addActivity(act4_1_4);
		Leg leg4_1_4 = PopulationUtils.createLeg(TransportMode.walk);
		leg4_1_4.setRoute(route4_1_3);
		person4_plan1.addLeg(leg4_1_4);
		Activity act4_1_5 = PopulationUtils.createActivityFromLinkId("pt interaction", link3);
		person4_plan1.addActivity(act4_1_5);
		Leg leg4_1_5 = PopulationUtils.createLeg(TransportMode.pt);
		leg4_1_5.setRoute(route4_1_2);
		person4_plan1.addLeg(leg4_1_5);
		Activity act4_1_6 = PopulationUtils.createActivityFromLinkId("pt interaction", link1);
		person4_plan1.addActivity(act4_1_6);
		Leg leg4_1_6 = PopulationUtils.createLeg(TransportMode.walk);
		leg4_1_6.setRoute(route4_1_1);
		person4_plan1.addLeg(leg4_1_6);
		Activity act4_1_7 = PopulationUtils.createActivityFromLinkId("home", link1);// main mode pt
		person4_plan1.addActivity(act4_1_7);

		person4_plan1.setScore(134.00);
		person4.addPlan(person4_plan1);
		
		
		final Plan person4_plan2 = PopulationUtils
				.createPlan(person4);

		Activity act4_2_1 = PopulationUtils.createActivityFromLinkId("home", link1);
		person4_plan2.addActivity(act4_2_1);
		Leg leg4_2_1 = PopulationUtils.createLeg(TransportMode.walk);
		Route route4_2_1 = RouteUtils.createGenericRouteImpl(link1, link1);
		route4_2_1.setDistance(350);
		leg4_2_1.setRoute(route4_2_1);
		person4_plan2.addLeg(leg4_2_1);
		Activity act4_2_2 = PopulationUtils.createActivityFromLinkId("car interaction", link1);
		person4_plan2.addActivity(act4_2_2);
		Leg leg4_2_2 = PopulationUtils.createLeg(TransportMode.car);
		Route route4_2_2 = RouteUtils.createGenericRouteImpl(link1, link3);
		route4_2_2.setDistance(6500);
		leg4_2_2.setRoute(route4_2_2);
		person4_plan2.addLeg(leg4_2_2);
		Activity act4_2_3 = PopulationUtils.createActivityFromLinkId("car interaction", link3);
		person4_plan2.addActivity(act4_2_3);
		Leg leg4_2_3 = PopulationUtils.createLeg(TransportMode.walk);
		Route route4_2_3 = RouteUtils.createGenericRouteImpl(link3, link3);
		route4_2_3.setDistance(250);
		leg4_2_3.setRoute(route4_2_3);
		person4_plan2.addLeg(leg4_2_3);
		Activity act4_2_4 = PopulationUtils.createActivityFromLinkId("shopping", link3);// main mode pt
		person4_plan2.addActivity(act4_2_4);
		Leg leg4_2_4 = PopulationUtils.createLeg(TransportMode.walk);
		leg4_2_4.setRoute(route4_2_3);
		person4_plan2.addLeg(leg4_2_4);
		Activity act4_2_5 = PopulationUtils.createActivityFromLinkId("car interaction", link3);
		person4_plan2.addActivity(act4_2_5);
		Leg leg4_2_5 = PopulationUtils.createLeg(TransportMode.car);
		leg4_2_5.setRoute(route4_2_2);
		person4_plan2.addLeg(leg4_2_5);
		Activity act4_2_6 = PopulationUtils.createActivityFromLinkId("car interaction", link1);
		person4_plan2.addActivity(act4_2_6);
		Leg leg4_2_6 = PopulationUtils.createLeg(TransportMode.walk);
		leg4_2_6.setRoute(route4_2_1);
		person4_plan2.addLeg(leg4_2_6);
		Activity act4_2_7 = PopulationUtils.createActivityFromLinkId("home", link1);// main mode pt
		person4_plan2.addActivity(act4_2_7);

		//person4.setSelectedPlan(selectedPlan);
		person4_plan2.setScore(124.00);
		person4.addPlan(person4_plan2);
		
		population.addPerson(person4);
		
		
		performTest(utils.getOutputDirectory() + "/ScoreStatsControlerListener", population);
	
		
	}
	
	
	private void performTest(String outputDirectory, Population population) {
		ControlerConfigGroup controlerConfigGroup = new ControlerConfigGroup();
		OutputDirectoryHierarchy controlerIO = new OutputDirectoryHierarchy(outputDirectory,
				OverwriteFileSetting.overwriteExistingFiles, CompressionType.gzip);
		controlerConfigGroup.setCreateGraphs(true);
		controlerConfigGroup.setFirstIteration(0);
		controlerConfigGroup.setLastIteration(10);
		ScoreStatsControlerListener scoreStatsControlerListener = new ScoreStatsControlerListener(controlerConfigGroup, population, controlerIO, null, null);
		
		StartupEvent eventStart = new StartupEvent(null);
		scoreStatsControlerListener.notifyStartup(eventStart);
		
		IterationEndsEvent event0 = new IterationEndsEvent(null, 0, false);
		scoreStatsControlerListener.notifyIterationEnds(event0);
		
		readAndValidateValues(0, population);
		
		population.getPersons().remove(Id.create("2", Person.class));
		
		IterationEndsEvent event1 = new IterationEndsEvent(null, 1, false);
		scoreStatsControlerListener.notifyIterationEnds(event1);
		
		readAndValidateValues(1, population);
		
		population.getPersons().remove(Id.create("3", Person.class));
		
		IterationEndsEvent event2 = new IterationEndsEvent(null, 2, false);
		scoreStatsControlerListener.notifyIterationEnds(event2);
		
		readAndValidateValues(2, population);
		
		population.getPersons().remove(Id.create("4", Person.class));
		
		IterationEndsEvent event3 = new IterationEndsEvent(null, 3, true);
		scoreStatsControlerListener.notifyIterationEnds(event3);
		
		readAndValidateValues(3, population);
		
		ShutdownEvent eventShutdown = new ShutdownEvent(null, false, 3);
		scoreStatsControlerListener.notifyShutdown(eventShutdown);

	}
	
	private void readAndValidateValues(int itr, Population population) {
		
		String file = utils.getOutputDirectory() + "/ScoreStatsControlerListener" + "/scorestats.txt";
		BufferedReader br;
		String line;
		try {
			br = new BufferedReader(new FileReader(file));
			String firstRow = br.readLine();
			String[] columnNames = firstRow.split("	");
			decideColumns(columnNames);
			int iteration = 0;
			while ((line = br.readLine()) != null) {
				if (iteration == itr) {
					String[] column = line.split("	");
					
					// checking if column number in greater than 0, because 0th column is always
					// 'Iteration' and we don't need that --> see decideColumns() method
					Double avgExecuted = (avgexecuted > 0) ? Double.valueOf(column[avgexecuted]) : 0;
					Double avgWorst = (avgworst > 0) ? Double.valueOf(column[avgworst]) : 0;
					Double avgBest = (avgbest > 0) ? Double.valueOf(column[avgbest]) : 0;
					Double avgAverage = (avgaverage > 0) ? Double.valueOf(column[avgaverage]) : 0;
					
					Assert.assertEquals("avg. executed score does not match", (getScore(population, "avgexecuted")/(4-itr)), avgExecuted,
							0);
					Assert.assertEquals("avg. worst score does not match", (getScore(population, "avgworst")/(4-itr)), avgWorst,
							0);
					Assert.assertEquals("avg. best score does not match", (getScore(population, "avgbest")/(4-itr)), avgBest,
							0);
					Assert.assertEquals("avg average score does not match", (getScore(population, "avgaverage")/getNoOfPlans(population)), avgAverage,
							0);
				}
				iteration++;
			}
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private double getScore(Population population, String condition) {

		Double score = 0.0;
		for (Person person : population.getPersons().values()) {
			ArrayList<Double> scorelist = new ArrayList<Double>();
			ListIterator<? extends Plan> plansItr = person.getPlans().listIterator();
			if(condition == "avgexecuted") {
				Plan plan = plansItr.next();
				if(plan != null) {
					score += plan.getScore();
				}
			}else {
				while(plansItr.hasNext()) {
					Plan plan = plansItr.next();
					scorelist.add(plan.getScore());
				}
				if(condition == "avgworst") {
					score += Collections.min(scorelist);
				}else if(condition == "avgbest"){
					score += Collections.max(scorelist);
				}else if(condition == "avgaverage"){
					score += scorelist.stream().mapToDouble(a -> a).sum();
				}
			}	
		}
		return score;
	}
	
	private int getNoOfPlans(Population population) {
		
		int size = 0;
		for (Person person : population.getPersons().values()) {
			size += person.getPlans().size();
		}
		return size;
	}
	
	private static void decideColumns(String[] columnNames) {

		Integer i = 0;
		while (i < columnNames.length) {
			String name = columnNames[i];
			switch (name) {

			case "avg. EXECUTED":
				avgexecuted = i;
				break;

			case "avg. WORST":
				avgworst = i;
				break;
				
			case "avg. AVG":
				avgaverage = i;
				break;
				
			case "avg. BEST":
				avgbest = i;
				break;

			}
			i++;
		}
	}
}
