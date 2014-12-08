/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,     *
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

/**
 * 
 */
package org.matsim.contrib.matsim4urbansim.config.modules;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.core.config.ConfigGroup;

/**
 * @author thomas
 */
public class M4UControlerConfigModuleV3 extends ConfigGroup{
	// IMPORTANT: This is just a container for matsim4urbansim config data.  It is _not_ a matsim config group.

	public static final String GROUP_NAME = "matsim4urbansimControler";
	
	private boolean usingShapefileLocationDistribution;
	private String urbansimZoneRandomLocationDistributionShapeFile;
	private double urbansimZoneRandomLocationDistributionRadius;
	private boolean usingWarmStart;
    private boolean usingHotStart;
    private String warmStartPlansFileLocation;
    private String hotStartPlansFileLocation;
    
	public M4UControlerConfigModuleV3() {
		super(GROUP_NAME);
		this.usingHotStart	 = false;
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

		// Maybe we don't want this?  Users should set this via the OPUS GUI, not via the external matsim config file.
		// ??  kai, may'13

		return map ;
	}

	public void setUsingShapefileLocationDistribution(boolean useShapefileLocationDistribution){
		this.usingShapefileLocationDistribution = useShapefileLocationDistribution;
	}
	
	public boolean usingShapefileLocationDistribution(){
		return this.usingShapefileLocationDistribution;
	}
	
	public void setUrbansimZoneRandomLocationDistributionShapeFile(String shapefile){
		this.urbansimZoneRandomLocationDistributionShapeFile = shapefile;
	}
	
	public String getUrbansimZoneRandomLocationDistributionShapeFile() {
		return this.urbansimZoneRandomLocationDistributionShapeFile;
	}
	
	public void setUrbansimZoneRandomLocationDistributionRadius(double radius){
		this.urbansimZoneRandomLocationDistributionRadius = radius;
	}
	
	public double getUrbanSimZoneRadiusLocationDistribution() {
		return this.urbansimZoneRandomLocationDistributionRadius;
	}

    public boolean usingWarmStart(){
    	return this.usingWarmStart;
    }
    
    public void setWarmStart(boolean value){
    	this.usingWarmStart = value;
    }
	
    public void setWarmStartPlansLocation(String path){
    	this.warmStartPlansFileLocation = path;
    }
    
    public String getWarmStartPlansFileLocation(){
    	return this.warmStartPlansFileLocation;
    }
    
    public boolean usingHotStart(){
    	return this.usingHotStart;
    }
    
    public void setHotStart(boolean value){
    	this.usingHotStart = value;
    }
    
    public String getHotStartPlansFileLocation(){
    	return this.hotStartPlansFileLocation;
    }
    
    public void setHotStartPlansFileLocation(String path){
    	this.hotStartPlansFileLocation = path;
    }
}
