/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcRouteConfigGroup
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.core.config.groups;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.config.Module;
import org.matsim.core.utils.collections.Tuple;


/**
 * Config Module for PlansCalcRoute class.
 * Here you can specify the scale factors of freespeed travel time which are used 
 * as travel time for not microsimulated modes.
 * 
 * @author dgrether
 *
 */
public class PlansCalcRouteConfigGroup extends Module {
	
	public static final String GROUP_NAME = "planscalcroute";
	
	private static final String PT_SPEED_FACTOR = "ptSpeedFactor";
	private static final String WALK_SPEED_FACTOR = "walkSpeedFactor";
	private static final String BIKE_SPEED_FACTOR = "bikeSpeedFactor";
	private static final String UNDEFINED_MODE_SPEED_FACTOR = "undefinedModeSpeedFactor";
	
	/**
	 * Map containing param name, getter + setter
	 */
	private Map<String, Tuple<Method, Method>> paramMethods = new HashMap<String, Tuple<Method, Method>>();
	
	private double ptSpeedFactor = 2.0;
	
	private double walkSpeedFactor = 3.0 / 3.6; // 3.0 km/h --> m/s
	
	private double bikeSpeedFactor = 15.0 / 3.6; // 15.0 km/h --> m/s
	
	private double undefinedModeSpeedFactor = 50.0 / 3.6; // 50.0 km/h --> m/s

	public PlansCalcRouteConfigGroup() {
		super(GROUP_NAME);
		try {
			paramMethods.put(PT_SPEED_FACTOR, new Tuple<Method, Method>(
					PlansCalcRouteConfigGroup.class.getDeclaredMethod("getPtSpeedFactor", null),
					PlansCalcRouteConfigGroup.class.getDeclaredMethod("setPtSpeedFactor", new Class[] {double.class})));
			
			paramMethods.put(WALK_SPEED_FACTOR, new Tuple<Method, Method>(
					PlansCalcRouteConfigGroup.class.getDeclaredMethod("getWalkSpeedFactor", null),
					PlansCalcRouteConfigGroup.class.getDeclaredMethod("setWalkSpeedFactor", new Class[] {double.class})));
			
			paramMethods.put(BIKE_SPEED_FACTOR, new Tuple<Method, Method>(
					PlansCalcRouteConfigGroup.class.getDeclaredMethod("getBikeSpeedFactor", null),
					PlansCalcRouteConfigGroup.class.getDeclaredMethod("setBikeSpeedFactor", new Class[] {double.class})));
			
			paramMethods.put(UNDEFINED_MODE_SPEED_FACTOR, new Tuple<Method, Method>(
					PlansCalcRouteConfigGroup.class.getDeclaredMethod("getUndefinedModeSpeedFactor", null),
					PlansCalcRouteConfigGroup.class.getDeclaredMethod("setUndefinedModeSpeedFactor", new Class[] {double.class})));
			
			
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		
	}
	
	
	@Override
	public String getValue(final String key) {
		try {
		if (this.paramMethods.containsKey(key))
			return ((Double)this.paramMethods.get(key).getFirst().invoke(this, null)).toString();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		throw new IllegalArgumentException(key);		
	}

	@Override
	public void addParam(final String key, final String value) {
		if (this.paramMethods.containsKey(key)){
			try {
				this.paramMethods.get(key).getSecond().invoke(this, new Object[] {Double.valueOf(value)});
			} catch (NumberFormatException e) {
				e.printStackTrace();
				throw new IllegalArgumentException(key);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		else {
			throw new IllegalArgumentException(key);
		}
		
	}

	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		for (String s : this.paramMethods.keySet()){
			map.put(s, getValue(s));
		}
		return map;
	}
	
	

	public double getPtSpeedFactor() {
		return this.ptSpeedFactor;
	}

	
	public double getWalkSpeedFactor() {
		return walkSpeedFactor;
	}

	
	public void setWalkSpeedFactor(double walkSpeedFactor) {
		this.walkSpeedFactor = walkSpeedFactor;
	}

	
	public double getBikeSpeedFactor() {
		return bikeSpeedFactor;
	}

	
	public void setBikeSpeedFactor(double bikeSpeedFactor) {
		this.bikeSpeedFactor = bikeSpeedFactor;
	}

	
	public double getUndefinedModeSpeedFactor() {
		return undefinedModeSpeedFactor;
	}

	
	public void setUndefinedModeSpeedFactor(double undefinedModeSpeedFactor) {
		this.undefinedModeSpeedFactor = undefinedModeSpeedFactor;
	}

	
	public void setPtSpeedFactor(double ptSpeedFactor) {
		this.ptSpeedFactor = ptSpeedFactor;
	}

	
	
	
}
