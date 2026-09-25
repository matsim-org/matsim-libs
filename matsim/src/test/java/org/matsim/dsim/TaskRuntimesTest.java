package org.matsim.dsim;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.messages.ComputeNode;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.serialization.MessageTypeRegistry;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskRuntimesTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void runtimesAreAggregatedIntoBins() {

		LongList runtimes = new LongArrayList();
		SimTask.addRuntime(runtimes, 0, 1);
		SimTask.addRuntime(runtimes, 1, 2);
		SimTask.addRuntime(runtimes, 10, 4);
		SimTask.addRuntime(runtimes, 11, 8);
		SimTask.addRuntime(runtimes, 35, 16);

		// bin i holds (10 * (i - 1), 10 * i], nothing is dropped
		assertThat(runtimes.toLongArray()).containsExactly(1L, 6L, 8L, 0L, 16L);
	}

	@Test
	void eventHandlerRuntimeIsStoredAtExecutionTimeAndReset() {

		DistributedEventsManager em = mock(DistributedEventsManager.class);
		when(em.getComputeNode()).thenReturn(mock(ComputeNode.class));

		DefaultEventHandlerTask task = new DefaultEventHandlerTask(new NoopHandler(), 0, 1, em, MessageTypeRegistry.getInstance(), null);

		task.setTime(900);
		task.beforeExecution();
		// an async task may only finish when the simulation has already advanced
		task.setTime(1234);
		task.run();

		long[] runtimes = task.getRuntime().toLongArray();
		assertThat(runtimes).hasSize(91);
		assertThat(runtimes[90]).isPositive();

		// event handler tasks outlive the mobsim, runtimes of the previous iteration must not be kept
		task.resetTask(1);
		assertThat(task.getRuntime().toLongArray()).isEmpty();
	}

	public static final class NoopHandler implements LinkEnterEventHandler {
		@Override
		public void handleEvent(LinkEnterEvent event) {
		}
	}

	/**
	 * Runtimes of all tasks must lie within the simulated time, also for event handler tasks, which outlive the mobsim.
	 */
	@Test
	void taskRuntimesLieWithinSimulatedTime() throws IOException {

		Config config = utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.none);

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(1);
		config.controller().setMobsim("dsim");

		double endTime = 30 * 3600;

		config.dsim().setThreads(4);
		config.dsim().setPartitioning(DSimConfigGroup.Partitioning.bisect);
		config.dsim().setLinkDynamics(config.qsim().getLinkDynamics());
		config.dsim().setTrafficDynamics(config.qsim().getTrafficDynamics());
		config.dsim().setNetworkModes(new HashSet<>(config.qsim().getMainModes()));
		config.dsim().setStartTime(0);
		config.dsim().setEndTime(endTime);
		config.qsim().setFlowCapFactor(1.);
		config.qsim().setStorageCapFactor(1.);

		Activities.addScoringParams(config);

		new Controler(ScenarioUtils.loadScenario(config)).run();

		List<String[]> rows = Files.readAllLines(Path.of(utils.getOutputDirectory(), "runtimes-0", "tasks.csv")).stream()
			.skip(1)
			.map(l -> l.split(","))
			.filter(r -> !r[1].equals("-1"))
			.toList();

		assertThat(rows)
			.anyMatch(r -> r[0].equals("\"SimProcess\""))
			.anyMatch(r -> r[0].equals("\"VolumesAnalyzer\""))
			.allMatch(r -> Double.parseDouble(r[2]) <= endTime);
	}
}
