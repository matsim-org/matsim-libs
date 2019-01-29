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

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.net.URL;
import java.util.Map;

public class TemperatureChangeConfigGroup extends ReflectiveConfigGroup {

    public static final String GROUP_NAME = "temperature";
    public static final String TEMP_CHANGE_FILE = "temperatureChangeFile";
    public static final String TEMP_CHANGE_FILE_EXP = "Filename containing temperature changes. Expects CSV file with time;linkId;newTemperature";

    public static final String DELIMITER_TAG = "delimiterString";
    public static final String DELIMITER_EXP = "Delimiter. Default `;`";

    String delimiter = ";";
    String tempFile;


    public TemperatureChangeConfigGroup() {
        super(GROUP_NAME);
    }

    /**
     * @return -- {@value #DELIMITER_EXP}
     */
    @StringGetter(DELIMITER_TAG)
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * @param delimiter -- {@value #DELIMITER_EXP}
     */
    @StringSetter(DELIMITER_TAG)
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * @return -- {@value #TEMP_CHANGE_FILE_EXP}
     */
    @StringGetter(TEMP_CHANGE_FILE)
    public String getTempFile() {
        return tempFile;
    }

    /**
     * @return -- {@value #TEMP_CHANGE_FILE_EXP}
     */
    public URL getTemperatureFileURL(URL context) {
        return ConfigGroup.getInputFileURL(context, this.tempFile);
    }


    /**
     * @param tempFile -- {@value #TEMP_CHANGE_FILE_EXP}
     */
    @StringSetter(TEMP_CHANGE_FILE)
    public void setTempFile(String tempFile) {
        this.tempFile = tempFile;
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(DELIMITER_TAG, DELIMITER_EXP);
        map.put(TEMP_CHANGE_FILE, TEMP_CHANGE_FILE_EXP);
        return map;
    }
}
