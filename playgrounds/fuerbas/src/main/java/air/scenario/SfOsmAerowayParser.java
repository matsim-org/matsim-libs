/* *********************************************************************** *
 * project: org.matsim.*
 * SfAirScheduleBuilder
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

package air.scenario;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

// to extract aeroway nodes from planet.osm file use osmosis: http://wiki.openstreetmap.org/wiki/Osmosis/Detailed_Usage_0.39#--node-key_.28--nk.29

public class SfOsmAerowayParser extends MatsimXmlParser {
	
	private static final Logger log = Logger.getLogger(SfOsmAerowayParser.class);
	
	private final CoordinateTransformation transform;
	private OsmNode currentNode;
	protected Map<String, Coord> airports = new HashMap<String, Coord>();

	public SfOsmAerowayParser(final CoordinateTransformation transform) {
		this.transform = transform;
		this.setValidating(false);
	}
	

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if ("node".equals(name)) {
			long id = Long.parseLong(atts.getValue("id"));				
			double lat = Double.parseDouble(atts.getValue("lat"));
			double lon = Double.parseDouble(atts.getValue("lon"));
			this.currentNode = new OsmNode(id, this.transform.transform(new CoordImpl(lon, lat)));
		} else if ("way".equals(name)) {
//			log.debug("way gefunden.");
		} else if ("nd".equals(name)) {
//			log.debug("node gefunden.");
			}
		 else if ("tag".equals(name)) {
			if (this.currentNode != null) {
				if (atts.getValue("k").equals("iata")) {
					this.currentNode.tags.put(atts.getValue("k"), atts.getValue("v"));
					if (!airports.containsKey(atts.getValue("v"))) {
						String iataCode = this.currentNode.tags.get("iata");
						if (iataCode.length()>=3)iataCode = iataCode.substring(0, 3);
						airports.put(iataCode, this.currentNode.coord);
					}
				}
			}
		}
	}
	



	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if ("way".equals(name)) { }
		else if ("node".equals(name)){
			this.currentNode = null;
		}
	}
	
	
	
	private void parse(final String osmFilename, final InputStream stream) throws SAXException, ParserConfigurationException, IOException {
		
		SfOsmAerowayParser parser = new SfOsmAerowayParser(transform);
		if (stream != null) {
			parser.parse(new InputSource(stream));
		} else {
			parser.parse(osmFilename);
		}
	}
	
	public void writeToFile(String filename) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename)));
			Iterator it = this.airports.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pairs = (Map.Entry)it.next();
		        bw.write(pairs.getKey().toString()+pairs.getValue().toString());
		        bw.newLine();
		    }
		    bw.flush();
		    bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	class OsmNode {
		public long id;
		public final Coord coord;
		public final Map<String, String> tags = new HashMap<String, String>();

		public OsmNode(final long id, final Coord coord) {
			this.id = id;
			this.coord = coord;
		}
	}
	
	
}


	

