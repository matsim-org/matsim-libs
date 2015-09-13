/* *********************************************************************** *
 * project: org.matsim.*
 * MyShoppingReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package org.matsim.contrib.accessibility.osm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;


/**
 * Parses all landuse and building tags from an OpenStreetMap file.
 * 
 * @author dziemke
 * @see <a href="http://wiki.openstreetmap.org/wiki/Key:landuse">OpenStreetMap: Land Use</a>
 */
public class CombinedOsmReader {
	private final static Logger log = Logger.getLogger(CombinedOsmReader.class);
	private QuadTree<Id<ActivityFacility>> linkQT;
	private ActivityFacilities facilities;
	private ObjectAttributes facilityAttributes;
	
	//private final CoordinateTransformation ct;
	private final String outputCRS;
	
	private Map<String, String> osmLandUseToMatsimTypeMap;
	private Map<String, String> osmBuildingToMatsimTypeMap;
	//
	private Map<String, String> osmAmenityToMatsimTypeMap;
	private Map<String, String> osmLeisureToMatsimTypeMap;
	private Map<String, String> osmTourismToMatsimTypeMap;
	//
	
	private double buildingTypeFromVicinityRange;
	// private String[] tagsToIgnoreBuildings;
	

	/**
	 * Constructing a LandUseBuildingReader to parse the OpenStreetMap landuse and Buildings.
	 * 
	 * @param file the path to the *.osm OpenStreetMap file;
	 * @param ct the (projected) coordinate reference system to which the 
	 * 		  WGS84 coordinates of OpenStreetMap will be converted to; and
	 * @param osmToMatsimTypeMap a mapping of OpenStreetMap
	 * 		  <a href="http://wiki.openstreetmap.org/wiki/Key:landuse">Land Use</a>
	 * 		  to MATSim activity types.
	 */
	public CombinedOsmReader(
			//CoordinateTransformation ct, 
			String outputCRS,
			Map<String, String> osmLandUseToMatsimTypeMap,
			Map<String, String> osmBuildingToMatsimTypeMap,
			//
			Map<String, String> osmAmenityToMatsimTypeMap,
			Map<String, String> osmLeisureToMatsimTypeMap,
			Map<String, String> osmTourismToMatsimTypeMap,
			double buildingTypeFromVicinityRange
			//, String[] tagsToIgnoreBuildings
			)
			{
		log.info("Creating CombinedOsmReader");
		
		// this.ct = ct;
		this.outputCRS = outputCRS;
		
		this.osmLandUseToMatsimTypeMap = osmLandUseToMatsimTypeMap;
		this.osmBuildingToMatsimTypeMap = osmBuildingToMatsimTypeMap;
		//
		this.osmAmenityToMatsimTypeMap = osmAmenityToMatsimTypeMap;
		this.osmLeisureToMatsimTypeMap = osmLeisureToMatsimTypeMap;
		this.osmTourismToMatsimTypeMap = osmTourismToMatsimTypeMap;
		//
		
		this.facilities = FacilitiesUtils.createActivityFacilities("OpenStreetMap landuse ???");
		
		this.buildingTypeFromVicinityRange = buildingTypeFromVicinityRange;
		//this.tagsToIgnoreBuildings = tagsToIgnoreBuildings;
	}
	
	
	public QuadTree<Id<ActivityFacility>> getQuadTree(){
		return this.linkQT;
	}
	
	
	/**
	 * Parses a given <i>OpenStreetMap</i> file for land use.
	 * @param osmFile the {@code *.osm} file to parse for land use
	 * @throws FileNotFoundException 
	 */
	// public void parseLandUseAndBuildings(String file) throws FileNotFoundException{
	public void parseFile(String osmFile) throws FileNotFoundException{
		File file = new File(osmFile);
		if(!file.exists()){
			throw new FileNotFoundException("Could not find OSM file " + osmFile);
		}
		//LandUseBuildingSink landUseBuildingSink = new LandUseBuildingSink(
		CombinedOsmSink combinedOsmSink = new CombinedOsmSink(
				//this.ct,
				this.outputCRS,
				this.osmLandUseToMatsimTypeMap, this.osmBuildingToMatsimTypeMap,
				//
				this.osmAmenityToMatsimTypeMap,	this.osmLeisureToMatsimTypeMap,
				this.osmTourismToMatsimTypeMap,
				this.buildingTypeFromVicinityRange
				//, this.tagsToIgnoreBuildings
				);
		XmlReader xmlReader = new XmlReader(file, false, CompressionMethod.None);
		xmlReader.setSink(combinedOsmSink);
		xmlReader.run();		
		
		this.facilities = combinedOsmSink.getFacilities();
		this.facilityAttributes = combinedOsmSink.getFacilityAttributes();		
	}

	
	/**
	 * Writes the facility coordinates so that it can be imported into QGis.
	 */
	public void writeFacilityCoordinates(String file){
		log.info("Writing facility coordinates to " + file);
		BufferedWriter bw = IOUtils.getBufferedWriter(file);
		try{
			bw.write("FacilityId,Long,Lat,Type");
			bw.newLine();
			for(Id<ActivityFacility> id : this.facilities.getFacilities().keySet()){
				ActivityFacility facility = this.facilities.getFacilities().get(id);
				bw.write(id.toString());
				bw.write(",");
				bw.write(String.format("%.0f,%.0f\n", facility.getCoord().getX(), facility.getCoord().getY()));
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter " + file);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter " + file);
			}
		}
		log.info("Done writing coordinates to file.");
	}
	
	
	/**
	 * Writes the facilities {@link Facility}s to file.
	 * @param facilitiesFile
	 */
	public void writeFacilities(String facilitiesFile){
		FacilitiesWriter fw = new FacilitiesWriter(this.facilities);
		fw.write(facilitiesFile);
	}
	
	
	/**
	 * Writes the facility attributes to file.
	 * @param facilityAttributeFile
	 */
	public void writeFacilityAttributes(String facilityAttributeFile){
		ObjectAttributesXmlWriter ow = new ObjectAttributesXmlWriter(this.facilityAttributes);
		ow.writeFile(facilityAttributeFile);
	}
	
	
	public ActivityFacilities getActivityFacilities(){
		return this.facilities;
	}
	

}

