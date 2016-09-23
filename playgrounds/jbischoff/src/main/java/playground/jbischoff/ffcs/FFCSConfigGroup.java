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

/**
 * 
 */
package playground.jbischoff.ffcs;

import java.net.URL;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class FFCSConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "freefloating";
	public static final String FFCS_VEHICLES_FILE = "vehiclesFile";
	public static final String PUNISHMENTFORMODESWITCH = "punishmentForModeSwitch";
	public static final String MAXIMUMWALKDISTANCE = "maximumWalkDistance_m";
	public static final String ZONESSHP = "allowedZonesShapeFile";
	public static final String ZONESXML = "allowedZonesXmlFile";
	public static final String RESETVEHICLES = "resetVehicles";
	
	
	
	private String vehiclesFiles = null;
	private double punishMentForModeSwitch = 0.0;
	private double maximumWalkDistance = 1500;
	private String zonesXMLFile = null;
	private String zonesShapeFile = null;
	private boolean resetVehicles;

	public static FFCSConfigGroup get(Config config) {
		return (FFCSConfigGroup) config.getModule(GROUP_NAME);
	}

	public FFCSConfigGroup() {
		super(GROUP_NAME);
	}
	
	
	/**
	 * @return the resetVehicles
	 */
	@StringGetter(RESETVEHICLES)
	public boolean resetVehicles() {
		return resetVehicles;
	}
	
	
	/**
	 * @param resetVehicles the resetVehicles to set
	/
	 */
	@StringSetter(RESETVEHICLES)
	public void setResetVehicles(boolean resetVehicles) {
		this.resetVehicles = resetVehicles;
	}
	
	/**
	 * @return the zonesShape
	 */
	@StringGetter(ZONESSHP)
	public String getZonesShapeFile() {
		return zonesShapeFile;
	}
	/**
	 * @param zonesShape the zonesShape to set
	 */
	@StringSetter(ZONESSHP)
	public void setZonesShape(String zonesShape) {
		this.zonesShapeFile = zonesShape;
	}
	/**
	 * @return the zonesXML
	 */
	@StringGetter(ZONESXML)
	public String getZonesXMLFile() {
		return zonesXMLFile;
	}
	/**
	 * @param zonesXML the zonesXML to set
	 */
	@StringSetter(ZONESXML)
	public void setZonesXML(String zonesXML) {
		this.zonesXMLFile = zonesXML;
	}

	@StringGetter(FFCS_VEHICLES_FILE)
	/**
	 * @return the vehiclesFiles
	 */
	public String getVehiclesFiles() {
		return vehiclesFiles;
	}
	
	public URL getVehiclesFileUrl(URL context){
		return ConfigGroup.getInputFileURL(context, this.vehiclesFiles);

	}
	public URL getZonesXMLFileUrl(URL context){
		return ConfigGroup.getInputFileURL(context, this.zonesXMLFile);

	}
	public URL getZonesShapeFileUrl(URL context){
		return ConfigGroup.getInputFileURL(context, this.zonesShapeFile);

	}
	@StringSetter(FFCS_VEHICLES_FILE)
	/**
	 * @param vehiclesFiles
	 *            the vehiclesFiles to set
	 */
	public void setVehiclesFiles(String vehiclesFiles) {
		this.vehiclesFiles = vehiclesFiles;
	}
	
	@StringGetter(PUNISHMENTFORMODESWITCH)
	/**
	 * @return the punishMentForModeSwitch
	 */
	public double getPunishmentForModeSwitch() {
		return punishMentForModeSwitch;
	}
	
	@StringSetter(PUNISHMENTFORMODESWITCH)
	/**
	 * @param punishMentForModeSwitch the punishMentForModeSwitch to set
	 */
	public void setPunishMentForModeSwitch(double punishMentForModeSwitch) {
		this.punishMentForModeSwitch = punishMentForModeSwitch;
	}

	/**
	 * @return the maximumWalkDistance
	 */
	@StringGetter(MAXIMUMWALKDISTANCE)
	public double getMaximumWalkDistance() {
		return maximumWalkDistance;
	}
	/**
	 * @param maximumWalkDistance the maximumWalkDistance to set
	 */
	@StringSetter(MAXIMUMWALKDISTANCE)
	public void setMaximumWalkDistance(double maximumWalkDistance) {
		this.maximumWalkDistance = maximumWalkDistance;
	}
}
