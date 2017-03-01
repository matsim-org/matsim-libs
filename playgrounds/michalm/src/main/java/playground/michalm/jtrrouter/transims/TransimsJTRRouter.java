/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.jtrrouter.transims;

import java.io.*;
import java.util.*;

import org.apache.commons.configuration.*;

import playground.michalm.jtrrouter.*;

/**
 * @author michalm
 */
public class TransimsJTRRouter extends JTRRouter {
	private static final int TRAVEL_TIME = 1800;

	private final List<TransimsPlan> plans = new ArrayList<>();
	private final List<TransimsVehicle> vehicles = new ArrayList<>();

	// <flow node="10" inParking="1" outParking="2" next="2">
	// ....<vehicle type="1" subtype="0" no="681"/>
	// ....<vehicle type="2" subtype="0" no="42"/>
	// ....<vehicle type="2" subtype="1" no="19"/>
	// ....<vehicle type="5" subtype="0" no="11"/>
	// </flow>
	protected void initFlow(HierarchicalConfiguration flowCfg) {
		int node = flowCfg.getInt("[@node]");
		int next = flowCfg.getInt("[@next]");

		int in = flowCfg.getInt("[@inParking]", -1);
		int out = flowCfg.getInt("[@outParking]", -1);

		int length = flowCfg.getMaxIndex("vehicle") + 1;

		int[] types = new int[length];
		int[] subTypes = new int[length];
		int[] nos = new int[length];

		for (int i = 0; i < length; i++) {
			Configuration vehCfg = flowCfg.subset("vehicle(" + i + ')');
			types[i] = vehCfg.getInt("[@type]");
			subTypes[i] = vehCfg.getInt("[@subtype]");
			nos[i] = vehCfg.getInt("[@no]");
		}

		flows[node] = new TransimsFlow(node, in, out, next, types, subTypes, nos);
	}

	protected void addPlan(int id, int startTime, Route route, int subFlow) {
		TransimsFlow tFlow = (TransimsFlow)route.getInFlow();
		int type = tFlow.types[subFlow];
		int subType = tFlow.subTypes[subFlow];

		plans.add(new TransimsPlan(id, route, startTime, startTime + TRAVEL_TIME, TRAVEL_TIME));
		vehicles.add(new TransimsVehicle(id, tFlow.inParking, type, subType));
	}

	protected void writePlans(String dir) {
		Collections.sort(plans);

		PrintWriter planWriter = initWriter(dir, "Plan.txt");

		for (TransimsPlan p : plans) {
			p.write(planWriter);
		}

		planWriter.close();

		PrintWriter vehicleWriter = initWriter(dir, "Vehicle.txt");
		vehicleWriter.println(TransimsVehicle.HEADER);

		for (TransimsVehicle v : vehicles) {
			v.write(vehicleWriter);
		}

		vehicleWriter.close();
	}

	private PrintWriter initWriter(String dir, String file) {
		try {
			return new PrintWriter(new BufferedWriter(new FileWriter(dir + "\\" + file)));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		String dir = System.getProperty("dir");
		String flowsFile = System.getProperty("flows");
		String turnsFile = System.getProperty("turns");

		dir = "d:\\PP-pub\\TransProblems-2010-JTRRouterTransims";
		// dir = "C:\\inzynierka\\demand";
		dir = "F:\\inz\\demand";
		flowsFile = "flows.xml";
		turnsFile = "turns.xml";

		new TransimsJTRRouter().generate(dir, flowsFile, turnsFile);
	}
}
