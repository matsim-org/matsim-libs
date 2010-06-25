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
	
	public M2UStringbuilder(String root, String studyArea, String version) {
		this.root = root;
		this.studyArea = studyArea;
		this.version = version;
	}

	public String getShapefile(){
		return root + "Shapefiles/" + studyArea + "/" + "TransportZone_UTM.shp";
	}

	public String getFullNetworkFilename() {
		return root + studyArea + "/" + version + "/networkFull.xml";
	}
	
	public String getSmallNetworkFilename() {
		return root + studyArea + "/" + version + "/networkSmall.xml";
	}
	
	public String getEmmeNetworkFilename() {
		return root + studyArea + "/" + version + "/networkEmme.xml";
	}

	public String getPlansFile() {
		return root + studyArea + "/" + version + "/plans.xml";
	}


}

