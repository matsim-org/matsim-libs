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

package org.matsim.contrib.accessibility;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.core.config.ReflectiveConfigGroup;

public final class AccessibilityConfigGroup extends ReflectiveConfigGroup{
	// yyyy todo: change in similar way as with other modes ("_mode") 
	
	private static final String USING_CUSTOM_BOUNDING_BOX = "usingCustomBoundingBox";

	private static final String BOUNDING_BOX_BOTTOM = "boundingBoxBottom";

	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger( AccessibilityConfigGroup.class ) ;

	public static final String GROUP_NAME = "accessibility";
	
	private static final String USING_RAW_SUMS_WITHOUT_LN = "usingRawSumsWithoutLn";
	private static final String ACCESSIBILITY_DESTINATION_SAMPLING_RATE = "accessibilityDestinationSamplingRate";
	// ===
	private Double accessibilityDestinationSamplingRate;
	private Boolean usingRawSumsWithoutLn = false ;

	private double boundingBoxTop;
	private double boundingBoxLeft;
    private double boundingBoxRight;
    private double boundingBoxBottom;
	
	private Integer cellSizeCellBasedAccessibility;
	private String shapeFileCellBasedAccessibility;
	
	private static final String AREA_OF_ACC_COMP = "areaOfAccessibilityComputation" ; 
	public static enum AreaOfAccesssibilityComputation{ fromNetwork, fromBoundingBox, fromShapeFile } 
	private AreaOfAccesssibilityComputation areaOfAccessibilityComputation = AreaOfAccesssibilityComputation.fromNetwork ;
	private Set<Modes4Accessibility> isComputingMode = EnumSet.noneOf(Modes4Accessibility.class);

	
	// ===

	public static final String TIME_OF_DAY = "timeOfDay";
	private Double timeOfDay = 8.*3600 ;

	public AccessibilityConfigGroup() {
		super(GROUP_NAME);
		isComputingMode.add(Modes4Accessibility.freeSpeed);
	}
	
	@Override
	public Map<String,String> getComments() {
		Map<String,String> map = new TreeMap<String,String>() ;
		
		map.put(TIME_OF_DAY, "time of day at which trips for accessibility computations are assumed to start") ;
		
		map.put(ACCESSIBILITY_DESTINATION_SAMPLING_RATE, "if only a sample of destinations should be used " +
				"(reduces accuracy -- not recommended except when necessary for computational speed reasons)" ) ;
		
		map.put(USING_RAW_SUMS_WITHOUT_LN, "econometric accessibility usually returns the logsum. " +
				"Set to true if you just want the sum (without the ln)") ;
		
		map.put(USING_CUSTOM_BOUNDING_BOX, "true if custom bounding box should be used for accessibility computation (otherwise e.g. extent of network will be used)") ;
		map.put(BOUNDING_BOX_BOTTOM,"custom bounding box parameters for accessibility computation (if enabled)") ;
		
		StringBuilder stb = new StringBuilder() ;
		for ( AreaOfAccesssibilityComputation val : AreaOfAccesssibilityComputation.values() ) {
			stb.append(val.toString() ) ;
			stb.append( " " ) ;
		}
		map.put(AREA_OF_ACC_COMP, "method to determine the area for which the accessibility will be computed; possible values: " + stb ) ;
		
		return map ;
	}
	
	public void setComputingAccessibilityForMode(Modes4Accessibility mode, boolean val) {
		if (val) {
			this.isComputingMode.add(mode);
		} else {
			this.isComputingMode.remove(mode);
		}
	}

	public Set<Modes4Accessibility> getIsComputingMode() {
		return isComputingMode;
	}

	
	// NOTE: It seems ok to have the string constants immediately here since having them separately really does not help
	// keeping the code compact
	
	@StringGetter("cellSizeForCellBasedAccessibility") 
    public Integer getCellSizeCellBasedAccessibility() {
		return this.cellSizeCellBasedAccessibility;
    }
	@StringSetter("cellSizeForCellBasedAccessibility")  // size in meters (whatever that is since the coord system does not know about meters)
    public void setCellSizeCellBasedAccessibility(int value) {
		if (value <= 0) {
			throw new IllegalArgumentException("Cell size must be greater than zero.");
		}
		if (value < 100) {
			log.warn("Cell size is set to " + value + ". This is a comparatively small" +
					" value, which may lead to computational problems.");
		}
		this.cellSizeCellBasedAccessibility = value;
    }
	@StringGetter("extentOfAccessibilityComputationShapeFile")
    public String getShapeFileCellBasedAccessibility() {
        return this.shapeFileCellBasedAccessibility;
    }
	@StringSetter("extentOfAccessibilityComputationShapeFile")
    public void setShapeFileCellBasedAccessibility(String value) {
        this.shapeFileCellBasedAccessibility = value;
    }

	@StringGetter(TIME_OF_DAY)
	public Double getTimeOfDay() {
		return this.timeOfDay ;
	}
	@StringSetter(TIME_OF_DAY)
	public void setTimeOfDay( Double val ) {
		this.timeOfDay = val ;
	}
	
	@StringGetter(ACCESSIBILITY_DESTINATION_SAMPLING_RATE)
	public Double getAccessibilityDestinationSamplingRate(){
		return this.accessibilityDestinationSamplingRate;
	}
	@StringSetter(ACCESSIBILITY_DESTINATION_SAMPLING_RATE)
	public void setAccessibilityDestinationSamplingRate(Double sampleRate){
		this.accessibilityDestinationSamplingRate = sampleRate;
	}
    @StringGetter(USING_RAW_SUMS_WITHOUT_LN)
    public Boolean isUsingRawSumsWithoutLn() {
        return usingRawSumsWithoutLn;
    }
    @StringSetter(USING_RAW_SUMS_WITHOUT_LN)
    public void setUsingRawSumsWithoutLn(Boolean value) {
        this.usingRawSumsWithoutLn = value;
    }
    @StringGetter("boundingBoxTop")
    public double getBoundingBoxTop() {
        return this.boundingBoxTop;
    }
    @StringSetter("boundingBoxTop")
    public void setBoundingBoxTop(double value) {
        this.boundingBoxTop = value;
    }
    @StringGetter("boundingBoxLeft")
    public double getBoundingBoxLeft() {
        return this.boundingBoxLeft;
    }
    @StringSetter("boundingBoxLeft")
    public void setBoundingBoxLeft(double value) {
        this.boundingBoxLeft = value;
    }
    @StringGetter("boundingBoxRight")
    public double getBoundingBoxRight() {
        return this.boundingBoxRight;
    }
    @StringSetter("boundingBoxRight")
    public void setBoundingBoxRight(double value) {
        this.boundingBoxRight = value;
    }
    @StringGetter(BOUNDING_BOX_BOTTOM)
    public double getBoundingBoxBottom() {
        return this.boundingBoxBottom;
    }
    @StringSetter(BOUNDING_BOX_BOTTOM)
    public void setBoundingBoxBottom(double value) {
        this.boundingBoxBottom = value;
    }

    @StringGetter(AREA_OF_ACC_COMP)
	public String getAreaOfAccessibilityComputation() {
		return areaOfAccessibilityComputation.toString();
	}

    @StringSetter(AREA_OF_ACC_COMP)
	public void setAreaOfAccessibilityComputation(
			String areaOfAccessibilityComputation) {
    	boolean problem = true ;
    	for ( AreaOfAccesssibilityComputation var : AreaOfAccesssibilityComputation.values() ) {
    		if ( var.toString().equals(areaOfAccessibilityComputation) ) {
    			this.areaOfAccessibilityComputation = var ;
    			problem = false ;
    		}
    	}
    	if ( problem ){
    		throw new RuntimeException("string typo error") ;
    	}
	}
    
}
