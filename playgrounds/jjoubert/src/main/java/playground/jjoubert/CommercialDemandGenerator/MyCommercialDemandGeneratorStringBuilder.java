/* *********************************************************************** *
 * project: org.matsim.*
 * MyCommercialDemandGeneratorStringBuilder.java
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

package playground.jjoubert.CommercialDemandGenerator;

public class MyCommercialDemandGeneratorStringBuilder {
	private final String root;
	private final String studyAreaName;
	
	public MyCommercialDemandGeneratorStringBuilder(String root, String studyAreaName){
		this.root = root;
		this.studyAreaName = studyAreaName;
	}
	
	public String getVehicleStatsFilename(String version, String threshold, String sample){
		return root + studyAreaName + "/" + version + "/" + threshold + "/" + 
				sample + "/Activities/" + studyAreaName + "_VehicleStats.txt";
	}

	public String getXmlSourceFolderName(String version, String threshold, String sample) {
		return root + "DigiCore/XML/" + version + "/" + threshold + "/" + 
		sample + "/";
	}

	public String getMatrixFileLocation(String version, String threshold, String sample) {
		return root + studyAreaName + "/" + version + "/" + threshold + "/" + 
		sample + "/Activities/";
	}
	

}
