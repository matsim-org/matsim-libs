/* *********************************************************************** *
 * project: org.matsim.*
 * OSMXMLParser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.io.osmparser;

import java.util.List;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

public class OSMXMLParser extends MatsimXmlParser {
	
	private OSMWay currentWay = null;
	private OSMNode currentNode = null;
	

	private List<String> keys = null;
	private final OSM osm;

	public OSMXMLParser(OSM osm){
		this.osm = osm;
	}
	
	public void setKeyFilter(List<String> keys) {
		this.keys  = keys;
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (name.equals("node")) {
			Id id = new IdImpl(atts.getValue("id"));
			double lat = Double.parseDouble(atts.getValue("lat"));
			double lon = Double.parseDouble(atts.getValue("lon"));
			OSMNode node = new OSMNode(lat, lon, id);
			this.currentNode = node;
		} else if (name.equals("way")) {
			Id id = new IdImpl(atts.getValue("id"));
			OSMWay way = new OSMWay(id);
			this.currentWay = way;
		} else if (name.equals("nd")) {
			Id ref = new IdImpl(atts.getValue("ref"));
			this.currentWay.addNodeRef(ref);
		} else if (name.equals("tag")) {
			String key = atts.getValue("k");
			String val = atts.getValue("v");
			if (this.currentWay != null) {
				this.currentWay.addTag(key, val);
			} else if (this.currentNode != null) {
				this.currentNode.addTad(key, val);
			}
		}

	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if (name.equals("way")) {
			for (String key : this.keys) {
				if (this.currentWay.getTags().get(key) != null) {
					this.osm.getWays().add(this.currentWay);
					for (Id id : this.currentWay.getNodeRefs()) {
						this.osm.getRefNodes().add(id);
					}
					break;
				}
			}
			this.currentWay = null;
		}else if (name.equals("node")) {
			this.osm.getUnfilteredNodes().add(this.currentNode);
			this.currentNode = null;
		}

	}

}
