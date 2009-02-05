/* *********************************************************************** *
 * project: org.matsim.*
 * Getagentcoods.java
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

package playground.dressler.util;

import java.util.Collection;

import java.util.*;

import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.StartupListener;
import org.matsim.events.Events;
import org.matsim.events.algorithms.EventWriterTXT;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.QueueSimulation;
//import org.matsim.network.MatsimNetworkReader;
//import org.matsim.network.NetworkLayer;
import org.matsim.network.*;
//import org.matsim.population.MatsimPopulationReader;
//import org.matsim.population.Population;
import org.matsim.population.*;
import org.matsim.run.OTFVis;
import org.matsim.utils.vis.netvis.NetVis;
import org.matsim.utils.vis.netvis.streaming.StreamConfig;
import org.matsim.utils.vis.otfvis.executables.OTFEvent2MVI;
import org.matsim.world.World;
import org.matsim.utils.geometry.Coord;
import org.matsim.basic.v01.Id;

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;


public class Getagentcoords {

	public static void main(final String[] args) {
		// choose instance
		final String netFilename = "/homes/combi/dressler/V/Project/padang/network/padang_net_evac.xml";
		//final String plansFilename = "/homes/combi/dressler/V/Project/padang/plans/padang_plans_10p.xml.gz";
		final String plansFilename = "/homes/combi/dressler/V/code/workspace/matsim/examples/meine_EA/padangplans.xml";
		
		final String outputPngFilename = "/homes/combi/dressler/V/code/workspace/matsim/output/exitmap.png";
		boolean planstats = true;

		@SuppressWarnings("unused")
		Config config = Gbl.createConfig(null);

		World world = Gbl.getWorld();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);
		world.complete();

		Population population = new Population();
		new MatsimPopulationReader(population).readFile(plansFilename);


		// get evac links
		Node evac1node = network.getNode("en1");
		Map<Id,? extends Link> evaclinks = null;		
		if (evac1node != null) {
			evaclinks = evac1node.getInLinks();
			for (Link link : evaclinks.values()) {
				System.out.println(link.getId().toString());
			}
		}

		// colours
		Map<Id,Color> colours = new HashMap<Id, Color>();
		Color noexit = Color.BLACK;

		if (evaclinks != null) {
			for (Link link : evaclinks.values()) {
				colours.put(link.getId(), Color.getHSBColor(colours.size()/evaclinks.size(),1.0f,1.0f));
			}		   			
		}

		// create chosen-exit map		
		if (planstats) {			
			Double minc = +500000000d;
			Double maxc = -500000000d;

			int width = 300;
			int height = 300;
			BufferedImage image = new BufferedImage(width,
					height,BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2D = image.createGraphics(); 

			for (Person p : population.getPersons().values()) {
				Plan plan = p.getSelectedPlan();
				// p.setVisualizerData(visualizerData)
				if (plan == null) continue;

				Coord c = plan.getFirstActivity().getLink().getFromNode().getCoord();
				//Coord c = plan.getFirstActivity().getCoord();
				//if (c == null) continue; // why would this happen? but happens ...
				minc = Math.min(c.getX(), minc);
				minc = Math.min(c.getY(), minc);
				maxc = Math.max(c.getX(), maxc);
				maxc = Math.max(c.getY(), maxc);
			}

			Double scale = ((double) width) / (maxc - minc)*0.98;			
			Double offset = -minc*scale + 3;

			System.out.println("minc "+ minc + " maxc " + maxc);
			Integer foundpeople = 0;
			for (Person p : population.getPersons().values()) {
				Plan plan = p.getSelectedPlan();
				// p.setVisualizerData(visualizerData)
				if (plan == null) continue;

				Coord c = plan.getFirstActivity().getLink().getFromNode().getCoord();
				
				//if (c == null) continue; // why would this happen? but happens ...
				
				//System.out.println(c.getX() + " " + c.getY());
				Leg leg = plan.getNextLeg(plan.getFirstActivity());
				if (leg == null || evaclinks == null) {
					//System.out.println(-99);								
					continue;
				}

				Double x = c.getX() * scale + offset;
				Double y = c.getY() * scale + offset;					  
				int X = x.intValue();
				int Y = y.intValue();
				//System.out.println(X + " " + Y);


				boolean found = false; 
				for (Id id : leg.getRoute().getLinkIds()) {			      
					if (evaclinks.containsKey(id)) {
						found = true;
						//System.out.println("Juhu " + id);
						foundpeople++;
						g2D.setColor(colours.get(id));
						g2D.drawOval(X, Y, 1, 1);
					}
				}
				if (!found) {
					g2D.setColor(noexit);
					g2D.drawOval(X, Y, 1, 1);
				}
			}
			System.out.println(foundpeople);
			File pngfile = new File(outputPngFilename);
			try {
				ImageIO.write(image, "png", pngfile);
			} catch (IOException e) {
			}

		}
	}
}
