package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.OccupancyData.DepartureData;
import ch.sbb.matsim.routing.pt.raptor.OccupancyData.LineData;
import ch.sbb.matsim.routing.pt.raptor.OccupancyData.RouteData;
import ch.sbb.matsim.routing.pt.raptor.OccupancyData.StopData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.handler.PersonScoreEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.dsim.Activities;
import org.matsim.dsim.DSimConfigGroup;
import org.matsim.examples.ExamplesUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The occupancy tracker publishes occupancy data to the router with the QSim and the DSim. With the DSim, each partition
 * has its own occupancy tracker. The published data and the occupancy-dependent scores must be the same as with a
 * single partition.
 */
class OccupancyTrackerDSimTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void sameOccupancyWithMultiplePartitions() {

		Result single = run(ControllerConfigGroup.MobsimType.dsim, 1);
		Result multi = run(ControllerConfigGroup.MobsimType.dsim, 4);

		assertThat(single.departures).isNotEmpty();
		assertThat(multi.departures.keySet()).isEqualTo(single.departures.keySet());
		single.departures.forEach((key, v) -> assertThat(multi.departures.get(key)).as(key).isEqualTo(v));

		assertThat(single.occupancyScores).isNotEmpty();
		assertThat(multi.occupancyScores).isEqualTo(single.occupancyScores);
	}

	@Test
	void qsimPublishesOccupancy() {

		Result result = run(ControllerConfigGroup.MobsimType.qsim, 1);

		assertThat(result.departures).isNotEmpty();
		assertThat(result.occupancyScores).isNotEmpty();
	}

	private record Departure(double vehDepTime, int paxCountAtDeparture, double earliestWaitStart, double latestWaitStart) {
	}

	private record Result(Map<String, Departure> departures, Map<String, Double> occupancyScores) {
	}

	public static final class OccupancyScoreCollector implements PersonScoreEventHandler {

		private final Map<String, Double> scores = new HashMap<>();

		@Override
		public void handleEvent(PersonScoreEvent event) {
			if (event.getKind().equals("pt-occupancy")) {
				scores.merge(event.getPersonId() + "@" + event.getTime(), event.getAmount(), Double::sum);
			}
		}
	}

	private Result run(ControllerConfigGroup.MobsimType mobsim, int threads) {

		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("kelheim"), "config.xml"));

		config.controller().setOutputDirectory(utils.getOutputDirectory() + mobsim + "-threads-" + threads);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(0);
		config.controller().setMobsim(mobsim.name());
		config.controller().setWriteEventsInterval(0);
		config.routing().setRoutingRandomness(0);

		Activities.addScoringParams(config);

		config.dsim().setThreads(threads);
		config.dsim().setPartitioning(DSimConfigGroup.Partitioning.bisect);
		config.dsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
		config.dsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.FIFO);
		config.dsim().setVehicleBehavior(QSimConfigGroup.VehicleBehavior.teleport);
		config.dsim().setNetworkModes(Set.of(TransportMode.car, "freight"));
		config.dsim().setStartTime(0);
		config.dsim().setEndTime(36 * 3600);

		ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class).setUseCapacityConstraints(true);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		var carAndFreight = Set.of(TransportMode.car, "freight", TransportMode.ride);
		scenario.getNetwork().getLinks().values().stream()
			.filter(l -> l.getAllowedModes().contains(TransportMode.car))
			.forEach(l -> l.setAllowedModes(carAndFreight));

		OccupancyScoreCollector scoreCollector = new OccupancyScoreCollector();
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(RaptorInVehicleCostCalculator.class).toInstance(new CapacityDependentInVehicleCostCalculator());
				addEventHandlerBinding().toInstance(scoreCollector);
			}
		});
		controler.run();

		OccupancyData occupancyData = controler.getInjector().getInstance(OccupancyData.class);

		Map<String, Departure> departures = new HashMap<>();
		for (Map.Entry<Id<TransitLine>, LineData> line : occupancyData.lineData.entrySet()) {
			for (Map.Entry<Id<TransitRoute>, RouteData> route : line.getValue().routeData.entrySet()) {
				for (Map.Entry<Id<TransitStopFacility>, StopData> stop : route.getValue().stopData.entrySet()) {
					for (DepartureData dep : stop.getValue().depList) {
						String key = line.getKey() + "/" + route.getKey() + "/" + stop.getKey() + "/" + dep.departureId;
						departures.put(key, new Departure(dep.vehDepTime, dep.paxCountAtDeparture, dep.earliestWaitStart, dep.latestWaitStart));
					}
				}
			}
		}

		return new Result(departures, scoreCollector.scores);
	}
}
