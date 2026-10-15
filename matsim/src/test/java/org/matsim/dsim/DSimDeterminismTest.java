package org.matsim.dsim;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two runs of {@code dsim} with the same configuration and the same seed must produce the same traffic.
 * <p>
 * The scenario is deliberately congested: it loads equil far beyond capacity, so that links run out of storage capacity and queues reach
 * back over the nodes. Spillback is the situation in which the intersection update order decides who moves, which is what
 * {@code ActiveNodes} had to be made reproducible for - it held its active nodes in a {@link java.util.HashSet} of {@code SimNode}s, which
 * define no {@code hashCode()}, so the update order followed {@link System#identityHashCode(Object)} and differed on every run.
 * <p>
 * Both runs happen in the same JVM, which is enough to catch an identity-hash ordering: the two runs build separate {@code SimNode}
 * objects and their identity hash codes differ. Note that this scenario is small enough that it did not expose a difference in the
 * traffic even before that fix - it guards the property, it does not prove the fix. {@code dsim.threads} is greater than one, so the
 * cross-partition paths are exercised too.
 */
class DSimDeterminismTest {

	/** Copies of the 100 equil agents, to push the network into spillback. */
	private static final int CLONES = 29;

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void twoRunsWithTheSameSeedProduceTheSameTraffic() {

		List<Event> first = run("run1");
		List<Event> second = run("run2");

		assertThat(first).as("the scenario must produce traffic to be a meaningful test").hasSizeGreaterThan(1000);
		assertThat(second).as("event count differs between two identical runs").hasSameSizeAs(first);

		// The events as a canonical multiset: the physical outcome, independent of the order in which the partitions happened to report the
		// events of a time step.
		List<String> firstSorted = first.stream().map(Event::toString).sorted().toList();
		List<String> secondSorted = second.stream().map(Event::toString).sorted().toList();

		for (int i = 0; i < firstSorted.size(); i++) {
			if (!firstSorted.get(i).equals(secondSorted.get(i))) {
				throw new AssertionError(
					"two identical dsim runs produce different traffic, first difference at %d of %d:%n  run 1: %s%n  run 2: %s"
						.formatted(i, firstSorted.size(), firstSorted.get(i), secondSorted.get(i)));
			}
		}
	}

	/**
	 * Within a time step, the partitions report their events in whatever order they finish, so the emitted event stream is not reproducible
	 * even when the traffic is. This is a separate defect from the node ordering above and is not fixed; the test documents it.
	 */
	@Test
	@Disabled("known open issue: with dsim.threads > 1 the event order within a time step is not reproducible")
	void twoRunsWithTheSameSeedEmitEventsInTheSameOrder() {

		List<Event> first = run("order1");
		List<Event> second = run("order2");

		for (int i = 0; i < first.size(); i++) {
			if (!first.get(i).toString().equals(second.get(i).toString())) {
				throw new AssertionError(
					"two identical dsim runs emit the same events in a different order, first difference at event %d of %d:%n  run 1: %s%n  run 2: %s"
						.formatted(i, first.size(), first.get(i), second.get(i)));
			}
		}
	}

	private List<Event> run(String name) {

		Config config = utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.none);

		config.controller().setOutputDirectory(utils.getOutputDirectory() + "/" + name);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(0);
		config.controller().setWriteEventsInterval(1);
		config.controller().setMobsim("dsim");
		config.global().setRandomSeed(4711);

		config.dsim().setThreads(4);
		config.dsim().setPartitioning(DSimConfigGroup.Partitioning.bisect);
		config.dsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
		config.dsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
		config.dsim().setNetworkModes(new java.util.HashSet<>(config.qsim().getMainModes()));
		config.dsim().setStartTime(0);
		config.dsim().setEndTime(30 * 3600);

		config.qsim().setFlowCapFactor(1.);
		config.qsim().setStorageCapFactor(1.);
		// do not abort queued vehicles, we want them to queue
		config.dsim().setStuckTime(3600);

		Activities.addScoringParams(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		// Squeeze the network, so that links run out of storage capacity and spill back. Without spillback, every node can serve every
		// offering in-link in every time step and the update order cannot change the outcome. equil as shipped is far too generous:
		// 36000 veh/h on 10 km links, which the 100 agents never fill.
		for (Link link : scenario.getNetwork().getLinks().values()) {
			link.setCapacity(150);
			link.setNumberOfLanes(1);
			link.setLength(100);
		}

		// ... and raise the demand well above what that network can carry, by cloning the 100 equil agents. Queues then reach back over the
		// nodes, which is the situation in which the update order decides who moves.
		Population population = scenario.getPopulation();
		List<Person> originals = List.copyOf(population.getPersons().values());
		for (int copy = 1; copy <= CLONES; copy++) {
			for (Person original : originals) {
				Person clone = population.getFactory().createPerson(Id.createPersonId(original.getId() + "_" + copy));
				// a deep copy: the clones must not share plan instances, the scoring keeps per-plan state
				PopulationUtils.copyFromTo(original, clone);
				population.addPerson(clone);
			}
		}

		EventsCollector collector = new EventsCollector();
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(collector);
			}
		});
		controler.run();

		return collector.getEvents();
	}
}
