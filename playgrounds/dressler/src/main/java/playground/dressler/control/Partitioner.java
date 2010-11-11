/* *********************************************************************** *
 * project: org.matsim.*
 * Partitioner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.dressler.control;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.NetworkRoute;

import playground.dressler.ea_flow.PathStep;
import playground.dressler.ea_flow.TimeExpandedPath;


public class Partitioner {	
	// what to cut off from each path to determine the relevant sink
	public int goBackHowMany = 0; 

	// statistics of who goes where	
	public HashMap<Id, HashMap<Id, Integer>> exitStatistics;
	// statistics of where the flow travelling to a specific sink came from
	public HashMap<Id, HashMap<Id, Integer>> exitStatisticsInverse;

	private FlowCalculationSettings _settings;	

	public Partitioner(FlowCalculationSettings settings) {
		this._settings = settings;
	}

	public Node getWhereTo(TimeExpandedPath path) {
		Node node;
		LinkedList<PathStep> steps = path.getPathSteps();

		node = steps.get(steps.size() - 1 - goBackHowMany).getArrivalNode().getRealNode();

		return node; 
	}

	/**
	 * Counts how many agents from which node go to which sink. 
	 * @param paths a collection of paths
	 */
	public void determineStatistics(Collection <TimeExpandedPath> paths) {
		exitStatistics = new HashMap<Id, HashMap<Id, Integer>>();

		for (TimeExpandedPath TEP : paths) {
			Node source = TEP.getSource();
			Node sink = getWhereTo(TEP);
			HashMap<Id, Integer> whereTo = exitStatistics.get(source.getId());
			if (whereTo == null) {
				whereTo = new HashMap<Id, Integer>();
				exitStatistics.put(source.getId(), whereTo);
			}

			Integer count = whereTo.get(sink.getId());

			if (count == null) {
				count = 0;
			}

			whereTo.put(sink.getId(), count + TEP.getFlow());
		}

		exitStatisticsInverse = new HashMap<Id, HashMap<Id, Integer>>();
		for (TimeExpandedPath TEP : paths) {
			Node source = TEP.getSource();
			Node sink = getWhereTo(TEP);

			HashMap<Id, Integer> whereFrom = exitStatisticsInverse.get(sink.getId());
			if (whereFrom == null) {
				whereFrom = new HashMap<Id, Integer>();
				exitStatisticsInverse.put(sink.getId(), whereFrom);
			}

			Integer count = whereFrom.get(source.getId());

			if (count == null) {
				count = 0;
			}

			whereFrom.put(source.getId(), count + TEP.getFlow());
		}
	}

	public void printStatistics() {
		System.out.println("Exit statistics:");
		int count = 0;
		if (exitStatistics != null) {
			for (Node source : this._settings.getNetwork().getNodes().values()) {
				HashMap<Id, Integer> whereTo = exitStatistics.get(source.getId());
				if (whereTo != null) {
					for (Id sinkid : whereTo.keySet()) {
						Integer c = whereTo.get(sinkid);
						if (c != null) {
							count += c;
							System.out.println("Node " + source.getId() + " to " + sinkid + " sends " + c);
						}
					}
				}
			}
		}
		System.out.println("Total count: " + count);

		System.out.println("Exit statistics Inverse:");
		count = 0;
		if (exitStatisticsInverse != null) {
			for (Node sink : this._settings.getNetwork().getNodes().values()) {
				HashMap<Id, Integer> whereFrom = exitStatisticsInverse.get(sink.getId());
				if (whereFrom != null) {
					for (Id sourceid : whereFrom.keySet()) {
						Integer c = whereFrom.get(sourceid);
						if (c != null) {
							count += c;
							System.out.println("Sink " + sink.getId() + " from " + sourceid + " receives " + c);
						}
					}
				}
			}
		}
		System.out.println("Total count: " + count);

	}
	
	public void drawStatistics(String outputBaseName, int width, int height) {
		drawStatistics(outputBaseName, width, height, true, true); // pretty colourful picture
		drawStatistics(outputBaseName, width, height, false, false); // many detailed pictures
	}

	public void drawStatistics(String outputBaseName, int width, int height, boolean useColor, boolean overlay) {

		if (exitStatistics == null) return;

		// determine a few parameters ...
		Double minx = +500000000d;
		Double maxx = -500000000d;
		Double miny = +500000000d;
		Double maxy = -500000000d;

		for (Node node : this._settings.getNetwork().getNodes().values()) {
			Coord c = node.getCoord();

			minx = Math.min(c.getX(), minx);
			miny = Math.min(c.getY(), miny);
			maxx = Math.max(c.getX(), maxx);
			maxy = Math.max(c.getY(), maxy);
		}

		//System.out.println("minx "+ minx + " maxx " + maxx);
		//System.out.println("miny "+ miny + " maxy " + maxy);

		
		int radiusSink = Math.min(width, height) / 100;
		if (radiusSink < 2) radiusSink = 2;
		int radiusNode = Math.min(width, height) / 250;
		if (radiusNode < 1) radiusNode = 1;

		int nsinks =  exitStatisticsInverse.keySet().size();
		int countsinks = 0;
		
		Double scalex = (width) / (maxx - minx)*0.98;
		Double scaley = -(height) / (maxy - miny)*0.98;
		Double offsetx = -minx*scalex + width*0.01;
		Double offsety = -maxy*scaley - height*0.01;
		
		BufferedImage image = null;
		Graphics2D g2D = null;
		
		if (overlay) {
			image = new BufferedImage(width, height,BufferedImage.TYPE_INT_ARGB);
			g2D = image.createGraphics();
		}

		for (Id sinkid : exitStatisticsInverse.keySet()) {

			NetworkImpl network = this._settings.getNetwork();
			Node sink = network.getNodes().get(sinkid);

			// colours
			Color color;
			if (useColor) {
				color = Color.getHSBColor((float) (countsinks * 7 % nsinks )/ (float) nsinks,1.0f,1.0f);
			} else {
				color = Color.BLACK;
			}
			
			if (!overlay) {
					image = new BufferedImage(width, height,BufferedImage.TYPE_INT_ARGB);
					g2D = image.createGraphics();
			}
			
			// draw exits ...
			{
				Coord c = sink.getCoord();
				System.out.println(c.toString());
				Double x = c.getX() * scalex + offsetx;
				Double y = c.getY() * scaley + offsety;
				int X = x.intValue();
				int Y = y.intValue();
				g2D.setColor(color);
				g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
				g2D.fillOval(X-radiusSink, Y-radiusSink, 2 * radiusSink, 2 * radiusSink);
			}

			// draw the people

			HashMap<Id, Integer> whereFrom = exitStatisticsInverse.get(sink.getId());
			if (whereFrom != null) {
				for (Id sourceid : whereFrom.keySet()) {
					Node source = network.getNodes().get(sourceid);
					Integer howmany = whereFrom.get(sourceid);
					Integer demand = this._settings.getDemand(source);
					if (demand == null) {
						System.out.println("Strange, demand of source " + sourceid + " is undefined.");
					} else {

						// get coords
						Coord c = source.getCoord();
						Double x = c.getX() * scalex + offsetx;
						Double y = c.getY() * scaley + offsety;
						int X = x.intValue();
						int Y = y.intValue();

						g2D.setColor(color);
						float alpha = howmany / (float) demand;
						g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
						//g2D.drawLine(X, Y, X, Y);
						g2D.fillOval(X-radiusNode, Y-radiusNode, 2*radiusNode-1 , 2*radiusNode -1);
						//g2D.fillOval(X-1, Y-1, 2, 2);
						//g2D.drawOval(X, Y, 1, 1);
					}
				}
			} else {
				System.out.println("Strange. Sink " + sink.getId() + " was suddenly no longer in exitStatisticsInverse.");
			}

			if (!overlay) {
				String outputPngFilename = outputBaseName + sink.getId() + ".png";
				File pngfile = new File(outputPngFilename);
				try {
					ImageIO.write(image, "png", pngfile);
				} catch (IOException e) {
					System.out.println("Failed to write " + outputPngFilename);
				}				
			}
			countsinks++;
		}
		
		if (overlay) {
			String outputPngFilename = outputBaseName + "overlay.png";
			File pngfile = new File(outputPngFilename);
			try {
				ImageIO.write(image, "png", pngfile);
			} catch (IOException e) {
				System.out.println("Failed to write " + outputPngFilename);
			}				
		}
	}
}
