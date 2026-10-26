package org.matsim.dsim.simulation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.mobsim.dsim.NodeSingleton;
import org.matsim.core.mobsim.dsim.NodeSingletons;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.dsim.Activities;
import org.matsim.dsim.DSimConfigGroup;
import org.matsim.dsim.LocalContext;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SimProviderTest {

	// ── Fixture types for NodeSingletonDetector tests ────────────────────────

	@NodeSingleton
	static class DirectlyAnnotated {
	}

	@NodeSingleton
	interface AnnotatedIface {
	}

	static class ImplementsAnnotatedIface implements AnnotatedIface {
	}

	@NodeSingleton
	static class AnnotatedBase {
	}

	static class ExtendsAnnotatedBase extends AnnotatedBase {
	}

	// deep: ParentIface extends AnnotatedIface, LeafClass implements ParentIface
	interface ParentIface extends AnnotatedIface {
	}

	static class ImplementsParentIface implements ParentIface {
	}

	static class PlainClass {
	}

	interface PlainIface {
	}

	static class ImplementsPlainIface implements PlainIface {
	}

	// ── Group 1: NodeSingletonDetector ───────────────────────────────────────

	@Test
	void detectsDirectAnnotation() {
		assertThat(NodeSingletons.isNodeSingleton(new DirectlyAnnotated())).isTrue();
	}

	@Test
	void detectsAnnotatedInterface() {
		// This is the DrtOptimizer pattern: interface is @NodeSingleton, implementing class is not
		assertThat(NodeSingletons.isNodeSingleton(new ImplementsAnnotatedIface())).isTrue();
	}

	@Test
	void detectsAnnotatedSuperclass() {
		assertThat(NodeSingletons.isNodeSingleton(new ExtendsAnnotatedBase())).isTrue();
	}

	@Test
	void detectsDeepInterfaceHierarchy() {
		// Class → PlainIface → AnnotatedIface: annotation must be found transitively
		assertThat(NodeSingletons.isNodeSingleton(new ImplementsParentIface())).isTrue();
	}

	@Test
	void rejectsPlainClass() {
		assertThat(NodeSingletons.isNodeSingleton(new PlainClass())).isFalse();
	}

	@Test
	void rejectsUnannotatedInterface() {
		assertThat(NodeSingletons.isNodeSingleton(new ImplementsPlainIface())).isFalse();
	}

	// ── Integration: multi-iteration regression ──────────────────────────────

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * Regression test for the SimProvider.listeners accumulation bug.
	 * <p>
	 * Before the fix, {@code SimProvider.listeners} was never cleared between iterations. NodeSingleton listeners (e.g. {@code MoiaEDrtOptimizer} via
	 * the {@code @NodeSingleton} {@code DrtOptimizer} interface) accumulated across iterations. In iteration 2, both the stale iteration-1 instance
	 * (with a shut-down ForkJoinPool) and the fresh iteration-2 instance were fired, causing a {@code RejectedExecutionException}.
	 */
	@Test
	void nodeSingletonListenersNotAccumulatedAcrossIterations() {
		var kelheim = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("kelheim"), "config-with-drt.xml");
		Config config = ConfigUtils.loadConfig(kelheim);

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controller().setLastIteration(1);
		config.controller().setWriteEventsInterval(1);
		config.controller().setMobsim(ControllerConfigGroup.MobsimType.dsim.name());
		config.dsim().setThreads(1);
		config.routing().setRoutingRandomness(0);
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);

		config.dsim().setLinkDynamics(config.qsim().getLinkDynamics());
		config.dsim().setTrafficDynamics(config.qsim().getTrafficDynamics());
		config.dsim().setStuckTime(config.qsim().getStuckTime());
		config.dsim().setNetworkModes(new HashSet<>(config.qsim().getMainModes()));
		config.dsim().setStartTime(config.qsim().getStartTime().orElse(0));
		config.dsim().setEndTime(config.qsim().getEndTime().orElse(86400));
		config.dsim().setVehicleBehavior(config.qsim().getVehicleBehavior());
		config.dsim().setPartitioning(DSimConfigGroup.Partitioning.bisect);

		Activities.addScoringParams(config);

		MultiModeDrtConfigGroup multiModeDrtConfig = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		ConfigUtils.addOrGetModule(config, DvrpConfigGroup.class);
		DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfig, config.scoring(), config.routing());

		var scenario = ScenarioUtils.loadScenario(config);

		scenario.getNetwork().getLinks().values().parallelStream()
			.filter(l -> l.getAllowedModes().contains("car"))
			.forEach(l -> l.setAllowedModes(Stream.concat(l.getAllowedModes().stream(), Stream.of("freight")).collect(Collectors.toSet())));

		scenario.getPopulation().getFactory().getRouteFactories()
			.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

		Controler controler = new Controler(scenario, LocalContext.create(scenario.getConfig()));
		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new MultiModeDrtModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtConfig));

		assertThatCode(controler::run).doesNotThrowAnyException();
	}
}
