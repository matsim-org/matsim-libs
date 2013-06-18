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

import org.matsim.core.config.Module;

/**
 * @author thomas
 */
public class M4UControlerConfigModuleV3 extends Module{
	// IMPORTANT: This is just a container for matsim4urbansim config data.  It is _not_ a matsim config group.

	public static final String GROUP_NAME = "matsim4urbansimControler";
	
	private boolean zone2ZoneImpedance;
	private boolean agentPerformance;
	private boolean zoneBasedAccessibility;
	private boolean cellBasedAccessibility;
    private boolean isColdStart;
    private boolean isWarmStart;
    private Boolean isHotStart;
    private String hotStartTargetLocation;
    
	public M4UControlerConfigModuleV3() {
		super(GROUP_NAME);
		this.isColdStart = false;
		this.isWarmStart = false;
		this.isHotStart	 = false;
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
   public boolean isZone2ZoneImpedance() {
        return this.zone2ZoneImpedance;
    }

    public void setZone2ZoneImpedance(boolean value) {
        this.zone2ZoneImpedance = value;
    }

    public boolean isAgentPerformance() {
        return this.agentPerformance;
    }

    public void setAgentPerformance(boolean value) {
        this.agentPerformance = value;
    }

    public boolean isZoneBasedAccessibility() {
        return this.zoneBasedAccessibility;
    }

    public void setZoneBasedAccessibility(boolean value) {
        this.zoneBasedAccessibility = value;
    }

    public boolean isCellBasedAccessibility() {
        return this.cellBasedAccessibility;
    }

    public void setCellBasedAccessibility(boolean value) {
        this.cellBasedAccessibility = value;
    }
    
    public boolean isColdStart(){
    	return this.isColdStart;
    }
    
    public void setColdStart(boolean value){
    	this.isColdStart = value;
    }

    public boolean isWarmStart(){
    	return this.isWarmStart;
    }
    
    public void setWarmStart(boolean value){
    	this.isWarmStart = value;
    }
    
    public boolean isHotStart(){
    	return this.isHotStart;
    }
    
    public void setHotStart(boolean value){
    	this.isHotStart = value;
    }
    
    public String getHotStartTargetLocation(){
    	return this.hotStartTargetLocation;
    }
    
    public void setHotStartTargetLocation(String value){
    	this.hotStartTargetLocation = value;
    }
}
