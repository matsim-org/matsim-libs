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


package playground.polettif.publicTransitMapping.osm;

import org.apache.log4j.Logger;
import playground.polettif.publicTransitMapping.osm.core.*;

import java.util.*;

/*
	<relation> ...
	<member type="way" ref="37564441" role="forward"/>
    <member type="node" ref="440129144" role="stop"/>
    <member type="way" ref="37562757" role="backward"/>
    <member type="way" ref="5135398" role="backward"/>
    <member type="way" ref="25099183" role=""/>
    <member type="way" ref="39430702" role=""/>
    <member type="way" ref="56982667" role=""/>
    <member type="way" ref="212289347" role=""/>
    <member type="way" ref="212289346" role=""/>
    <member type="way" ref="56982632" role=""/>
    <member type="node" ref="440108358" role="stop_forward"/>
    <member type="node" ref="440108339" role="stop_backward"/>
    <member type="way" ref="183817568" role="forward"/>
    <member type="way" ref="16973242" role=""/>
    <member type="way" ref="319127930" role=""/>
    <member type="way" ref="210579057" role=""/>
    <member type="way" ref="24655790" role="forward"/>
    <member type="way" ref="149758941" role="forward"/>
    <member type="way" ref="149758948" role=""/>
    <member type="way" ref="319127926" role=""/>
    <member type="way" ref="319127925" role=""/>
    <member type="way" ref="319127927" role=""/>
    <member type="way" ref="256144796" role=""/>
    <member type="way" ref="256144802" role=""/>
    ...
    <tag k="network" v="RVL"/>
    <tag k="operator" v="SWEG"/>
    <tag k="ref" v="12"/>
    <tag k="route" v="bus"/>
    <tag k="type" v="route"/>
  </relation>
	 */

/**
 * Handler to read out osm data (nodes, ways and relations). Just stores the data.
 */
public class OsmParserHandler implements OsmNodeHandler, OsmRelationHandler, OsmWayHandler {
	
	private static final Logger log = Logger.getLogger(OsmParserHandler.class);

	private TagFilter nodeFilter;
	private TagFilter wayFilter;
	private TagFilter relationFilter;

	private Map<Long, OsmParser.OsmNode> nodes = new HashMap<>();
	private Map<Long, OsmParser.OsmRelation> relations = new HashMap<>();
	private Map<Long, OsmParser.OsmWay> ways = new HashMap<>();

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

