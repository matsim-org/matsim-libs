package playground.mmoyo;

import org.junit.Assert;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;

import playground.mmoyo.ptRouterAdapted.AdaptedTransitRouter;
import playground.mmoyo.ptRouterAdapted.AdaptedTransitRouterNetworkTravelTimeCost;
import playground.mmoyo.ptRouterAdapted.MyTransitRouterConfig;
import playground.mmoyo.utils.DataLoader;

public class AdaptedRouterTest extends MatsimTestCase {

	/**validates that the given transitConfigValues are read from the instance class*/
	public void testEquil() {
	
		String inputDir = this.getInputDirectory();
		//String inputDir = "../playgrounds/mmoyo/test/input/playground/mmoyo/AdaptedRouterTest/";
		
		System.out.println(" Input Dir " + this.getInputDirectory());
		System.out.println(" Output Dir " + this.getOutputDirectory());
	
		ScenarioImpl scenarioImpl = new DataLoader().loadScenarioWithTrSchedule(inputDir + "5x5config.xml");

		MyTransitRouterConfig myConfig = new MyTransitRouterConfig( scenarioImpl.getConfig().planCalcScore(),
				scenarioImpl.getConfig().plansCalcRoute() );

		AdaptedTransitRouterNetworkTravelTimeCost adaptedTravelTimeCost = new AdaptedTransitRouterNetworkTravelTimeCost(myConfig);
		AdaptedTransitRouter adaptedTransitRouter = new AdaptedTransitRouter(myConfig, scenarioImpl.getTransitSchedule());

		//only transit links without transfer
		double accumTime=28800;  //8:00am first departure
		final String msg = "";
		for (Link link : adaptedTransitRouter.getTransitRouterNetwork().getLinks().values()){
			double travelCost = adaptedTravelTimeCost.getLinkGeneralizedTravelCost(link, accumTime);
			double travelTime = adaptedTravelTimeCost.getLinkTravelTime(link, accumTime);
			Assert.assertEquals(msg , travelCost, -travelTime * myConfig.getEffectiveMarginalUtilityOfTravelTimePt_utl_s() - link.getLength() * myConfig.getMarginalUtilityOfTravelDistancePt_utl_m() , MatsimTestUtils.EPSILON);
			accumTime += travelTime;
		}
		
		//test travel parameter values coming from config file
		String str_expected = "[beelineWalkConnectionDistance=100.0][beelineWalkSpeed=0.8333333333333333][costLineSwitch_utl=0.4][extensionRadius=200.0][marginalUtilityOfTravelDistanceTransit=-0.0][marginalUtilityOfTravelTimeTransit=-0.0016666666666666668][marginalUtilityOfTravelTimeWalk=-0.0016666666666666668][searchRadius=1000.0]";
		Assert.assertEquals("travel parameters are different as in config file" , str_expected , adaptedTransitRouter.toString());
	}
}
