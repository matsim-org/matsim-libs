package org.matsim.dsim;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The volumes analyzer is called concurrently by all partitions. Volumes must be the same as with a single partition.
 */
class VolumesAnalyzerDSimTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void sameVolumesWithMultiplePartitions() {

		Result single = run(1);
		Result multi = run(4);

		assertThat(single.volumes).isNotEmpty();
		assertThat(multi.volumes.keySet()).isEqualTo(single.volumes.keySet());
		single.volumes.forEach((key, v) -> assertThat(multi.volumes.get(key)).as(key).isEqualTo(v));

		assertThat(multi.modes).isEqualTo(single.modes);
	}

	private record Result(Map<String, int[]> volumes, Set<String> modes) {
	}

	private Result run(int threads) {

		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("kelheim"), "config.xml"));

		config.controller().setOutputDirectory(utils.getOutputDirectory() + "threads-" + threads);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(0);
		config.controller().setMobsim(ControllerConfigGroup.MobsimType.dsim.name());
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

		Scenario scenario = ScenarioUtils.loadScenario(config);
		var carAndFreight = Set.of(TransportMode.car, "freight", TransportMode.ride);
		scenario.getNetwork().getLinks().values().stream()
			.filter(l -> l.getAllowedModes().contains(TransportMode.car))
			.forEach(l -> l.setAllowedModes(carAndFreight));

		Controler controler = new Controler(scenario);
		controler.run();

		VolumesAnalyzer analyzer = controler.getInjector().getInstance(VolumesAnalyzer.class);

		Map<String, int[]> volumes = new HashMap<>();
		for (Id<Link> linkId : analyzer.getLinkIds()) {
			volumes.put(linkId.toString(), analyzer.getVolumesForLink(linkId).clone());
			for (String mode : analyzer.getModes()) {
				int[] v = analyzer.getVolumesForLink(linkId, mode);
				if (v != null)
					volumes.put(linkId + "/" + mode, v.clone());
			}
		}

		return new Result(volumes, analyzer.getModes());
	}
}
