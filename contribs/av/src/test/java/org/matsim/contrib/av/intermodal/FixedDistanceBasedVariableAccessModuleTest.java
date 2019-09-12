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
import org.matsim.contrib.av.intermodal.router.FixedDistanceBasedVariableAccessModule;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author vsp-gleich
 */
public class FixedDistanceBasedVariableAccessModuleTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testGetAccessEgressModeAndTraveltime() throws MalformedURLException {
		URL configUrl = new File(utils.getPackageInputDirectory() + "config.xml").toURI().toURL();
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeTaxiConfigGroup());
		Scenario scenario = ScenarioUtils.loadScenario(config);
		FixedDistanceBasedVariableAccessModule module = new FixedDistanceBasedVariableAccessModule(
				scenario.getNetwork(), config);

		/*
		 * 		FixedDistanceBasedVariableAccessModule.getModeForDistance() currently
		 * 		only returns the first mode found that is suited for the beeline
		 * 		distance between the coords. As distanceToMode in
		 * 		FixedDistanceBasedVariableAccessModule is implemented as TreeMap
		 * 		getModeForDistance() currently finds the mode with least
		 * 		maximumAccessDistance first and returns it.
		 */
		module.registerMode("av", 2500, false);
		module.registerMode("walk", 1000, true);
		module.registerMode("bike", 2000, true);

		Person personCarAlwaysAvailable = scenario.getPopulation()
				.getPersons()
				.get(Id.create("car_always_available", Person.class));

		// Check decision points between available modes
		Leg leg999m = module.getAccessEgressModeAndTraveltime(personCarAlwaysAvailable,
				CoordUtils.createCoord(4949.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Assert.assertTrue(leg999m.getMode().equals("walk"));

		Leg leg1000m = module.getAccessEgressModeAndTraveltime(personCarAlwaysAvailable,
				CoordUtils.createCoord(4950.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Assert.assertTrue(leg1000m.getMode().equals("walk"));

		Leg leg1001m = module.getAccessEgressModeAndTraveltime(personCarAlwaysAvailable,
				CoordUtils.createCoord(4951.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Assert.assertTrue(leg1001m.getMode().equals("bike"));

		Leg leg1999m = module.getAccessEgressModeAndTraveltime(personCarAlwaysAvailable,
				CoordUtils.createCoord(5949.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Assert.assertTrue(leg1999m.getMode().equals("bike"));

		Leg leg2000m = module.getAccessEgressModeAndTraveltime(personCarAlwaysAvailable,
				CoordUtils.createCoord(5950.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Assert.assertTrue(leg2000m.getMode().equals("bike"));

		Leg leg2001m = module.getAccessEgressModeAndTraveltime(personCarAlwaysAvailable,
				CoordUtils.createCoord(5951.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Assert.assertTrue(leg2001m.getMode().equals("av"));

		Leg leg1001mDiagonal = module.getAccessEgressModeAndTraveltime(personCarAlwaysAvailable,
				CoordUtils.createCoord(4949.00, 1113.25), CoordUtils.createCoord(3950.00, 1050.00), 8 * 60 * 60);
		Assert.assertTrue(leg1001mDiagonal.getMode().equals("bike"));

		// egressDistance > maximumAccessDistance of all available modes
		//		Leg leg2901m = module.getAccessEgressModeAndTraveltime(personCarAlwaysAvailable, CoordUtils.createCoord(6851.00, 1050.00), CoordUtils.createCoord(3950.00, 1050.00), 8*60*60);
		//		Assert.assertTrue(leg2901m.getMode().equals("walk"));

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
		double distanceNonTeleported = 2001 * 1.3;
		Assert.assertEquals(distanceNonTeleported, leg2001m.getRoute().getDistance(), 0.001);
		Assert.assertEquals(distanceNonTeleported / 7.25, leg2001m.getTravelTime(), 0.001);

		// Check further leg attributes: departure time, start link and end link
		Assert.assertEquals(8 * 60 * 60, leg999m.getDepartureTime(), 0.1);
		// start 5141, end 4131
		Assert.assertEquals(Id.create("4151", Link.class), leg999m.getRoute().getStartLinkId());
		Assert.assertEquals(Id.create("23", Link.class), leg999m.getRoute().getEndLinkId());

		//		Why is there no departure time set for non-teleport modes
		//		Assert.assertEquals(8*60*60, leg2001m.getDepartureTime(), 0.1);
		// start 6151, end 4131
		Assert.assertEquals(Id.create("5161", Link.class), leg2001m.getRoute().getStartLinkId());
		Assert.assertEquals(Id.create("23", Link.class), leg2001m.getRoute().getEndLinkId());
	}

}
