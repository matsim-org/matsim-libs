/* *********************************************************************** *
 * project: org.matsim.*
 * MyTests.java
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

package playground.gregor.treeTest;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;

public class MyTests {

	//static Double value;
	
	
	
	private final static Integer LOOKUPS = 1000;
	
	private static final Logger log = Logger.getLogger(MyTests.class);
	
	
	private static long baseLineTest() {
//		log.info("starting BaseLine test");
		double dummy = 0.;
		long start = Long.valueOf(System.currentTimeMillis());
		long stop = Long.valueOf(System.currentTimeMillis());
		long time = 0;
		double value;
		
//		log.info("simulating " + LOOKUPS + " lookups");
		for (int i = 0; i < LOOKUPS; i++) {
			 value = Gbl.random.nextDouble();
			start = Long.valueOf(System.currentTimeMillis());
			double tmp = value;
			stop = Long.valueOf(System.currentTimeMillis());
			time += stop-start;
			dummy = Math.max(tmp, dummy);
		}
//		System.out.println("... it took:" + time + " ms");
//		System.out.println("dummy " + dummy);
		
		return time;
	}
	
	
	
	public static long testTreeMap() {
	
		Double dummy = 0.;
		TreeMap<Integer, Double> map = new TreeMap<Integer,Double>();
//		log.info("starting TreeMap test");
		Double dbl = 10.0;
		
//		log.info("building up the tree ...");
		long start = Long.valueOf(System.currentTimeMillis());
		for (int i = 0; i < 3600*24;) {
			
			dbl = Gbl.random.nextDouble();
			map.put(i, dbl);
			i += Gbl.random.nextInt(3600);
		}
		long stop = Long.valueOf(System.currentTimeMillis());
//		log.info("tree built-up took:" + (stop-start) + " ms. Tree size is:" + map.size() );
		
		
		long time = 0;
//		log.info("simulating " + LOOKUPS + " lookups");
		for (int i = 0; i < LOOKUPS; i++) {
			int query = Gbl.random.nextInt(20);
			start = Long.valueOf(System.currentTimeMillis());
			Double value = (Double) map.floorEntry(query).getValue();
			stop = Long.valueOf(System.currentTimeMillis());
			time += stop-start;
			dummy = Math.max(value, dummy);
		}
//		System.out.println("... it took:" + time + " ms");
//		System.out.println("dummy " + dummy);
		
		return time;
	}
	
	public static long  testClasspathTreeMap(){
		Double dummy = 0.;
		org.matsim.utils.collections.gnuclasspath.TreeMap<Integer, Double> map = new org.matsim.utils.collections.gnuclasspath.TreeMap<Integer,Double>();
//		log.info("starting TreeMap test");
		Double dbl = 10.0;
		
//		log.info("building up the tree ...");
		long start = Long.valueOf(System.currentTimeMillis());
		for (int i = 0; i < 3600*24;) {
			
			dbl = Gbl.random.nextDouble();
			map.put(i, dbl);
			i += Gbl.random.nextInt(3600);
		}
		long stop = Long.valueOf(System.currentTimeMillis());
//		log.info("tree built-up took:" + (stop-start) + " ms. Tree size is:" + map.size() );
		
		
		long time = 0;
//		log.info("simulating " + LOOKUPS + " lookups");
		for (int i = 0; i < LOOKUPS; i++) {
			int query = Gbl.random.nextInt(20);
			start = Long.valueOf(System.currentTimeMillis());
			Double value = (Double) map.floorEntry(query).getValue();
			stop = Long.valueOf(System.currentTimeMillis());
			time += stop-start;
			dummy = Math.max(value, dummy);
		}
//		System.out.println("... it took:" + time + " ms");
//		System.out.println("dummy " + dummy);
		
		return time;		
	}
	
	
	private static boolean testCompatibility() {
		
		TreeMap<Integer, Double> map = new TreeMap<Integer,Double>();
		org.matsim.utils.collections.gnuclasspath.TreeMap<Integer, Double> gnumap = new org.matsim.utils.collections.gnuclasspath.TreeMap<Integer,Double>();
		Double dbl = 10.0;
				
				
		for (int i = 0; i < 3600*24;) {
			
			dbl = Gbl.random.nextDouble();
			map.put(i, dbl);
			gnumap.put(i, dbl);
//			i += Gbl.random.nextInt(3600);
			i++;
		}
		for (int i = 0; i < LOOKUPS; i++) {
			int query = Gbl.random.nextInt(20);
		
			Double value = (Double) map.floorEntry(query).getValue();
			Double gnuvalue = (Double) gnumap.floorEntry(query).getValue();
			
			if (value != gnuvalue) {
				return false;
			}
		
		}
		
		
		
		// TODO Auto-generated method stub
		return true;
	}
	
	
	public static void main(String [] args) {
		
//		 
		boolean compatible = testCompatibility();
		System.out.println("combatible:" + compatible);
		
		long base = 0;
		long tree = 0;
		long classpathTree = 0;
		for (int i = 0; i < 1000; i++) {
			base += baseLineTest();

			tree += testTreeMap();
			
			classpathTree += testClasspathTreeMap();

		}

		System.out.println("base:" + base + "ms | tree:"  + tree + "ms" + " | classpath tree:" + classpathTree + "ms." );
		
		
		
		
		
	}






}
