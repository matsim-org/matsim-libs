package playground.gleich.contrib_av;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
 * combined access+pt+egress trip and
 * for the decision point between two transit stops.
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
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		TransitRouterConfig trConfig = new TransitRouterConfig(config);
		trConfig.setMarginalUtilityOfTravelDistancePt_utl_m(-0.01);
		trConfig.setMarginalUtilityOfTravelDistanceWalk_utl_m(-0.05);
		trConfig.setMarginalUtilityOfTravelTimePt_utl_s(-0.01);
		trConfig.setMarginalUtilityOfTravelTimeWalk_utl_s(-0.05);
		trConfig.setMarginalUtilityOfWaitingPt_utl_s(-0.01);
		
		PreparedTransitSchedule preparedTransitSchedule = new PreparedTransitSchedule(scenario.getTransitSchedule());
		TransitRouterNetwork ptRouterNetwork = TransitRouterNetwork.createFromSchedule(scenario.getTransitSchedule(), 1000000.0);
		TransitRouterNetworkTravelTimeAndDisutility transitRouterNetworkTravelTimeAndDisutility = 
				new TransitRouterNetworkTravelTimeAndDisutility(trConfig, preparedTransitSchedule);
		TravelTime travelTime = transitRouterNetworkTravelTimeAndDisutility;
		TransitTravelDisutility travelDisutility = transitRouterNetworkTravelTimeAndDisutility;
		
		FixedDistanceBasedVariableAccessModule variableAccessModule = 
				new FixedDistanceBasedVariableAccessModule(scenario.getNetwork(), config);
		variableAccessModule.registerMode("av", 4500, false);
		variableAccessModule.registerMode("walk", 3000, true);
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
		 * distance = 1000m*1.3 = 1300m (see PlansCalcRouteConfigGroup BeelineDistanceFactor = 1.3)
		 * travel time = 1300m/( (3.0m/s) / 3.6 ) = 1560s (see PlansCalcRouteConfigGroup TeleportedModeSpeed of mode walk = 3.0 / 3.6)
		 * utility = 1300m*(-0.05/m) + 1560s*-0.05/s = -143
		 * 
		 * cost pt leg from stop id 1 to stop id 3:
		 * MarginalUtilityOfTravelDistancePt_utl_m = -0.01/m
		 * MarginalUtilityOfTravelTimePt_utl_s = -0.01/s
		 * MarginalUtilityOfWaitingPt_utl_s = -0.05/s
		 * arrival at stop id 1 = 8:00:00 + travelTime = 8:26:00
		 * next departure = 8:30:00
		 * waiting time = 8:30:00 - 8:26:00 = 240s
		 * travel time = 0:09:00 = 540s
		 * travelled distance in pt = 100m + 2400m + 3600m = 6100m
		 * utility = 240s*-0.05s + 540s*-0.01/s + 6100m*-0.01/m = -78.4
		 * 
		 * cost egress walk leg from transit stop id 3:
		 * 
		 */
		
		ActivityFacility homeXCoord2050 = actFacilFacImpl.createActivityFacility(Id.create("homeXCoord2050", ActivityFacility.class), CoordUtils.createCoord(2050.0, 1050.0));
		
		ActivityFacility workXCoord3950 = actFacilFacImpl.createActivityFacility(Id.create("workXCoord3950", ActivityFacility.class), CoordUtils.createCoord(3950.0, 1050.0));
		ActivityFacility workXCoord2950 = actFacilFacImpl.createActivityFacility(Id.create("home1001m", ActivityFacility.class), CoordUtils.createCoord(4451.0, 1050.0));
		ActivityFacility workXCoord3050 = actFacilFacImpl.createActivityFacility(Id.create("home2501m", ActivityFacility.class), CoordUtils.createCoord(5550.0, 1050.0));
		
		Person personCarNeverAvailable = scenario.getPopulation().getPersons().get(Id.create("car_never_available", Person.class));
		router.calcRoute(homeXCoord2050, workXCoord3950, 8*60*60, personCarNeverAvailable);
//		direct walk cost 167.2
//		path cost 153.58190974780297, whereof 88.0 access walk and 65.58190974780295 pt travelCost, p (path) p.travelTime=780.0
//		debug modes shows MarginalUtility values set above in the transitRouterConfig, so these should be used for calculation
		
		/* Check decision point between two Transit Stops
		 * 
		 */
		
	}

}
