/* *********************************************************************** *
 * project: org.matsim.*
 * PlanChecker.java
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

/**
 * 
 */
package playground.yu.mautZH;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.routes.CarRoute;

import playground.yu.analysis.PlanModeJudger;

/**
 * @author yu
 * 
 */
public class PlanChecker extends AbstractPersonAlgorithm {

	private DataOutputStream out, out14, out41;
	private final HashMap<String, Integer> hm;

	/**
	 * @param fileName
	 * 
	 */
	public PlanChecker(final String fileName) {
		try {
			out = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(new File(fileName))));
			out.writeBytes("ratio\tamount\n");
			out14 = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(new File(fileName + "_14.txt"))));
			String head = "agend-ID\t" + "type\t" + "distance1\ttraveltime1\t"
					+ "distance2\ttraveltime2\n";
			out14.writeBytes(head);
			out41 = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(new File(fileName + "_41.txt"))));
			out41.writeBytes(head);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("  begins to write txt-file about iv/oev-ratio");
		hm = new HashMap<String, Integer>();
	}

	@Override
	public void run(final Person person) {
		int ivCnt = 0;
		int oevCnt = 0;
		for (Plan pl : person.getPlans())
			if (
			// pl.getType().equals(Type.CAR)
			PlanModeJudger.useCar(pl))
				ivCnt++;
			else
				oevCnt++;
		String k = "iv" + ivCnt + "oev" + oevCnt;
		int i = 0;
		if (hm.containsKey(k))
			i = hm.get(k);
		i++;
		hm.put(k, i);
		if (k.equals("iv4oev1"))
			run41(person);
		else if (k.equals("iv1oev4"))
			run14(person);
	}

	public void run14(final Person person) {
		run_(person, out14);
	}

	public void run41(final Person person) {
		run_(person, out41);
	}

	public void run_(final Person person, final DataOutputStream out) {
		for (Plan pl : person.getPlans()) {
			// if (pl.getType().equals("oev")) {
			StringBuilder text = new StringBuilder("\t");
			for (Iterator<Leg> it = pl.getIteratorLeg(); it.hasNext();) {
				Leg l = it.next();
				CarRoute r = (CarRoute) l.getRoute();
				text.append(r.getDist() + "\t" + r.getTravelTime() + "\t");
			}
			try {
				out.writeBytes(person.getId().toString() + "\t"
						+ PlanModeJudger.getMode(pl) + text + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
			// }
		}
	}

	public void writeResult() {
		System.out.println(": Writer begins to write rows");
		for (String k : hm.keySet())
			try {
				out.writeBytes(k + "\t" + Integer.toString(hm.get(k)) + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		System.out.println(": Done.");
		try {
			System.out.println(": Writer begins to close!");
			out.close();
			out14.close();
			out41.close();
			System.out.println(": Done.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
