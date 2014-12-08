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

package playground.andreas.aas.modules.cellBasedAccessibility.config;

import org.matsim.core.config.ConfigGroup;

public class MATSim4UrbanSimControlerConfigModule extends ConfigGroup{
	
	public static final String GROUP_NAME = "matsim4urbansimControler";
	
	private boolean zone2ZoneImpedance;
	
	private boolean agentPerformance;
	
	private boolean zoneBasedAccessibility;
	
	private boolean cellBasedAccessibility;
	
	private int cellSizeCellBasedAccessibility;
	
	private boolean isCellBasedAccessibilityNetwork;
	
	private boolean isCellbasedAccessibilityShapeFile;

	private String shapeFileCellBasedAccessibility;
	
	private boolean useCustomBoundingBox;
	
	private double boundingBoxTop;
	
	private double boundingBoxLeft;
	
    private double boundingBoxRight;
    
    private double boundingBoxBottom;
    
    private boolean isColdStart;
    
    private boolean isWarmStart;
    
    private Boolean isHotStart;
    
    private String hotStartTargetLocation;

	public MATSim4UrbanSimControlerConfigModule(String name) {
		super(name);
		this.isColdStart = false;
		this.isWarmStart = false;
		this.isHotStart	 = false;
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
    
    public boolean isCellBasedAccessibilityShapeFile() {
        return this.isCellbasedAccessibilityShapeFile;
    }

    public void setCellBasedAccessibilityShapeFile(boolean value) {
        this.isCellbasedAccessibilityShapeFile = value;
    }

    public boolean isCellBasedAccessibilityNetwork() {
        return this.isCellBasedAccessibilityNetwork;
    }

    public void setCellBasedAccessibilityNetwork(boolean value) {
        this.isCellBasedAccessibilityNetwork = value;
    }

    public int getCellSizeCellBasedAccessibility() {
        return this.cellSizeCellBasedAccessibility;
    }

    public void setCellSizeCellBasedAccessibility(int value) {
        this.cellSizeCellBasedAccessibility = value;
    }

    public String getShapeFileCellBasedAccessibility() {
        return this.shapeFileCellBasedAccessibility;
    }

    public void setShapeFileCellBasedAccessibility(String value) {
        this.shapeFileCellBasedAccessibility = value;
    }

    public boolean isUseCustomBoundingBox() {
        return this.useCustomBoundingBox;
    }

    public void setUseCustomBoundingBox(boolean value) {
        this.useCustomBoundingBox = value;
    }

    public double getBoundingBoxTop() {
        return this.boundingBoxTop;
    }

    public void setBoundingBoxTop(double value) {
        this.boundingBoxTop = value;
    }

    public double getBoundingBoxLeft() {
        return this.boundingBoxLeft;
    }

    public void setBoundingBoxLeft(double value) {
        this.boundingBoxLeft = value;
    }

    public double getBoundingBoxRight() {
        return this.boundingBoxRight;
    }

    public void setBoundingBoxRight(double value) {
        this.boundingBoxRight = value;
    }

    public double getBoundingBoxBottom() {
        return this.boundingBoxBottom;
    }

    public void setBoundingBoxBottom(double value) {
        this.boundingBoxBottom = value;
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
