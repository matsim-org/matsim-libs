/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcHectarPopulation.java
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

package playground.balmermi.algos;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.basic.v01.BasicPlan;
import org.matsim.gbl.Gbl;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.utils.geometry.Coord;

public class PlansCalcHectarPopulation extends AbstractPersonAlgorithm implements PersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private FileWriter fw = null;
	private BufferedWriter out = null;

	// TreeMap<XCOORD,TreeMap<YCOORD,PCOUNT>>
	private final TreeMap<Integer,TreeMap<Integer,Integer>> hectars = new TreeMap<Integer, TreeMap<Integer,Integer>>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansCalcHectarPopulation() {
		super();
		try {
			fw = new FileWriter("output/hectarPopulation.txt");
			out = new BufferedWriter(fw);
			out.write("XCOORD\tYCOORD\tPCOUNT\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private final void writeSummary() {
		try {
			Iterator<Integer> x_it = hectars.keySet().iterator();
			while (x_it.hasNext()) {
				Integer x = x_it.next();
				TreeMap<Integer,Integer> tmp = hectars.get(x);
				Iterator<Integer> y_it = tmp.keySet().iterator();
				while (y_it.hasNext()) {
					Integer y = y_it.next();
					Integer value = tmp.get(y);
					out.write(x + "\t" + y + "\t" + value + "\n");
					out.flush();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// final method
	//////////////////////////////////////////////////////////////////////

	public final void close() {
		writeSummary();
		try {
			out.flush();
			out.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		int nof_plans = person.getPlans().size();
		if (nof_plans != 1) {
			Gbl.errorMsg("Person id=" + person.getId() + " does not have a single plan");
		}
		BasicPlan plan = person.getPlans().get(0);
		Act act = (Act)plan.getIteratorAct().next();
		if (act == null) {
			Gbl.errorMsg("Person id=" + person.getId() + " does not have a first act");
		}
//		if (act.getType() != "h") {
//			Gbl.errorMsg("Person id=" + person.getId() + " does not have a first act with type 'h'");
//		}
		Coord c = act.getCoord();
		if (c == null) {
			Gbl.errorMsg("Person id=" + person.getId() + " does not have a coord at the first act (of type 'h')");
		}
		int x = (int)c.getX();
		int y = (int)c.getY();
		x = x / 100; x = x * 100;
		y = y / 100; y = y * 100;
		TreeMap<Integer,Integer> tmp = hectars.get(x);
		if (tmp == null) {
			tmp = new TreeMap<Integer, Integer>();
			tmp.put(y,1);
			hectars.put(x,tmp);
		}
		else {
			Integer value = tmp.get(y);
			if (value == null) {
				tmp.put(y,1);
			}
			else {
				value++;
				tmp.put(y,value);
			}
		}
	}
}
