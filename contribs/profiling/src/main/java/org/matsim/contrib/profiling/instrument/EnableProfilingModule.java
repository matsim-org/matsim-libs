/* ********************************************************************** *
 * project: org.matsim.*
 * EnableProfilingModule.java
 *                                                                        *
 * ********************************************************************** *
 *                                                                        *
 * copyright       : (C) 2025 by the members listed in the COPYING,       *
 *                   LICENSE and WARRANTY file.                           *
 * email           : info at matsim dot org                               *
 *                                                                        *
 * ********************************************************************** *
 *                                                                        *
 *   This program is free software; you can redistribute it and/or modify *
 *   it under the terms of the GNU General Public License as published by *
 *   the Free Software Foundation; either version 2 of the License, or    *
 *   (at your option) any later version.                                  *
 *   See also COPYING, LICENSE and WARRANTY file                          *
 *                                                                        *
 * ********************************************************************** */

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
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Map;
import java.util.Objects;

/**
 * Create a JFR profiling recording for the duration of the configured MATSim iterations
 */
public class EnableProfilingModule extends AbstractModule {

	private static final Logger log = LogManager.getLogger(EnableProfilingModule.class);

	private final ProfilingControlRegistry profilingControlRegistry;


	/**
	 * Create a profile.jfr recording for the given iteration.
	 *
	 * @param startIteration iteration to create a profiler recording for
	 */
	public EnableProfilingModule(int startIteration) {
		this(startIteration, startIteration, "profile-"+startIteration, false);
	}

	/**
	 * Create a profile.jfr recording between given start and end iterations (including).
	 *
	 * @param startIteration iteration before which to start the profiler recording
	 * @param endIteration iteration after which to end the profiler recording
	 */
	public EnableProfilingModule(int startIteration, int endIteration) {
		this(startIteration, endIteration, "profile-"+startIteration+"-"+endIteration, false);
	}

	/**
	 * @param startIteration iteration before which to start the profiler recording
	 * @param endIteration iteration after which to end the profiler recording
	 * @param outputFilename name of the .jfr recording file within the {@link ControllerConfigGroup#getOutputDirectory()}
	 */
	public EnableProfilingModule(int startIteration, int endIteration, String outputFilename) {
		this(startIteration, endIteration, outputFilename, false);
	}

	/**
	 * @param startIteration iteration before which to start the profiler recording
	 * @param endIteration iteration after which to end the profiler recording
	 * @param outputFilename name of the .jfr recording file within the {@link ControllerConfigGroup#getOutputDirectory()}
	 * @param trace Whether to set {@link Trace} for the duration of the recording
	 */
	public EnableProfilingModule(int startIteration, int endIteration, String outputFilename, boolean trace) {
		this.profilingControlRegistry = new ProfilingControlRegistry(startIteration, endIteration, outputFilename, trace);
	}

	@Override
	public void install() {
		addControlerListenerBinding().toInstance(profilingControlRegistry);
		addControlerListenerBinding().toInstance(new ProfilingStartListener(profilingControlRegistry));
		addControlerListenerBinding().toInstance(new ProfilingEndListener(profilingControlRegistry));
	}

	private class ProfilingControlRegistry implements StartupListener {

		private final int startIteration;
		private final int endIteration;
		private final String outputFilename;
		private final boolean trace;
		private Recording recording;

		public ProfilingControlRegistry(int startIteration, int endIteration, String outputFilename, boolean trace) {
			if (startIteration < 0 || endIteration < 0 || startIteration > endIteration) {
				throw new IllegalArgumentException("startIteration must be positive and less than or equal endIteration, but was: " + startIteration + ", endIteration: " + endIteration);
			}

			this.startIteration = startIteration;
			this.endIteration = endIteration;
			this.outputFilename = Objects.requireNonNull(outputFilename);
			this.trace = trace;
		}

		/**
		 * Initialize the jfr {@link Recording} and potentially error out as early as possible if the Recording cannot be instantiated.
		 */
		public void notifyStartup(StartupEvent startupEvent) {
			if (recording != null) {
				throw new IllegalStateException("Already initialized");
			}

			try {
				log.info("Instantiating JFR Recording");
				// todo default to profile for now, but could be configurable
				recording = new Recording(Configuration.getConfiguration("profile"));
				recording.setDestination(Path.of(ConfigUtils.addOrGetModule(getConfig(), ControllerConfigGroup.class).getOutputDirectory(), outputFilename + ".jfr"));
				recording.setName("instrumented-profile-" + startIteration + "-" + endIteration);
			} catch (IOException | ParseException e) {
				log.error("Could not instantiate JFR Recording", e);
				throw new RuntimeException(e);
			}
			// required for multiple recordings
			// toDisk writes to /tmp by default; to use a different directory (e.g. for more space or faster storage)
			// add the java option -XX:FlightRecorderOptions=stackdepth=2048,repository="/fast"
			recording.setToDisk(true); // might be better for longer recordings? memory usage vs disk IO?
			recording.setDumpOnExit(true); // in case the jvm exits prematurely?

			// debug: dump all current JFR recoding settings
			log.info("[PROFILING] Recording settings");
			for (Map.Entry<String,String> setting : recording.getSettings().entrySet()) {
				log.info("{}: {}", setting.getKey(), setting.getValue());
			}

		}
	}

	private record ProfilingStartListener(ProfilingControlRegistry settings) implements IterationStartsListener {

		@Override
		public double priority() {
			// highest priority to start recording before all other startListeners
			return Double.POSITIVE_INFINITY;
		}

		@Override
		public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {
			if (iterationStartsEvent.getIteration() == settings.startIteration) {
				// start recording
				log.info("[PROFILING] Starting Recording at iteration {}", iterationStartsEvent.getIteration());
				if (settings.trace) {
					Trace.enable();
				}
				settings.recording.start();
			}
		}
	}

	private record ProfilingEndListener(ProfilingControlRegistry settings) implements IterationEndsListener {

		@Override
		public double priority() {
			// lowest priority to stop recording after all other endListeners
			return Double.NEGATIVE_INFINITY;
		}

		@Override
		public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
			if (iterationEndsEvent.getIteration() == settings.endIteration) {
				// stop recording - automatically dumped since output path is set and then closed as well
				settings.recording.stop();
				if (settings.trace) {
					// todo if multiple recordings with trace are active, this might turn it off with another still running
					Trace.disable();
				}
			}
			log.info("[PROFILING] {} Current iteration: {}", settings.recording.getState(), iterationEndsEvent.getIteration());
		}
	}

}
