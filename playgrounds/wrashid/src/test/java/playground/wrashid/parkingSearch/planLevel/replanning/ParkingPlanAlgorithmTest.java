package playground.wrashid.parkingSearch.planLevel.replanning;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.lib.Reflection;
import playground.wrashid.parkingSearch.planLevel.LinkFacilityAssociationTest;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;

public class ParkingPlanAlgorithmTest extends MatsimTestCase {

	public void testReplaceParking() {
		Controler controler;
		String configFilePath="test/input/playground/wrashid/parkingSearch/planLevel/chessConfig2.xml";
		controler = new Controler(configFilePath);
		
		ScenarioImpl sc = new ScenarioImpl();

		NetworkLayer net = LinkFacilityAssociationTest.loadNetwork(sc);

		Plan plan = sc.getPopulation().getPersons().get(new IdImpl("1")).getSelectedPlan();

		// init parkings
		ParkingRoot.init(sc.getActivityFacilities(), net);
		
		// confirm the parking before the change (and compare it after the change)
		
		assertEquals("36", ((Activity) plan.getPlanElements().get(4)).getFacilityId().toString());
		assertEquals("36", ((Activity) plan.getPlanElements().get(8)).getFacilityId().toString());
		
		// change the parking for the work activity to facility 35, instead of 36
		
		ActivityFacilityImpl newParking = ParkingRoot.getClosestParkingMatrix()
				.getClosestParkings(sc.getActivityFacilities().getFacilities().get(new IdImpl("35")).getCoord(), 1, 0).get(0);

//		ParkingPlanAlgorithm.replaceParking(plan, 6, newParking, controler, net);

		// test, if the facility has changed
		
//		assertEquals("35", ((Activity) plan.getPlanElements().get(4)).getFacilityId().toString());
//		assertEquals("35", ((Activity) plan.getPlanElements().get(8)).getFacilityId().toString());
		
		
		// check, if rerouting happened properly		
			
		
		
		
		// test if the route has been changed????

	}

}
