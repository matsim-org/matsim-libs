/* *********************************************************************** *
 * project: org.matsim.*
 * CollectionSum.java
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
package playground.yu.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author yu
 * 
 */
public class CollectionSum {
	public static double getSum(Collection clt) {
		double sum = 0.0;
		for (Iterator ir = clt.iterator(); ir.hasNext();) {
			sum += (Double) ir.next();
		}
		return sum;
	}

	public static void main(String[] args) {
		Set<Double> dbs = new HashSet<Double>();
		for (int i = 0; i < 10; i++)
			dbs.add(Math.random());
		System.out.println("sum=" + getSum(dbs));
	}
}
