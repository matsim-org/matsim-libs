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

package org.matsim.contrib.ev.temperature;/*
 * created by jbischoff, 15.08.2018
 */

import java.net.URL;
import java.util.Map;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

public final class TemperatureChangeConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "temperature";

	public static final String TEMPERATURE_CHANGE_FILE = "temperatureChangeFile";
	public static final String TEMPERATURE_CHANGE_FILE_EXP = "Filename containing temperature changes. Expects CSV file with time;linkId;newTemperature";

	public static final String DELIMITER = "delimiter";
	public static final String DELIMITER_EXP = "Delimiter. Default `;`";

	@NotBlank
	private String delimiter = ";";

	@NotNull
	private String temperatureChangeFile;

	public TemperatureChangeConfigGroup() {
		super(GROUP_NAME);
	}

	/**
	 * @return -- {@value #DELIMITER_EXP}
	 */
	@StringGetter(DELIMITER)
	public String getDelimiter() {
		return delimiter;
	}

	/**
	 * @param delimiter -- {@value #DELIMITER_EXP}
	 */
	@StringSetter(DELIMITER)
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	/**
	 * @return -- {@value #TEMPERATURE_CHANGE_FILE_EXP}
	 */
	@StringGetter(TEMPERATURE_CHANGE_FILE)
	public String getTemperatureChangeFile() {
		return temperatureChangeFile;
	}

	/**
	 * @return -- {@value #TEMPERATURE_CHANGE_FILE_EXP}
	 */
	public URL getTemperatureFileURL(URL context) {
		return ConfigGroup.getInputFileURL(context, this.temperatureChangeFile);
	}

	/**
	 * @param temperatureChangeFile -- {@value #TEMPERATURE_CHANGE_FILE_EXP}
	 */
	@StringSetter(TEMPERATURE_CHANGE_FILE)
	public void setTemperatureChangeFile(String temperatureChangeFile) {
		this.temperatureChangeFile = temperatureChangeFile;
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(DELIMITER, DELIMITER_EXP);
		map.put(TEMPERATURE_CHANGE_FILE, TEMPERATURE_CHANGE_FILE_EXP);
		return map;
	}
}
