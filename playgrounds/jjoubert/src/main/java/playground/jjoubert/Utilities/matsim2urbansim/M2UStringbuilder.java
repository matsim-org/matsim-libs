/* *********************************************************************** *
 * project: org.matsim.*
 * M2UStringbuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.jjoubert.Utilities.matsim2urbansim;

public class M2UStringbuilder {
	private String root;
	private String studyArea;
	private String version;
	private String percentage;
	
	public M2UStringbuilder(String root, String studyArea, String version, String percentage) {
		this.root = root;
		this.studyArea = studyArea;
		this.version = version;
		this.percentage = percentage;
	}

	public String getTransportZoneShapefile(){
		return root + "Shapefiles/" + studyArea + "/" + studyArea + "_TZ_UTM.shp";
	}

	public String getSubPlaceShapefile(){
		return root + "Shapefiles/" + studyArea + "/" + studyArea + "_SP_UTM.shp";
	}

	public String getShapefile(){
		return root + "Shapefiles/" + studyArea + "/" + studyArea + "_UTM.shp";
	}

	public String getFullNetworkFilename() {
		return root + studyArea + "/" + version + "/output_networkFull.xml";
	}
	
	public String getSmallNetworkFilename() {
		return root + studyArea + "/" + version + "/output_networkSmall.xml";
	}
	
	public String getEmmeNetworkFilename() {
		return root + studyArea + "/" + version + "/output_network_" + percentage + "_Emme.xml.gz";
	}

	public String getEmmePtNetworkFilename() {
		return root + studyArea + "/" + version + "/output_network_" + percentage + "_Emme_Pt.xml.gz";
	}
	
	public String getEmmeTransitLinesByLinkFilename(){
		return root + studyArea + "/" + version + "/Input/TransitLinesByLink.txt";
	}

	public String getPlansFile() {
		return root + studyArea + "/" + version + "/output_plans_" + percentage + ".xml.gz";
	}
	
	public Integer getIdField(){
		Integer result = null;
		if(studyArea.equalsIgnoreCase("eThekwini")){
			result = 1;
		}
		if(result==null){
			throw new RuntimeException("Can not find Id index for " + studyArea + " shapefile.");
		}
		return result;
	}

	public String getDbfOutputFile() {
		return root + studyArea + "/" + version + "/TravelTimeDbf_" + percentage + ".dbf";
	}
	
	public String getIterationEventsFile(String iteration){
		return root + studyArea + "/" + version + "/" + iteration + ".events_" + percentage + ".txt.gz";
	}

	public String getIterationLinkstatsFile(String iteration){
		return root + studyArea + "/" + version + "/" + iteration + ".linkstats_" + percentage + ".txt";
	}

	public String getSubPlaceTable() {
		return root + studyArea + "/" + version + "/Input/PtSubplaceTable.csv";
	}

	public String getSubPlaceDistanceFilename() {
		return root + studyArea + "/" + version + "/DistanceToPt.csv";
	}
	
	public Integer getSubplaceIdField(){
		Integer result = null;
		if(this.studyArea.equalsIgnoreCase("eThekwini")){
			result = 2;
		} else{
			throw new RuntimeException("Don't have a subplace Id field for " + studyArea + " in M2UStringbuilder.");
		}
		return result;
	}

}

