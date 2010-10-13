/* *********************************************************************** *
 * project: org.matsim.*
 * TupleSetTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.yu.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.core.utils.collections.Tuple;

public class TupleSetTest {
	public static void main(String[] args) {
		Set<Tuple<Integer, Double>> set = new HashSet<Tuple<Integer, Double>>();
		set.add(new Tuple<Integer, Double>(360, 1200d));
		set.add(new Tuple<Integer, Double>(360, 1200d));
		System.out.println("set:\t" + set);
		System.out.println("size of set:\t" + set.size());

		List<Tuple<Integer, Double>> list = new ArrayList<Tuple<Integer, Double>>();
		list.add(new Tuple<Integer, Double>(360, 1200d));
		list.add(new Tuple<Integer, Double>(360, 1200d));
		System.out.println("list:\t" + list);
		System.out.println("size of list:\t" + list.size());
	}
}
