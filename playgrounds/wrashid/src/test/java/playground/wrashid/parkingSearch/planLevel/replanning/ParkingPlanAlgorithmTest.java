package playground.wrashid.parkingSearch.planLevel.replanning;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.lib.Reflection;
import playground.wrashid.parkingSearch.planLevel.LinkFacilityAssociationTest;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingBookKeeper;
import playground.wrashid.parkingSearch.planLevel.scenario.BaseControlerScenario;

public class ParkingPlanAlgorithmTest extends MatsimTestCase implements IterationStartsListener {

	ParkingBookKeeper parkingBookKeeper = null;

	public void testReplaceParking() {
		Controler controler;
		String configFilePath = "test/input/playground/wrashid/parkingSearch/planLevel/chessConfig3.xml";
		controler = new Controler(configFilePath);

		BaseControlerScenario bs = new BaseControlerScenario(controler);
		parkingBookKeeper = bs.parkingBookKeeper;

		controler.addControlerListener(this);

		controler.run();

	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		Plan plan = GlobalRegistry.controler.getPopulation().getPersons().get(new IdImpl("1")).getSelectedPlan();

		// confirm the parking before the change (and compare it after the change)

		assertEquals("36", ((Activity) plan.getPlanElements().get(4)).getFacilityId().toString());
		assertEquals("36", ((Activity) plan.getPlanElements().get(8)).getFacilityId().toString());

		// change the parking for the work activity to facility 35, instead of 36

		ActivityFacilityImpl newParking = (ActivityFacilityImpl) GlobalRegistry.controler.getFacilities().getFacilities()
				.get(new IdImpl("35"));

		ParkingPlanAlgorithm.replaceParking(plan, (ActivityImpl) plan.getPlanElements().get(6), newParking,
				GlobalRegistry.controler, (NetworkLayer) GlobalRegistry.controler.getNetwork());

		assertEquals("35", ((Activity) plan.getPlanElements().get(4)).getFacilityId().toString());
		assertEquals("35", ((Activity) plan.getPlanElements().get(8)).getFacilityId().toString());

		assertEquals("1", ((Activity) plan.getPlanElements().get(0)).getFacilityId().toString());
		assertEquals("1", ((Activity) plan.getPlanElements().get(12)).getFacilityId().toString());

		// change the parking for the home activity to facility 35, instead of 36
		
		ParkingPlanAlgorithm.replaceParking(plan, (ActivityImpl) plan.getPlanElements().get(0), newParking,
				GlobalRegistry.controler, (NetworkLayer) GlobalRegistry.controler.getNetwork());

		assertEquals("35", ((Activity) plan.getPlanElements().get(2)).getFacilityId().toString());
		assertEquals("35", ((Activity) plan.getPlanElements().get(10)).getFacilityId().toString());

	}

}
