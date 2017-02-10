package playground.gleich.contrib_av;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.av.intermodal.router.FixedDistanceBasedVariableAccessModule;
import org.matsim.contrib.av.intermodal.router.FlexibleDistanceBasedVariableAccessModule;
import org.matsim.contrib.av.intermodal.router.VariableAccessTransitRouterImpl;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.config.TransitRouterConfigGroup;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.pt.router.TransitTravelDisutility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * Tests VariableAccessTransitRouterImpl
 * for the decision point between between a direct walk and a 
 * combined access+pt+egress trip,
 * for the decision point between two transit stops and
 * for the legs returned.
 * 
 * Apparently calcRoute decides only between a direct walk (only mode walk, not
 * direct bike or direct av) and a combined 
 * access (walk/bike/av/...) + pt + egress (walk/bike/av/...) trip. So only this
 * is tested.
 * 
 * @author vsp-gleich
 *
 */
public class VariableAccessTransitRouterImplTest {
	@Test
	public void testCalcRoute(){
		Config config = ConfigUtils.loadConfig(
				"./src/test/resources/intermodal_scenario/config.xml",
				new TaxiConfigGroup());
		
		config.plansCalcRoute().setBeelineDistanceFactor(1.3);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		TransitRouterConfig trConfig = new TransitRouterConfig(config);
		trConfig.setSearchRadius(1000.0);
		trConfig.setMarginalUtilityOfTravelDistancePt_utl_m(-0.01);
		trConfig.setMarginalUtilityOfTravelDistanceWalk_utl_m(-0.05);
		trConfig.setMarginalUtilityOfTravelTimePt_utl_s(-0.01);
		trConfig.setMarginalUtilityOfTravelTimeWalk_utl_s(-0.05);
		trConfig.setMarginalUtilityOfWaitingPt_utl_s(-0.05);
		trConfig.setBeelineWalkSpeed(0.641025641025641); // default value, equals result of default beeline distance factor 1.3 and default walk speed 3.0 km/h
		// The directWalkFactor can only be set in the TransitRouterConfigGroup, not in the TransitRouterConfig. Assuming the default remains 1.0 .
		
		PreparedTransitSchedule preparedTransitSchedule = new PreparedTransitSchedule(scenario.getTransitSchedule());
		TransitRouterNetwork ptRouterNetwork = TransitRouterNetwork.createFromSchedule(scenario.getTransitSchedule(), 1000000.0);
		TransitRouterNetworkTravelTimeAndDisutility transitRouterNetworkTravelTimeAndDisutility = 
				new TransitRouterNetworkTravelTimeAndDisutility(trConfig, preparedTransitSchedule);
		TravelTime travelTime = transitRouterNetworkTravelTimeAndDisutility;
		TransitTravelDisutility travelDisutility = transitRouterNetworkTravelTimeAndDisutility;
		
		FixedDistanceBasedVariableAccessModule variableAccessModule = 
				new FixedDistanceBasedVariableAccessModule(scenario.getNetwork(), config);
		variableAccessModule.registerMode("av", 4500, false);
		variableAccessModule.registerMode("walk", 2000, true);
		variableAccessModule.registerMode("bike", 4000, true);
		variableAccessModule.registerMode("car", 4900, false);
		
		VariableAccessTransitRouterImpl router = new VariableAccessTransitRouterImpl(trConfig, 
				preparedTransitSchedule, ptRouterNetwork, travelTime, travelDisutility, 
				variableAccessModule, scenario.getNetwork());
		
		ActivityFacilitiesFactoryImpl actFacilFacImpl = new ActivityFacilitiesFactoryImpl();
		
		
		/* Check decision point between direct walk and access+pt+egress combined trip
		 * 
		 * start near stop id 1 
		 * end moves from stop id 3 towards stop id 1 
		 * ->using the pt line between these stops becomes less attractive
		 * maximum walk distance = 3000m -> direct walk is always available
		 * 
		 * departure time = 8:00:00 
		 * 
		 * Calculation of decision point based on the distances and travel 
		 * times used in FixedDistanceBasedVariableAccessModule
		 * 
		 * cost access walk leg to transit stop id 1:
		 * MarginalUtilityOfTravelDistanceWalk_utl_m = -0.05/m
		 * MarginalUtilityOfTravelTimeWalk_utl_s= -0.05/s
		 * // apparently in all calculations in TransitRouterNetworkTravelTimeAndDisutility: cost = -utility
		 * // VariableAccessTransitRouterImpl.calcRoute() ->wrappedFromNodes = this.locateWrappedNearestTransitNodes ->initialCost = getAccessEgressDisutility() ->TransitRouterNetworkTravelTimeAndDisutility.getTravelDisutility()  recalculates beeline distances without beeline distance factor and uses TransitRouterConfig.getBeelineWalkSpeed() as speed and furthermore uses MarginalUtilityOfTravelDistancePt_utl_m instead of MarginalUtilityOfTravelDistanceWalk_utl_m although employed for a walk leg. At this point no mode has been chosen.
		 * // old calculation according to beeline distance with beelineDistanceFactor [distance = 1000m*1.3 = 1300m (see PlansCalcRouteConfigGroup BeelineDistanceFactor = 1.3)]
		 * // old calculation according to beeline distance with beelineDistanceFactor and usual walk speed: travel time = 1300m/( (3.0m/s) / 3.6 ) = 1560s (see PlansCalcRouteConfigGroup TeleportedModeSpeed of mode walk = 3.0 / 3.6)
		 * //old calculation using MarginalUtilityOfTravelDistanceWalk_utl_m and beeline distance with beelineDistanceFactor: distance utility = 1300m*(-0.05/m) + 1560s*-0.05/s = -143
		 * //new calculation using MarginalUtilityOfTravelDistancePt_utl_m as in TransitRouterNetworkTravelTimeAndDisutility:
		 * distance = 1000m
		 * travel time = 1000m/0.641025641025641 = 1560s
		 * distance utility = 1000m*(-0.01/m) + 1560s*-0.05/s = -88
		 * 
		 * cost pt leg from stop id 1 to stop id 3:
		 * MarginalUtilityOfTravelDistancePt_utl_m = -0.01/m
		 * MarginalUtilityOfTravelTimePt_utl_s = -0.01/s
		 * MarginalUtilityOfWaitingPt_utl_s = -0.05/s
		 * arrival at stop id 1 = 8:00:00 + travelTime = 8:26:00
		 * next departure = 8:30:00
		 * waiting time = 8:30:00 - 8:26:00 = 240s
		 * travel time = 0:09:00 = 540s
		 * // old calculation according to real network distances [travelled distance in pt = 100m + 2400m + 3600m = 6100m]
		 * // apparently TransitRouterNetworkTravelTimeAndDisutility relies on the TransitRouterNetwork which appears to consist of a node at each stop and links between them whose length equals the beeline distance instead of the lengths given in the network file for the links the pt route travels on while going between two stops: While debugging, the first link shown started at stop id 1 and ended at stop id 2a and its length was 2138.2469455140113 which equals the beeline distance between these two stops' coordinates
		 * travelled distance in pt = 2138.2469455140113 (stop 1->stop 2a) + 2679.944029266283 (stop 2a->stop 3) = 4818.191
		 * utility = 240s*-0.05s + 540s*-0.01/s + 4818.191m*-0.01/m = -65.58191
		 * 
		 * cost egress walk leg from transit stop id 3:
		 * combined distance and time cost per meter:
		 * combined cost per meter = -0.01/m + 1/0.641025641025641 * -0.05/s = -0.088
		 * for work destination xCoord = 3950.0, yCoord = 1050.0
		 * distance = 0m
		 * combined cost = 0
		 * 
		 * total cost for combined access+pt+egress trip to coord x=3950.0, y=1050.0
		 * = 153.58190974780297
		 * 
		 * direct walk cost (see also above: calculation for access walk leg):
		 * VariableAccessTransitRouterImpl.calcRoute() ->VariableAccessTransitRouterImpl.getAccessEgressDisutility() ->TransitRouterNetworkTravelTimeAndDisutility.getTravelDisutility()  recalculates beeline distances without beeline distance factor and uses TransitRouterConfig.getBeelineWalkSpeed() as speed and furthermore uses MarginalUtilityOfTravelDistancePt_utl_m instead of MarginalUtilityOfTravelDistanceWalk_utl_m although employed for a walk leg. At this point no mode has been chosen.
		 * distance = 1900m
		 * travel time = 1900m/0.641025641025641 = 2964s
		 * distance utility = 1900m*(-0.01/m) + 2964s*-0.05/s = 167.2
		 * 
		 * decision point: work destination xCoord = 3950.0, yCoord = 1050.0 moved by -x along x-axis from stop 3 towards stop 1
		 * assuming trConfig.getDirectWalkFactor() = 1 (cannot be set here, 1 appears to be the default)
		 * 153.58190974780297 + x*0.088 = 167.2 - x*0.088
		 * x*0.176 = 13.61809025219703
		 * x = 77.37551279657403
		 * decision point at work destination xCoord = 3872.62448720342596, yCoord = 1050.0
		 */
		trConfig.getDirectWalkFactor(); 
		ActivityFacility homeXCoord2050 = actFacilFacImpl.createActivityFacility(Id.create("homeXCoord2050", ActivityFacility.class), CoordUtils.createCoord(2050.0, 1050.0));
		
		ActivityFacility workXCoord3950 = actFacilFacImpl.createActivityFacility(Id.create("workXCoord3950", ActivityFacility.class), CoordUtils.createCoord(3950.0, 1050.0));
		ActivityFacility workAtPtSideOfDecisionPoint = actFacilFacImpl.createActivityFacility(Id.create("workAtPtSideOfDecisionPoint", ActivityFacility.class), CoordUtils.createCoord(3873.0, 1050.0));
		// x-coordinate workAtDecisionPoint slightly rounded up in order to avoid rounding issues
		ActivityFacility workAtDecisionPoint = actFacilFacImpl.createActivityFacility(Id.create("workAtDecisionPoint", ActivityFacility.class), CoordUtils.createCoord(3872.62448720343, 1050.0));
		ActivityFacility workAtDirectWalkSideOfDecisionPoint = actFacilFacImpl.createActivityFacility(Id.create("workAtDirectWalkSideOfDecisionPoint", ActivityFacility.class), CoordUtils.createCoord(3872.0, 1050.0));
		
		Person personCarNeverAvailable = scenario.getPopulation().getPersons().get(Id.create("car_never_available", Person.class));
		router.calcRoute(homeXCoord2050, workXCoord3950, 8*60*60, personCarNeverAvailable);
//		direct walk cost 167.2
//		path cost 153.58190974780297, whereof 88.0 access walk and 65.58190974780295 pt travelCost, p (path) p.travelTime=780.0
//		debug mode shows MarginalUtility values set above in the transitRouterConfig, so these should already be used for calculation
		
		// Check decision point
		List<Leg> legsAtPtSideOfDecisionPoint = router.calcRoute(homeXCoord2050, workAtPtSideOfDecisionPoint, 8*60*60, personCarNeverAvailable);
		Assert.assertTrue(legsAtPtSideOfDecisionPoint.get(0).getMode().equals("walk"));
		Assert.assertTrue(legsAtPtSideOfDecisionPoint.get(1).getMode().equals("pt"));
		Assert.assertTrue(legsAtPtSideOfDecisionPoint.get(2).getMode().equals("walk"));
		Assert.assertEquals(3, legsAtPtSideOfDecisionPoint.size());
		
		List<Leg> legsAtDecisionPoint = router.calcRoute(homeXCoord2050, workAtDecisionPoint, 8*60*60, personCarNeverAvailable);
		Assert.assertTrue(legsAtDecisionPoint.get(0).getMode().equals("walk"));
		Assert.assertTrue(legsAtDecisionPoint.get(1).getMode().equals("pt"));
		Assert.assertTrue(legsAtDecisionPoint.get(2).getMode().equals("walk"));
		Assert.assertEquals(3, legsAtDecisionPoint.size());
		
		List<Leg> legsAtDirectWalkSideOfDecisionPoint = router.calcRoute(homeXCoord2050, workAtDirectWalkSideOfDecisionPoint, 8*60*60, personCarNeverAvailable);
		Assert.assertTrue(legsAtDirectWalkSideOfDecisionPoint.get(0).getMode().equals("walk"));
		Assert.assertEquals(1, legsAtDirectWalkSideOfDecisionPoint.size());
		
		// Check legs returned - start and end links - for a combined access+pt+egress trip and for a direct walk trip
		Assert.assertEquals(Id.create("2122", Link.class), legsAtPtSideOfDecisionPoint.get(0).getRoute().getStartLinkId());
		Assert.assertEquals(Id.create("11", Link.class), legsAtPtSideOfDecisionPoint.get(0).getRoute().getEndLinkId());
		Assert.assertEquals(Id.create("11", Link.class), legsAtPtSideOfDecisionPoint.get(1).getRoute().getStartLinkId());
		Assert.assertEquals(Id.create("33", Link.class), legsAtPtSideOfDecisionPoint.get(1).getRoute().getEndLinkId());
		Assert.assertEquals(Id.create("33", Link.class), legsAtPtSideOfDecisionPoint.get(2).getRoute().getStartLinkId());
		Assert.assertEquals(Id.create("23", Link.class), legsAtPtSideOfDecisionPoint.get(2).getRoute().getEndLinkId());  // on a train link!
		
		Assert.assertEquals(Id.create("2122", Link.class), legsAtDirectWalkSideOfDecisionPoint.get(0).getRoute().getStartLinkId());
		Assert.assertEquals(Id.create("23", Link.class), legsAtDirectWalkSideOfDecisionPoint.get(0).getRoute().getEndLinkId());
		
		// Check legs returned - distances and travel times - for a combined access+pt+egress trip and for a direct walk trip
		Assert.assertEquals(1000.0*1.3, legsAtPtSideOfDecisionPoint.get(0).getRoute().getDistance(), 0.001);
//		Assert.assertEquals(100.00+2400.00+3600.00, legsAtPtSideOfDecisionPoint.get(1).getRoute().getDistance(), 0.001); // 3770
		Assert.assertEquals(77.0*1.3, legsAtPtSideOfDecisionPoint.get(2).getRoute().getDistance(), 0.001);
		Assert.assertEquals(1000.0/0.641025641025641, legsAtPtSideOfDecisionPoint.get(0).getTravelTime(), 0.001);
		Assert.assertEquals(240.0+540.0, legsAtPtSideOfDecisionPoint.get(1).getTravelTime(), 0.001);
		Assert.assertEquals(77.0/0.641025641025641, legsAtPtSideOfDecisionPoint.get(2).getTravelTime(), 0.001);
		
		Assert.assertEquals(1822.0*1.3, legsAtDirectWalkSideOfDecisionPoint.get(0).getRoute().getDistance(), 0.001);
		Assert.assertEquals(1822.0/0.641025641025641, legsAtDirectWalkSideOfDecisionPoint.get(0).getTravelTime(), 0.001);
		
		/* Check decision point between two Transit Stops
		 * 
		 */
		
	}

}
