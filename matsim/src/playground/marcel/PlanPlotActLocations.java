/* *********************************************************************** *
 * project: org.matsim.*
 * PlanPlotActLocations.java
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

package playground.marcel;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;

public class PlanPlotActLocations extends PersonAlgorithm implements PlanAlgorithmI {

	private BufferedWriter out = null;
	private int cnt = 0;
	private ArrayList<String> types = null;

	public PlanPlotActLocations(String filename, String[] acttypes) {
		types = new ArrayList<String>(acttypes.length);
		for (String acttype : acttypes) {
			types.add(acttype);
		}
		try {
			out = new BufferedWriter(new FileWriter(filename));
			out.write("VEHICLE\tTIME\tLINK\tNODE\tLANE\tDISTANCE\tVELOCITY\tVEHTYPE\tACCELER\tDRIVER\tPASSENGERS\tEASTING\tNORTHING\tELEVATION\tAZIMUTH\tUSER\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void run(Person person) {
		for(Plan plan : person.getPlans()) {
			if (plan.isSelected()) {
				run(plan);
			}
		}
	}
	
	public void run(Plan plan) {
		for(int i = 0; i < plan.getActsLegs().size(); i += 2) {
			Act act = (Act)plan.getActsLegs().get(i);
			writeLocation(act.getCoord().getX(), act.getCoord().getY(), act.getType());
		}
	}

	private void writeLocation(double x, double y, String acttype) {
		if (out != null) {
			int actIdx = types.indexOf(acttype);
			try {
				out.write(cnt + "\t0\t1\t1\t1\t" + // vehicle, time, link, node, lane
						"0\t0\t1\t0\t" + cnt + "\t" + // distance, velocity, vehtype, acceler, driver
						"0\t" + x + "\t" + y + "\t0\t0\t" + actIdx + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}  // passengers, easting, northing, elevation, azimuth, user
			cnt++;
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
