/* *********************************************************************** *
 * project: org.matsim.*
 * VisumWriteRoutes.java
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

package org.matsim.visum;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.matsim.network.Node;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.world.Location;
import org.matsim.world.ZoneLayer;

/**
 * @author mrieser
 *
 * writes route-information for each (selected) plan in a format VISUM
 * should be able to read. This can be used to analyze routes, origin
 * and destination of simulated traffic in VISUM.
 */
public class VisumWriteRoutes extends PersonAlgorithm implements PlanAlgorithmI {

	private BufferedWriter out = null;
	private ZoneLayer tvzLayer = null;
	
	public VisumWriteRoutes(String filename, ZoneLayer tvzLayer) {
		this.tvzLayer = tvzLayer;
		try {
			out = new BufferedWriter(new FileWriter(filename));
			out.write("$VISION\n$ROUTENIMPORT\n$VERSION 1\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run(Person person) {
		for(Plan plan : person.getPlans()) {
			if (plan.isSelected()) {
				run(plan);
			}
		}
	}

	public void run(Plan plan) {
		for (int i = 1; i < plan.getActsLegs().size(); i += 2) {
			Leg leg = (Leg)plan.getActsLegs().get(i);
			String visum = "";
			ArrayList<Node> route = leg.getRoute().getRoute();
			
			if (route.size() > 0) {
				ArrayList<Location> locs = tvzLayer.getNearestLocations(route.get(0).getCoord(), null);
				// from bezirk
				if (locs.size() > 0) {
					visum += locs.get(0).getId() + ";";
				} else {
					visum += "00;";
				}
				// to bezirk
				locs = tvzLayer.getNearestLocations(route.get(route.size() - 1).getCoord(), null);
				if (locs.size() > 0) {
					visum += locs.get(0).getId() + ";";
				} else {
					visum += "00;";
				}
				// anzahl
				visum += "1.0;";
				// knoten
				for (Node node : route) {
					visum += node.getId() + ";";
				}
				// abschluss
				visum += "-1\n";
				try {
					out.write(visum);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				visum += "00;00;";
			}
		}

	}
	
	public void close() {
		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
