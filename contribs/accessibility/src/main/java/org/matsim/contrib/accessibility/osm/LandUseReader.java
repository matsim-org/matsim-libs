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
 * Parses all landuse ??? from an OpenStreetMap file.
 * 
 * @author dziemke
 * @see <a href="http://wiki.openstreetmap.org/wiki/Key:landuse">OpenStreetMap: Land Use</a>
 */
public class LandUseReader {
	private final static Logger log = Logger.getLogger(LandUseReader.class);
	private QuadTree<Id<ActivityFacility>> linkQT;
	private ActivityFacilities landuse;
	private ObjectAttributes amenityAttributes;
	private final CoordinateTransformation ct;
	private Map<String, String> osmToMatsimTypeMap;
	

	/**
	 * Constructing a landuse ??? reader to parse the OpenStreetMap landuse ???.
	 * 
	 * @param file the path to the *.osm OpenStreetMap file;
	 * @param ct the (projected) coordinate reference system to which the 
	 * 		  WGS84 coordinates of OpenStreetMap will be converted to; and
	 * @param osmToMatsimTypeMap a mapping of OpenStreetMap
	 * 		  <a href="http://wiki.openstreetmap.org/wiki/Key:landuse">Land Use</a>
	 * 		  to MATSim activity types.
	 */
//	public LandUseReader(String file, CoordinateTransformation ct, 
//			Map<String, String> osmToMatsimTypeMap) {
	public LandUseReader(CoordinateTransformation ct, Map<String, String> osmToMatsimTypeMap) {
		log.info("Creating landuse ??? reader");
		
		this.ct = ct;
		this.osmToMatsimTypeMap = osmToMatsimTypeMap;
		this.landuse = FacilitiesUtils.createActivityFacilities("OpenStreetMap landuse ???");
//		this.amenityAttributes = new ObjectAttributes();
	}
	
	
	public QuadTree<Id<ActivityFacility>> getQuadTree(){
		return this.linkQT;
	}
	
	
	/**
	 * Parses a given <i>OpenStreetMap</i> file for land use.
	 * @param file the {@code *.osm} file to parse for land use
	 * @throws FileNotFoundException 
	 */
	public void parseLandUse(String file) throws FileNotFoundException{
		File f = new File(file);
		if(!f.exists()){
			throw new FileNotFoundException("Could not find " + file);
		}
		LandUseSink landUseSink = new LandUseSink(this.ct, this.osmToMatsimTypeMap);
		XmlReader xmlReader = new XmlReader(f, false, CompressionMethod.None);
		xmlReader.setSink(landUseSink);
		xmlReader.run();		
		
		this.landuse = landUseSink.getFacilities();
		this.amenityAttributes = landUseSink.getFacilityAttributes();		
	}
	
	
	// here


	/**
	 * Writes the facility coordinates so that it can be imported into QGis.
	 */
	public void writeFacilityCoordinates(String file){
		log.info("Writing facility coordinates to " + file);
		BufferedWriter bw = IOUtils.getBufferedWriter(file);
		try{
			bw.write("FacilityId,Long,Lat,Type");
			bw.newLine();
			for(Id<ActivityFacility> id : this.landuse.getFacilities().keySet()){
				ActivityFacility facility = this.landuse.getFacilities().get(id);
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
	 * Writes the amenities {@link Facility}s to file.
	 * @param file
	 */
	public void writeFacilities(String file){
		FacilitiesWriter fw = new FacilitiesWriter(this.landuse);
		fw.write(file);
	}
	
	
	/**
	 * Writes the facility attributes to file.
	 * @param file
	 */
	public void writeFacilityAttributes(String file){
		ObjectAttributesXmlWriter ow = new ObjectAttributesXmlWriter(this.amenityAttributes);
		ow.writeFile(file);
	}
	
	
	public ActivityFacilities getActivityFacilities(){
		return this.landuse;
	}
	

}

