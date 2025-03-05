package org.matsim.contrib.profiling;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.profiling.events.ProfilingEventsModule;
import org.matsim.contrib.profiling.instrument.ProfilerInstrumentationModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.Controller;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.contrib.profiling.instrument.ProfilerInstrumentationConfiguration.defaultConfiguration;

public class ProfilingIT {

	private final static Logger log = LogManager.getLogger(ProfilingIT.class);

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void whenProfilingTwoIterationsOfThreeTotal_expectResultFileToContainJFREvents() {
		// use simple scenario configuration for testing
		Config config = this.utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.controller().setLastIteration(2);
		Controller controller = new Controler(config);
		// configure events and instrument modules
		controller.addOverridingModule(new ProfilerInstrumentationModule(defaultConfiguration()));
		controller.addOverridingModule(new ProfilingEventsModule());
		// run scenario
		controller.run();
		// check for profile.jfr to exist
		assertThat(new File(config.controller().getOutputDirectory(), "profile.jfr")).exists();
		// read the profile.jfr and expect the events defined by the module and via AOP to be there

	}

}
