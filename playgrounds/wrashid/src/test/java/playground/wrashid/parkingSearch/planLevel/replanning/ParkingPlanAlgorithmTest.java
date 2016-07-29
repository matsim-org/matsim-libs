package playground.wrashid.parkingSearch.planLevel.replanning;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingBookKeeper;
import playground.wrashid.parkingSearch.planLevel.scenario.ParkingUtils;

public class ParkingPlanAlgorithmTest extends MatsimTestCase implements IterationStartsListener {

	ParkingBookKeeper parkingBookKeeper = null;

	public void testReplaceParking() {
		Controler controler;
		String configFilePath = "test/input/playground/wrashid/parkingSearch/planLevel/chessConfig3.xml";
		final Config config = this.loadConfig(configFilePath);

		config.plansCalcRoute().setInsertingAccessEgressWalk(false);
		// too many things don't work with access/egress walk true. kai, jun'16

		controler = new Controler(config);

		parkingBookKeeper = ParkingUtils.initializeParking(controler) ;

		controler.addControlerListener(this);

		controler.run();

	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
        Plan plan = GlobalRegistry.controler.getScenario().getPopulation().getPersons().get(Id.create("1", Person.class)).getSelectedPlan();

		// confirm the parking before the change (and compare it after the change)

		assertEquals("36", ((Activity) plan.getPlanElements().get(4)).getFacilityId().toString());
		assertEquals("36", ((Activity) plan.getPlanElements().get(8)).getFacilityId().toString());

		// change the parking for the work activity to facility 35, instead of 36

        ActivityFacilityImpl newParking = (ActivityFacilityImpl) GlobalRegistry.controler.getScenario().getActivityFacilities().getFacilities()
				.get(Id.create("35", ActivityFacility.class));

        ParkingPlanAlgorithm.replaceParking(plan, (Activity) plan.getPlanElements().get(6), newParking,
				GlobalRegistry.controler, (Network) GlobalRegistry.controler.getScenario().getNetwork());

		assertEquals("35", ((Activity) plan.getPlanElements().get(4)).getFacilityId().toString());
		assertEquals("35", ((Activity) plan.getPlanElements().get(8)).getFacilityId().toString());

		assertEquals("1", ((Activity) plan.getPlanElements().get(0)).getFacilityId().toString());
		assertEquals("1", ((Activity) plan.getPlanElements().get(12)).getFacilityId().toString());

		// change the parking for the home activity to facility 35, instead of 36

        ParkingPlanAlgorithm.replaceParking(plan, (Activity) plan.getPlanElements().get(0), newParking,
				GlobalRegistry.controler, (Network) GlobalRegistry.controler.getScenario().getNetwork());

		assertEquals("35", ((Activity) plan.getPlanElements().get(2)).getFacilityId().toString());
		assertEquals("35", ((Activity) plan.getPlanElements().get(10)).getFacilityId().toString());

	}

}
