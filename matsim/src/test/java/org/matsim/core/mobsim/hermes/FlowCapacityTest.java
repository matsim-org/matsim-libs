package org.matsim.core.mobsim.hermes;

import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.hermes.HermesTest.Fixture;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;

@RunWith(Parameterized.class)
public class FlowCapacityTest {

	private final static Logger log = Logger.getLogger(FlowCapacityTest.class);

	private final boolean isUsingFastCapacityUpdate;

	public FlowCapacityTest(boolean isUsingFastCapacityUpdate) {
		this.isUsingFastCapacityUpdate = isUsingFastCapacityUpdate;
	}

	@Parameters(name = "{index}: isUsingfastCapacityUpdate == {0}")
	public static Collection<Object> parameterObjects () {
		Object [] capacityUpdates = new Object [] { false, true };
		return Arrays.asList(capacityUpdates);
	}

	/**
	 * Tests that the flow capacity can be reached (but not exceeded) by
	 * agents driving over a link.
	 *
	 * @author mrieser
	 */
	@Test
	public void testFlowCapacityDriving() {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);

		// add a lot of persons with legs from link1 to link3, starting at 6:30
		for (int i = 1; i <= 12000; i++) {
			Person person = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
			Plan plan = PersonUtils.createAndAddPlan(person, true);
			/* exact dep. time: 6:29:48. The agents needs:
			 * - at the specified time, the agent goes into the waiting list, and if space is available, into
			 * the buffer of link 1.
			 * - 1 sec later, it leaves the buffer on link 1 and enters link 2
			 * - the agent takes 10 sec. to travel along link 2, after which it gets placed in the buffer of link 2
			 * - 1 sec later, the agent leaves the buffer on link 2 (if flow-cap allows this) and enters link 3
			 * - as we measure the vehicles leaving link 2, and the first veh should leave at exactly 6:30, it has
			 * to start 1 + 10 + 1 = 12 secs earlier.
			 * So, the start time is 7*3600 - 1800 - 12 = 7*3600 - 1812
			 */
			Activity a = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
			a.setEndTime(7*3600 - 1812);
			Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
			TripStructureUtils.setRoutingMode( leg, TransportMode.car );
			NetworkRoute route = f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, f.link1.getId(), f.link3.getId());
			route.setLinkIds(f.link1.getId(), f.linkIds2, f.link3.getId());
			leg.setRoute(route);
			PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link3.getId());
			f.plans.addPerson(person);
		}

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(3600, 9*3600, f.network);
		events.addHandler(vAnalyzer);

		/* run sim */
		Hermes sim = HermesTest.createHermes(f, events);
		sim.run();

		/* finish */
		int[] volume = vAnalyzer.getVolumesForLink(f.link2.getId());
		System.out.println("#vehicles 3-4: " + Integer.toString(volume[3]));
		System.out.println("#vehicles 4-5: " + Integer.toString(volume[4]));
		System.out.println("#vehicles 5-6: " + Integer.toString(volume[5]));
		System.out.println("#vehicles 6-7: " + Integer.toString(volume[6]));
		System.out.println("#vehicles 7-8: " + Integer.toString(volume[7]));
		System.out.println("#vehicles 8-9: " + Integer.toString(volume[8]));


		Assert.assertEquals(0, volume[5]);    // no vehicles
		Assert.assertEquals(3602, volume[6]); // we should have half of the maximum flow in this hour
		Assert.assertEquals(7200, volume[7]); // we should have maximum flow in this hour
		Assert.assertEquals(1198, volume[8]); // all the rest

	}

	/**
	 * Tests that on a link with a flow capacity of 0.25 vehicles per time step, after the first vehicle
	 * at time step t, the second vehicle may pass in time step t + 4 and the third in time step t+8.
	 *
	 * @author michaz
	 */
	@Test
	public void testFlowCapacityDrivingFraction() {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);
		f.link2.setCapacity(900.0); // One vehicle every 4 seconds

		// add a lot of persons with legs from link1 to link3, starting at 6:30
		for (int i = 1; i <= 3; i++) {
			Person person = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
			Plan plan = PersonUtils.createAndAddPlan(person, true);
			/* exact dep. time: 6:29:48. The agents needs:
			 * - at the specified time, the agent goes into the waiting list, and if space is available, into
			 * the buffer of link 1.
			 * - 1 sec later, it leaves the buffer on link 1 and enters link 2
			 * - the agent takes 10 sec. to travel along link 2, after which it gets placed in the buffer of link 2
			 * - 1 sec later, the agent leaves the buffer on link 2 (if flow-cap allows this) and enters link 3
			 * - as we measure the vehicles leaving link 2, and the first veh should leave at exactly 6:30, it has
			 * to start 1 + 10 + 1 = 12 secs earlier.
			 * So, the start time is 7*3600 - 1800 - 12 = 7*3600 - 1812
			 */
			Activity a = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", f.link1.getId());
			a.setEndTime(7*3600 - 1812);
			Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
			TripStructureUtils.setRoutingMode( leg, TransportMode.car );
			NetworkRoute route = f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, f.link1.getId(), f.link3.getId());
			route.setLinkIds(f.link1.getId(), f.linkIds2, f.link3.getId());
			leg.setRoute(route);
			PopulationUtils.createAndAddActivityFromLinkId(plan, "w", f.link3.getId());
			f.plans.addPerson(person);
		}

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(1, 7*3600, f.network);
		events.addHandler(vAnalyzer);

		/* run sim */
		Hermes sim = HermesTest.createHermes(f, events);
		sim.run();

		/* finish */
		int[] volume = vAnalyzer.getVolumesForLink(f.link2.getId());
		Assert.assertEquals(1, volume[7*3600 - 1801]); // First vehicle
		Assert.assertEquals(1, volume[7*3600 - 1801 + 4]); // Second vehicle
		Assert.assertEquals(1, volume[7*3600 - 1801 + 8]); // Third vehicle
	}

}
