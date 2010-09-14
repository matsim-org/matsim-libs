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

package playground.rost.eaflow.util;

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


public class Getagentcoords {

	public static void main(final String[] args) {
		// choose instance
		//final String netFilename = "./examples/meine_EA/siouxfalls_network_5s.xml";
		//final String netFilename = "./examples/meine_EA/swissold_network_5s.xml";
		final String netFilename = "/homes/combi/dressler/V/Project/padang/network/padang_net_evac.xml";

		//final String plansFilename = "/homes/combi/dressler/V/Project/padang/plans/padang_plans_100p_flow_10s_test.xml";
		final String plansFilename = "/homes/combi/dressler/V/code/workspace/matsim/examples/meine_EA/padang_plans_100p_flow_10s_test.xml";
		//final String plansFilename = "./examples/meine_EA/siouxfalls_plans_5s_demand_100.xml";
		//final String plansFilename = "./output/siouxfalls_5s_eaf/plans_iter100.xml";
		//final String plansFilename = "./examples/meine_EA/swissold_plans_5s_demands_100.xml";

		final String outputPngFilename = "./output/exitmap_padang_100p_flow_10s.png";
		boolean planstats = true;

		final float alpha = 0.01f; // transparency factor. depends on maximum demands.

		ScenarioImpl scenario = new ScenarioImpl();

		NetworkImpl network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(plansFilename);


		// get evac links
		Node evac1node = network.getNodes().get(new IdImpl("en1"));
		Map<Id,? extends Link> evaclinks = null;
		if (evac1node != null) {
			evaclinks = evac1node.getInLinks();
			/*for (Link link : evaclinks.values()) {
				System.out.println(link.getId().toString());
			}*/
		}

		// colours
		Map<String,Color> colours = new HashMap<String, Color>();
		Color noexit = Color.BLACK;

		if (evaclinks != null) {
			for (Link link : evaclinks.values()) {
				colours.put(link.getId().toString(), Color.getHSBColor((float) ((colours.size()*7) % evaclinks.size() )/ (float) evaclinks.size(),1.0f,1.0f));
				//colours.put(link.getId(), Color.getHSBColor(0.3f,1.0f,1.0f));
				//System.out.println(colours.get(link.getId().toString()));
			}
		}

		// create chosen-exit map
		if (planstats) {
			Double minx = +500000000d;
			Double maxx = -500000000d;
			Double miny = +500000000d;
			Double maxy = -500000000d;

			int width = 2000;
			int height = 2000;
			BufferedImage image = new BufferedImage(width,
					height,BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2D = image.createGraphics();

			for (Person p : population.getPersons().values()) {
				Plan plan = p.getSelectedPlan();
				// p.setVisualizerData(visualizerData)
				if (plan == null) continue;

				 Id i = ((PlanImpl) plan).getFirstActivity().getLinkId();
				 Link l = network.getLinks().get(i);
				 Coord c = l.getFromNode().getCoord();
				//Coord c = plan.getFirstActivity().getCoord();
				//if (c == null) continue; // why would this happen? but happens ...
				minx = Math.min(c.getX(), minx);
				miny = Math.min(c.getY(), miny);
				maxx = Math.max(c.getX(), maxx);
				maxy = Math.max(c.getY(), maxy);
			}

			Double scalex = (width) / (maxx - minx)*0.98;
			Double scaley = -((double) height) / (maxy - miny)*0.98;
			Double offsetx = -minx*scalex + 3;
			Double offsety = -maxy*scaley + 3;

			System.out.println("minx "+ minx + " maxx " + maxx);
			System.out.println("miny "+ miny + " maxy " + maxy);

			// print exits ...
			for (Link link : evaclinks.values()) {
				Coord c = link.getFromNode().getCoord();
				Double x = c.getX() * scalex + offsetx;
				Double y = c.getY() * scaley + offsety;
				int X = x.intValue();
				int Y = y.intValue();
				g2D.setColor(colours.get(link.getId().toString()));
				g2D.fillOval(X-8, Y-8, 16, 16);
			}

			Integer foundpeople = 0;
			Integer notfoundpeople = 0;
			for (Person p : population.getPersons().values()) {
				Plan plan = p.getSelectedPlan();
				// p.setVisualizerData(visualizerData)
				if (plan == null) {notfoundpeople++; continue;}

				 Id i = ((PlanImpl) plan).getFirstActivity().getLinkId();
				 Link l = network.getLinks().get(i);
				 Coord c = l.getFromNode().getCoord();

				//if (c == null) continue; // why would this happen? but happens ...

				//System.out.println(c.getX() + " " + c.getY());
				Leg leg = ((PlanImpl) plan).getNextLeg(((PlanImpl) plan).getFirstActivity());
				if (leg == null || evaclinks == null) {
					notfoundpeople++;
					continue;
				}

				Double x = c.getX() * scalex + offsetx;
				Double y = c.getY() * scaley + offsety;
				int X = x.intValue();
				int Y = y.intValue();
				//System.out.println(x + " " + y);


				boolean found = false;
				if (leg.getRoute() != null) if (leg.getRoute() instanceof NetworkRoute) if (((NetworkRoute) leg.getRoute()).getLinkIds() != null)
				for (Id id : ((NetworkRoute) leg.getRoute()).getLinkIds()) {
					if (evaclinks.containsKey(id)) {
						found = true;
						//System.out.println("Juhu " + id);
						foundpeople++;
						g2D.setColor(colours.get(id.toString()));
						g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
						//g2D.drawLine(X, Y, X, Y);
						g2D.fillOval(X-2, Y-2, 5, 5);
						//g2D.fillOval(X-1, Y-1, 2, 2);
						//g2D.drawOval(X, Y, 1, 1);
					}
				}
				if (!found) {
					notfoundpeople++;
					g2D.setColor(noexit);
					//g2D.drawLine(X, Y, X, Y);
					g2D.fillRect(X-1, Y-1, 2, 2);
				}



			}


			System.out.println("Found: " + foundpeople +  " not found: " + notfoundpeople);
			File pngfile = new File(outputPngFilename);
			try {
				ImageIO.write(image, "png", pngfile);
			} catch (IOException e) {
			}

		}
	}
}
