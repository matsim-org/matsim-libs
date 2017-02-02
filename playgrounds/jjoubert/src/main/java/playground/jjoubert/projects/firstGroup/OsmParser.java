/* *********************************************************************** *
 * project: org.matsim.*
 * OsmParser.java
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

/**
 * 
 */
package playground.jjoubert.projects.firstGroup;

import java.io.File;
import java.io.FileNotFoundException;

import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;

import playground.southafrica.utilities.Header;

/**
 * Class to parse the street names and distances for OpenStreetMap data. This is
 * for Elias Willemse's FirstGroup project.
 * 
 * @author jwjoubert
 */
public class OsmParser {
	private final String nodefile;
	private final String wayfile;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(OsmParser.class.toString(), args);
		String osmFile = args[0];
		String wayFile = args[1];
		String nodeFile = args[2];
		
		OsmParser op = new OsmParser(wayFile, nodeFile);
		try {
			op.parse(osmFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("Could not parse OSM file.");
		}
		Header.printFooter();
	}
	
	public OsmParser(String wayFile, String nodeFile) {
		this.nodefile = nodeFile;
		this.wayfile = wayFile;
	}

	public void parse(String file) throws FileNotFoundException{
		File f = new File(file);
		if(!f.exists()){
			throw new FileNotFoundException("Could not find " + file);
		}
		
		RoadSink rs = new RoadSink(this.wayfile, this.nodefile);
		XmlReader xmlr = new XmlReader(f, false, CompressionMethod.None);
		xmlr.setSink(rs);
		xmlr.run();
	}

}
