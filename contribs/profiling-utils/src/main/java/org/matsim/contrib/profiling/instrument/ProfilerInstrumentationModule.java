package org.matsim.contrib.profiling.instrument;

import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Map;

/**
 * Start/Stop JFR profiling recordings.
 */
public class ProfilerInstrumentationModule extends AbstractModule {

	private static final Logger log = LogManager.getLogger(ProfilerInstrumentationModule.class);

	private final ProfilerInstrumentationConfiguration config;

	public ProfilerInstrumentationModule(ProfilerInstrumentationConfiguration config) {
		this.config = config;

		int startIteration = config.getStartIteration();
		int endIteration = config.getEndIteration();

		if (startIteration < 0 || endIteration < 0 || startIteration > endIteration) {
			throw new IllegalArgumentException("startIteration must be positive and greater than endIteration, but was: " + startIteration + ", endIteration: " + endIteration);
		}
	}

	@Override // fixme install is called 4 times?!
	public void install() {
		if (this.config.getOutputPath() == null) {
			// cannot be in the constructor as the config is injected later
			this.config.outputPath(Path.of(ConfigUtils.addOrGetModule(getConfig(), ControllerConfigGroup.class).getOutputDirectory(), "profile.jfr"));
		}

		Recording recording;
		// todo more than one recording?
		try {
			log.info("Instantiating JFR Recording");
			recording = new Recording(Configuration.getConfiguration("profile"));
			recording.setDestination(config.getOutputPath());
			recording.setName("instrumented-profile-" + config.getStartIteration() + "-" + config.getEndIteration());
		} catch (IOException | ParseException e) {
			log.error("Could not instantiate JFR Recording", e);
			throw new RuntimeException(e);
		}
		recording.setToDisk(false); // true might be better for longer recordings? memory usage vs disk IO?
		// todo if disk=true; how to set repository setting here?
		recording.setDumpOnExit(true); // in case it exits prematurely?

		// debug: dump all current JFR recoding settings
		log.info("[PROFILING] Recording settings");
		for (Map.Entry<String,String> setting : recording.getSettings().entrySet()) {
			log.info("{}: {}", setting.getKey(), setting.getValue());
		}

		addControlerListenerBinding().toInstance(new ProfilingStartListener(recording, config.getStartIteration()));
		addControlerListenerBinding().toInstance(new ProfilingEndListener(recording, config.getEndIteration()));
	}


	static class ProfilingStartListener implements IterationStartsListener {
		private final Recording recording;
		private final int startIteration;

		ProfilingStartListener(Recording recording, int startIteration) {
			this.recording = recording;
			this.startIteration = startIteration;
		}

		@Override
		public double priority() {
			// highest priority to start recording before all other startListeners
			return Double.MAX_VALUE;
		}

		@Override
		public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {
			if (iterationStartsEvent.getIteration() == startIteration) {
				// start recording
				log.info("[PROFILING] Starting Recording at iteration {}", iterationStartsEvent.getIteration());
				recording.start();
			}
		}
	}

	static class ProfilingEndListener implements IterationEndsListener {
		private final Recording recording;
		private final int endIteration;

		ProfilingEndListener(Recording recording, int startIteration) {
			this.recording = recording;
			this.endIteration = startIteration;
		}

		@Override
		public double priority() {
			// lowest priority to stop recording after all other endListeners
			return Double.MIN_VALUE;
		}

		@Override
		public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
			if (iterationEndsEvent.getIteration() == endIteration) {
				// stop recording - automatically dumped since output path is set and then closed as well
				recording.stop();
			}
			log.info("[PROFILING] {} Current iteration: {}", recording.getState(), iterationEndsEvent.getIteration());
		}
	}

}
