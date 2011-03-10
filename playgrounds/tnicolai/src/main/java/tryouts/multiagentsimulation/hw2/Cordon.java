/* *********************************************************************** *
 * project: org.matsim.*
 * Cordon.java
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

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.controller.Controller;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.charts.XYLineChart;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author thomas
 *
 */
public class Cordon {
	
	private Collection<Link> inLinks = new HashSet<Link>();
	private Collection<Link> outLinks = new HashSet<Link>();
	
	private static final int MAXSIZE = 24; // slots 0..11 are regular slots, slot 12 is anything above
	
	/**
	 * main
	 * @param args
	 */
	public static void main(String[] args) {
		Cordon cordon = new Cordon();
		cordon.run();
	}

	private void run() {
		String fileName = "./tnicolai/configs/munich-small/config.xml";
		ScenarioLoaderImpl scenarioLoader = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(fileName);
		scenarioLoader.loadScenario();
		Scenario scenario = scenarioLoader.getScenario();
		final Network network = scenario.getNetwork();
		getCordonLinks(network);

		System.out.println(inLinks.size());
		System.out.println(outLinks.size());
		
		
		final MyLinkEventHandler linkLeaveEventHandler = new MyLinkEventHandler(network, inLinks, outLinks);
			
		final Controller controller = new Controller(fileName);
		controller.setOverwriteFiles(true);
		controller.addEventHandler(linkLeaveEventHandler);
		controller.run();
		
		double[] hours = new double[MAXSIZE + 1];
		for (int i=0; i < hours.length; i++)
			hours[i] = i;
		
		System.out.println();
		for (double iin : linkLeaveEventHandler.getInGoingCounts())
			System.out.print(iin + " ");
		
		for (double oout : linkLeaveEventHandler.getOutGoingCounts())
			System.out.print(oout + " ");
		
		XYLineChart chart = new XYLineChart("Traffic link 2", "iteration", "last time");
		chart.addSeries("in", hours, linkLeaveEventHandler.getInGoingCounts());
		chart.addSeries("out", hours, linkLeaveEventHandler.getOutGoingCounts());
		chart.saveAsPng(scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it."+ scenario.getConfig().controler().getLastIteration() +"/chart.png", 1920, 1080);
	}

	/**
	 * get all links from within inner cordon
	 * @param network
	 */
	private void getCordonLinks(Network network) {		
		for (Link link : network.getLinks().values()) {
			// add outgoing link to collection
			if (contained(link.getFromNode()) && !contained(link.getToNode())) {
				outLinks.add(link);	
			}
			// add incomming link to collection
			else if (!contained(link.getFromNode()) && contained(link.getToNode())) {
				inLinks.add(link);
			}
		}
	}
	
	/**
	 * define inner cordon for munich
	 * @param fromNode
	 * @return
	 * @throws IOException 
	 */
	private boolean contained(Node fromNode){
		
//		try{
//			// get cordon from shapefile
//			String shapeFile = "./tnicolai/configs/munich-small/q_gis_data/myLayer.shp";
//			FeatureSource fts = ShapeFileReader.readDataFile(shapeFile);
//			Feature ft = (Feature) fts.getFeatures().iterator().next();
//			Geometry geo = ft.getDefaultGeometry();
//			
//			Coord coord = fromNode.getCoord();
//			Point point = MGC.coord2Point(coord);
//			if (geo.contains(point)){
//				System.out.println("Contains Point !!!");
//				return true;
//			}
//		}
//		catch(Exception e){
//			e.printStackTrace();
//			System.exit(0);
//		}
//		return false;
		
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
	 * @author thomas
	 *
	 */
	private final class MyLinkEventHandler implements LinkLeaveEventHandler {
		private final Network network;
		private Collection<Link> inLinks, outLinks;
		
		private int SLOTSIZE = 3600;
		private int MAXSIZE = 24;
		
		private double[] in  = new double[MAXSIZE + 1];
		private double[] out = new double[MAXSIZE + 1];

		private MyLinkEventHandler(Network network, Collection<Link> inLinkSet, Collection<Link> outLinkSet) {
			this.network = network;
			this.inLinks = inLinkSet;
			this.outLinks= outLinkSet;
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Link link = network.getLinks().get( event.getLinkId() );
			
			if(inLinks.contains(link)){
				in[getTimeOfDay(event.getTime())]++;
			}
			else if(outLinks.contains(link)){
				out[getTimeOfDay(event.getTime())]++;
			}
		}

		@Override
		public void reset(int iteration) {
			
		}
		
		/**
		 * get time of day
		 * @param time
		 * @return
		 */
		private int getTimeOfDay(final double time){
			int hour = (int)(time/SLOTSIZE);
			if(hour > MAXSIZE)
				hour = MAXSIZE;
			return hour;
		}
		
		public double[] getInGoingCounts(){
			return in;
		}
		
		public double[] getOutGoingCounts(){
			return out;
		}
	}
	
}
