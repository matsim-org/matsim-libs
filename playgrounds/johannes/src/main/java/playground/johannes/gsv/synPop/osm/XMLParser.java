/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.osm;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

/**
 * @author johannes
 *
 */
public class XMLParser extends MatsimXmlParser {

	private static final String NODE_TAG = "node";
	
	private static final String ID_KEY = "id";
	
	private static final String LAT_KEY = "lat";
	
	private static final String LON_KEY = "lon";
	
	private static final String WAY_TAG = "way";
	
	private static final String ND_TAG = "nd";
	
	private static final String REF_KEY = "ref";
	
	private static final String TAG_TAG = "tag";
	
	private static final String KEY_KEY = "k";
	
	private static final String VALUE_KEY = "v";
	
	private Map<String, OSMNode> nodes = new HashMap<String, OSMNode>();
	
	private Map<String, OSMWay> ways = new HashMap<String, OSMWay>();
	
	private OSMWay activeWay;
	
	public Map<String, OSMNode> getNodes() {
		return nodes;
	}
	
	public Map<String, OSMWay> getWays() {
		return ways;
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.core.utils.io.MatsimXmlParser#startTag(java.lang.String, org.xml.sax.Attributes, java.util.Stack)
	 */
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if(name.equalsIgnoreCase(NODE_TAG)) {
			OSMNode node = new OSMNode(atts.getValue(ID_KEY));
			double x = Double.parseDouble(atts.getValue(LON_KEY));
			double y = Double.parseDouble(atts.getValue(LAT_KEY));
			node.setLongitude(x);
			node.setLatitude(y);
			
			nodes.put(node.getId(), node);
			/*
			 * node tags are currently ignored
			 */
		} else if(name.equalsIgnoreCase(WAY_TAG)) {
			OSMWay way = new OSMWay(atts.getValue(ID_KEY));
			activeWay = way;
		} else if(name.equalsIgnoreCase(ND_TAG)) {
			if(activeWay != null) {
				OSMNode node = nodes.get(atts.getValue(REF_KEY));
				activeWay.addNode(node);
			}
		} else if(name.equalsIgnoreCase(TAG_TAG)) {
			if(activeWay != null) {
				String key = atts.getValue(KEY_KEY);
				String val = atts.getValue(VALUE_KEY);
				activeWay.addTag(key, val);
			}
		}

	}

	/* (non-Javadoc)
	 * @see org.matsim.core.utils.io.MatsimXmlParser#endTag(java.lang.String, java.lang.String, java.util.Stack)
	 */
	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(name.equalsIgnoreCase(WAY_TAG)) {
			ways.put(activeWay.getId(), activeWay);
			activeWay = null;
		}

	}

}
