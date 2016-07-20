/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package contrib.publicTransitMapping.osm.lib;

import org.apache.log4j.Logger;
import contrib.publicTransitMapping.osm.lib.handler.OsmNodeHandler;
import contrib.publicTransitMapping.osm.lib.handler.OsmRelationHandler;
import contrib.publicTransitMapping.osm.lib.handler.OsmWayHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler to read out osm data (nodes, ways and relations). Just stores the data.
 */
public class OsmParserHandler implements OsmNodeHandler, OsmRelationHandler, OsmWayHandler {
	
	private static final Logger log = Logger.getLogger(OsmParserHandler.class);

	private TagFilter nodeFilter;
	private TagFilter wayFilter;
	private TagFilter relationFilter;

	private final Map<Long, OsmParser.OsmNode> nodes = new HashMap<>();
	private final Map<Long, OsmParser.OsmRelation> relations = new HashMap<>();
	private final Map<Long, OsmParser.OsmWay> ways = new HashMap<>();

	public OsmParserHandler() {
	}

	public void addFilter(TagFilter nodeFilter, TagFilter wayFilter, TagFilter relationFilter) {
		this.nodeFilter = nodeFilter;
		this.wayFilter = wayFilter;
		this.relationFilter = relationFilter;
	}

	public Map<Long, OsmParser.OsmWay> getWays() {
		return ways;
	}

	public Map<Long, OsmParser.OsmNode> getNodes() {
		return nodes;
	}

	public Map<Long, OsmParser.OsmRelation> getRelations() {
		return relations;
	}

	@Override
	public void handleNode(OsmParser.OsmNode node) {
		if(nodeFilter == null || nodeFilter.matches(node.tags)) {
			nodes.put(node.id, node);
		}
	}

	@Override
	public void handleRelation(OsmParser.OsmRelation relation) {
		if(relationFilter == null || relationFilter.matches(relation.tags)) {
			relations.put(relation.id, relation);
		}
	}

	@Override
	public void handleWay(OsmParser.OsmWay way) {
		if(wayFilter == null || wayFilter.matches(way.tags)) {
			ways.put(way.id, way);
		}
	}
}

