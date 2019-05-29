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

package org.matsim.contrib.taxi.optimizer.zonal;

import java.util.Map;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;

import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;

public class ZonalTaxiOptimizerParams extends RuleBasedTaxiOptimizerParams {
	public static final String SET_NAME = "ZonalTaxiOptimizer";

	public static final String ZONES_XML_FILE = "zonesXmlFile";
	static final String ZONES_XML_FILE_EXP = "An XML file specifying the zonal system";
	@NotBlank
	private String zonesXmlFile;

	public static final String ZONES_SHP_FILE = "zonesShpFile";
	static final String ZONES_SHP_FILE_EXP = "A shape file specifying the geometries of zones";
	@NotBlank
	private String zonesShpFile;

	public static final String EXPANSION_DISTANCE = "expansionDistance";
	static final String EXPANSION_DISTANCE_EXP = "";
	@PositiveOrZero
	private double expansionDistance = 0;

	public ZonalTaxiOptimizerParams() {
		super(SET_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(ZONES_XML_FILE, ZONES_XML_FILE_EXP);
		map.put(ZONES_SHP_FILE, ZONES_SHP_FILE_EXP);
		map.put(EXPANSION_DISTANCE, EXPANSION_DISTANCE_EXP);
		return map;
	}

	/**
	 * @return {@value #ZONES_XML_FILE_EXP}
	 */
	@StringGetter(ZONES_XML_FILE)
	public String getZonesXmlFile() {
		return zonesXmlFile;
	}

	/**
	 * @param zonesXmlFile {@value #ZONES_XML_FILE_EXP}
	 */
	@StringSetter(ZONES_XML_FILE)
	public void setZonesXmlFile(String zonesXmlFile) {
		this.zonesXmlFile = zonesXmlFile;
	}

	/**
	 * @return {@value #ZONES_SHP_FILE_EXP}
	 */
	@StringGetter(ZONES_SHP_FILE)
	public String getZonesShpFile() {
		return zonesShpFile;
	}

	/**
	 * @param zonesShpFile {@value #ZONES_SHP_FILE_EXP}
	 */
	@StringSetter(ZONES_SHP_FILE)
	public void setZonesShpFile(String zonesShpFile) {
		this.zonesShpFile = zonesShpFile;
	}

	/**
	 * @return {@value #EXPANSION_DISTANCE_EXP}
	 */
	@StringGetter(EXPANSION_DISTANCE)
	public double getExpansionDistance() {
		return expansionDistance;
	}

	/**
	 * @param expansionDistance {@value #EXPANSION_DISTANCE_EXP}
	 */
	@StringSetter(EXPANSION_DISTANCE)
	public void setExpansionDistance(double expansionDistance) {
		this.expansionDistance = expansionDistance;
	}
}
