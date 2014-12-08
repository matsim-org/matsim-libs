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

package org.matsim.contrib.matsim4urbansim.config.modules;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.core.config.ConfigGroup;

public class UrbanSimParameterConfigModuleV3 extends ConfigGroup{
	// IMPORTANT: This is just a container for matsim4urbansim config data.  It is _not_ a matsim config group.
	
	public static final String GROUP_NAME = "urbansimParameter";

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
	private String customParameter;
	private boolean usingZone2ZoneImpedance;
	private boolean usingAgentPerformance;
	private boolean usingZoneBasedAccessibility;
	private boolean usingGridBasedAccessibility;
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

	public void setCustomParameter(String testParameter){
		this.customParameter = testParameter;
	}
	
	public String getCustomParameter(){
		return this.customParameter;
	}
	
	public void setUsingZone2ZoneImpedance(boolean value){
		this.usingZone2ZoneImpedance = value;
	}
	
	public boolean usingZone2ZoneImpedance(){
		return this.usingZone2ZoneImpedance;
	}
	
	public void setUsingAgentPerformance(boolean value){
		this.usingAgentPerformance = value;
	}
	
	public boolean usingAgentPerformance(){
		return this.usingAgentPerformance;
	}
	
	public void setUsingZoneBasedAccessibility(boolean value){
		this.usingZoneBasedAccessibility = value;
	}
	
	public boolean usingZoneBasedAccessibility(){
		return this.usingZoneBasedAccessibility;
	}
	
	public void setUsingGridBasedAccessibility(boolean value){
		this.usingGridBasedAccessibility = value;
	}
	
	public boolean usingGridBasedAccessibility(){
		return this.usingGridBasedAccessibility;
	}
	
	public void setBackup(boolean isBackup){
		this.isBackup = isBackup;
	}
	
	public boolean isBackup(){
		return this.isBackup;
	}
}
