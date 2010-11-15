package playground.mmoyo;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.testcases.MatsimTestCase;

import playground.mmoyo.ptRouterAdapted.AdaptedTransitRouter;
import playground.mmoyo.ptRouterAdapted.AdaptedTransitRouterNetworkTravelTimeCost;
import playground.mmoyo.ptRouterAdapted.MyTransitRouterConfig;
import playground.mmoyo.utils.DataLoader;

public class AdaptedRouterTest extends MatsimTestCase {

	/*validates that the given transitConfigValues are read from the instance class*/
	public void testEquil() {
		String configFile = "test/input/playground/mmoyo/AdaptedRouterTest/5x5config.xml";
		MyTransitRouterConfig myConfig = new MyTransitRouterConfig();

		//variables from superclass
		myConfig.searchRadius = 600.0;
		myConfig.extensionRadius = 300.0;
		myConfig.beelineWalkConnectionDistance = 300.0;
		myConfig.beelineWalkSpeed = 4.0/3.6;
		myConfig.marginalUtilityOfTravelTimeWalk = -6.0 / 3600.0;
		myConfig.marginalUtilityOfTravelTimeTransit = -6.0 / 3600.0;
		myConfig.marginalUtilityOfTravelDistanceTransit = -0.0;
		myConfig.costLineSwitch = 60.0 * -myConfig.marginalUtilityOfTravelTimeTransit;

		//variables from instance
		myConfig.allowDirectWalks= true;
		myConfig.noCarPlans= true;
		myConfig.fragmentPlans = false;
		myConfig.compressPlan = true;
		myConfig.minStationsNum= 2;
		myConfig.scenarioName= "test";

		ScenarioImpl scenarioImpl = new DataLoader ().loadScenarioWithTrSchedule(configFile);
		AdaptedTransitRouterNetworkTravelTimeCost adaptedTravelTimeCost = new AdaptedTransitRouterNetworkTravelTimeCost(myConfig);
		AdaptedTransitRouter adaptedTransitRouter = new AdaptedTransitRouter(myConfig, scenarioImpl.getTransitSchedule());

		//only transit links without transfer
		double accumTime=28800;  //8:00am first departure
		for (Link link : adaptedTransitRouter.getTransitRouterNetwork().getLinks().values()){
			double travelCost = adaptedTravelTimeCost.getLinkTravelCost(link, accumTime);
			double travelTime = adaptedTravelTimeCost.getLinkTravelTime(link, accumTime);
			assertEquals(travelCost, -travelTime * myConfig.marginalUtilityOfTravelTimeTransit - link.getLength() * myConfig.marginalUtilityOfTravelDistanceTransit);
			accumTime += travelTime;
		}
	}
}
