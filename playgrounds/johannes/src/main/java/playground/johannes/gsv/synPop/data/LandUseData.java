/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.data;

import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;

import java.util.Map;

/**
 * @author johannes
 *
 */
public class LandUseData {

	public static final String POPULATION_KEY = "population";
	
	public static final String NAME_KEY = "name";
	
	private ZoneLayer<Map<String, Object>> nuts1Layer;
	
	private ZoneLayer<Map<String, Object>> nuts3Layer;
	
	private ZoneLayer<Map<String, Object>> lau2Layer;
	
	private ZoneLayer<Map<String, Object>> modenaLayer;
	
	void setNuts1Layer(ZoneLayer<Map<String, Object>> nuts1Layer) {
		this.nuts1Layer = nuts1Layer;
	}
	
	void setNuts3Layer(ZoneLayer<Map<String, Object>> nuts2Layer) {
		this.nuts3Layer = nuts2Layer;
	}
	
	void setLau2Layer(ZoneLayer<Map<String, Object>> lau2Layer) {
		this.lau2Layer = lau2Layer;
	}
	
	void setModenaLayer(ZoneLayer<Map<String, Object>> modenaLayer) {
		this.modenaLayer = modenaLayer;
	}
	
	public ZoneLayer<Map<String, Object>> getNuts1Layer() {
		return nuts1Layer;
	}

	public ZoneLayer<Map<String, Object>> getNuts3Layer() {
		return nuts3Layer;
	}
	
	public ZoneLayer<Map<String, Object>> getLau2Layer() {
		return lau2Layer;
	}
	
	public ZoneLayer<Map<String, Object>> getModenaLayer() {
		return modenaLayer;
	}
}
