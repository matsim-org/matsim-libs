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

import org.matsim.core.config.groups.ControllerConfigGroup;

import java.util.Objects;

public class ProfilerInstrumentationConfiguration {

	/**
	 * @return a default configuration starting at iteration 1, ending at iteration 2, and creating the recording in
	 * 		   the configured output directory as "profile.jfr".
	 */
	public static ProfilerInstrumentationConfiguration defaultConfiguration() {
		return new ProfilerInstrumentationConfiguration();
	}

	private String outputFilename = "profile";
	private int startIteration = 1;
	private int endIteration = 2;

	/**
	 * @return name of the output file (without extension)
	 */
	public String getOutputFilename() {
		return outputFilename;
	}

	public int getStartIteration() {
		return startIteration;
	}

	public int getEndIteration() {
		return endIteration;
	}

	/**
	 * The file will be created in {@link ControllerConfigGroup#getOutputDirectory()}
	 * with this given name and the {@code .jfr} extension.
	 *
	 * @param outputFilename name of the .jfr recording file
	 * @return this for further configuring in builder style
	 */
	public ProfilerInstrumentationConfiguration outputFilename(String outputFilename) {
		this.outputFilename = Objects.requireNonNull(outputFilename);
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
