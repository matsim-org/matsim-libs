/**
 * 
 */
package org.matsim.analysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

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
	
	Config config = ConfigUtils.createConfig();
	Scenario scenario = ScenarioUtils.createScenario(config);
	TransitSchedule schedule = scenario.getTransitSchedule();
	TransitScheduleFactory sBuilder = schedule.getFactory();
	ActivityFacilities facilities = scenario.getActivityFacilities();
	ActivityFacilitiesFactory aff = facilities.getFactory();

	public Plan createPlanOne() {

		/****************************
		 * Plan 1 - creating plan 1
		 ************************************/
		final Plan plan = PopulationUtils
				.createPlan(PopulationUtils.getFactory().createPerson(Id.create("1", Person.class)));

		Coord coord1 = CoordUtils.createCoord(30.0, 50.0);
		ActivityFacility f1 = aff.createActivityFacility(
				Id.create("id1", ActivityFacility.class),
				coord1, link1);
		facilities.addActivityFacility(f1);
		Activity act1 = PopulationUtils.createActivityFromFacilityId("home", f1.getId());
		act1.setStartTime(21599.0);
		act1.setEndTime(21600.0);
		plan.addActivity(act1);
		Coord coord2 = CoordUtils.createCoord(100.0, 120.0);
		ActivityFacility f2 = aff.createActivityFacility(
				Id.create("id2", ActivityFacility.class),
				coord2, link2);
		facilities.addActivityFacility(f2);
		Leg leg1 = PopulationUtils.createLeg(TransportMode.walk);
		leg1.getAttributes().putAttribute("startcoord", coord1);
		leg1.getAttributes().putAttribute("endcoord", coord2);
		leg1.setDepartureTime(21600.0);
		Route route = RouteUtils.createGenericRouteImpl(link1, link1);
		route.setDistance(100);
		route.setTravelTime(100.0);
		leg1.setRoute(route);
		leg1.setTravelTime(100.0);
		plan.addLeg(leg1);
		Activity act2 = PopulationUtils.createActivityFromFacilityId("leisure", f2.getId());// main mode walk
		act2.setStartTime(21700.0);
		act2.setEndTime(23500.0);
		plan.addActivity(act2);
		Coord coord3 = CoordUtils.createCoord(206.0, 226.0);
		ActivityFacility f3 = aff.createActivityFacility(
				Id.create("id3", ActivityFacility.class),
				coord3, link1);
		facilities.addActivityFacility(f3);
		Leg leg2 = PopulationUtils.createLeg(TransportMode.walk);
		leg2.getAttributes().putAttribute("startcoord", coord2);
		leg2.getAttributes().putAttribute("endcoord", coord3);
		leg2.setDepartureTime(23500.0);
		Route route2 = RouteUtils.createGenericRouteImpl(link1, link1);
		route2.setDistance(150);
		route2.setTravelTime(150.0);
		leg2.setRoute(route2);
		leg2.setTravelTime(150.0);
		plan.addLeg(leg2);
		Activity act3 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coord3, link1, TransportMode.car);
		act3.setFacilityId(f3.getId());
//		act3.setStartTime(23650.0);
//		act3.setEndTime(23651.0);
		plan.addActivity(act3);
		Coord coord4 = CoordUtils.createCoord(3529.0, 3309.0);
		ActivityFacility f4 = aff.createActivityFacility(
				Id.create("id4", ActivityFacility.class),
				coord4, link2);
		facilities.addActivityFacility(f4);
		Leg leg3 = PopulationUtils.createLeg(TransportMode.car);
		leg3.getAttributes().putAttribute("startcoord", coord3);
		leg3.getAttributes().putAttribute("endcoord", coord4);
		leg3.setDepartureTime(23651.0);
		leg3.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 23651.0);
		leg3.getAttributes().putAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME, Id.createVehicleId("car3"));
		Route route3 = RouteUtils.createGenericRouteImpl(link1, link2);
		route3.setDistance(5000);
		route3.setTravelTime(1250.0);
		leg3.setRoute(route3);
		leg3.setTravelTime(1250.0);
		plan.addLeg(leg3);
		Activity act4 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coord4, link2, TransportMode.car);
//		act4.setStartTime(24901.0);
//		act4.setEndTime(24902.0);
		act4.setFacilityId(f4.getId());
		plan.addActivity(act4);
		Coord coord5 = CoordUtils.createCoord(3741.0, 3521.0);
		ActivityFacility f5 = aff.createActivityFacility(
				Id.create("id5", ActivityFacility.class),
				coord5, link2);
		facilities.addActivityFacility(f5);
		Leg leg4 = PopulationUtils.createLeg(TransportMode.walk);
		leg4.getAttributes().putAttribute("startcoord", coord4);
		leg4.getAttributes().putAttribute("endcoord", coord5);
		leg4.setDepartureTime(24902.0);
		Route route4 = RouteUtils.createGenericRouteImpl(link2, link2);
		route4.setDistance(300);
		route4.setTravelTime(300.0);
		leg4.setRoute(route4);
		leg4.setTravelTime(300.0);
		plan.addLeg(leg4);
		Activity act5 = PopulationUtils.createActivityFromFacilityId("work", f5.getId());// main mode car
		act5.setStartTime(25202.0);
		act5.setEndTime(43202.0);
		plan.addActivity(act5);
		Coord coord6 = coord4; // CoordUtils.createCoord(3529.0, 3309.0);
		ActivityFacility f6 = aff.createActivityFacility(
				Id.create("id6", ActivityFacility.class),
				coord6, link2);
		facilities.addActivityFacility(f6);
		Leg leg5 = PopulationUtils.createLeg(TransportMode.walk);
		leg5.getAttributes().putAttribute("startcoord", coord5);
		leg5.getAttributes().putAttribute("endcoord", coord6);
		leg5.setDepartureTime(43202.0);
		Route route5 = RouteUtils.createGenericRouteImpl(link2, link2);
		route5.setDistance(300);
		route5.setTravelTime(300.0);
		leg5.setRoute(route5);
		leg5.setTravelTime(300.0);
		plan.addLeg(leg5);
		Activity act6 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coord6, link2, TransportMode.car);
//		act6.setStartTime(43502.0);
//		act6.setEndTime(43503.0);
		act6.setFacilityId(f6.getId());
		plan.addActivity(act6);
		Coord coord7 = CoordUtils.createCoord(8478.0, 8258.0);
		ActivityFacility f7 = aff.createActivityFacility(
				Id.create("id7", ActivityFacility.class),
				coord7, link3);
		facilities.addActivityFacility(f7);
		Leg leg6 = PopulationUtils.createLeg(TransportMode.car);
		leg6.getAttributes().putAttribute("startcoord", coord6);
		leg6.getAttributes().putAttribute("endcoord", coord7);
		leg6.setDepartureTime(43503.0);
		leg6.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 43503.0);
		leg6.getAttributes().putAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME, Id.createVehicleId("car6"));
		Route route6 = RouteUtils.createGenericRouteImpl(link2, link3);
		route6.setDistance(7000);
		route6.setTravelTime(1750.0);
		leg6.setRoute(route6);
		leg6.setTravelTime(1750.0);
		plan.addLeg(leg6);
		Activity act7 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coord7, link3, TransportMode.car);
//		act7.setStartTime(45253.0);
//		act7.setEndTime(45254.0);
		act7.setFacilityId(f7.getId());
		plan.addActivity(act7);
		Coord coord8 = CoordUtils.createCoord(8584.0, 8364.0);
		ActivityFacility f8 = aff.createActivityFacility(
				Id.create("id8", ActivityFacility.class),
				coord8, link3);
		facilities.addActivityFacility(f8);
		Leg leg7 = PopulationUtils.createLeg(TransportMode.walk);
		leg7.getAttributes().putAttribute("startcoord", coord7);
		leg7.getAttributes().putAttribute("endcoord", coord8);
		leg7.setDepartureTime(45254.0);
		Route route7 = RouteUtils.createGenericRouteImpl(link3, link3);
		route7.setDistance(150);
		route7.setTravelTime(150.0);
		leg7.setRoute(route7);
		leg7.setTravelTime(150.0);
		plan.addLeg(leg7);
		Activity act8 = PopulationUtils.createActivityFromFacilityId("leisure", f8.getId());// main mode car
		act8.setStartTime(45404.0);
		act8.setEndTime(47204.0);
		plan.addActivity(act8);
		Coord coord9 = coord7; // CoordUtils.createCoord(8478.0, 8258.0);
		ActivityFacility f9 = aff.createActivityFacility(
				Id.create("id9", ActivityFacility.class),
				coord9, link3);
		facilities.addActivityFacility(f9);
		Leg leg8 = PopulationUtils.createLeg(TransportMode.walk);
		leg8.getAttributes().putAttribute("startcoord", coord8);
		leg8.getAttributes().putAttribute("endcoord", coord9);
		leg8.setDepartureTime(47204.0);
		leg8.setRoute(route7);
		leg8.setTravelTime(150.0);
		plan.addLeg(leg8);
		Activity act9 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coord9, link3, TransportMode.car);
//		act9.setStartTime(47354.0);
//		act9.setEndTime(47355.0);
		act9.setFacilityId(f9.getId());
		plan.addActivity(act9);
		Coord coord10 = CoordUtils.createCoord(12720.0, 12500.0);
		ActivityFacility f10 = aff.createActivityFacility(
				Id.create("id10", ActivityFacility.class),
				coord10, link4);
		facilities.addActivityFacility(f10);
		Leg leg9 = PopulationUtils.createLeg(TransportMode.car);
		leg9.getAttributes().putAttribute("startcoord", coord9);
		leg9.getAttributes().putAttribute("endcoord", coord10);
		leg9.setDepartureTime(47355.0);
		leg9.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 47355.0);
		leg9.getAttributes().putAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME, Id.createVehicleId("car9"));
		Route route9 = RouteUtils.createGenericRouteImpl(link3, link4);
		route9.setDistance(6000);
		route9.setTravelTime(1500.0);
		leg9.setRoute(route9);
		leg9.setTravelTime(1500.0);
		plan.addLeg(leg9);
		Activity act10 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coord10, link4,
				TransportMode.car);
//		act10.setStartTime(48855.0);
//		act10.setEndTime(48856.0);
		act10.setFacilityId(f10.getId());
		plan.addActivity(act10);
		Coord coord11 = CoordUtils.createCoord(13002.0, 12782.0);
		ActivityFacility f11 = aff.createActivityFacility(
				Id.create("id11", ActivityFacility.class),
				coord11, link4);
		facilities.addActivityFacility(f11);
		Leg leg10 = PopulationUtils.createLeg(TransportMode.walk);
		leg10.getAttributes().putAttribute("startcoord", coord10);
		leg10.getAttributes().putAttribute("endcoord", coord11);
		leg10.setDepartureTime(48856.0);
		Route route10 = RouteUtils.createGenericRouteImpl(link4, link4);
		route10.setDistance(400);
		route10.setTravelTime(400.0);
		leg10.setRoute(route10);
		leg10.setTravelTime(400.0);
		plan.addLeg(leg10);
		Activity act11 = PopulationUtils.createActivityFromFacilityId("shopping", f11.getId());// main mode car
		act11.setStartTime(49256.0);
		act11.setEndTime(51056.0);
		plan.addActivity(act11);
		Coord coord12 = CoordUtils.createCoord(13214.0, 12994.0);
		ActivityFacility f12 = aff.createActivityFacility(
				Id.create("id12", ActivityFacility.class),
				coord12, link4);
		facilities.addActivityFacility(f12);
		Leg leg11 = PopulationUtils.createLeg(TransportMode.walk);
		leg11.getAttributes().putAttribute("startcoord", coord11);
		leg11.getAttributes().putAttribute("endcoord", coord12);
		leg11.setDepartureTime(51056.0);
		Route route11 = RouteUtils.createGenericRouteImpl(link4, link4);
		route11.setDistance(300);
		route11.setTravelTime(300.0);
		leg11.setRoute(route11);
		leg11.setTravelTime(300.0);
		plan.addLeg(leg11);
		Activity act12 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coord12, link4, TransportMode.pt);
//		act12.setStartTime(51356.0);
//		act12.setEndTime(51357.0);
		act12.setFacilityId(f12.getId());
		plan.addActivity(act12);
		Coord coord13 = CoordUtils.createCoord(13916.0, 13696.0);
		ActivityFacility f13 = aff.createActivityFacility(
				Id.create("id13", ActivityFacility.class),
				coord13, link5);
		facilities.addActivityFacility(f13);
		Leg leg12 = PopulationUtils.createLeg(TransportMode.pt);
		leg12.getAttributes().putAttribute("startcoord", coord12);
		leg12.getAttributes().putAttribute("endcoord", coord13);
		leg12.setDepartureTime(51357.0);
		leg12.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 51367.0);
		leg12.getAttributes().putAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME, Id.createVehicleId("pt12"));
		//Route route12 = RouteUtils.createGenericRouteImpl(link4, link5);
		//pt route
		TransitStopFacility egressFacility = sBuilder.createTransitStopFacility(Id.create("1", TransitStopFacility.class), coord12, false);
		TransitStopFacility accessFacility = sBuilder.createTransitStopFacility(Id.create("2", TransitStopFacility.class), coord12, false);
		TransitPassengerRoute ptRoute = new DefaultTransitPassengerRoute(accessFacility.getLinkId(), egressFacility.getLinkId(), accessFacility.getId(), egressFacility.getId(), Id.create("transitline1", TransitLine.class), Id.create("transitroute1", TransitRoute.class));
		ptRoute.setDistance(1000);
		ptRoute.setTravelTime(250.0);
		ptRoute.setStartLinkId(link4);
		ptRoute.setEndLinkId(link5);
		leg12.setRoute(ptRoute);
		leg12.setTravelTime(250.0);
		plan.addLeg(leg12);
		Activity act13 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coord13, link5, TransportMode.pt);
//		act13.setStartTime(51617.0);
//		act13.setEndTime(51618.0);
		act13.setFacilityId(f13.getId());
		plan.addActivity(act13);
		Coord coord14 = CoordUtils.createCoord(14057.0, 13837.0);
		ActivityFacility f14 = aff.createActivityFacility(
				Id.create("id14", ActivityFacility.class),
				coord14, link5);
		facilities.addActivityFacility(f14);
		Leg leg13 = PopulationUtils.createLeg(TransportMode.walk);
		leg13.getAttributes().putAttribute("startcoord", coord13);
		leg13.getAttributes().putAttribute("endcoord", coord14);
		leg13.setDepartureTime(51618.0);
		Route route13 = RouteUtils.createGenericRouteImpl(link5, link5);
		route13.setDistance(200);
		route13.setTravelTime(200.0);
		leg13.setRoute(route13);
		leg13.setTravelTime(200.0);
		plan.addLeg(leg13);
		Activity act14 = PopulationUtils.createActivityFromFacilityId("shopping", f14.getId());// main mode pt
		act14.setStartTime(51818.0);
		act14.setEndTime(53618.0);
		plan.addActivity(act14);
		Coord coord15 = CoordUtils.createCoord(13916.0, 13696.0);
		ActivityFacility f15 = aff.createActivityFacility(
				Id.create("id15", ActivityFacility.class),
				coord15, link5);
		facilities.addActivityFacility(f15);
		Leg leg14 = PopulationUtils.createLeg(TransportMode.walk);
		leg14.getAttributes().putAttribute("startcoord", coord14);
		leg14.getAttributes().putAttribute("endcoord", coord15);
		leg14.setDepartureTime(53618.0);
		leg14.setRoute(route13);
		leg14.setTravelTime(200.0);
		plan.addLeg(leg14);
		Activity act15 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coord15, link5, TransportMode.pt);
//		act15.setStartTime(53818.0);
//		act15.setEndTime(53819.0);
		act15.setFacilityId(f15.getId());
		plan.addActivity(act15);
		Coord coord16 = CoordUtils.createCoord(13214.0, 12994.0);
		ActivityFacility f16 = aff.createActivityFacility(
				Id.create("id16", ActivityFacility.class),
				coord16, link4);
		facilities.addActivityFacility(f16);
		Leg leg15 = PopulationUtils.createLeg(TransportMode.pt);
		leg15.getAttributes().putAttribute("startcoord", coord15);
		leg15.getAttributes().putAttribute("endcoord", coord16);
		leg15.setDepartureTime(53819.0);
		leg15.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 53829.0);
		leg15.getAttributes().putAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME, Id.createVehicleId("pt15"));
		leg15.setRoute(ptRoute);
		leg15.setTravelTime(250.0);
		plan.addLeg(leg15);
		Activity act16 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coord16, link4, TransportMode.pt);
//		act16.setStartTime(54079.0);
//		act16.setEndTime(54080.0);
		act16.setFacilityId(f16.getId());
		plan.addActivity(act16);
		Coord coord17 = coord10; // CoordUtils.createCoord(12720.0, 12500.0);
		ActivityFacility f17 = aff.createActivityFacility(
				Id.create("id17", ActivityFacility.class),
				coord17, link4);
		facilities.addActivityFacility(f17);
		Leg leg16 = PopulationUtils.createLeg(TransportMode.walk);
		leg16.getAttributes().putAttribute("startcoord", coord16);
		leg16.getAttributes().putAttribute("endcoord", coord17);
		leg16.setDepartureTime(54080.0);
		Route route16 = RouteUtils.createGenericRouteImpl(link4, link4);
		route16.setDistance(700);
		route16.setTravelTime(700.0);
		leg16.setRoute(route16);
		leg16.setTravelTime(700.0);
		plan.addLeg(leg16);
		Activity act17 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coord17, link4,
				TransportMode.car);
//		act17.setStartTime(54780.0);
//		act17.setEndTime(54781.0);
		act17.setFacilityId(f17.getId());
		plan.addActivity(act17);
		Coord coord18 = coord3; // CoordUtils.createCoord(206.0, 226.0);
		ActivityFacility f18 = aff.createActivityFacility(
				Id.create("id18", ActivityFacility.class),
				coord18, link1);
		facilities.addActivityFacility(f18);
		Leg leg17 = PopulationUtils.createLeg(TransportMode.car);
		leg17.getAttributes().putAttribute("startcoord", coord17);
		leg17.getAttributes().putAttribute("endcoord", coord18);
		leg17.setDepartureTime(54781.0);
		leg17.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 54781.0);
		leg17.getAttributes().putAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME, Id.createVehicleId("car17"));
		Route route17 = RouteUtils.createGenericRouteImpl(link4, link1);
		route17.setDistance(18000);
		route17.setTravelTime(4500.0);
		leg17.setRoute(route17);
		leg17.setTravelTime(4500.0);
		plan.addLeg(leg17);
		Activity act18 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coord18, link1,
				TransportMode.car);
//		act18.setStartTime(59281.0);
//		act18.setEndTime(59282.0);
		act18.setFacilityId(f18.getId());
		plan.addActivity(act18);
		Leg leg18 = PopulationUtils.createLeg(TransportMode.walk);
		leg18.getAttributes().putAttribute("startcoord", coord18);
		leg18.getAttributes().putAttribute("endcoord", coord1);
		leg18.setDepartureTime(59282.0);
		Route route18 = RouteUtils.createGenericRouteImpl(link1, link1);
		route18.setDistance(250);
		route18.setTravelTime(250.0);
		leg18.setRoute(route18);
		leg18.setTravelTime(250.0);
		plan.addLeg(leg18);
		Activity act19 = PopulationUtils.createActivityFromFacilityId("home", f1.getId());// main mode car
		act19.setStartTime(59382.0);
		act19.setEndTime(59382.0);
		plan.addActivity(act19);

		plan.setScore(123.0);

		return plan;
	}

	public Plan createPlanTwo() {

		/********************************
		 * Plan 2 - creating Plan 2
		 ********************************/
		final Plan plan2 = PopulationUtils
				.createPlan(PopulationUtils.getFactory().createPerson(Id.create("2", Person.class)));

		Coord coordp2_1 = CoordUtils.createCoord(30.0, 50.0);
		ActivityFacility fp2_1 = aff.createActivityFacility(
				Id.create("p2_id1", ActivityFacility.class),
				coordp2_1, link1);
		facilities.addActivityFacility(fp2_1);
		Activity actp2_1 = PopulationUtils.createActivityFromFacilityId("home", fp2_1.getId());
		actp2_1.setStartTime(21599.0);
		actp2_1.setEndTime(21600.0);
		plan2.addActivity(actp2_1);
		Coord coordp2_2 = CoordUtils.createCoord(100.0, 120.0);
		ActivityFacility fp2_2 = aff.createActivityFacility(
				Id.create("p2_id2", ActivityFacility.class),
				coordp2_2, link1);
		facilities.addActivityFacility(fp2_2);
		Leg legp2_1 = PopulationUtils.createLeg(TransportMode.walk);
		legp2_1.getAttributes().putAttribute("startcoord", coordp2_1);
		legp2_1.getAttributes().putAttribute("endcoord", coordp2_2);
		legp2_1.setDepartureTime(21600.0);
		Route route2_1 = RouteUtils.createGenericRouteImpl(link1, link1);
		route2_1.setDistance(100);
		route2_1.setTravelTime(100.0);
		legp2_1.setRoute(route2_1);
		legp2_1.setTravelTime(100.0);
		plan2.addLeg(legp2_1);
		Activity actp2_2 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coordp2_2, link1,
				TransportMode.car);
//		actp2_2.setStartTime(21700.0);
//		actp2_2.setEndTime(21701.0);
		actp2_2.setFacilityId(fp2_2.getId());
		plan2.addActivity(actp2_2);
		Coord coordp2_3 = CoordUtils.createCoord(4342.0, 4362.0);
		ActivityFacility fp2_3 = aff.createActivityFacility(
				Id.create("p2_id3", ActivityFacility.class),
				coordp2_3, link4);
		facilities.addActivityFacility(fp2_3);
		Leg legp2_2 = PopulationUtils.createLeg(TransportMode.car);
		legp2_2.getAttributes().putAttribute("startcoord", coordp2_2);
		legp2_2.getAttributes().putAttribute("endcoord", coordp2_3);
		legp2_2.setDepartureTime(21701.0);
		legp2_2.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 21701.0);
		legp2_2.getAttributes().putAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME, Id.createVehicleId("carp2_2"));
		Route route2_2 = RouteUtils.createGenericRouteImpl(link1, link4);
		route2_2.setDistance(6000);
		route2_2.setTravelTime(1500.0);
		legp2_2.setRoute(route2_2);
		legp2_2.setTravelTime(1500.0);
		plan2.addLeg(legp2_2);
		Activity actp2_3 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coordp2_3, link4,
				TransportMode.car);
//		actp2_3.setStartTime(23201.0);
//		actp2_3.setEndTime(23202.0);
		actp2_3.setFacilityId(fp2_3.getId());
		plan2.addActivity(actp2_3);
		Coord coordp2_4 = CoordUtils.createCoord(4483.0, 4503.0);
		ActivityFacility fp2_4 = aff.createActivityFacility(
				Id.create("p2_id4", ActivityFacility.class),
				coordp2_4, link4);
		facilities.addActivityFacility(fp2_4);
		Leg legp2_3 = PopulationUtils.createLeg(TransportMode.walk);
		legp2_3.getAttributes().putAttribute("startcoord", coordp2_3);
		legp2_3.getAttributes().putAttribute("endcoord", coordp2_4);
		legp2_3.setDepartureTime(23202.0);
		Route route2_3 = RouteUtils.createGenericRouteImpl(link4, link4);
		route2_3.setDistance(200);
		route2_3.setTravelTime(200.0);
		legp2_3.setRoute(route2_3);
		legp2_3.setTravelTime(200.0);
		plan2.addLeg(legp2_3);
		Activity actp2_4 = PopulationUtils.createActivityFromFacilityId("work", fp2_4.getId());// main mode car
		actp2_4.setStartTime(23402.0);
		actp2_4.setEndTime(41402.0);
		plan2.addActivity(actp2_4);
		Coord coordp2_5 = CoordUtils.createCoord(4659.0, 4679.0);
		ActivityFacility fp2_5 = aff.createActivityFacility(
				Id.create("p2_id5", ActivityFacility.class),
				coordp2_5, link5);
		facilities.addActivityFacility(fp2_5);
		Leg legp2_4 = PopulationUtils.createLeg(TransportMode.walk);
		legp2_4.getAttributes().putAttribute("startcoord", coordp2_4);
		legp2_4.getAttributes().putAttribute("endcoord", coordp2_5);
		legp2_4.setDepartureTime(41402.0);
		Route route2_4 = RouteUtils.createGenericRouteImpl(link4, link4);
		route2_4.setDistance(250);
		route2_4.setTravelTime(250.0);
		legp2_4.setRoute(route2_4);
		legp2_4.setTravelTime(250.0);
		plan2.addLeg(legp2_4);
		Activity actp2_5 = PopulationUtils.createActivityFromFacilityId("leisure", fp2_5.getId());// main mode walk
		actp2_5.setStartTime(41652.0);
		actp2_5.setEndTime(43452.0);
		plan2.addActivity(actp2_5);
		Coord coordp2_6 = CoordUtils.createCoord(4483.0, 4503.0);
		ActivityFacility fp2_6 = aff.createActivityFacility(
				Id.create("p2_id6", ActivityFacility.class),
				coordp2_6, link4);
		facilities.addActivityFacility(fp2_6);
		Leg legp2_5 = PopulationUtils.createLeg(TransportMode.walk);
		legp2_5.getAttributes().putAttribute("startcoord", coordp2_5);
		legp2_5.getAttributes().putAttribute("endcoord", coordp2_6);
		legp2_5.setDepartureTime(43452.0);
		legp2_5.setRoute(route2_4);
		legp2_5.setTravelTime(250.0);
		plan2.addLeg(legp2_5);
		Activity actp2_6 = PopulationUtils.createActivityFromFacilityId("work", fp2_6.getId());// main mode walk
		actp2_6.setStartTime(43702.0);
		actp2_6.setEndTime(54502.0);
		plan2.addActivity(actp2_6);
		Coord coordp2_7 = CoordUtils.createCoord(4342.0, 4362.0);
		ActivityFacility fp2_7 = aff.createActivityFacility(
				Id.create("p2_id7", ActivityFacility.class),
				coordp2_7, link4);
		facilities.addActivityFacility(fp2_7);
		Leg legp2_6 = PopulationUtils.createLeg(TransportMode.walk);
		legp2_6.getAttributes().putAttribute("startcoord", coordp2_6);
		legp2_6.getAttributes().putAttribute("endcoord", coordp2_7);
		legp2_6.setDepartureTime(54502.0);
		legp2_6.setRoute(route2_3);
		legp2_6.setTravelTime(200.0);
		plan2.addLeg(legp2_6);
		Activity actp2_7 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coordp2_7, link4,
				TransportMode.car);
//		actp2_7.setStartTime(54702.0);
//		actp2_7.setEndTime(54703.0);
		actp2_7.setFacilityId(fp2_7.getId());
		plan2.addActivity(actp2_7);
		Coord coordp2_8 = CoordUtils.createCoord(100.0, 120.0);
		ActivityFacility fp2_8 = aff.createActivityFacility(
				Id.create("p2_id8", ActivityFacility.class),
				coordp2_8, link1);
		facilities.addActivityFacility(fp2_8);
		Leg legp2_7 = PopulationUtils.createLeg(TransportMode.car);
		legp2_7.getAttributes().putAttribute("startcoord", coordp2_7);
		legp2_7.getAttributes().putAttribute("endcoord", coordp2_8);
		legp2_7.setDepartureTime(54703.0);
		legp2_7.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 54703.0);
		legp2_7.getAttributes().putAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME, Id.createVehicleId("carp2_7"));
		legp2_7.setRoute(route2_2);
		legp2_7.setTravelTime(1500.0);
		plan2.addLeg(legp2_7);
		Activity actp2_8 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coordp2_8, link1,
				TransportMode.car);
//		actp2_8.setStartTime(56203.0);
//		actp2_8.setEndTime(56204.0);
		actp2_8.setFacilityId(fp2_8.getId());
		plan2.addActivity(actp2_8);
		Leg legp2_8 = PopulationUtils.createLeg(TransportMode.walk);
		legp2_8.getAttributes().putAttribute("startcoord", coordp2_8);
		legp2_8.getAttributes().putAttribute("endcoord", coordp2_1);
		legp2_8.setDepartureTime(56204.0);
		legp2_8.setRoute(route2_1);
		legp2_8.setTravelTime(100.0);
		plan2.addLeg(legp2_8);
		Activity actp2_9 = PopulationUtils.createActivityFromFacilityId("home", fp2_1.getId());// main mode car
		actp2_9.setStartTime(56304.0);
		actp2_9.setEndTime(56304.0);
		plan2.addActivity(actp2_9);

		plan2.setScore(234.0);

		return plan2;
	}

	public Plan createPlanThree() {

		/*****************************
		 * Plan 3 - creating Plan 3
		 ************************************/
		final Plan plan3 = PopulationUtils
				.createPlan(PopulationUtils.getFactory().createPerson(Id.create("3", Person.class)));

		Coord coordp3_1 = CoordUtils.createCoord(30.0, 50.0);
		ActivityFacility fp3_1 = aff.createActivityFacility(
				Id.create("p3_id1", ActivityFacility.class),
				coordp3_1, link1);
		facilities.addActivityFacility(fp3_1);
		Activity actp3_1 = PopulationUtils.createActivityFromFacilityId("home", fp3_1.getId());
		actp3_1.setStartTime(21590.0);
		actp3_1.setEndTime(21600.0);
		plan3.addActivity(actp3_1);
		Coord coordp3_2 = CoordUtils.createCoord(100.0, 120.0);
		ActivityFacility fp3_2 = aff.createActivityFacility(
				Id.create("p3_id2", ActivityFacility.class),
				coordp3_2, link1);
		facilities.addActivityFacility(fp3_2);
		Leg legp3_1 = PopulationUtils.createLeg(TransportMode.walk);
		legp3_1.getAttributes().putAttribute("startcoord", coordp3_1);
		legp3_1.getAttributes().putAttribute("endcoord", coordp3_2);
		legp3_1.setDepartureTime(21600.0);
		Route route3_1 = RouteUtils.createGenericRouteImpl(link1, link1);
		route3_1.setDistance(100);
		route3_1.setTravelTime(100.0);
		legp3_1.setRoute(route3_1);
		legp3_1.setTravelTime(100.0);
		plan3.addLeg(legp3_1);
		Activity actp3_2 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coordp3_2, link1,
				TransportMode.car);
//		actp3_2.setStartTime(21700.0);
//		actp3_2.setEndTime(21701.0);
		actp3_2.setFacilityId(fp3_2.getId());
		plan3.addActivity(actp3_2);
		Coord coordp3_3 = CoordUtils.createCoord(5756.0, 5776.0);
		ActivityFacility fp3_3 = aff.createActivityFacility(
				Id.create("p3_id3", ActivityFacility.class),
				coordp3_3, link5);
		facilities.addActivityFacility(fp3_3);
		Leg legp3_2 = PopulationUtils.createLeg(TransportMode.car);
		legp3_2.getAttributes().putAttribute("startcoord", coordp3_2);
		legp3_2.getAttributes().putAttribute("endcoord", coordp3_3);
		legp3_2.setDepartureTime(21701.0);
		legp3_2.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 21701.0);
		legp3_2.getAttributes().putAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME, Id.createVehicleId("carp3_2"));
		Route route3_2 = RouteUtils.createGenericRouteImpl(link1, link5);
		route3_2.setDistance(8000);
		route3_2.setTravelTime(2000.0);
		legp3_2.setRoute(route3_2);
		legp3_2.setTravelTime(2000.0);
		plan3.addLeg(legp3_2);
		Activity actp3_3 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coordp3_3, link5,
				TransportMode.car);
//		actp3_3.setStartTime(23701.0);
//		actp3_3.setEndTime(23702.0);
		actp3_3.setFacilityId(fp3_3.getId());
		plan3.addActivity(actp3_3);
		Coord coordp3_4 = CoordUtils.createCoord(5968.0, 5988.0);
		ActivityFacility fp3_4 = aff.createActivityFacility(
				Id.create("p3_id4", ActivityFacility.class),
				coordp3_4, link5);
		facilities.addActivityFacility(fp3_4);
		Leg legp3_3 = PopulationUtils.createLeg(TransportMode.walk);
		legp3_3.getAttributes().putAttribute("startcoord", coordp3_3);
		legp3_3.getAttributes().putAttribute("endcoord", coordp3_4);
		legp3_3.setDepartureTime(23702.0);
		Route route3_3 = RouteUtils.createGenericRouteImpl(link5, link5);
		route3_3.setDistance(300);
		route3_3.setTravelTime(300.0);
		legp3_3.setRoute(route3_3);
		legp3_3.setTravelTime(300.0);
		plan3.addLeg(legp3_3);
		Activity actp3_4 = PopulationUtils.createActivityFromFacilityId("shopping", fp3_4.getId());// main mode car
		actp3_4.setStartTime(24002.0);
		actp3_4.setEndTime(25802.0);
		plan3.addActivity(actp3_4);
		Coord coordp3_5 = CoordUtils.createCoord(5756.0, 5776.0);
		ActivityFacility fp3_5 = aff.createActivityFacility(
				Id.create("p3_id5", ActivityFacility.class),
				coordp3_5, link5);
		facilities.addActivityFacility(fp3_5);
		Leg legp3_4 = PopulationUtils.createLeg(TransportMode.walk);
		legp3_4.getAttributes().putAttribute("startcoord", coordp3_4);
		legp3_4.getAttributes().putAttribute("endcoord", coordp3_5);
		legp3_4.setDepartureTime(25802.0);
		legp3_4.setRoute(route3_3);
		legp3_4.setTravelTime(300.0);
		plan3.addLeg(legp3_4);
		Activity actp3_5 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coordp3_5, link5,
				TransportMode.car);
//		actp3_5.setStartTime(26102.0);
//		actp3_5.setEndTime(26103.0);
		actp3_5.setFacilityId(fp3_5.getId());
		plan3.addActivity(actp3_5);
		Coord coordp3_6 = CoordUtils.createCoord(100.0, 120.0);
		ActivityFacility fp3_6 = aff.createActivityFacility(
				Id.create("p3_id6", ActivityFacility.class),
				coordp3_6, link1);
		facilities.addActivityFacility(fp3_6);
		Leg legp3_5 = PopulationUtils.createLeg(TransportMode.car);
		legp3_5.getAttributes().putAttribute("startcoord", coordp3_5);
		legp3_5.getAttributes().putAttribute("endcoord", coordp3_6);
		legp3_5.setDepartureTime(26103.0);
		legp3_5.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 26103.0);
		legp3_5.getAttributes().putAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME, Id.createVehicleId("carp3_5"));
		legp3_5.setRoute(route3_2);
		legp3_5.setTravelTime(2000.0);
		plan3.addLeg(legp3_5);
		Activity actp3_6 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coordp3_6, link1,
				TransportMode.car);
//		actp3_6.setStartTime(28103.0);
//		actp3_6.setEndTime(28104.0);
		actp3_6.setFacilityId(fp3_6.getId());
		plan3.addActivity(actp3_6);
		Leg legp3_6 = PopulationUtils.createLeg(TransportMode.walk);
		legp3_6.getAttributes().putAttribute("startcoord", coordp3_6);
		legp3_6.getAttributes().putAttribute("endcoord", coordp3_1);
		legp3_6.setDepartureTime(28104.0);
		legp3_6.setRoute(route3_1);
		legp3_6.setTravelTime(100.0);
		plan3.addLeg(legp3_6);
		Activity actp3_8 = PopulationUtils.createActivityFromFacilityId("home", fp3_1.getId());// main mode car
		actp3_8.setStartTime(28204.0);
		actp3_8.setEndTime(28204.0);
		plan3.addActivity(actp3_8);

		plan3.setScore(345.67);

		return plan3;
	}

	public Plan createPlanFour() {

		/************************
		 * Plan 4-----creating Plan 4
		 **************************************/
		final Plan plan4 = PopulationUtils
				.createPlan(PopulationUtils.getFactory().createPerson(Id.create("4", Person.class)));

		Coord coordp4_1 = CoordUtils.createCoord(30.0, 50.0);
		ActivityFacility fp4_1 = aff.createActivityFacility(
				Id.create("p4_id1", ActivityFacility.class),
				coordp4_1, link1);
		facilities.addActivityFacility(fp4_1);
		Activity actp4_1 = PopulationUtils.createActivityFromFacilityId("home", fp4_1.getId());
		actp4_1.setStartTime(21590.0);
		actp4_1.setEndTime(21600.0);
		plan4.addActivity(actp4_1);
		Coord coordp4_2 = CoordUtils.createCoord(277.0, 297.0);
		ActivityFacility fp4_2 = aff.createActivityFacility(
				Id.create("p4_id2", ActivityFacility.class),
				coordp4_2, link1);
		facilities.addActivityFacility(fp4_2);
		Leg legp4_1 = PopulationUtils.createLeg(TransportMode.walk);
		legp4_1.getAttributes().putAttribute("startcoord", coordp4_1);
		legp4_1.getAttributes().putAttribute("endcoord", coordp4_2);
		legp4_1.setDepartureTime(21600.0);
		Route route4_1 = RouteUtils.createGenericRouteImpl(link1, link1);
		route4_1.setDistance(350);
		route4_1.setTravelTime(350.0);
		legp4_1.setRoute(route4_1);
		legp4_1.setTravelTime(350.0);
		plan4.addLeg(legp4_1);
		Activity actp4_2 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coordp4_2, link1,
				TransportMode.pt);
//		actp4_2.setStartTime(21950.0);
//		actp4_2.setEndTime(21951.0);
		actp4_2.setFacilityId(fp4_2.getId());
		plan4.addActivity(actp4_2);
		Coord coordp4_3 = CoordUtils.createCoord(4873.0, 4893.0);
		ActivityFacility fp4_3 = aff.createActivityFacility(
				Id.create("p4_id3", ActivityFacility.class),
				coordp4_3, link3);
		facilities.addActivityFacility(fp4_3);
		Leg legp4_2 = PopulationUtils.createLeg(TransportMode.pt);
		legp4_2.getAttributes().putAttribute("startcoord", coordp4_2);
		legp4_2.getAttributes().putAttribute("endcoord", coordp4_3);
		legp4_2.setDepartureTime(21951.0);
		legp4_2.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 21961.0);
		legp4_2.getAttributes().putAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME, Id.createVehicleId("ptp4_2"));
		//pt route
		TransitStopFacility egressFacility = sBuilder.createTransitStopFacility(Id.create("3", TransitStopFacility.class), coordp4_2, false);
		TransitStopFacility accessFacility = sBuilder.createTransitStopFacility(Id.create("4", TransitStopFacility.class), coordp4_2, false);
		TransitPassengerRoute ptRoute = new DefaultTransitPassengerRoute(accessFacility.getLinkId(), egressFacility.getLinkId(), accessFacility.getId(), egressFacility.getId(), Id.create("transitline2", TransitLine.class), Id.create("transitroute2", TransitRoute.class));
		//Route route4_2 = RouteUtils.createGenericRouteImpl(link1, link3);
		ptRoute.setDistance(6500);
		ptRoute.setTravelTime(1625.0);
		ptRoute.setStartLinkId(link1);
		ptRoute.setEndLinkId(link3);
		legp4_2.setRoute(ptRoute);
		legp4_2.setTravelTime(1625.0);
		plan4.addLeg(legp4_2);
		Activity actp4_3 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coordp4_3, link3,
				TransportMode.pt);
//		actp4_3.setStartTime(23586.0);
//		actp4_3.setEndTime(23587.0);
		actp4_3.setFacilityId(fp4_3.getId());
		plan4.addActivity(actp4_3);
		Coord coordp4_4 = CoordUtils.createCoord(5049.0, 5069.0);
		ActivityFacility fp4_4 = aff.createActivityFacility(
				Id.create("p4_id4", ActivityFacility.class),
				coordp4_4, link3);
		facilities.addActivityFacility(fp4_4);
		Leg legp4_3 = PopulationUtils.createLeg(TransportMode.walk);
		legp4_3.getAttributes().putAttribute("startcoord", coordp4_3);
		legp4_3.getAttributes().putAttribute("endcoord", coordp4_4);
		legp4_3.setDepartureTime(23587.0);
		Route route4_3 = RouteUtils.createGenericRouteImpl(link3, link3);
		route4_3.setDistance(250);
		route4_3.setTravelTime(250.0);
		legp4_3.setRoute(route4_3);
		legp4_3.setTravelTime(250.0);
		plan4.addLeg(legp4_3);
		Activity actp4_4 = PopulationUtils.createActivityFromFacilityId("shopping", fp4_4.getId());// main mode pt
		actp4_4.setStartTime(23837.0);
		actp4_4.setEndTime(25637.0);
		plan4.addActivity(actp4_4);
		Coord coordp4_5 = CoordUtils.createCoord(4873.0, 4893.0);
		ActivityFacility fp4_5 = aff.createActivityFacility(
				Id.create("p4_id5", ActivityFacility.class),
				coordp4_5, link3);
		facilities.addActivityFacility(fp4_5);
		Leg legp4_4 = PopulationUtils.createLeg(TransportMode.walk);
		legp4_4.getAttributes().putAttribute("startcoord", coordp4_4);
		legp4_4.getAttributes().putAttribute("endcoord", coordp4_5);
		legp4_4.setDepartureTime(25637.0);
		legp4_4.setRoute(route4_3);
		legp4_4.setTravelTime(250.0);
		plan4.addLeg(legp4_4);
		Activity actp4_5 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coordp4_5, link3,
				TransportMode.pt);
//		actp4_5.setStartTime(25887.0);
//		actp4_5.setEndTime(25888.0);
		actp4_5.setFacilityId(fp4_5.getId());
		plan4.addActivity(actp4_5);
		Coord coordp4_6 = CoordUtils.createCoord(277.0, 297.0);
		ActivityFacility fp4_6 = aff.createActivityFacility(
				Id.create("p4_id6", ActivityFacility.class),
				coordp4_6, link1);
		facilities.addActivityFacility(fp4_6);
		Leg legp4_5 = PopulationUtils.createLeg(TransportMode.pt);
		legp4_5.getAttributes().putAttribute("startcoord", coordp4_5);
		legp4_5.getAttributes().putAttribute("endcoord", coordp4_6);
		legp4_5.setDepartureTime(25888.0);
		legp4_5.getAttributes().putAttribute(EventsToLegs.ENTER_VEHICLE_TIME_ATTRIBUTE_NAME, 25898.0);
		legp4_5.getAttributes().putAttribute(EventsToLegs.VEHICLE_ID_ATTRIBUTE_NAME, Id.createVehicleId("ptp4_5"));
		legp4_5.setRoute(ptRoute);
		legp4_5.setTravelTime(1625.0);
		plan4.addLeg(legp4_5);
		Activity actp4_6 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(coordp4_6, link1,
				TransportMode.pt);
//		actp4_6.setStartTime(27523.0);
//		actp4_6.setEndTime(27524.0);
		actp4_6.setFacilityId(fp4_6.getId());
		plan4.addActivity(actp4_6);
		Leg legp4_6 = PopulationUtils.createLeg(TransportMode.walk);
		legp4_6.getAttributes().putAttribute("startcoord", coordp4_6);
		legp4_6.getAttributes().putAttribute("endcoord", coordp4_1);
		legp4_6.setDepartureTime(27524.0);
		legp4_6.setRoute(route4_1);
		legp4_6.setTravelTime(350.0);
		plan4.addLeg(legp4_6);
		Activity actp4_8 = PopulationUtils.createActivityFromFacilityId("home", fp4_1.getId());// main mode pt
		actp4_8.setStartTime(27624.0);
		actp4_8.setEndTime(27624.0);
		plan4.addActivity(actp4_8);

		// do not set score (test

		return plan4;
	}
	
	public Plan createPlanFive() {

		/****************************
		 * Plan 5 - creating plan 5
		 ************************************/
		final Plan plan = PopulationUtils
				.createPlan(PopulationUtils.getFactory().createPerson(Id.create("1", Person.class)));

		Coord coordp5_1 = CoordUtils.createCoord(30.0, 50.0);
		ActivityFacility fp5_1 = aff.createActivityFacility(
				Id.create("p5_id1", ActivityFacility.class),
				coordp5_1, link1);
		facilities.addActivityFacility(fp5_1);
		Activity act1 = PopulationUtils.createActivityFromFacilityId("home", fp5_1.getId());
		act1.setStartTime(21599.0);
		act1.setEndTime(21600.0);
		plan.addActivity(act1);
		Coord coordp5_2 = CoordUtils.createCoord(100.0, 120.0);
		ActivityFacility fp5_2 = aff.createActivityFacility(
				Id.create("p5_id2", ActivityFacility.class),
				coordp5_2, link1);
		facilities.addActivityFacility(fp5_2);
		Leg leg1 = PopulationUtils.createLeg(TransportMode.walk);
		leg1.getAttributes().putAttribute("startcoord", coordp5_1);
		leg1.getAttributes().putAttribute("endcoord", coordp5_2);
		leg1.setDepartureTime(21600.0);
		Route route = RouteUtils.createGenericRouteImpl(link1, link1);
		route.setDistance(100);
		route.setTravelTime(100.0);
		leg1.setRoute(route);
		leg1.setTravelTime(100.0);
		plan.addLeg(leg1);
		Activity act2 = PopulationUtils.createActivityFromFacilityId("leisure", fp5_2.getId());// main mode walk
		act2.setStartTime(21700.0);
		act2.setEndTime(23500.0);
		plan.addActivity(act2);
		Leg leg2 = PopulationUtils.createLeg(TransportMode.walk);
		leg2.getAttributes().putAttribute("startcoord", coordp5_2);
		leg2.getAttributes().putAttribute("endcoord", coordp5_1);
		leg2.getAttributes().putAttribute("link", link1);
		leg2.setDepartureTime(23500.0);
		Route route2 = RouteUtils.createGenericRouteImpl(link1, link1);
		route2.setDistance(150);
		route2.setTravelTime(150.0);
		leg2.setRoute(route2);
		leg2.setTravelTime(150.0);
		plan.addLeg(leg2);
		Activity act3 = PopulationUtils.createActivityFromFacilityId("home", fp5_1.getId());
		act3.setStartTime(23650.0);
		act3.setEndTime(23650.0);
		plan.addActivity(act3);
		
		return plan;
	}
}
