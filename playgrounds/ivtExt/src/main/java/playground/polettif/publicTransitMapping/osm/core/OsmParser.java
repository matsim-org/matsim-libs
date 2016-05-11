/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.osm.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.polettif.publicTransitMapping.gtfs.GTFSReader;

/**
 * @author mrieser / Senozon AG
 */
public class OsmParser {

	private static final Logger log = Logger.getLogger(OsmParser.class);

	private final List<OsmHandler> handlers = new ArrayList<>();

	private static CoordinateTransformation transformation = new IdentityTransformation();

	public OsmParser() {
	}

	public OsmParser(CoordinateTransformation ct) {
		transformation = ct;
	}

	public void addHandler(final OsmHandler handler) {
		this.handlers.add(handler);
	}

	public void readFile(final String filename) throws UncheckedIOException {
		OsmHandler distributor = new DataDistributor(this.handlers);
		if (filename.toLowerCase(Locale.ROOT).endsWith(".osm.pbf")) {
			log.error("*.osm.pbf are not supported. Use *.osm (xml format) instead.");
		} else {
			new OsmXmlParser(distributor).parse(filename);
		}
	}

	public static class OsmWay {
		public final long id;
		public final List<Long> nodes = new ArrayList<>(6);
		public final Map<String, String> tags = new HashMap<>(5, 0.9f);

		public boolean used = true;

		public OsmWay(final long id) {
			this.id = id;
		}
	}

	public static class OsmNode {
		public final long id;
		public final Coord coord;
		public final Map<String, String> tags = new HashMap<>(5, 0.9f);

		public boolean used = false;
		public int ways = 0;

		public OsmNode(final long id, final Coord coord) {
			this.id = id;
			this.coord = transformation.transform(coord);
		}
	}

	public static class OsmRelation {
		public final long id;
		public final List<OsmRelationMember> members = new ArrayList<>(8);
		public final Map<String, String> tags = new HashMap<>(5, 0.9f);

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

	public enum OsmRelationMemberType { NODE, WAY, RELATION }

	private static class DataDistributor implements OsmNodeHandler, OsmWayHandler, OsmRelationHandler {

		private final List<OsmNodeHandler> nodeHandlers = new ArrayList<>();
		private final List<OsmWayHandler> wayHandlers = new ArrayList<>();
		private final List<OsmRelationHandler> relHandlers = new ArrayList<>();

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
