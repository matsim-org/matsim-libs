/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.temperature;
/*
 * created by jbischoff, 15.08.2018
 */

import org.matsim.core.config.ReflectiveConfigGroup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class TemperatureChangeConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "temperature";

	@Parameter
	@Comment("Filename containing temperature changes. Expects CSV file with time;linkId;newTemperature")
	@NotNull
	public String temperatureChangeFile;

	@Parameter
	@Comment("Delimiter. Default `;`")
	@NotBlank
	public String delimiter = ";";

	public TemperatureChangeConfigGroup() {
		super(GROUP_NAME);
	}
}
