package org.matsim.contrib.profiling.instrument;

import java.nio.file.Path;

public class ProfilerInstrumentationConfiguration {

	public static ProfilerInstrumentationConfiguration defaultConfiguration() {
		return new ProfilerInstrumentationConfiguration();
	}

	/**
	 * Path outputPath = Path.of(ConfigUtils.addOrGetModule(config, ControllerConfigGroup.class).getOutputDirectory(), "profile.jfr");
	 */
	private Path outputPath = null;
	private int startIteration = 1;
	private int endIteration = 2;

	public Path getOutputPath() {
		return outputPath;
	}

	public int getStartIteration() {
		return startIteration;
	}

	public int getEndIteration() {
		return endIteration;
	}

	public ProfilerInstrumentationConfiguration outputPath(Path outputPath) {
		this.outputPath = outputPath;
		return this;
	}

	private ProfilerInstrumentationConfiguration startIteration(int startIteration) {
		this.startIteration = startIteration;
		return this;
	}

	private ProfilerInstrumentationConfiguration endIteration(int endIteration) {
		this.endIteration = endIteration;
		return this;
	}
}
