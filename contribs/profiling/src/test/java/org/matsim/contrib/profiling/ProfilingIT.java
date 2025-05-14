package org.matsim.contrib.profiling;

import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.RecordingFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.profiling.aop.TraceProfilingEvent;
import org.matsim.contrib.profiling.events.FireDefaultProfilingEventsModule;
import org.matsim.contrib.profiling.events.JFRMatsimEvent;
import org.matsim.contrib.profiling.instrument.EnableProfilingModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.Controller;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class ProfilingIT {

	private final static Logger log = LogManager.getLogger(ProfilingIT.class);

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void whenProfilingTwoIterationsOfThreeTotal_expectResultFileToContainJFREvents() throws IOException {
		// use simple scenario configuration for testing
		Config config = this.utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.controller().setLastIteration(2);
		Controller controller = new Controler(config);
		// configure events and instrument modules
		controller.addOverridingModule(new EnableProfilingModule(1, 2));
		controller.addOverridingModule(new FireDefaultProfilingEventsModule());
		// run scenario
		controller.run();
		// check for profile.jfr to exist
		File recording = new File(config.controller().getOutputDirectory(), "profile-1-2.jfr");
		assertThat(recording).exists();
		assertThat(recording).isNotEmpty();
		// read the profile.jfr and expect the events defined by the module and via AOP to be there (but none from TestTraceProfilingAspect)
		assertThat(RecordingFile.readAllEvents(recording.toPath())
			.stream()
			.filter(recordedEvent -> recordedEvent.getEventType().getCategoryNames().contains("MATSim"))
		).isNotEmpty();
		assertThat(RecordingFile.readAllEvents(recording.toPath())
			.stream()
			.filter(recordedEvent -> recordedEvent.getEventType().getName().equals(TraceProfilingEvent.class.getName()))
		).isEmpty();
	}

	@Test
	public void whenRecordingMultipleTimes_expectSeveralRecordingFiles() {
		// use simple scenario configuration for testing
		Config config = this.utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.controller().setLastIteration(3);
		Controller controller = new Controler(config);
		// configure events and instrument modules
		controller.addOverridingModule(new EnableProfilingModule(0));
		controller.addOverridingModule(new EnableProfilingModule(1));
		controller.addOverridingModule(new EnableProfilingModule(2));
		controller.addOverridingModule(new EnableProfilingModule(1, 3, "profile"));
		controller.addOverridingModule(new FireDefaultProfilingEventsModule());
		// run scenario
		controller.run();
		// check for the recordings to exist
		File file1 = new File(config.controller().getOutputDirectory(), "profile-0.jfr");
		File file2 = new File(config.controller().getOutputDirectory(), "profile-1.jfr");
		File file3 = new File(config.controller().getOutputDirectory(), "profile-2.jfr");
		File file4 = new File(config.controller().getOutputDirectory(), "profile.jfr");
		assertThat(file1).exists();
		assertThat(file1).isNotEmpty();
		assertThat(file2).exists();
		assertThat(file2).isNotEmpty();
		assertThat(file3).exists();
		assertThat(file3).isNotEmpty();
		assertThat(file4).exists();
		assertThat(file4).isNotEmpty();
	}

	@Test
	public void whenTraceEnabled_expectResultFileToContainTraceEvents() throws IOException {
		// use simple scenario configuration for testing
		Config config = this.utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.controller().setLastIteration(2);
		Controller controller = new Controler(config);
		// configure events and instrument modules
		controller.addOverridingModule(new EnableProfilingModule(1, 2, "trace-profile", true));
		// run scenario
		controller.run();
		// check for profile.jfr to exist
		File recording = new File(config.controller().getOutputDirectory(), "trace-profile.jfr");
		assertThat(recording).exists();
		assertThat(recording).isNotEmpty();
		// read the profile.jfr and expect the trace events defined via TestTraceProfilingAspect to be there (and none from the module)
		AtomicBoolean hasTraceEvent = new AtomicBoolean(false);
		EventStream eventStream = EventStream.openFile(recording.toPath());
		eventStream.onEvent(TraceProfilingEvent.class.getName(), e -> hasTraceEvent.set(true));
		// assert that no events from FireDefaultProfilingEventsModule are here
		eventStream.onEvent(e -> {
			if (e.getEventType().getName().startsWith(JFRMatsimEvent.class.getPackageName()) &&
				!e.getString("type").startsWith("AOP") // test aspects like ScoringListenerProfilingAspect are active
			) {
				fail();
			}
		});
		try (eventStream) {
			eventStream.start();
		}
		assertTrue(hasTraceEvent.get());
	}
}
