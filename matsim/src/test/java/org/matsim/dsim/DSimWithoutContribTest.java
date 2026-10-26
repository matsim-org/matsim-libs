package org.matsim.dsim;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A single-node run of the {@code dsim} mobsim must work with only the matsim core on the classpath. Fory is not a
 * dependency of this module, so any attempt to load {@code ForySerializationProvider} would fail with a
 * {@link NoClassDefFoundError}. The distributed-simulation contrib is only needed for runs across multiple compute nodes.
 */
class DSimWithoutContribTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void runsSingleNodeWithoutSerializationCodec() {

		Config config = utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.none);

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(0);
		config.controller().setWriteEventsInterval(1);
		config.controller().setMobsim("dsim");

		config.dsim().setThreads(1);
		config.dsim().setPartitioning(DSimConfigGroup.Partitioning.bisect);
		config.dsim().setLinkDynamics(config.qsim().getLinkDynamics());
		config.dsim().setTrafficDynamics(config.qsim().getTrafficDynamics());
		config.dsim().setNetworkModes(new java.util.HashSet<>(config.qsim().getMainModes()));
		config.dsim().setStartTime(0);
		config.dsim().setEndTime(30 * 3600);
		config.qsim().setFlowCapFactor(1.);
		config.qsim().setStorageCapFactor(1.);

		Activities.addScoringParams(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		EventsCollector collector = new EventsCollector();
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(collector);
			}
		});
		controler.run();

		// the mobsim ran and produced traffic events - without Fory on the classpath
		assertThat(collector.getEvents()).anyMatch(PersonDepartureEvent.class::isInstance);
		assertThat(collector.getEvents()).anyMatch(PersonArrivalEvent.class::isInstance);
	}
}
