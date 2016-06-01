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
import org.matsim.core.utils.io.UncheckedIOException;

import java.util.*;

/**
 * @author mrieser / Senozon AG
 */
public class OsmParser {

	private final List<OsmHandler> handlers = new ArrayList<OsmHandler>();

	public OsmParser() {
	}

	public void addHandler(final OsmHandler handler) {
		this.handlers.add(handler);
	}

	public void readFile(final String filename) throws UncheckedIOException {
		OsmHandler distributor = new DataDistributor(this.handlers);
		if (filename.toLowerCase(Locale.ROOT).endsWith(".osm.pbf")) {
//			new OsmPbfParser(distributor).parse(filename);
		} else {
			new OsmXmlParser(distributor).parse(filename);
		}
	}

	public static class OsmWay {
		public final long id;
		public final List<Long> nodes = new ArrayList<Long>(6);
		public final Map<String, String> tags = new HashMap<String, String>(5, 0.9f);

		public OsmWay(final long id) {
			this.id = id;
		}
		
		public long getStartNode() {
			return this.nodes.get(0);
		}
		
		public long getEndNode() {
			return this.nodes.get(this.nodes.size()-1);
		}
	}

	public static class OsmNode {
		public final long id;
		public final Coord coord;
		public final Map<String, String> tags = new HashMap<String, String>(5, 0.9f);

		public OsmNode(final long id, final Coord coord) {
			this.id = id;
			this.coord = coord;
		}
	}

	public static class OsmRelation {
		public final long id;
		public final List<OsmRelationMember> members = new ArrayList<OsmRelationMember>(8);
		public final Map<String, String> tags = new HashMap<String, String>(5, 0.9f);

		public OsmRelation(final long id) {
			this.id = id;
		}
	}

	public static class OsmRelationMember {
		public final OsmRelationMemberType type;
		public final long refId;
		public final String role;
		public OsmRelationMember(final OsmRelationMemberType type, final long refId, final String role) {
			this.type = type;
			this.refId = refId;
			this.role = role;
		}
	}

	public static enum OsmRelationMemberType { NODE, WAY, RELATION };

	private static class DataDistributor implements OsmNodeHandler, OsmWayHandler, OsmRelationHandler {

		private final List<OsmNodeHandler> nodeHandlers = new ArrayList<OsmNodeHandler>();
		private final List<OsmWayHandler> wayHandlers = new ArrayList<OsmWayHandler>();
		private final List<OsmRelationHandler> relHandlers = new ArrayList<OsmRelationHandler>();

		public DataDistributor(final List<OsmHandler> handlers) {
			for (OsmHandler h : handlers) {
				if (h instanceof OsmNodeHandler) {
					this.nodeHandlers.add((OsmNodeHandler) h);
				}
				if (h instanceof OsmWayHandler) {
					this.wayHandlers.add((OsmWayHandler) h);
				}
				if (h instanceof OsmRelationHandler) {
					this.relHandlers.add((OsmRelationHandler) h);
				}
			}
		}

		@Override
		public void handleNode(final OsmNode node) {
			for (OsmNodeHandler handler : this.nodeHandlers) {
				handler.handleNode(node);
			}
		}

		@Override
		public void handleWay(final OsmWay way) {
			for (OsmWayHandler handler : this.wayHandlers) {
				handler.handleWay(way);
			}
		}

		@Override
		public void handleRelation(final OsmRelation relation) {
			for (OsmRelationHandler handler : this.relHandlers) {
				handler.handleRelation(relation);
			}
		}

	}
}
