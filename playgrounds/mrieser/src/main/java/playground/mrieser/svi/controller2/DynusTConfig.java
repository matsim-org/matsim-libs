/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.mrieser.svi.controller2;

import playground.mrieser.svi.data.ActivityToZoneMapping;
import playground.mrieser.svi.data.ZoneIdToIndexMapping;
import playground.mrieser.svi.data.Zones;

/**
 * @author mrieser
 */
public class DynusTConfig {

	private String dynusTDirectory = null;
	private String modelDirectory = null;
	private String outputDirectory = null;
	private String zonesShapeFile = null;
	private String zoneIdToIndexMappingFile = null;
	private double demandFactor = 1.0;
	private String zoneIdAttributeName = "id";
	private int timeBinSize_min = 10;

	private final Zones zones = new Zones();
	private final ActivityToZoneMapping actToZoneMapping = new ActivityToZoneMapping();
	private final ZoneIdToIndexMapping zoneIdToIndexMapping = new ZoneIdToIndexMapping();

	public String getDynusTDirectory() {
		return this.dynusTDirectory;
	}

	public void setDynusTDirectory(final String dynusTDirectory) {
		this.dynusTDirectory = dynusTDirectory;
	}

	public String getModelDirectory() {
		return this.modelDirectory;
	}

	public void setModelDirectory(final String modelDirectory) {
		this.modelDirectory = modelDirectory;
	}

	public String getOutputDirectory() {
		return this.outputDirectory;
	}

	public void setOutputDirectory(final String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public String getZonesShapeFile() {
		return this.zonesShapeFile;
	}

	public void setZonesShapeFile(final String zonesShapeFile) {
		this.zonesShapeFile = zonesShapeFile;
	}

	public String getZoneIdToIndexMappingFile() {
		return this.zoneIdToIndexMappingFile;
	}

	public void setZoneIdToIndexMappingFile(final String zoneIdToIndexMappingFile) {
		this.zoneIdToIndexMappingFile = zoneIdToIndexMappingFile;
	}

	public Zones getZones() {
		return this.zones;
	}

	public ActivityToZoneMapping getActToZoneMapping() {
		return this.actToZoneMapping;
	}

	public ZoneIdToIndexMapping getZoneIdToIndexMapping() {
		return this.zoneIdToIndexMapping;
	}

	public void setDemandFactor(final double demandFactor) {
		this.demandFactor = demandFactor;
	}

	public double getDemandFactor() {
		return this.demandFactor;
	}

	public String getZoneIdAttributeName() {
		return zoneIdAttributeName;
	}
	
	public void setZoneIdAttributeName(String zoneIdAttributeName) {
		this.zoneIdAttributeName = zoneIdAttributeName;
	}

	public int getTimeBinSize_min() {
		return timeBinSize_min;
	}
	
	public void setTimeBinSize_min(int timeBinSize_min) {
		this.timeBinSize_min = timeBinSize_min;
	}
	
}
