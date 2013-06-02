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

package org.matsim.contrib.matsim4opus.config.modules;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.core.config.Module;

public class UrbanSimParameterConfigModuleV3 extends Module{
	// IMPORTANT: This is just a container for matsim4urbansim config data.  It is _not_ a matsim config group.
	
	public static final String GROUP_NAME = "urbansimParameter";

//	public static final String PROJECT_NAME = "projectName";
//	public static final String IS_PARCEL_MODE = "isParcelMode";
//	public static final String POPULATION_SAMPLING_RATE = "populationSampleRate";
//	public static final String YEAR = "year";
//	public static final String OPUS_HOME_PATH = "opusHome";
//	public static final String OPUS_DATA_PATH = "opusDataPath";
//	public static final String MATSIM4OPUS_PATH = "matsim4Opus";
//	public static final String MATSIM4OPUS_OUTPUT_PATH = "matsim4OpusOutput";
//	public static final String MATSIM4OPUS_TEMP_PATH = "matsim4OpusTemp";
//	public static final String IS_TEST_RUN = "isTestRun";
//	public static final String USING_SHAPEFILE_LOCATION_DISTRIBUTION = "useShapefileLocationDistribution";
//	public static final String URBANSIM_ZONE_SHAPEFILE_LOCATION_DISTRIBUTION = "urbanSimZoneShapefileLocationDistribution";
//	public static final String URBANSIM_ZONE_RADIUS_LOCATION_DISTRIBUTION = "urbanSimZoneRadiusLocationDistribution";
//	public static final String TEST_PARAMETER = "testParameter";
//	public static final String IS_BACKUP_RUN_DATA = "isBackup";
	
	private String projectName;
	private boolean isParcel;
	private double populationSampleRate;
	private int year;
	private String opusHome;
	private String opusDataPath;
	private String matsim4Opus;
	private String matsim4OpusConfig;
	private String matsim4OpusOutput;
	private String matsim4OpusTemp;
	private String matsim4OpusBackup;
	private boolean isTestRun;
	private boolean usingShapefileLocationDistribution;
	private String urbanSimZoneShapefileLocationDistribution;
	private double urbanSimZoneRadiusLocationDistribution;
	private String testParameter;
	private boolean isBackup;
	
	public UrbanSimParameterConfigModuleV3(){
		super(GROUP_NAME);
		// This class feels quite dangerous to me; one can have inconsistent entries between the Map and the typed values. kai, apr'13
		// The way it (hopefully) works now: as long as the config group is not "materialized", one has to use addParam/getValue.
		// Once the class is materialized, one can only use the direct getters/setters.  kai, may'13
	}
	
	@Override
	@Deprecated
	public String getValue(final String key) {
		throw new RuntimeException(" use direct getter; aborting ... " ) ;
	}

	@Override
	@Deprecated
	public void addParam(String param_name, String value) {
		throw new RuntimeException(" use direct setter; aborting ... " ) ;
	}
	
	@Override
	public final Map<String,String> getParams() {
		Map<String,String> map = new LinkedHashMap<String,String>() ;

		// The following is correct but ...
		// ... maybe we don't want this.  Users should set this via the OPUS GUI, not via the external matsim config file.
		// ??  kai, may'13
		
		return map ;
	}
	
	public void setProjectName(String projectName){
		this.projectName = projectName;
	}
	
	public String getProjectName(){
		return this.projectName;
	}

	public void setSpatialUnitFlag(String spatialUnit){
		if(spatialUnit.equalsIgnoreCase("parcel"))
			this.isParcel = true;
		else if(spatialUnit.equalsIgnoreCase("zone"))
			this.isParcel = false;
	}
	
	public boolean isParcelMode(){
		return this.isParcel;
	}
	
	public void setPopulationSampleRate(double sampleRate){
		this.populationSampleRate = sampleRate;
	}
	
	public double getPopulationSampleRate(){
		return this.populationSampleRate;
	}
	
	public void setUsingShapefileLocationDistribution(boolean useShapefileLocationDistribution){
		this.usingShapefileLocationDistribution = useShapefileLocationDistribution;
	}
	
	public boolean isUsingShapefileLocationDistribution(){
		return this.usingShapefileLocationDistribution;
	}
	
	public void setUrbanSimZoneShapefileLocationDistribution(String shapefile){
		this.urbanSimZoneShapefileLocationDistribution = shapefile;
	}
	
	public String getUrbanSimZoneShapefileLocationDistribution() {
		return this.urbanSimZoneShapefileLocationDistribution;
	}
	
	public void setUrbanSimZoneRadiusLocationDistribution(double radius){
		this.urbanSimZoneRadiusLocationDistribution = radius;
	}
	
	public double getUrbanSimZoneRadiusLocationDistribution() {
		return this.urbanSimZoneRadiusLocationDistribution;
	}
	
	public void setYear(int year){
		this.year = year;
	}
	
	public int getYear(){
		return this.year;
	}

	public void setOpusHome(String opusHome){
		this.opusHome = opusHome;
	}
	
	public String getOpusHome(){
		return this.opusHome;
	}
	
	public void setOpusDataPath(String opusDataPath){
		this.opusDataPath = opusDataPath;
	}
	
	public String getOpusDataPath(){
		return this.opusDataPath;
	}
	
	public void setMATSim4Opus(String matsim4Opus){
		this.matsim4Opus = matsim4Opus;
	}
	
	public String getMATSim4Opus(){
		return this.matsim4Opus;
	}
	
	public void setMATSim4OpusConfig(String matsim4OpusConfig){
		this.matsim4OpusConfig = matsim4OpusConfig;
	}
	
	public String getMATSim4OpusConfig(){
		return this.matsim4OpusConfig;
	}
	
	public void setMATSim4OpusOutput(String matsim4OpusOutput){
		this.matsim4OpusOutput = matsim4OpusOutput;
	}
	
	public String getMATSim4OpusOutput(){
		return this.matsim4OpusOutput;
	}
	
	public void setMATSim4OpusTemp(String matsim4OpusTemp){
		this.matsim4OpusTemp = matsim4OpusTemp;
	}
	
	public String getMATSim4OpusTemp(){
		return this.matsim4OpusTemp;
	}
	
	public void setMATSim4OpusBackup(String matsim4OpusBackup){
		this.matsim4OpusBackup = matsim4OpusBackup;
	}
	
	public String getMATSim4OpusBackup(){
		return this.matsim4OpusBackup;
	}
	
	public void setTestRun(boolean isTestRun){
		this.isTestRun = isTestRun;
	}
	
	public boolean isTestRun(){
		return this.isTestRun;
	}
	
	public void setTestParameter(String testParameter){
		this.testParameter = testParameter;
	}
	
	public String getTestParameter(){
		return this.testParameter;
	}
	
	public void setBackup(boolean isBackup){
		this.isBackup = isBackup;
	}
	
	public boolean isBackup(){
		return this.isBackup;
	}
}
