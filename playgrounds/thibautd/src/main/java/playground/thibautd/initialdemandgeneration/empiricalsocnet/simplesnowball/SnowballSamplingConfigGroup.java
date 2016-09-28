/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.thibautd.initialdemandgeneration.empiricalsocnet.simplesnowball;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class SnowballSamplingConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "snowballBasedSampling";

	private String outputDirectory = null;
	private String inputCliquesCsv = null;

	public SnowballSamplingConfigGroup( ) {
		super( GROUP_NAME );
	}

	@StringGetter("outputDirectory")
	public String getOutputDirectory() {
		return outputDirectory;
	}

	@StringSetter("outputDirectory")
	public void setOutputDirectory( final String outputDirectory ) {
		this.outputDirectory = outputDirectory;
	}

	@StringGetter("inputCliquesCsv")
	public String getInputCliquesCsv() {
		return inputCliquesCsv;
	}

	@StringSetter("inputCliquesCsv")
	public void setInputCliquesCsv( final String inputCliquesCsv ) {
		this.inputCliquesCsv = inputCliquesCsv;
	}
}
