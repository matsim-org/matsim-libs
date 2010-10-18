/* *********************************************************************** *
 * project: org.matsim.*
 * Collection2Array.java
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

package playground.yu.utils.container;

import static playground.yu.utils.container.Collection2Array.toArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Collection2Array {
	public static void main(String[] args) {
		List<Integer> list = new ArrayList<Integer>();
		Random r = new Random();
		for (int i = 0; i < 10; i++) {
			list.add(r.nextInt());
		}
		int[] intArray = toArray(list);
		System.out.println("----------------------");
		for (int i = 0; i < intArray.length; i++) {
			System.out.println("intArray[\t" + i + "] :\t" + intArray[i]);
		}
		System.out.println("----------------------");

		System.out.println("----------------------");
		Set<Double> set = new HashSet<Double>();
		// Random r = new Random();
		for (int i = 0; i < 10; i++) {
			set.add(r.nextDouble());
		}
		double[] doubleArray = toArray(set);
		System.out.println("----------------------");
		for (int i = 0; i < doubleArray.length; i++) {
			System.out.println("doubleArray[\t" + i + "] :\t" + doubleArray[i]);
		}
		System.out.println("----------------------");

		List<Integer> list2 = new ArrayList<Integer>();
		// Random r = new Random();
		for (int i = 0; i < 10; i++) {
			list2.add(r.nextInt());
		}
		double[] doubleArray2 = toDoubleArray(list2);
		System.out.println("----------------------");
		System.out.println("original List<Integer> :\t" + list2);
		System.out.println("------------array----------");
		for (int i = 0; i < doubleArray2.length; i++) {
			System.out.println("intArray[\t" + i + "] :\t" + doubleArray2[i]);
		}
		System.out.println("----------------------");
	}

	public static double[] toArray(Collection<Double> collection) {
		int size = collection.size();
		double[] array = new double[size];
		Iterator<Double> it = collection.iterator();
		for (int i = 0; it.hasNext(); i++) {
			array[i] = it.next();
		}
		return array;
	}

	public static int[] toArray(Collection<Integer> collection) {
		int size = collection.size();
		int[] array = new int[size];
		Iterator<Integer> it = collection.iterator();
		for (int i = 0; it.hasNext(); i++) {
			array[i] = it.next();
		}
		return array;
	}

	public static double[] toDoubleArray(Collection<Integer> collection) {
		int size = collection.size();
		double[] array = new double[size];
		Iterator<Integer> it = collection.iterator();
		for (int i = 0; it.hasNext(); i++) {
			array[i] = it.next();
		}
		return array;
	}
}
