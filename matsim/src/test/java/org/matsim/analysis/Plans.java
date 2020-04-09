/**
 * 
 */
package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scoring.EventsToLegs;

/**
 * @author Aravind
 *
 */
public class Plans {

	final Id<Link> link1 = Id.create(10723, Link.class);
	final Id<Link> link2 = Id.create(123160, Link.class);
	final Id<Link> link3 = Id.create(130181, Link.class);
	final Id<Link> link4 = Id.create(139117, Link.class);
	final Id<Link> link5 = Id.create(139100, Link.class);

	public Plan createPlanOne() {

		/****************************
		 * Plan 1 - creating plan 1
		 ************************************/
		final Plan plan = PopulationUtils
				.createPlan(PopulationUtils.getFactory().createPerson(Id.create("1", Person.class)));

		Activity act1 = PopulationUtils.createActivityFromLinkId("home", link1);
		plan.addActivity(act1);
		Leg leg1 = PopulationUtils.createLeg(TransportMode.walk);
		leg1.setDepartureTime(21600.0);
		Route route = RouteUtils.createGenericRouteImpl(link1, link1);
		route.setDistance(100);
		route.setTravelTime(100.0);
		leg1.setRoute(route);
		plan.addLeg(leg1);
		Activity act2 = PopulationUtils.createActivityFromLinkId("leisure", link1);// main mode walk
		plan.addActivity(act2);
		Leg leg2 = PopulationUtils.createLeg(TransportMode.walk);
		leg2.setDepartureTime(21700.0);
		Route route2 = RouteUtils.createGenericRouteImpl(link1, link1);
		route2.setDistance(150);
		route2.setTravelTime(150.0);
		leg2.setRoute(route2);
		plan.addLeg(leg2);
		Activity act3 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1, TransportMode.car);
		act3.setStartTime(21850.0);
		act3.setEndTime(21851.0);
		plan.addActivity(act3);
		Leg leg3 = PopulationUtils.createLeg(TransportMode.car);
		leg3.setDepartureTime(21851.0);
		leg3.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 21851.0);
		Route route3 = RouteUtils.createGenericRouteImpl(link1, link2);
		route3.setDistance(5000);
		route3.setTravelTime(1250.0);
		leg3.setRoute(route3);
		plan.addLeg(leg3);
		Activity act4 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link2, TransportMode.car);
		act4.setStartTime(23101.0);
		act4.setEndTime(23102.0);
		plan.addActivity(act4);
		Leg leg4 = PopulationUtils.createLeg(TransportMode.walk);
		leg4.setDepartureTime(23102.0);
		Route route4 = RouteUtils.createGenericRouteImpl(link2, link2);
		route4.setDistance(300);
		route4.setTravelTime(300.0);
		leg4.setRoute(route4);
		plan.addLeg(leg4);
		Activity act5 = PopulationUtils.createActivityFromLinkId("work", link2);// main mode car
		plan.addActivity(act5);
		Leg leg5 = PopulationUtils.createLeg(TransportMode.walk);
		leg5.setDepartureTime(23402.0);
		Route route5 = RouteUtils.createGenericRouteImpl(link2, link2);
		route5.setDistance(300);
		route5.setTravelTime(300.0);
		leg5.setRoute(route5);
		plan.addLeg(leg5);
		Activity act6 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link2, TransportMode.car);
		act6.setStartTime(23702.0);
		act6.setEndTime(23703.0);
		plan.addActivity(act6);
		Leg leg6 = PopulationUtils.createLeg(TransportMode.car);
		leg6.setDepartureTime(23703.0);
		leg6.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 23703.0);
		Route route6 = RouteUtils.createGenericRouteImpl(link2, link3);
		route6.setDistance(7000);
		route6.setTravelTime(1750.0);
		leg6.setRoute(route6);
		plan.addLeg(leg6);
		Activity act7 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link3, TransportMode.car);
		act7.setStartTime(25453.0);
		act7.setEndTime(25454.0);
		plan.addActivity(act7);
		Leg leg7 = PopulationUtils.createLeg(TransportMode.walk);
		leg7.setDepartureTime(25454.0);
		Route route7 = RouteUtils.createGenericRouteImpl(link3, link3);
		route7.setDistance(150);
		route7.setTravelTime(150.0);
		leg7.setRoute(route7);
		plan.addLeg(leg7);
		Activity act8 = PopulationUtils.createActivityFromLinkId("leisure", link3);// main mode car
		plan.addActivity(act8);
		Leg leg8 = PopulationUtils.createLeg(TransportMode.walk);
		leg8.setDepartureTime(25604.0);
		leg8.setRoute(route7);
		plan.addLeg(leg8);
		Activity act9 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link3, TransportMode.car);
		act9.setStartTime(25754.0);
		act9.setEndTime(25755.0);
		plan.addActivity(act9);
		Leg leg9 = PopulationUtils.createLeg(TransportMode.car);
		leg9.setDepartureTime(25755.0);
		leg9.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 25755.0);
		Route route9 = RouteUtils.createGenericRouteImpl(link3, link4);
		route9.setDistance(6000);
		route9.setTravelTime(1500.0);
		leg9.setRoute(route9);
		plan.addLeg(leg9);
		Activity act10 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4,
				TransportMode.car);
		act10.setStartTime(27255.0);
		act10.setEndTime(27256.0);
		plan.addActivity(act10);
		Leg leg10 = PopulationUtils.createLeg(TransportMode.walk);
		leg10.setDepartureTime(27256.0);
		Route route10 = RouteUtils.createGenericRouteImpl(link4, link4);
		route10.setDistance(400);
		route10.setTravelTime(400.0);
		leg10.setRoute(route10);
		plan.addLeg(leg10);
		Activity act11 = PopulationUtils.createActivityFromLinkId("shopping", link4);// main mode car
		plan.addActivity(act11);
		Leg leg11 = PopulationUtils.createLeg(TransportMode.walk);
		leg11.setDepartureTime(27656.0);
		Route route11 = RouteUtils.createGenericRouteImpl(link4, link4);
		route11.setDistance(300);
		route11.setTravelTime(300.0);
		leg11.setRoute(route11);
		plan.addLeg(leg11);
		Activity act12 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4, TransportMode.pt);
		act12.setStartTime(27956.0);
		act12.setEndTime(27957.0);
		plan.addActivity(act12);
		Leg leg12 = PopulationUtils.createLeg(TransportMode.pt);
		leg12.setDepartureTime(27957.0);
		leg12.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 27967.0);
		Route route12 = RouteUtils.createGenericRouteImpl(link4, link5);
		route12.setDistance(1000);
		route12.setTravelTime(250.0);
		leg12.setRoute(route12);
		plan.addLeg(leg12);
		Activity act13 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link5, TransportMode.pt);
		act13.setStartTime(28217.0);
		act13.setEndTime(28218.0);
		plan.addActivity(act13);
		Leg leg13 = PopulationUtils.createLeg(TransportMode.walk);
		leg13.setDepartureTime(28218.0);
		Route route13 = RouteUtils.createGenericRouteImpl(link5, link5);
		route13.setDistance(200);
		route13.setTravelTime(200.0);
		leg13.setRoute(route13);
		plan.addLeg(leg13);
		Activity act14 = PopulationUtils.createActivityFromLinkId("shopping", link5);// main mode pt
		plan.addActivity(act14);
		Leg leg14 = PopulationUtils.createLeg(TransportMode.walk);
		leg14.setDepartureTime(28418.0);
		leg14.setRoute(route13);
		plan.addLeg(leg14);
		Activity act15 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link5, TransportMode.pt);
		act15.setStartTime(28618.0);
		act15.setEndTime(28619.0);
		plan.addActivity(act15);
		Leg leg15 = PopulationUtils.createLeg(TransportMode.pt);
		leg15.setDepartureTime(28619.0);
		leg15.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 28629.0);
		leg15.setRoute(route12);
		plan.addLeg(leg15);
		Activity act16 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4, TransportMode.pt);
		act16.setStartTime(28879.0);
		act16.setEndTime(28880.0);
		plan.addActivity(act16);
		Leg leg16 = PopulationUtils.createLeg(TransportMode.walk);
		leg16.setDepartureTime(28880.0);
		Route route16 = RouteUtils.createGenericRouteImpl(link4, link4);
		route16.setDistance(300);
		route16.setTravelTime(300.0);
		leg16.setRoute(route16);
		plan.addLeg(leg16);
		Activity act17 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4,
				TransportMode.car);
		act17.setStartTime(29180.0);
		act17.setEndTime(29181.0);
		plan.addActivity(act17);
		Leg leg17 = PopulationUtils.createLeg(TransportMode.car);
		leg17.setDepartureTime(29181.0);
		leg17.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 29181.0);
		Route route17 = RouteUtils.createGenericRouteImpl(link4, link1);
		route17.setDistance(6000);
		route17.setTravelTime(1500.0);
		leg17.setRoute(route17);
		plan.addLeg(leg17);
		Activity act18 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1,
				TransportMode.car);
		act18.setStartTime(30681.0);
		act18.setEndTime(30682.0);
		plan.addActivity(act18);
		Leg leg18 = PopulationUtils.createLeg(TransportMode.walk);
		leg18.setDepartureTime(30682.0);
		leg18.setRoute(route);
		plan.addLeg(leg18);
		Activity act19 = PopulationUtils.createActivityFromLinkId("home", link1);// main mode car
		plan.addActivity(act19);

		return plan;
	}

	public Plan createPlanTwo() {

		/********************************
		 * Plan 2 - creating Plan 2
		 ********************************/
		final Plan plan2 = PopulationUtils
				.createPlan(PopulationUtils.getFactory().createPerson(Id.create("2", Person.class)));

		Activity actp2_1 = PopulationUtils.createActivityFromLinkId("home", link1);
		plan2.addActivity(actp2_1);
		Leg legp2_1 = PopulationUtils.createLeg(TransportMode.walk);
		legp2_1.setDepartureTime(21600.0);
		Route route2_1 = RouteUtils.createGenericRouteImpl(link1, link1);
		route2_1.setDistance(100);
		route2_1.setTravelTime(100.0);
		legp2_1.setRoute(route2_1);
		plan2.addLeg(legp2_1);
		Activity actp2_2 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1,
				TransportMode.car);
		actp2_2.setStartTime(21700.0);
		actp2_2.setEndTime(21701.0);
		plan2.addActivity(actp2_2);
		Leg legp2_2 = PopulationUtils.createLeg(TransportMode.car);
		legp2_2.setDepartureTime(21701.0);
		legp2_2.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 21701.0);
		Route route2_2 = RouteUtils.createGenericRouteImpl(link1, link4);
		route2_2.setDistance(6000);
		route2_2.setTravelTime(1500.0);
		legp2_2.setRoute(route2_2);
		plan2.addLeg(legp2_2);
		Activity actp2_3 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4,
				TransportMode.car);
		actp2_3.setStartTime(23201.0);
		actp2_3.setEndTime(23202.0);
		plan2.addActivity(actp2_3);
		Leg legp2_3 = PopulationUtils.createLeg(TransportMode.walk);
		legp2_3.setDepartureTime(23202.0);
		Route route2_3 = RouteUtils.createGenericRouteImpl(link4, link4);
		route2_3.setDistance(200);
		route2_3.setTravelTime(200.0);
		legp2_3.setRoute(route2_3);
		plan2.addLeg(legp2_3);
		Activity actp2_4 = PopulationUtils.createActivityFromLinkId("work", link4);// main mode car
		plan2.addActivity(actp2_4);
		Leg legp2_4 = PopulationUtils.createLeg(TransportMode.walk);
		legp2_4.setDepartureTime(23402.0);
		Route route2_4 = RouteUtils.createGenericRouteImpl(link4, link4);
		route2_4.setDistance(250);
		route2_4.setTravelTime(250.0);
		legp2_4.setRoute(route2_4);
		plan2.addLeg(legp2_4);
		Activity actp2_5 = PopulationUtils.createActivityFromLinkId("leisure", link4);// main mode walk
		plan2.addActivity(actp2_5);
		Leg legp2_5 = PopulationUtils.createLeg(TransportMode.walk);
		legp2_5.setDepartureTime(23652.0);
		legp2_5.setRoute(route2_4);
		plan2.addLeg(legp2_5);
		Activity actp2_6 = PopulationUtils.createActivityFromLinkId("work", link4);// main mode walk
		plan2.addActivity(actp2_6);
		Leg legp2_6 = PopulationUtils.createLeg(TransportMode.walk);
		legp2_6.setDepartureTime(23902.0);
		legp2_6.setRoute(route2_3);
		plan2.addLeg(legp2_6);
		Activity actp2_7 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link4,
				TransportMode.car);
		actp2_7.setStartTime(24102.0);
		actp2_7.setEndTime(24103.0);
		plan2.addActivity(actp2_7);
		Leg legp2_7 = PopulationUtils.createLeg(TransportMode.car);
		legp2_7.setDepartureTime(24103.0);
		legp2_7.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 24103.0);
		legp2_7.setRoute(route2_2);
		plan2.addLeg(legp2_7);
		Activity actp2_8 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1,
				TransportMode.car);
		actp2_8.setStartTime(25603.0);
		actp2_8.setEndTime(25604.0);
		plan2.addActivity(actp2_8);
		Leg legp2_8 = PopulationUtils.createLeg(TransportMode.walk);
		legp2_8.setDepartureTime(25604.0);
		legp2_8.setRoute(route2_1);
		plan2.addLeg(legp2_8);
		Activity actp2_9 = PopulationUtils.createActivityFromLinkId("home", link1);// main mode car
		plan2.addActivity(actp2_9);

		return plan2;
	}

	public Plan createPlanThree() {

		/*****************************
		 * Plan 3 - creating Plan 3
		 ************************************/
		final Plan plan3 = PopulationUtils
				.createPlan(PopulationUtils.getFactory().createPerson(Id.create("3", Person.class)));

		Activity actp3_1 = PopulationUtils.createActivityFromLinkId("home", link1);
		plan3.addActivity(actp3_1);
		Leg legp3_1 = PopulationUtils.createLeg(TransportMode.walk);
		legp3_1.setDepartureTime(21600.0);
		Route route3_1 = RouteUtils.createGenericRouteImpl(link1, link1);
		route3_1.setDistance(100);
		route3_1.setTravelTime(100.0);
		legp3_1.setRoute(route3_1);
		plan3.addLeg(legp3_1);
		Activity actp3_2 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1,
				TransportMode.car);
		actp3_2.setStartTime(21700.0);
		actp3_2.setEndTime(21701.0);
		plan3.addActivity(actp3_2);
		Leg legp3_2 = PopulationUtils.createLeg(TransportMode.car);
		legp3_2.setDepartureTime(21701.0);
		legp3_2.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 21701.0);
		Route route3_2 = RouteUtils.createGenericRouteImpl(link1, link5);
		route3_2.setDistance(8000);
		route3_2.setTravelTime(2000.0);
		legp3_2.setRoute(route3_2);
		plan3.addLeg(legp3_2);
		Activity actp3_3 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link5,
				TransportMode.car);
		actp3_3.setStartTime(23701.0);
		actp3_3.setEndTime(23702.0);
		plan3.addActivity(actp3_3);
		Leg legp3_3 = PopulationUtils.createLeg(TransportMode.walk);
		legp3_3.setDepartureTime(23702.0);
		Route route3_3 = RouteUtils.createGenericRouteImpl(link5, link5);
		route3_3.setDistance(300);
		route3_3.setTravelTime(300.0);
		legp3_3.setRoute(route3_3);
		plan3.addLeg(legp3_3);
		Activity actp3_4 = PopulationUtils.createActivityFromLinkId("shopping", link5);// main mode car
		plan3.addActivity(actp3_4);
		Leg legp3_4 = PopulationUtils.createLeg(TransportMode.walk);
		legp3_4.setDepartureTime(24002.0);
		legp3_4.setRoute(route3_3);
		plan3.addLeg(legp3_4);
		Activity actp3_5 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link5,
				TransportMode.car);
		actp3_5.setStartTime(24002.0);
		actp3_5.setEndTime(24003.0);
		plan3.addActivity(actp3_5);
		Leg legp3_5 = PopulationUtils.createLeg(TransportMode.car);
		legp3_5.setDepartureTime(24003.0);
		legp3_5.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 24003.0);
		legp3_5.setRoute(route3_2);
		plan3.addLeg(legp3_5);
		Activity actp3_6 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1,
				TransportMode.car);
		actp3_6.setStartTime(26003.0);
		actp3_6.setEndTime(26004.0);
		plan3.addActivity(actp3_6);
		Leg legp3_6 = PopulationUtils.createLeg(TransportMode.walk);
		legp3_6.setDepartureTime(26004.0);
		legp3_6.setRoute(route3_1);
		plan3.addLeg(legp3_6);
		Activity actp3_8 = PopulationUtils.createActivityFromLinkId("home", link1);// main mode car
		plan3.addActivity(actp3_8);

		return plan3;
	}

	public Plan createPlanFour() {

		/************************
		 * Plan 4-----creating Plan 4
		 **************************************/
		final Plan plan4 = PopulationUtils
				.createPlan(PopulationUtils.getFactory().createPerson(Id.create("4", Person.class)));

		Activity actp4_1 = PopulationUtils.createActivityFromLinkId("home", link1);
		plan4.addActivity(actp4_1);
		Leg legp4_1 = PopulationUtils.createLeg(TransportMode.walk);
		legp4_1.setDepartureTime(21600.0);
		Route route4_1 = RouteUtils.createGenericRouteImpl(link1, link1);
		route4_1.setDistance(350);
		route4_1.setTravelTime(350.0);
		legp4_1.setRoute(route4_1);
		plan4.addLeg(legp4_1);
		Activity actp4_2 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1,
				TransportMode.car);
		actp4_2.setStartTime(21950.0);
		actp4_2.setEndTime(21951.0);
		plan4.addActivity(actp4_2);
		Leg legp4_2 = PopulationUtils.createLeg(TransportMode.pt);
		legp4_2.setDepartureTime(21951.0);
		legp4_2.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 21961.0);
		Route route4_2 = RouteUtils.createGenericRouteImpl(link1, link3);
		route4_2.setDistance(6500);
		route4_2.setTravelTime(1625.0);
		legp4_2.setRoute(route4_2);
		plan4.addLeg(legp4_2);
		Activity actp4_3 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link3,
				TransportMode.pt);
		actp4_3.setStartTime(23586.0);
		actp4_3.setEndTime(23587.0);
		plan4.addActivity(actp4_3);
		Leg legp4_3 = PopulationUtils.createLeg(TransportMode.walk);
		legp4_3.setDepartureTime(23587.0);
		Route route4_3 = RouteUtils.createGenericRouteImpl(link3, link3);
		route4_3.setDistance(250);
		route4_3.setTravelTime(250.0);
		legp4_3.setRoute(route4_3);
		plan4.addLeg(legp4_3);
		Activity actp4_4 = PopulationUtils.createActivityFromLinkId("shopping", link3);// main mode pt
		plan4.addActivity(actp4_4);
		Leg legp4_4 = PopulationUtils.createLeg(TransportMode.walk);
		legp4_4.setDepartureTime(23837.0);
		legp4_4.setRoute(route4_3);
		plan4.addLeg(legp4_4);
		Activity actp4_5 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link3,
				TransportMode.pt);
		actp4_5.setStartTime(24087.0);
		actp4_5.setEndTime(24088.0);
		plan4.addActivity(actp4_5);
		Leg legp4_5 = PopulationUtils.createLeg(TransportMode.pt);
		legp4_5.setDepartureTime(24088.0);
		legp4_5.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 24098.0);
		legp4_5.setRoute(route4_2);
		plan4.addLeg(legp4_5);
		Activity actp4_6 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(null, link1,
				TransportMode.car);
		actp4_6.setStartTime(25723.0);
		actp4_6.setEndTime(25724.0);
		plan4.addActivity(actp4_6);
		Leg legp4_6 = PopulationUtils.createLeg(TransportMode.walk);
		legp4_6.setDepartureTime(25724.0);
		legp4_6.setRoute(route4_1);
		plan4.addLeg(legp4_6);
		Activity actp4_8 = PopulationUtils.createActivityFromLinkId("home", link1);// main mode pt
		plan4.addActivity(actp4_8);

		return plan4;
	}
}
