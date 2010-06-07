/* *********************************************************************** *
 * project: org.matsim.*
 * CordonTemplate.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package tryouts.multiagentsimulation.hw2;

import java.util.Collection;
import java.util.HashSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.controller.Controller;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.scenario.ScenarioLoaderImpl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class CordonTemplate {
	
	private final class MyLinkEventHandler implements LinkLeaveEventHandler {
		private final Network network;

		private MyLinkEventHandler(Network network) {
			this.network = network;
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			// Den EventHandler mÃ¼ssen Sie natÃ¼rlich selbst programmieren.
		}

		@Override
		public void reset(int iteration) {
			
		}
	}

	private Collection<Link> inLinks = new HashSet<Link>();
	private Collection<Link> outLinks = new HashSet<Link>();
	
	/**
	 * main
	 * @param args
	 */
	public static void main(String[] args) {
				
		CordonTemplate cordon = new CordonTemplate();
		cordon.run();
	}

	private void run() {
		String fileName = "./tnicolai/configs/munich-small/config.xml";
		ScenarioLoader scenarioLoader = new ScenarioLoaderImpl(fileName);
		scenarioLoader.loadScenario();
		Scenario scenario = scenarioLoader.getScenario();
		final Network network = scenario.getNetwork();
		getCordonLinks(network);

		System.out.println(inLinks.size());
		System.out.println(outLinks.size());
		
		
		final LinkLeaveEventHandler linkLeaveEventHandler = new MyLinkEventHandler(network);

		final Controller controller = new Controller(fileName);
		controller.setOverwriteFiles(true);
		controller.addEventHandler(linkLeaveEventHandler);
		controller.run();
		
		printResults();

	}

	private void printResults() {
		// Geben Sie Ihre Ergebnisse aus.
	}


	private void getCordonLinks(Network network) {		
		for (Link link : network.getLinks().values()) {
			if (contained(link.getFromNode()) && !contained(link.getToNode())) {
				outLinks.add(link);
			} else if (!contained(link.getFromNode()) && contained(link.getToNode())) {
				inLinks.add(link);
			}
		}
	}

	private boolean contained(Node fromNode) {
		double xmin = 4467184;
		double xmax = 4471382;
		double ymin = 5332355;
		double ymax = 5334572;
		Coordinate c1 = new Coordinate(xmin, ymin);
		Coordinate c2 = new Coordinate(xmin, ymax);
		Coordinate c3 = new Coordinate(xmax, ymax);
		Coordinate c4 = new Coordinate(xmax, ymin);
		Coordinate c5 = c1;
		GeometryFactory geofac = new GeometryFactory();
		Coordinate[] coordinates = new Coordinate[] {c1, c2, c3, c4, c5};
		com.vividsolutions.jts.geom.LinearRing linearRing = geofac.createLinearRing(coordinates);
		com.vividsolutions.jts.geom.Polygon polygon = geofac.createPolygon(linearRing, null);
		
		Coord coord = fromNode.getCoord();
		
		// return coord.getX() > xmin && coord.getX() < xmax && coord.getY() > ymin && coord.getY() < ymax;
		return polygon.contains(geofac.createPoint(new Coordinate(coord.getX(), coord.getY())));
	}

	/**
	 * 
	 * So erzeugt man mit GeoTools ein Viereck.
	 * Vielleicht können Sie das ja gebrauchen (und verbessern).
	 * 
	 * @param xmin
	 * @param xmax
	 * @param ymin
	 * @param ymax
	 * @return
	 */
	private Polygon erzeugeViereck(double xmin, double xmax, double ymin, double ymax) {
		Coordinate c1 = new Coordinate(xmin, ymin);
		Coordinate c2 = new Coordinate(xmin, ymax);
		Coordinate c3 = new Coordinate(xmax, ymax);
		Coordinate c4 = new Coordinate(xmax, ymin);
		Coordinate c5 = c1;
		GeometryFactory geofac = new GeometryFactory();
		Coordinate[] coordinates = new Coordinate[] {c1, c2, c3, c4, c5};
		LinearRing linearRing = geofac.createLinearRing(coordinates);
		Polygon polygon = geofac.createPolygon(linearRing, null);
		return polygon;
	}

}
