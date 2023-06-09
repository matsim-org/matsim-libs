package ch.sbb.matsim.contrib.railsim.integration;

import ch.sbb.matsim.contrib.railsim.RailsimModule;
import ch.sbb.matsim.contrib.railsim.events.RailsimTrainStateEvent;
import ch.sbb.matsim.contrib.railsim.qsimengine.RailsimQSimModule;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.vsp.scenario.SnzActivities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.vehicles.VehicleType;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Set;

public class RailsimIntegrationTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void scenario_kelheim() {

		URL base = ExamplesUtils.getTestScenarioURL("kelheim");

		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(base, "config.xml"));
		config.controler().setLastIteration(0);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setCreateGraphs(false);
		config.controler().setDumpDataAtEnd(false);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		// Convert all pt to rail
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.car))
				link.setAllowedModes(Set.of(TransportMode.car, "ride", "freight"));

			if (link.getAllowedModes().contains(TransportMode.pt)) {
				link.setFreespeed(50);
				link.setAllowedModes(Set.of("rail"));
			}
		}

		// Maximum velocity must be configured
		for (VehicleType type : scenario.getTransitVehicles().getVehicleTypes().values()) {
			type.setMaximumVelocity(30);
			type.setLength(100);
		}

		SnzActivities.addScoringParams(config);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new RailsimModule());
		controler.configureQSimComponents(components -> new RailsimQSimModule().configure(components));

		controler.run();
	}

	@Test
	public void test0_simple() {

		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "0_simple"));


	}

	@Test
	public void scenario_genf() {

		// TODO: can probably be replaced by the genf_bern test

		File dir = new File(utils.getPackageInputDirectory(), "test_genf");

		Config config = ConfigUtils.loadConfig(new File(dir, "config.xml").toString());

		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		config.controler().setCreateGraphs(false);
		config.controler().setDumpDataAtEnd(false);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new RailsimModule());
		controler.configureQSimComponents(components -> new RailsimQSimModule().configure(components));

		controler.run();

	}

	private EventsCollector runSimulation(File scenarioDir) {
		Config config = ConfigUtils.loadConfig(new File(scenarioDir, "config.xml").toString());

		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setDumpDataAtEnd(false);
		config.controler().setCreateGraphs(false);
		config.controler().setLastIteration(0);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new RailsimModule());
		controler.configureQSimComponents(components -> new RailsimQSimModule().configure(components));

		EventsCollector collector = new EventsCollector();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(collector);
			}
		});

		controler.run();

		return collector;
	}

	private double timeToAccelerate(double v0, double v, double a) {
		return (v - v0) / a;
	}

	private double distanceTravelled(double v0, double a, double t) {
		return 0.5 * a * t * t + v0 * t;
	}

	private double timeForDistance(double d, double v) {
		return d / v;
	}

	private void assertTrainState(double time, double speed, double targetSpeed, double acceleration, double headPosition, RailsimTrainStateEvent event) {
		Assert.assertEquals(Math.ceil(time), event.getTime(), 1e-7);
		Assert.assertEquals(speed, event.getSpeed(), 1e-5);
		Assert.assertEquals(targetSpeed, event.getTargetSpeed(), 1e-7);
		Assert.assertEquals(acceleration, event.getAcceleration(), 1e-5);
		Assert.assertEquals(headPosition, event.getHeadPosition(), 1e-5);
	}

	@Test
	public void test0_varyingCapacities() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "0_varyingCapacities"));

		// print events of train1 for debugging
		List<RailsimTrainStateEvent> train1events = collector.getEvents().stream().filter(event -> event instanceof RailsimTrainStateEvent).map(event -> (RailsimTrainStateEvent) event).filter(event -> event.getVehicleId().toString().equals("train1")).toList();
		train1events.forEach(System.out::println);

		// calculation of expected time for train1: acceleration = 0.5, length = 100
		// route: - station: 2.77777m/s
		//        - link: 50000m, 13.8889m/s
		//        - station: 200m, 2.777777m/s
		//        - link: 50000m, 13.8889m/s
		//        - station: 200m, 2.777777m/s

		double departureTime = 8 * 3600;
		double trainLength = 100;
		double acceleration = 0.5;
		double stationSpeed = 2.7777777777777777;
		double stationLength = 200;
		double linkSpeed = 13.8889;
		double linkLength = 50000;

		double currentTime = departureTime;
		assertTrainState(currentTime, 0, 0, 0, stationLength, train1events.get(0));

		// train starts in the station, accelerates to station speed and continues until the train has left the station link
		assertTrainState(currentTime, 0, stationSpeed, acceleration, 0, train1events.get(1));

		double accTime1 = timeToAccelerate(0, stationSpeed, acceleration);
		double accDistance1 = distanceTravelled(0, acceleration, accTime1);
		currentTime += accTime1;
		assertTrainState(currentTime, stationSpeed, stationSpeed, 0, accDistance1, train1events.get(2));

		double cruiseTime2 = timeForDistance(trainLength - accDistance1, stationSpeed);
		double cruiseDistance2 = distanceTravelled(stationSpeed, 0, cruiseTime2); // should be = trainLength - accDistance1
		currentTime += cruiseTime2;
		assertTrainState(currentTime, stationSpeed, stationSpeed, 0, accDistance1 + cruiseDistance2, train1events.get(3));

		// train further accelerates to link speed
		double accTime3 = timeToAccelerate(stationSpeed, linkSpeed, acceleration);
		double accDistance3 = distanceTravelled(stationSpeed, acceleration, accTime3);
		currentTime += accTime3;
		assertTrainState(currentTime, linkSpeed, linkSpeed, 0, accDistance1 + cruiseDistance2 + accDistance3, train1events.get(4));

		// train can cruise with link speed until it needs to decelerate for next station

		double decTime5 = timeToAccelerate(linkSpeed, stationSpeed, -acceleration);
		double decDistance5 = distanceTravelled(linkSpeed, -acceleration, decTime5);

		double cruiseDistance4 = linkLength - accDistance1 - cruiseDistance2 - accDistance3 - decDistance5;
		double cruiseTime4 = timeForDistance(cruiseDistance4, linkSpeed);
		currentTime += cruiseTime4;
		assertTrainState(currentTime, linkSpeed, linkSpeed, 0, linkLength - decDistance5, train1events.get(6));
		// start deceleration
		assertTrainState(currentTime, linkSpeed, stationSpeed, -acceleration, linkLength - decDistance5, train1events.get(7));
		currentTime += decTime5;
		assertTrainState(currentTime, stationSpeed, stationSpeed, 0, linkLength, train1events.get(8));

		// train passes station completely
		double cruiseDistance6 = stationLength + trainLength;
		double cruiseTime6 = timeForDistance(cruiseDistance6, stationSpeed);
		currentTime += cruiseTime6;

		// TODO from here on forward, train-events are not yet correctly matched and the test will fail

		// train can accelerate again to link speed
//		assertTrainState(currentTime, stationSpeed, linkSpeed, acceleration, trainLength, train1events.get(10)); // TODO
		double accTime7 = timeToAccelerate(stationSpeed, linkSpeed, acceleration);
		double accDistance7 = distanceTravelled(stationSpeed, accDistance1, accTime7);
		currentTime += accTime7;
//		assertTrainState(currentTime, linkSpeed, linkSpeed, 0, trainLength + accDistance7, train1events.get(11)); // TODO

		// train can cruise with link speed until it needs to decelerate for final station
		double decTime9 = timeToAccelerate(linkSpeed, stationSpeed, -acceleration);
		double decDistance9 = distanceTravelled(linkSpeed, -acceleration, decTime9);

		double cruiseDistance8 = linkLength - trainLength - accDistance7 - decDistance9;
		double cruiseTime8 = timeForDistance(cruiseDistance8, linkSpeed);

		currentTime += cruiseTime8;
//		assertTrainState(currentTime, linkSpeed, stationSpeed, -acceleration, trainLength + accDistance7, train1events.get(12)); // TODO
		currentTime += decTime9;
//		assertTrainState(currentTime, stationSpeed, stationSpeed, 0, linkLength, train1events.get(13)); // TODO

		// train can cruise into station link until it needs to fully brake
		double decTime11 = timeToAccelerate(stationSpeed, 0, -acceleration);
		double decDistance11 = distanceTravelled(stationSpeed, -acceleration, decTime11);

		double cruiseDistance10 = stationLength - decDistance11;
		double cruiseTime10 = timeForDistance(cruiseDistance10, stationSpeed);
		currentTime += cruiseTime10;
//		assertTrainState(currentTime, stationSpeed, 0, -acceleration, stationLength - cruiseDistance10, train1events.get(14)); // TODO
		// final train arrival
		currentTime += decTime11;
//		assertTrainState(currentTime, 0, 0, 0, stationLength, train1events.get(15)); // TODO
	}

	@Test @Ignore(value="no assert statements yet")
	public void test1_oppositeTraffic() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "1_oppositeTraffic"));

//		List<VehicleArrivesAtFacilityEvent> arrivalEvents = collector.getEvents()
//			.stream()
//			.filter(event -> event instanceof VehicleArrivesAtFacilityEvent)
//			.map(event -> (VehicleArrivesAtFacilityEvent) event)
//			.filter(event -> event.getFacilityId().toString().equals("t3_A-B"))
//			.toList();
//		VehicleArrivesAtFacilityEvent train0Arrival = arrivalEvents.stream()
//			.filter(event -> event.getFacilityId().toString().equals("t3_A-B"))
//			.filter(event -> event.getVehicleId().toString().equals("train1"))
//			.findFirst().orElseThrow();
//		Assert.assertEquals("train1 should arrive at 10:00:00", 36000.0, train0Arrival.getTime(), 1e-7); // TODO fix times
//		VehicleArrivesAtFacilityEvent train10Arrival = arrivalEvents.stream()
//			.filter(event -> event.getFacilityId().toString().equals("t1_B-A"))
//			.filter(event -> event.getVehicleId().toString().equals("train2"))
//			.findFirst().orElseThrow();
//		Assert.assertEquals("train2 should arrive at 10:00:00", 36000.0, train0Arrival.getTime(), 1e-7); // TODO fix times
	}

	@Test @Ignore(value="no assert statements yet")
	public void test2_oppositeTraffic_multipleTrains_oneSlowTrain() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "2_multipleOppositeTraffic"));
	}

	@Test @Ignore(value="no assert statements yet")
	public void test3_twoSources() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "3_twoSources"));
	}

	@Test @Ignore(value="no assert statements yet")
	public void test4_genf_bern() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "4_genf_bern"));
	}

	@Test @Ignore(value="no assert statements yet")
	public void test5_complexTwoSources() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "5_complexTwoSources"));
	}

	@Test @Ignore(value="no assert statements yet")
	public void test6_threeTracksMicroscopic() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "6_threeTracksMicroscopic"));
	}

	@Test @Ignore(value="no assert statements yet")
	public void test7_trainFollowing() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "7_trainFollowing"));
	}

	@Test @Ignore(value="no assert statements yet")
	public void test8_microStation() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "8_microStation"));
	}

	@Test @Ignore(value="no assert statements yet")
	public void test9_microStation2() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "9_microStation2"));
	}

	@Test @Ignore(value="no assert statements yet")
	public void test10_cross() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "10_cross"));
	}

	@Test @Ignore(value="no assert statements yet")
	public void test11_mesoStation() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "11_mesoStation"));
	}

	@Test @Ignore(value="no assert statements yet")
	public void test12_mesoStation2() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "12_mesoStation2"));
	}

	@Test @Ignore(value="no assert statements yet")
	public void test13_Y() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "13_Y"));
	}

	@Test @Ignore(value="no assert statements yet")
	public void test14_mesoStations() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "14_mesoStations"));
	}

	@Test @Ignore(value="no assert statements yet")
	public void test_station_rerouting() {
		EventsCollector collector = runSimulation(new File(utils.getPackageInputDirectory(), "station_rerouting"));
	}

}
