package org.matsim.contrib.av.intermodal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.av.intermodal.router.FlexibleDistanceBasedVariableAccessModule;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author vsp-gleich
 */
public class FlexibleDistanceBasedVariableAccessModuleTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	/*
	 * Currently fails because FlexibleDistanceBasedVariableAccessModule
	 * checks maximumAccessDistance>(beeline egressDistance) for mode walk
	 * whereas FixedDistanceBasedVariableAccessModule also accepts
	 * maximumAccessDistance==beeline.
	 * Furthermore FlexibleDistanceBasedVariableAccessModule returns mode walk
	 * for legs exceeding the maximumAccessDistance of all modes whereas
	 * FixedDistanceBasedVariableAccessModule throws an exception.
	 * Furthermore FlexibleDistanceBasedVariableAccessModule makes cars
	 * available for all agents without any car availability attribute set
	 * whereas FixedDistanceBasedVariableAccessModule does not cater for cars
	 * at all.
	 * Both modules should behave consistently.
	 *
	 * Randomness of mode returned for
	 * egressDistances>(maximumAccessDistance of mode walk)
	 * is difficult to test.
	 *
	 */
	@Test
	public void testGetAccessEgressModeAndTraveltime() throws MalformedURLException {
		URL configUrl = new File(utils.getPackageInputDirectory() + "config.xml").toURI().toURL();
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeTaxiConfigGroup());
		Scenario scenario = ScenarioUtils.loadScenario(config);
		FlexibleDistanceBasedVariableAccessModule module = new FlexibleDistanceBasedVariableAccessModule(
				scenario.getNetwork(), config);

		module.registerMode("av", 2500, false);
		module.registerMode("walk", 1000, true);
		module.registerMode("bike", 2000, true);
		module.registerMode("car", 2900, false);

		Person personCarAlwaysAvailable = scenario.getPopulation()
				.getPersons()
				.get(Id.create("car_always_available", Person.class));
		Person personCarNeverAvailable = scenario.getPopulation()
				.getPersons()
				.get(Id.create("car_never_available", Person.class));

		// Check decision points between available modes
		Leg leg999m = module.getAccessEgressModeAndTraveltime(personCarNeverAvailable,
				CoordUtils.createCoord(4949.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Assert.assertTrue(leg999m.getMode().equals("walk"));

		Leg leg1000m = module.getAccessEgressModeAndTraveltime(personCarNeverAvailable,
				CoordUtils.createCoord(4950.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Assert.assertTrue(leg1000m.getMode().equals("walk"));

		Leg leg1001m = module.getAccessEgressModeAndTraveltime(personCarNeverAvailable,
				CoordUtils.createCoord(4951.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Assert.assertTrue(leg1001m.getMode().equals("bike") || leg1001m.getMode().equals("av"));

		Leg leg1999m = module.getAccessEgressModeAndTraveltime(personCarNeverAvailable,
				CoordUtils.createCoord(5949.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Assert.assertTrue(leg1999m.getMode().equals("bike") || leg1999m.getMode().equals("av"));

		Leg leg2000m = module.getAccessEgressModeAndTraveltime(personCarNeverAvailable,
				CoordUtils.createCoord(5950.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Assert.assertTrue(leg2000m.getMode().equals("bike") || leg2000m.getMode().equals("av"));

		Leg leg2001m = module.getAccessEgressModeAndTraveltime(personCarNeverAvailable,
				CoordUtils.createCoord(5951.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Assert.assertTrue(leg2001m.getMode().equals("av") || leg2001m.getMode().equals("car"));

		Leg leg2501m = module.getAccessEgressModeAndTraveltime(personCarAlwaysAvailable,
				CoordUtils.createCoord(6451.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Assert.assertTrue(leg2501m.getMode().equals("car"));

		/*
		 *  Check randomness.
		 *  Fails in the very unlikely event that the MatsimRandom class
		 *  changes and all MatsimRandom class calls happen to return the same
		 *  random number.
		 *  [probability = (1/possibleModes.size())^9 = 5.08*10^-5 ]
		 */
		Leg leg1001m_2nd_random_call = module.getAccessEgressModeAndTraveltime(personCarNeverAvailable,
				CoordUtils.createCoord(4951.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Leg leg1001m_3rd_random_call = module.getAccessEgressModeAndTraveltime(personCarNeverAvailable,
				CoordUtils.createCoord(4951.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Leg leg1001m_4th_random_call = module.getAccessEgressModeAndTraveltime(personCarNeverAvailable,
				CoordUtils.createCoord(4951.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Leg leg1001m_5th_random_call = module.getAccessEgressModeAndTraveltime(personCarNeverAvailable,
				CoordUtils.createCoord(4951.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Leg leg1001m_6th_random_call = module.getAccessEgressModeAndTraveltime(personCarNeverAvailable,
				CoordUtils.createCoord(4951.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Leg leg1001m_7th_random_call = module.getAccessEgressModeAndTraveltime(personCarNeverAvailable,
				CoordUtils.createCoord(4951.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Leg leg1001m_8th_random_call = module.getAccessEgressModeAndTraveltime(personCarNeverAvailable,
				CoordUtils.createCoord(4951.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Leg leg1001m_9th_random_call = module.getAccessEgressModeAndTraveltime(personCarNeverAvailable,
				CoordUtils.createCoord(4951.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Leg leg1001m_10th_random_call = module.getAccessEgressModeAndTraveltime(personCarNeverAvailable,
				CoordUtils.createCoord(4951.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Assert.assertTrue(
				!(leg1001m_2nd_random_call.getMode().equals(leg1001m.getMode())
						&& leg1001m_3rd_random_call.getMode()
						.equals(leg1001m.getMode())
						&& leg1001m_4th_random_call.getMode().equals(leg1001m.getMode())
						&& leg1001m_5th_random_call.getMode().equals(leg1001m.getMode())
						&& leg1001m_6th_random_call.getMode().equals(leg1001m.getMode())
						&& leg1001m_7th_random_call.getMode().equals(leg1001m.getMode())
						&& leg1001m_8th_random_call.getMode().equals(leg1001m.getMode())
						&& leg1001m_9th_random_call.getMode().equals(leg1001m.getMode())
						&& leg1001m_10th_random_call.getMode().equals(leg1001m.getMode())));

		// Car availability
		Leg leg1001mCar = module.getAccessEgressModeAndTraveltime(personCarAlwaysAvailable,
				CoordUtils.createCoord(4951.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Assert.assertTrue(
				leg1001mCar.getMode().equals("bike") || leg1001mCar.getMode().equals("av") || leg1001mCar.getMode()
						.equals("car"));

		// Diagonal leg: total beeline egressDistance > maximumAccessDistance of walk, but orthogonal part on the road network < maximumAccessDistance of walk
		Leg leg1001mDiagonal = module.getAccessEgressModeAndTraveltime(personCarNeverAvailable,
				CoordUtils.createCoord(4949.00, 1113.25), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Assert.assertTrue(leg1001mDiagonal.getMode().equals("bike") || leg1001mDiagonal.getMode().equals("av"));

		// egressDistance > maximumAccessDistance of all available modes
		Leg leg2901m = module.getAccessEgressModeAndTraveltime(personCarAlwaysAvailable,
				CoordUtils.createCoord(6851.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Assert.assertTrue(leg2901m.getMode().equals("walk"));

		// Check distance and travel time - teleported mode
		double distf = config.plansCalcRoute().getModeRoutingParams().get(leg999m.getMode()).getBeelineDistanceFactor();
		double speedTeleported = config.plansCalcRoute()
				.getModeRoutingParams()
				.get(leg999m.getMode())
				.getTeleportedModeSpeed();
		double distanceTeleported = 999 * distf;
		Assert.assertEquals(distanceTeleported, leg999m.getRoute().getDistance(), 0.001);
		Assert.assertEquals(distanceTeleported / speedTeleported, leg999m.getTravelTime(), 0.001);

		// Check distance and travel time - non-teleported mode
		double distanceNonTeleported = 2501 * 1.3;
		Assert.assertEquals(distanceNonTeleported, leg2501m.getRoute().getDistance(), 0.001);
		Assert.assertEquals(distanceNonTeleported / 7.25, leg2501m.getTravelTime(), 0.001);

		// Check further leg attributes: departure time, start link and end link
		Assert.assertEquals(8 * 60 * 60, leg999m.getDepartureTime(), 0.1);
		Assert.assertEquals(Id.create("4151", Link.class), leg999m.getRoute().getStartLinkId());
		Assert.assertEquals(Id.create("23", Link.class), leg999m.getRoute().getEndLinkId());

		//		Why is there no departure time set for non-teleport modes
		//		Assert.assertEquals(8*60*60, leg2501m.getDepartureTime(), 0.1);
		Assert.assertEquals(Id.create("6171", Link.class), leg2501m.getRoute().getStartLinkId());
		Assert.assertEquals(Id.create("23", Link.class), leg2501m.getRoute().getEndLinkId());
	}

}
