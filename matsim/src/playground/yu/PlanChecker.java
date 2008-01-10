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
package playground.yu;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.matsim.basic.v01.BasicPlan.LegIterator;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Route;
import org.matsim.plans.algorithms.PersonAlgorithm;

/**
 * @author yu
 * 
 */
public class PlanChecker extends PersonAlgorithm {

	private DataOutputStream out, out14, out41;
	private HashMap<String, Integer> hm;

	/**
	 * @param fileName
	 * 
	 */
	public PlanChecker(String fileName) {
		try {
			out = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(new File(fileName))));
			out.writeBytes("ratio\tamount\n");
			out14 = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(new File(fileName + "_14.txt"))));
			String head = "agend-ID\t" + "distance1\ttraveltime1\t"
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
	public void run(Person person) {
		int ivCnt = 0;
		int oevCnt = 0;
		for (Plan pl : person.getPlans()) {
			if (pl.getType().equals("iv")) {
				ivCnt++;
			} else {
				oevCnt++;
			}
		}
		String k = "iv" + ivCnt + "oev" + oevCnt;
		int i = 0;
		if (hm.containsKey(k)) {
			i = hm.get(k);
		}
		i++;
		hm.put(k, i);
		if (k.equals("iv4oev1")) {
			run41(person);
		} else if (k.equals("iv1oev4")) {
			run14(person);
		}
	}

	public void run14(Person person) {
		run_(person, out14);
	}

	public void run41(Person person) {
		run_(person, out41);
	}

	public void run_(Person person, DataOutputStream out) {
		for (Plan pl : person.getPlans()) {
			if (pl.getType().equals("oev")) {
				String text = "\t";
				for (Iterator<Leg> it = pl.getIteratorLeg(); it.hasNext();) {
					Leg l = it.next();
					Route r = l.getRoute();
					text += r.getDist() + "\t" + r.getTravTime() + "\t";
				}
				try {
					out.writeBytes(person.getId().toString() + text + "\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void writeResult() {
		System.out.println(": Writer begins to write rows");
		for (String k : hm.keySet()) {
			try {
				out.writeBytes(k + "\t" + Integer.toString(hm.get(k)) + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
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
