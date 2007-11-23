/* *********************************************************************** *
 * project: org.matsim.*
 * ActChainDistributionWriter.java
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

package playground.lnicolas.hettinger2007;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.basic.v01.BasicPlan;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;

public class ActChainDistributionWriter {

	public static void run(Plans population, String filename) {
		TreeMap<String, Integer> actChainDistr = getActChainDistr(population);
		String[] actTypes = getActTypes(population);
		
		writeActChainDistr(actChainDistr, actTypes, filename);
	}
	
	public static void run(Plans population, Plans referencePopulation, String filename) {
		TreeMap<String, Integer> actChainDistr = getActChainDistr(population);
		TreeSet<String> referenceActChains = getReferenceActChains(referencePopulation);
		removeActChainsNotInReferenceSet(actChainDistr, referenceActChains);
		String[] actTypes = getActTypes(population);
		
		writeActChainDistr(actChainDistr, actTypes, filename);
		
	}

	private static void removeActChainsNotInReferenceSet(TreeMap<String, Integer> actChainDistr,
			TreeSet<String> referenceActChains) {
		Iterator<String> it = actChainDistr.keySet().iterator();
		ArrayList<String> actChainsToBeRemoved = new ArrayList<String>();
		while (it.hasNext()) {
			String actChain = it.next();
			if (referenceActChains.contains(actChain) == false) {
				actChainsToBeRemoved.add(actChain);
			}
		}
		
		for (String actChain : actChainsToBeRemoved) {
			actChainDistr.remove(actChain);
		}
	}

	private static TreeSet<String> getReferenceActChains(Plans population) {
		TreeSet<String> actChains = new TreeSet<String>();

		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				String actChain = "";
				BasicPlan.ActIterator it = plan.getIteratorAct();
				while (it.hasNext()) {
					actChain += it.next().getType();
				}
				actChains.add(actChain);
			}
		}
		return actChains;
	}

	private static void writeActChainDistr(TreeMap<String, Integer> actChainDistr,
			String[] actTypes, String filename) {
		
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter(filename));
			out.write("act_chain\tfreq\tnof_trips");
			for (String actType : actTypes) {
				out.write("\t" + actType);
			}
			out.newLine();
			
			Iterator<Map.Entry<String, Integer>> it = actChainDistr.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, Integer> entry = it.next();
				out.write(entry.getKey() + "\t" + entry.getValue() + "\t"
						+ (entry.getKey().length() - 1));
				int[] actCount = getActCount(entry.getKey(), actTypes);
				for (int count : actCount) {
					out.write("\t" + count);
				}
				out.newLine();
			}
			out.close();
		} catch (IOException e) {
			System.out.println("Error writing to file " + filename + ": "
				+ e.getMessage());
		}
		System.out.println("Activity chain distribution written to " + filename);
	}

	private static int[] getActCount(String actChain, String[] actTypes) {
		TreeMap<String, Integer> actCount = new TreeMap<String, Integer>();
		for (int i = 0; i < actChain.length(); i++) {
			String ch = "" + actChain.charAt(i);
			int cnt = 0;
			if (actCount.containsKey(ch)) {
				cnt = actCount.get(ch);
			}
			actCount.put(ch, cnt + 1);
		}
		
		int[] acts = new int[actTypes.length];
		for (int i = 0; i < acts.length; i++) {
			int cnt = 0;
			if (actCount.containsKey(actTypes[i])) {
				cnt = actCount.get(actTypes[i]);
			}
			acts[i] = cnt;
		}
		return acts;
	}

	private static String[] getActTypes(Plans population) {
		TreeSet<String> actTypes = new TreeSet<String>();
		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				BasicPlan.ActIterator it = plan.getIteratorAct();
				while (it.hasNext()) {
					actTypes.add(it.next().getType());
				}
			}
		}
		
		return actTypes.toArray(new String[0]);
	}

	private static TreeMap<String, Integer> getActChainDistr(Plans population) {
		TreeMap<String, Integer> actChainDist = new TreeMap<String, Integer>();

		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				String actChain = "";
				BasicPlan.ActIterator it = plan.getIteratorAct();
				while (it.hasNext()) {
					actChain += it.next().getType();
				}
				int cnt = 0;
				if (actChainDist.containsKey(actChain)) {
					cnt = actChainDist.get(actChain);
				}
				actChainDist.put(actChain, cnt + 1);
			}
		}
		return actChainDist;
	}
	
}
