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

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.gregor.sim2d_v4.io.osmparser.OSMRelation.Member;

public class OSMXMLParser extends MatsimXmlParser {

	private OSMWay currentWay = null;
	private OSMNode currentNode = null;
	private OSMRelation currentRelation = null;


	private List<String> keys = new ArrayList<String>();
	private List<Tuple<String, String>> keyValues = new ArrayList<Tuple<String,String>>();
	private final OSM osm;

	public OSMXMLParser(OSM osm){
		this.osm = osm;
		this.keys = osm.getKeys();
		this.keyValues = osm.getKeyValues();
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (name.equals("node")) {
			long id = Long.parseLong(atts.getValue("id"));
			double lat = Double.parseDouble(atts.getValue("lat"));
			double lon = Double.parseDouble(atts.getValue("lon"));
			OSMNode node = new OSMNode(lat, lon, id);
			this.currentNode = node;
		} else if (name.equals("relation")){
			long id = Long.parseLong(atts.getValue("id"));
			this.currentRelation = new OSMRelation(id);
		} else if (name.equals("way")) {
			long id = Long.parseLong(atts.getValue("id"));
			OSMWay way = new OSMWay(id);
			this.currentWay = way;
		} else if (name.equals("nd")) {
			long ref = Long.parseLong(atts.getValue("ref"));
			this.currentWay.addNodeRef(ref);
		} else if (name.equals("tag")) {
			String key = atts.getValue("k");
			String val = atts.getValue("v");
			if (this.currentWay != null) {
				this.currentWay.addTag(key, val);
			} else if (this.currentNode != null) {
				this.currentNode.addTag(key, val);
			} else if (this.currentRelation != null) {
				this.currentRelation.addTag(key, val);
			}
		} else if (name.equals("member")) {
			String type = atts.getValue("type");
			long refId = Long.parseLong(atts.getValue("ref"));
			String role = atts.getValue("role");
			this.currentRelation.addMember(new Member(type,refId,role));
		}

	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if (name.equals("way")) {
			for (String key : this.keys) {
				if (this.currentWay.getTags().get(key) != null) {
					this.osm.getWays().add(this.currentWay);
					for (long id : this.currentWay.getNodeRefs()) {
						this.osm.getRefNodes().add(id);
					}
					break;
				}
			}

			this.currentWay = null;
		}else if (name.equals("node")) {
			this.osm.getUnfilteredNodes().add(this.currentNode);
			this.currentNode = null;
		} else if (name.equals("relation")) {
			for (Tuple<String, String> keyValue : this.keyValues) {
				String val = this.currentRelation.getTags().get(keyValue.getFirst());
				if (val != null && val.equals(keyValue.getSecond())) {
					this.osm.getRelations().add(this.currentRelation);
					break;
				}
			}
			this.currentRelation = null;
		}

	}

}
