/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.polettif.boescpa.converters.osm.networkCreator.osmWithPT;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;
import playground.polettif.boescpa.converters.osm.networkCreator.osmWithPT.OsmParser.*;

import java.util.Locale;
import java.util.Stack;


/**
 * @author mrieser / Senozon AG
 */
/*package*/ class OsmXmlParser extends MatsimXmlParser {

	private final OsmNodeHandler nodeHandler;
	private final OsmWayHandler wayHandler;
	private final OsmRelationHandler relHandler;
	private OsmNode currentNode = null;
	private OsmWay currentWay = null;
	private OsmRelation currentRelation = null;
	private final Counter nodeCounter = new Counter("node ");
	private final Counter wayCounter = new Counter("way ");
	private final Counter relationCounter = new Counter("relation ");

	public OsmXmlParser(final OsmHandler handler) {
		super();
		if (handler instanceof OsmNodeHandler) {
			this.nodeHandler = (OsmNodeHandler) handler;
		} else {
			this.nodeHandler = null;
		}
		if (handler instanceof OsmWayHandler) {
			this.wayHandler = (OsmWayHandler) handler;
		} else {
			this.wayHandler = null;
		}
		if (handler instanceof OsmRelationHandler) {
			this.relHandler = (OsmRelationHandler) handler;
		} else {
			this.relHandler = null;
		}
		this.setValidating(false);
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if ("node".equals(name) & this.nodeHandler != null) {
			long id = Long.parseLong(atts.getValue("id"));
			double lat = Double.parseDouble(atts.getValue("lat"));
			double lon = Double.parseDouble(atts.getValue("lon"));
			this.currentNode = new OsmParser.OsmNode(id, new Coord(lon, lat));
		} else if ("way".equals(name) & this.wayHandler != null) {
			this.currentWay = new OsmWay(Long.parseLong(atts.getValue("id")));
		} else if ("relation".equals(name) & this.relHandler != null) {
			String id = StringCache.get(atts.getValue("id"));
			this.currentRelation = new OsmRelation(Long.parseLong(id));
			
		} else if ("nd".equals(name)) {
			if (this.currentWay != null) {
				this.currentWay.nodes.add(Long.valueOf(atts.getValue("ref")));
			}
		} else if ("tag".equals(name)) {
			if (this.currentNode != null) {
				this.currentNode.tags.put(StringCache.get(atts.getValue("k")), StringCache.get(atts.getValue("v")));
			} else if (this.currentWay != null) {
				this.currentWay.tags.put(StringCache.get(atts.getValue("k")), StringCache.get(atts.getValue("v")));
			} else if (this.currentRelation != null) {
				this.currentRelation.tags.put(StringCache.get(atts.getValue("k")), StringCache.get(atts.getValue("v")));
			}
		} else if ("member".equals(name)) {
			if (this.currentRelation != null) {
				OsmRelationMemberType type = null;
				String lcType = atts.getValue("type").toLowerCase(Locale.ROOT);
				if ("node".equals(lcType)) {
					type = OsmRelationMemberType.NODE;
				} else if ("way".equals(lcType)) {
					type = OsmRelationMemberType.WAY;
				} else if ("relation".equals(lcType)) {
					type = OsmRelationMemberType.RELATION;
				}
				this.currentRelation.members.add(new OsmRelationMember(type, Long.parseLong(atts.getValue("ref")), StringCache.get(atts.getValue("role"))));
			}
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if ("node".equals(name) & this.nodeHandler != null) {
			this.nodeCounter.incCounter();
			this.nodeHandler.handleNode(this.currentNode);
			this.currentNode = null;
		} else if ("way".equals(name) & this.wayHandler != null) {
			this.wayCounter.incCounter();
			this.wayHandler.handleWay(this.currentWay);
			this.currentWay = null;
		} else if ("relation".equals(name) & this.relHandler != null) {
			this.relationCounter.incCounter();
			this.relHandler.handleRelation(this.currentRelation);
			this.currentRelation = null;
		} else if ("osm".equals(name)) {
			this.nodeCounter.printCounter();
			this.wayCounter.printCounter();
			this.relationCounter.printCounter();
		}
	}

}