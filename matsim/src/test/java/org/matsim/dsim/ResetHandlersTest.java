package org.matsim.dsim;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.DistributedEventHandler;
import org.matsim.api.core.v01.events.handler.DistributedMode;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A NODE_CONCURRENT handler is registered with one task per partition, but shares a single instance. It must be reset
 * only once per iteration, otherwise expensive resets (e.g. {@link org.matsim.core.trafficmonitoring.TravelTimeCalculator})
 * scale with the number of partitions.
 */
class ResetHandlersTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void nodeConcurrentHandlerIsResetOncePerIteration() {

		Config config = utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.none);

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(1);
		config.controller().setMobsim("dsim");

		config.dsim().setThreads(4);
		config.dsim().setPartitioning(DSimConfigGroup.Partitioning.bisect);
		config.dsim().setLinkDynamics(config.qsim().getLinkDynamics());
		config.dsim().setTrafficDynamics(config.qsim().getTrafficDynamics());
		config.dsim().setNetworkModes(new HashSet<>(config.qsim().getMainModes()));
		config.dsim().setStartTime(0);
		config.dsim().setEndTime(30 * 3600);
		config.qsim().setFlowCapFactor(1.);
		config.qsim().setStorageCapFactor(1.);

		Activities.addScoringParams(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		ResetCounter counter = new ResetCounter();
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(counter);
			}
		});
		controler.run();

		assertThat(counter.resets).containsOnlyKeys(0, 1);
		assertThat(counter.resets.values()).allMatch(n -> n == 1);
	}

	@DistributedEventHandler(value = DistributedMode.NODE_CONCURRENT)
	public static final class ResetCounter implements LinkEnterEventHandler {

		private final Map<Integer, Integer> resets = new ConcurrentHashMap<>();

		@Override
		public void handleEvent(LinkEnterEvent event) {
		}

		@Override
		public void reset(int iteration) {
			resets.merge(iteration, 1, Integer::sum);
		}
	}
}
