/* *********************************************************************** *
 * project: org.matsim.*
 * PoiList
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
package playground.vsp.energy.validation;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
/**
 * @author dgrether
 */
@XmlType(propOrder = {"poiID", "maximumCapacity","poiTimeInfos"})
public class PoiInfo {

	private String poiID;
	
	private Double maximumCapacity;
	
	private List<PoiTimeInfo> timeInfo = new ArrayList<PoiTimeInfo>();
	
	public PoiInfo(){
	}

	@XmlElement(name="poi_id",nillable=false, required=true)
	public String getPoiID() {
		return poiID;
	}

	
	public void setPoiID(String poiId) {
		this.poiID = poiId;
	}

	@XmlElement(name="maximum_capacity", required=false)
	public Double getMaximumCapacity() {
		return maximumCapacity;
	}


	
	public void setMaximumCapacity(Double maximumCapacity) {
		this.maximumCapacity = maximumCapacity;
	}

	@XmlElement(name = "occupancy_information", required=false)
	public List<PoiTimeInfo> getPoiTimeInfos(){
		return this.timeInfo;
	}


	
}
