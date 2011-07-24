package playground.mmoyo.ptRouterAdapted;

import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;

import playground.mmoyo.ptRouterAdapted.AdaptedTransitRouter;
import playground.mmoyo.ptRouterAdapted.AdaptedTransitRouterNetworkTravelTimeCost;
import playground.mmoyo.ptRouterAdapted.MyTransitRouterConfig;
import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.Generic2ExpRouteConverter;

public class AdaptedRouterTest extends MatsimTestCase {

	/**validates that the given transitConfigValues are read from the instance class*/
	public void testEquil() {
		DataLoader dataLoader = new DataLoader();
		ScenarioImpl scenarioImpl = dataLoader.loadScenario(this.getClassInputDirectory() + "5x5config.xml");

		MyTransitRouterConfig myConfig = new MyTransitRouterConfig(scenarioImpl.getConfig().planCalcScore(),
				scenarioImpl.getConfig().plansCalcRoute(), scenarioImpl.getConfig().transitRouter(),
				scenarioImpl.getConfig().vspExperimental());

		AdaptedTransitRouterNetworkTravelTimeCost adaptedTravelTimeCost = new AdaptedTransitRouterNetworkTravelTimeCost(myConfig);
		AdaptedTransitRouter adaptedTransitRouter = new AdaptedTransitRouter(myConfig, scenarioImpl.getTransitSchedule());

		//test only transit links without transfer
		double accumTime=28800;  //8:00am first departure
		final String msg = "different result of cost calculation:";
		for (Link link : adaptedTransitRouter.getTransitRouterNetwork().getLinks().values()){
			double travelCost = adaptedTravelTimeCost.getLinkGeneralizedTravelCost(link, accumTime);
			double travelTime = adaptedTravelTimeCost.getLinkTravelTime(link, accumTime);
			Assert.assertEquals(msg , travelCost, -travelTime * myConfig.getMarginalUtilityOfTravelTimePt_utl_s() - link.getLength() * myConfig.getMarginalUtilityOfTravelDistancePt_utl_m() , MatsimTestUtils.EPSILON);
			accumTime += travelTime;
		}
		
		//test travel parameter values coming from config file
		String str_expected = "[beelineWalkConnectionDistance=100.0][beelineWalkSpeed=0.8333333333333333][costLineSwitch_utl=0.4][extensionRadius=200.0][marginalUtilityOfTravelDistanceTransit=-0.0][marginalUtilityOfTravelTimeTransit=-0.0016666666666666668][marginalUtilityOfTravelTimeWalk=-0.0016666666666666668][searchRadius=1000.0]";
		Assert.assertEquals("travel parameters are different as in config file" , str_expected , adaptedTransitRouter.toString());

		//test near nodes
		double searchDistance = Double.parseDouble(scenarioImpl.getConfig().getParam("ptRouter", "searchRadius"));
		//near initial node
		Coord coord1 = new CoordImpl(1000.0,2500.0);
		Collection<TransitRouterNetworkNode> nearNodes1 = adaptedTransitRouter.getTransitRouterNetwork().getNearestNodes(coord1, searchDistance);
		TransitRouterNetworkNode trNode = nearNodes1.iterator().next();
		Assert.assertEquals("different number of initial nodes:" , nearNodes1.size() , 1);
		Assert.assertEquals("different near initial node:" , trNode.getId().toString() , "0");
		Assert.assertEquals("different near initial transit route:" , dataLoader.getTransitRoute("Blue Line.Blue.101.H", scenarioImpl.getTransitSchedule()) , trNode.getRoute());
		//near final node
		Coord coord2 = new CoordImpl(1100.0,8800.0);
		Collection<TransitRouterNetworkNode> nearNodes2 = adaptedTransitRouter.getTransitRouterNetwork().getNearestNodes(coord2, searchDistance);
		TransitRouterNetworkNode trNode2 = nearNodes2.iterator().next();
		Assert.assertEquals("different number of final nodes:" , nearNodes2.size() , 1);
		Assert.assertEquals("different near final node:" , trNode2.getId().toString() , "3");
		Assert.assertEquals("different near final transit route:" , dataLoader.getTransitRoute("Blue Line.Blue.101.H", scenarioImpl.getTransitSchedule()) , trNode2.getRoute());
		
		//calculate a route and validate it 
		List<Leg> route = adaptedTransitRouter.calcRoute(coord1, coord2, 28700);
		assertNotNull("route not found:",route);
		
		Leg trWalk1Leg = route.get(0);
		Assert.assertEquals("different departure time:", Double.NEGATIVE_INFINITY,trWalk1Leg.getDepartureTime(),  MatsimTestUtils.EPSILON);
		Assert.assertEquals("different type of initial leg:",TransportMode.transit_walk, trWalk1Leg.getMode());
		Assert.assertEquals("different route:",null, trWalk1Leg.getRoute());
		Assert.assertEquals(379.4733192202056, trWalk1Leg.getTravelTime(),  MatsimTestUtils.EPSILON);
		
		Leg trLeg = route.get(1);
		Assert.assertEquals("different departure time:",Double.NEGATIVE_INFINITY,trLeg.getDepartureTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("different leg mode:",TransportMode.pt, trLeg.getMode());
		Assert.assertEquals("different travel time:",80.52668077979615, trLeg.getTravelTime(), MatsimTestUtils.EPSILON);
		
		ExperimentalTransitRoute expRoute = new Generic2ExpRouteConverter().convert((GenericRouteImpl) trLeg.getRoute(), scenarioImpl.getTransitSchedule());
		assertNotNull(expRoute);
		Assert.assertEquals("different route id:",new IdImpl("Blue Line.Blue.101.H"), expRoute.getRouteId());
		Assert.assertEquals("different route description:","PT1===stop1===Blue Line===Blue Line.Blue.101.H===stop4", expRoute.getRouteDescription());
		Assert.assertEquals("different route type:","experimentalPt1", expRoute.getRouteType());
		Assert.assertEquals("different route access stop:",new IdImpl("stop1"), expRoute.getAccessStopId());
		Assert.assertEquals("different route egress stop:",new IdImpl("stop4"), expRoute.getEgressStopId());
		Assert.assertEquals("different route start link:",new IdImpl("20"), expRoute.getStartLinkId());
		Assert.assertEquals("different route end link:", new IdImpl("23"), expRoute.getEndLinkId());
		//Assert.assertEquals(0.0, expRoute.getDistance());
		//Assert.assertEquals(null, expRoute.getTravelTime());
		
		Leg leg3 = route.get(2);
		Assert.assertEquals("different departure time:",Double.NEGATIVE_INFINITY, leg3.getDepartureTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("different leg mode:",TransportMode.transit_walk, leg3.getMode());
		Assert.assertEquals("different route:",null,leg3.getRoute());
		Assert.assertEquals("different travel time:",0.0, leg3.getTravelTime(), MatsimTestUtils.EPSILON);
	}
}
