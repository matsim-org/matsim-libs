/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.vissim.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.xml.sax.Attributes;

/**
 * Parses a Visum-ANMRoutes-File and returns all routes to route-handler.
 * The returned routes are held very simple.
 *
 * This implementation follows playground.scnadine.converters.osmCore.OsmXmlParser by mrieser.
 *
 * @author boescpa
 */
public class SimpleAnmroutesParser extends MatsimXmlParser {

	private final AnmRouteHandler routeHandler;
	private AnmRoute currentRoute = null;
	private final Counter routeCounter = new Counter("route ");

	public SimpleAnmroutesParser(AnmRouteHandler handler) {
		super();
		this.routeHandler = handler;
		this.setValidating(false);
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if ("ROUTE".equals(name)) {
			String idString = atts.getValue("FROMZONENO") + "-"
					+ atts.getValue("TOZONENO") + "-" + atts.getValue("INDEX");
			this.currentRoute = new AnmRoute(Id.create(idString, AnmRoute.class), 0.0);
		} else if ("ITEM".equals(name)) {
			if (this.currentRoute != null) {
				this.currentRoute.nodes.add(Id.create(Long.parseLong(atts.getValue("NODE")), Node.class));
			} else {
				throw new IllegalStateException("In anmroutes-file node without route found.");
			}
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if ("ROUTE".equals(name)) {
			this.routeHandler.handleRoute(this.currentRoute);
			this.currentRoute = null;
			this.routeCounter.incCounter();
		} else if ("ABSTRACTNETWORKMODEL".equals(name)) {
			routeCounter.printCounter();
		}
	}

	public interface AnmRouteHandler {
		void handleRoute(AnmRoute anmRoute);
	}

	public static class AnmRoute {
		/**
		 * Consists of a String Fromzone-Tozone-Index.
		 */
		public final Id<AnmRoute> id;
		public final Double demand;
		/**
		 * Node-ids as Longs.
		 */
		public final List<Id<Node>> nodes;

		public AnmRoute(Id<AnmRoute> id, Double demand) {
			this.id = id;
			this.demand = demand;
			this.nodes = new ArrayList<>();
		}
	}

}
