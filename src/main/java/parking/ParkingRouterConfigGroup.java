/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
  
package parking;

import java.net.URL;
import java.util.Map;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

public class ParkingRouterConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "parkingRouter";

	public static final String SHAPE = "inputShapeFile";
	static final String SHAPE_EXP = "Path to Shape File that includes parking zones. Links outside the shape will not have parking capacity constraints.";

	public static final String SHAPE_KEY = "key";
	static final String SHAPE_KEY_EXP = "Key in shape file that includes zone ID. Default == NO";
	
	public static final String PARK_CAP_KEY = "parkCapacityCalculation";
	static final String SPARK_CAP_KEY_EXP = "Sets how parking capacities are set for parking. Options: lengthbased (default; calculates capacity based on Link length) "
			+ "or useFromNetwork (expects a parkingCapacity network attribute (double) for each link)";
	
	private LinkParkingCapacityCalculationMethod capacityCalculationMethod = LinkParkingCapacityCalculationMethod.lengthbased;
	
	private String shapeFile = null;
	private String shape_key = "NO";

	public enum LinkParkingCapacityCalculationMethod {lengthbased, useFromNetwork};
	
	@SuppressWarnings("deprecation")
	public static ParkingRouterConfigGroup get(Config config) {
		return (ParkingRouterConfigGroup) config.getModule(GROUP_NAME);
	}
	
	public ParkingRouterConfigGroup() {
		super(GROUP_NAME);
	}
	
	public ParkingRouterConfigGroup(String name) {
		super(name);
	
	}
	@StringGetter(SHAPE)
	public String getShapeFile() {
		return shapeFile;
	}
	
	public URL getShapeFileUrl(URL context) {
		return ConfigGroup.getInputFileURL(context, this.shapeFile);
	}
	
	@StringSetter(SHAPE)
	public void setShapeFile(String shapeFile) {
		this.shapeFile = shapeFile;
	}
	
	@StringGetter(SHAPE_KEY)
	public String getShapeKey() {
		return shape_key;
	}
	
	@StringSetter(SHAPE_KEY)
	public void setShape_key(String shape_key) {
		this.shape_key = shape_key;
	}
	
	@StringGetter(PARK_CAP_KEY)
	public LinkParkingCapacityCalculationMethod getCapacityCalculationMethod() {
		return capacityCalculationMethod;
	}
	
	@StringSetter(PARK_CAP_KEY)
	public void setCapacityCalculationMethod(String capacityCalculationMethod) {
		this.capacityCalculationMethod = LinkParkingCapacityCalculationMethod.valueOf(capacityCalculationMethod);
	}
	
	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(SHAPE_KEY, SHAPE_KEY_EXP);
		map.put(SHAPE, SHAPE_EXP);
		map.put(PARK_CAP_KEY, SPARK_CAP_KEY_EXP);
		return map;
	}
	

}
