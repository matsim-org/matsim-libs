/* *********************************************************************** *
 * project: org.matsim.*
 * MATSimNet2Shape.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 *
 */
package playground.yu.utils.qgis;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * This class founds on many codes of Gregor Laemmel. man should for this "playground.yu.integration.cadyts.demandCalibration.withCarCounts.run"
 * install com.sun.media.jai and javax.media.jai from http://jai.dev.java.net
 *
 * @author ychen
 *
 */
public class MATSimNet2QGIS implements X2QGIS {

	protected static double flowCapFactor = 0.1;
	protected Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	protected CoordinateReferenceSystem crs = null;
	protected X2GraphImpl n2g = null;

	protected MATSimNet2QGIS() {
	}

	public MATSimNet2QGIS(String netFilename, String coordRefSys) {
		new MatsimNetworkReader(scenario).readFile(netFilename);
		crs = MGC.getCRS(coordRefSys);
		n2g = new Network2PolygonGraph(getNetwork(), crs);
	}

	public MATSimNet2QGIS(Scenario scenario, String coordRefSys) {
		this.scenario = scenario;
		crs = MGC.getCRS(coordRefSys);
		n2g = new Network2PolygonGraph(getNetwork(), crs);
	}

	public MATSimNet2QGIS(String netFilename, String coordRefSys,
			Set<Id<Link>> linkIds2paint) {
		this(netFilename, coordRefSys);
		if (linkIds2paint != null) {
			Set<Link> links2paint = new HashSet<Link>();
			Map<Id<Link>, ? extends Link> linkImpls = getNetwork().getLinks();
			for (Id<Link> linkId : linkIds2paint) {
				links2paint.add(linkImpls.get(linkId));
			}
			((Network2PolygonGraph) n2g).setLinks2paint(links2paint);
		}
	}

	/**
	 * @param flowCapFactor
	 *            the flowCapFactor to set
	 */
	public static void setFlowCapFactor(final double flowCapacityFactor) {
		flowCapFactor = flowCapacityFactor;
	}

	public void writeShapeFile(final String ShapeFilename) {
		ShapeFileWriter.writeGeometries(n2g.getFeatures(), ShapeFilename);
	}

	// /////////////////////////////
	public void addParameter(final String paraName, final Class<?> clazz,
			final Map<Id<Link>, ?> parameters) {
		n2g.addParameter(paraName, clazz, parameters);
	}
	// /////////////////////////////
	/**
	 * @return the network
	 */
	public Network getNetwork() {
		return scenario.getNetwork();
	}

	public void readEvents(final String eventsFilename,
			final EventHandler[] handlers) {
		EventsManager events = EventsUtils.createEventsManager();
		for (EventHandler handler : handlers) {
			events.addHandler(handler);
		}
		new MatsimEventsReader(events).readFile(eventsFilename);
	}

	public void readPlans(final String plansFilename,
			final AbstractPersonAlgorithm pa) {
		new MatsimPopulationReader(scenario).readFile(plansFilename);
		pa.run(scenario.getPopulation());
	}

	public CoordinateReferenceSystem getCrs() {
		return crs;
	}

	/**
	 * @param n2g
	 *            the n2g to set
	 */
	public void setN2g(final X2Graph n2g) {
		this.n2g = (X2GraphImpl) n2g;
	}

	public static void main(final String[] args) {
		String netFilename = "../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";

		Set<Id<Link>> linkIds2paint = null;
		// -------------------------------------------------------------
		linkIds2paint = new HashSet<>();
		// B-344 H
		linkIds2paint.add(Id.create("initial_792040", Link.class));
		linkIds2paint.add(Id.create("10292", Link.class));
		linkIds2paint.add(Id.create("10294R", Link.class));
		linkIds2paint.add(Id.create("3972", Link.class));
		linkIds2paint.add(Id.create("3974", Link.class));
		linkIds2paint.add(Id.create("3956R", Link.class));
		linkIds2paint.add(Id.create("3955", Link.class));
		linkIds2paint.add(Id.create("3959R", Link.class));
		linkIds2paint.add(Id.create("3960", Link.class));
		linkIds2paint.add(Id.create("3894R", Link.class));
		linkIds2paint.add(Id.create("3893R", Link.class));
		linkIds2paint.add(Id.create("3892R", Link.class));
		linkIds2paint.add(Id.create("3889", Link.class));
		linkIds2paint.add(Id.create("3891R", Link.class));
		linkIds2paint.add(Id.create("3891", Link.class));
		linkIds2paint.add(Id.create("3889R", Link.class));

		// B-344 R
		linkIds2paint.add(Id.create("initial_781015", Link.class));
		linkIds2paint.add(Id.create("3892", Link.class));
		linkIds2paint.add(Id.create("3893", Link.class));
		linkIds2paint.add(Id.create("3894", Link.class));
		linkIds2paint.add(Id.create("3960R", Link.class));
		linkIds2paint.add(Id.create("3959", Link.class));
		linkIds2paint.add(Id.create("3955R", Link.class));
		linkIds2paint.add(Id.create("3956", Link.class));
		linkIds2paint.add(Id.create("3974R", Link.class));
		linkIds2paint.add(Id.create("3972R", Link.class));
		linkIds2paint.add(Id.create("3964R", Link.class));
		linkIds2paint.add(Id.create("10289R", Link.class));

		// -------------------------------------------------------------
		MATSimNet2QGIS mn2q = new MATSimNet2QGIS(netFilename, gk4,
				linkIds2paint);
		mn2q
				.writeShapeFile("../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/TEST_network_multimodal.shp");
	}
}
