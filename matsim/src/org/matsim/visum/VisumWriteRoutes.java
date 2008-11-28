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
import java.util.List;

import org.matsim.network.Node;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.world.Location;
import org.matsim.world.ZoneLayer;

/**
 * @author mrieser
 *
 * writes route-information for each (selected) plan in a format VISUM
 * should be able to read. This can be used to analyze routes, origin
 * and destination of simulated traffic in VISUM.
 */
public class VisumWriteRoutes extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private BufferedWriter out = null;
	private ZoneLayer tvzLayer = null;

	public VisumWriteRoutes(String filename, ZoneLayer tvzLayer) {
		this.tvzLayer = tvzLayer;
		try {
			this.out = new BufferedWriter(new FileWriter(filename));
			this.out.write("$VISION\n$ROUTENIMPORT\n$VERSION 1\n");
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
			StringBuilder visum = new StringBuilder();
			List<Node> route = leg.getRoute().getNodes();

			if (route.size() > 0) {
				ArrayList<Location> locs = this.tvzLayer.getNearestLocations(route.get(0).getCoord(), null);
				// from bezirk
				if (locs.size() > 0) {
					visum.append(locs.get(0).getId());
					visum.append(";");
				} else {
					visum.append("00;");
				}
				// to bezirk
				locs = this.tvzLayer.getNearestLocations(route.get(route.size() - 1).getCoord(), null);
				if (locs.size() > 0) {
					visum.append(locs.get(0).getId());
					visum.append(";");
				} else {
					visum.append("00;");
				}
				// anzahl
				visum.append("1.0;");
				// knoten
				for (Node node : route) {
					visum.append(node.getId());
					visum.append(";");
				}
				// abschluss
				visum.append("-1\n");
				try {
					this.out.write(visum.toString());
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
//				visum.append("00;00;");
			}
		}

	}

	public void close() {
		try {
			this.out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
