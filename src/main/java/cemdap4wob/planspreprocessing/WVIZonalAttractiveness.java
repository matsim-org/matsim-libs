/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

/**
 * 
 */
package cemdap4wob.planspreprocessing;

import java.util.HashMap;
import java.util.Map;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class WVIZonalAttractiveness {

	private String zoneId;
	private Coord zoneCentroid;
	private double attractivenessWork;
	private double attractivenessShop;
	private double attractivenessOther;
	private double attractivenessLeisure;
	private double attractivenessEducation;
	public WVIZonalAttractiveness(String zoneId, Coord zoneCentroid, double attractivenessWork,
			double attractivenessShop, double attractivenessOther, double attractivenessLeisure,
			double attractivenessEducation) {
		this.zoneId = zoneId;
		this.zoneCentroid = zoneCentroid;
		this.attractivenessWork = attractivenessWork;
		this.attractivenessShop = attractivenessShop;
		this.attractivenessOther = attractivenessOther;
		this.attractivenessLeisure = attractivenessLeisure;
		this.attractivenessEducation = attractivenessEducation;
	}
	public String getZoneId() {
		return zoneId;
	}
	public Coord getZoneCentroid() {
		return zoneCentroid;
	}
	public double getAttractivenessWork() {
		return attractivenessWork;
	}
	public double getAttractivenessShop() {
		return attractivenessShop;
	}
	public double getAttractivenessOther() {
		return attractivenessOther;
	}
	public double getAttractivenessLeisure() {
		return attractivenessLeisure;
	}
	public double getAttractivenessEducation() {
		return attractivenessEducation;
	}
	
	
	public static Map<String,WVIZonalAttractiveness> readZonalAttractiveness(String attractivenessFile, Map<String, Geometry> zoneMap ){
		Map <String,WVIZonalAttractiveness> zonalAttractivness = new HashMap<>();
		
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(attractivenessFile);
		tabFileParserConfig.setDelimiterRegex(";");
		new TabularFileParser().parse(tabFileParserConfig, new TabularFileHandler() {
			
			@Override
			public void startRow(String[] row) {
				String zoneId = row[0];
				double attractivenessWork = Double.parseDouble(row[2]);
				double attractivenessShop = Double.parseDouble(row[3]);
				double attractivenessOther = Double.parseDouble(row[4]);
				double attractivenessLeisure = Double.parseDouble(row[5]);
				double attractivenessEducation = Double.parseDouble(row[6]);
				Coord centroid = MGC.point2Coord(zoneMap.get(zoneId).getCentroid());
				zonalAttractivness.put(zoneId, new WVIZonalAttractiveness(zoneId, centroid, attractivenessWork, attractivenessShop, attractivenessOther, attractivenessLeisure, attractivenessEducation));
			}
		});
		
		return zonalAttractivness;
		
	}
	
	

	
	
}
