/* ********************************************************************** *
 * project: org.matsim.*
 * ProfilerInstrumentationConfiguration.java
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
