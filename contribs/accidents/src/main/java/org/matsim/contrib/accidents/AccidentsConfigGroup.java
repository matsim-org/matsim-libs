/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.accidents;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.accidents.data.AccidentComputationApproach;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.io.IOUtils;

/**
 * 
 * @author ikaddoura
 */

public class AccidentsConfigGroup extends ReflectiveConfigGroup {
	private static final Logger log = Logger.getLogger(AccidentsConfigGroup.class);

	public static final String GROUP_NAME = "accidents" ;
	
	public AccidentsConfigGroup() {
		super(GROUP_NAME);
	}
	
	private boolean enableAccidentsModule = true;
	private boolean internalizeAccidentCosts = false;	
	
	// e.g. 1 for 100%; 10 for 10%; 100 for 1%
	private double sampleSize = 10.;
	
	// LAND-USE based on OSM (Berlin and Brandenburg) for AccidentAreaType in the Denmark Method & for built-up/nonbuilt-up area in the BVWP Method
	private String landuseOSMInputShapeFile = null;
	
	// POPULATION DENSITY based on OSM (Berlin) + statistics
	private String placesOSMInputFile = null;
	
	private String osmInputFileCRS = "EPSG:4326";
	
	private String[] tunnelLinks = {""};
	private String tunnelLinkCSVInputFile = null;
	
	private String[] planFreeLinks = {""};	
	private String planFreeLinkCSVInputFile = null;
		
	private AccidentComputationApproach accidentsComputationApproach = AccidentComputationApproach.BVWPforAllRoads;
	
	private double tollFactor = 1.;
	
	// add parameters if you need them
	
	@StringGetter( "enableAccidentsModule" )
	public boolean isEnableAccidentsModule() {
		return enableAccidentsModule;
	}

	@StringSetter( "enableAccidentsModule" )
	public void setEnableAccidentsModule(boolean enableAccidentsModule) {
		this.enableAccidentsModule = enableAccidentsModule;
	}

	@StringGetter( "internalizeAccidentCosts" )
	public boolean isInternalizeAccidentCosts() {
		return internalizeAccidentCosts;
	}

	@StringSetter( "internalizeAccidentCosts" )
	public void setInternalizeAccidentCosts(boolean internalizeAccidentCosts) {
		this.internalizeAccidentCosts = internalizeAccidentCosts;
	}

	@StringGetter( "sampleSize" )
	public double getSampleSize() {
		return sampleSize;
	}

	@StringSetter( "sampleSize" )
	public void setSampleSize(double sampleSize) {
		this.sampleSize = sampleSize;
	}

	@StringGetter( "landuseOSMInputShapeFile" )
	public String getLanduseOSMInputShapeFile() {
		return landuseOSMInputShapeFile;
	}

	@StringSetter( "landuseOSMInputShapeFile" )
	public void setLanduseOSMInputShapeFile(String landuseOSMInputShapeFile) {
		this.landuseOSMInputShapeFile = landuseOSMInputShapeFile;
	}

	@StringGetter( "placesOSMInputFile" )
	public String getPlacesOSMInputFile() {
		return placesOSMInputFile;
	}

	@StringSetter( "placesOSMInputFile" )
	public void setPlacesOSMInputFile(String placesOSMInputFile) {
		this.placesOSMInputFile = placesOSMInputFile;
	}

	@StringGetter( "tunnelLinkCSVInputFile" )
	public String getTunnelLinkCSVInputFile() {
		return tunnelLinkCSVInputFile;
	}

	@StringSetter( "tunnelLinkCSVInputFile" )
	public void setTunnelLinkCSVInputFile(String tunnelLinkCSVfile) {
		
		if (tunnelLinkCSVfile != null) {
			String[] tunnelLinksFromCSVFile = readCSVFile(tunnelLinkCSVfile);
			this.setTunnelLinksArray(tunnelLinksFromCSVFile);	
		}
		
		this.tunnelLinkCSVInputFile = tunnelLinkCSVfile;
	}

	@StringGetter( "tunnelLinks" )
	public String getTunnelLinks() {
		return CollectionUtils.arrayToString(tunnelLinks);
	}

	@StringSetter( "tunnelLinks" )
	public void setTunnelLinks(String tunnelLinks) {
		this.tunnelLinks = CollectionUtils.stringToArray(tunnelLinks);
	}
	
	public String[] getTunnelLinksArray() {
		return tunnelLinks;
	}
	
	public void setTunnelLinksArray(String[] tunnelLinks) {
		this.tunnelLinks = tunnelLinks;
	}

	@StringGetter( "planFreeLinkCSVInputFile" )
	public String getPlanFreeLinkCSVInputFile() {
		return this.planFreeLinkCSVInputFile;
	}

	@StringSetter( "planFreeLinkCSVInputFile" )
	public void setPlanFreeLinkCSVInputFile(String planFreeLinkCSVfile) {
		
		if (planFreeLinkCSVfile != null) {			
			String[] linkIDsFromCSVFile = readCSVFile(planFreeLinkCSVfile);
			this.setPlanFreeLinksArray(linkIDsFromCSVFile);
		}
		
		this.planFreeLinkCSVInputFile = planFreeLinkCSVfile;
	}


	private String[] readCSVFile(String csvFile) {
		ArrayList<Id<Link>> links = new ArrayList<>();

		BufferedReader br = IOUtils.getBufferedReader(csvFile);
		
		String line = null;
		try {
			line = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		} // headers

		try {
			int countWarning = 0;
			while ((line = br.readLine()) != null) {
				
				String[] columns = line.split(";");
				Id<Link> linkId = null;
				for (int column = 0; column < columns.length; column++) {
					if (column == 0) {
						linkId = Id.createLinkId(columns[column]);
					} else {
						if (countWarning < 1) {
							log.warn("Expecting the link Id to be in the first column. Ignoring further columns...");
						} else if (countWarning == 1) {
							log.warn("This message is only given once.");
						}
						countWarning++;
					}						
				}
				log.info("Adding link ID " + linkId);
				links.add(linkId);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String[] linkIDsArray = (String[]) links.toArray();
		return linkIDsArray ;
	}

	@StringGetter( "planFreeLinks" )
	public String getPlanfreeLinks() {
		return CollectionUtils.arrayToString(planFreeLinks);
	}

	@StringSetter( "planFreeLinks" )
	public void setPlanFreeLinks(String planFreeLinks) {
		this.planFreeLinks = CollectionUtils.stringToArray(planFreeLinks);
	}
	
	public String[] getPlanFreeLinksArray() {
		return planFreeLinks;
	}
	
	public void setPlanFreeLinksArray(String[] planFreeLinks) {
		this.planFreeLinks = planFreeLinks;
	}
	
	@StringGetter( "osmInputFileCRS" )
	public String getOsmInputFileCRS() {
		return osmInputFileCRS;
	}

	@StringSetter( "osmInputFileCRS" )
	public void setOsmInputFileCRS(String osmInputFileCRS) {
		this.osmInputFileCRS = osmInputFileCRS;
	}

	@StringGetter( "accidentsComputationApproach" )
	public AccidentComputationApproach getAccidentsComputationApproach() {
		return accidentsComputationApproach;
	}

	@StringSetter( "accidentsComputationApproach" )
	public void setAccidentsComputationApproach(AccidentComputationApproach accidentsComputationApproach) {
		this.accidentsComputationApproach = accidentsComputationApproach;
	}

	@StringGetter( "tollFactor" )
	public double getTollFactor() {
		return tollFactor;
	}

	@StringSetter( "tollFactor" )
	public void setTollFactor(double tollFactor) {
		this.tollFactor = tollFactor;
	}
			
}

