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
package playground.southafrica.utilities.openstreetmap.amenities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;

import playground.southafrica.utilities.Header;


/**
 * Parses all amenities from an OpenStreetMap file.
 * @author johanwjoubert
 */
public class MyAmenityReader {
	private final static Logger log = Logger.getLogger(MyAmenityReader.class);
	private QuadTree<Id> linkQT;
	private ActivityFacilitiesImpl amenities;
	private ObjectAttributes amenityAttributes;
	private final CoordinateTransformation ct;
	

	/**
	 * Implementing the {@link MyAmenityReader} class. The main method 
	 * requires three arguments:
	 * <ol>
	 * 	<li> the OpenStreetMap file, *.osm;
	 * 	<li> the output MATSim {@link Facility} file;
	 * 	<li> the output {@link ObjectAttributes} file containing the facility 
	 * 		 attributes.
	 * </ol>
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(MyAmenityReader.class.toString(), args);
		
		log.info("Parsing amenity facilities from OpenStreetMap.");
		String osmFile = args[0];
		String facilityFile = args[1];
		String attributeFile = args[2];
		String coordinateFile = null;
		if(args.length > 3){
			coordinateFile = args[3];
		}
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", "WGS84_SA_Albers");
		MyAmenityReader msr = new MyAmenityReader(osmFile, ct);
		try {
			msr.parseAmenity(osmFile);
			msr.writeFacilities(facilityFile);
			msr.writeFacilityAttributes(attributeFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}		
		
		if(coordinateFile != null){
			msr.writeFacilityCoordinates(coordinateFile);
		}
		
		log.info("------------------------------------------------");
		log.info("     Done.");
		log.info("================================================");
	}
	
	
	public MyAmenityReader(String file, CoordinateTransformation ct) {
		log.info("Creating amenity reader");
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		log.info("Reading limited network...");
		this.ct = ct;
		OsmNetworkReader onr = new OsmNetworkReader(scenario.getNetwork(), ct);
		onr.setHighwayDefaults(6, "service", 1, 30.0/3.6, 1.0, 600);
		onr.parse(file);
		double speedlimit = 80.0/3.6;
		this.linkQT = cleanNetwork(scenario, speedlimit);
		
		this.amenities = new ActivityFacilitiesImpl("OpenStreetMap amenities");
		this.amenityAttributes = new ObjectAttributes();
	}
	
	private QuadTree<Id> cleanNetwork(Scenario scOld, Double speedLimit){
		Double xmin = Double.POSITIVE_INFINITY;
		Double ymin = Double.POSITIVE_INFINITY;
		Double xmax = Double.NEGATIVE_INFINITY;
		Double ymax = Double.NEGATIVE_INFINITY;
		
		for(Id linkId : scOld.getNetwork().getLinks().keySet()){
			Link l = scOld.getNetwork().getLinks().get(linkId);
			
			/* Only consider links below the speed limit threshold. */
			if(l.getFreespeed() < speedLimit){
				if(l.getCoord().getX() < xmin){ xmin = l.getCoord().getX(); } 
				if(l.getCoord().getY() < ymin){ ymin = l.getCoord().getY(); } 
				if(l.getCoord().getX() > xmax){ xmax = l.getCoord().getX(); } 
				if(l.getCoord().getY() > ymax){ ymax = l.getCoord().getY(); }
			}
		}
		
		/* Add all the relevant links to the QuadTree. */
		QuadTree<Id> linkQT = new QuadTree<Id>(xmin, ymin, xmax, ymax);
		for(Id linkId : scOld.getNetwork().getLinks().keySet()){
			Link l = scOld.getNetwork().getLinks().get(linkId);
			
			/* Only consider links below the speed limit threshold. */
			if(l.getFreespeed() < speedLimit){
				linkQT.put(l.getCoord().getX(), l.getCoord().getY(), linkId);
			}
		}
		log.info("QuadTree created of limited network: ");
		log.info("   Old number of links: " + scOld.getNetwork().getLinks().size());
		log.info("   New number of links: " + linkQT.size());
		log.info("QuadTree extent:");
		log.info(String.format("   Min x: %10.2f", linkQT.getMinEasting()));
		log.info(String.format("   Min y: %10.2f", linkQT.getMinNorthing()));
		log.info(String.format("   Max x: %10.2f", linkQT.getMaxEasting()));
		log.info(String.format("   Max y: %10.2f", linkQT.getMaxNorthing()));
		return linkQT;
	}
	
	public QuadTree<Id> getQuadTree(){
		return this.linkQT;
	}
	
	
	/**
	 * Parses a given <i>OpenStreetMap</i> file for amenity facilities.
	 * @param file the {@code *.osm} file to parse for amenity facilities.
	 * @throws FileNotFoundException 
	 */
	public void parseAmenity(String file) throws FileNotFoundException{
		File f = new File(file);
		if(!f.exists()){
			throw new FileNotFoundException("Could not find " + file);
		}
		MyAmenitySink mes = new MyAmenitySink(this.ct);
		XmlReader xr = new XmlReader(f, false, CompressionMethod.None);
		xr.setSink(mes);
		xr.run();		
		
		this.amenities = mes.getFacilities();
		this.amenityAttributes = mes.getFacilityAttributes();		
		
		log.info("Assigning link Ids...");
		for(Id id : this.amenities.getFacilities().keySet()){
			ActivityFacilityImpl af = (ActivityFacilityImpl) this.amenities.getFacilities().get(id);
			Id linkId = linkQT.get(af.getCoord().getX(), af.getCoord().getY());
			af.setLinkId(linkId);
		}
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
			for(Id id : this.amenities.getFacilities().keySet()){
				ActivityFacility facility = this.amenities.getFacilities().get(id);
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
		FacilitiesWriter fw = new FacilitiesWriter(this.amenities);
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
	
	
	public ActivityFacilitiesImpl getShops(){
		return this.amenities;
	}
	

}

