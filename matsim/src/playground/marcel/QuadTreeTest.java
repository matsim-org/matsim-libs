/* *********************************************************************** *
 * project: org.matsim.*
 * QuadTreeTest.java
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

import java.util.Iterator;

import org.matsim.utils.misc.QuadTree;

public class QuadTreeTest {

	QuadTree<String> tree1 = new QuadTree<String>(0, 0, 100, 100);
	
	private void add(double x, double y, String value) {
		boolean changed1 = tree1.put(x, y, value);
		System.out.println("add(" + x + ", " + y + ", " + value + ") : " + changed1);
	}
	
	private void remove(double x, double y, String value) {
		boolean changed1 = tree1.remove(x, y, value);
		System.out.println("remove(" + x + ", " + y + ", " + value + ") : " + changed1);
	}
	
	private void get(double x, double y) {
		String str1 = tree1.get(x, y);
		System.out.println("get(" + x + ", " + y + ") : " + str1);
	}
	
	private void print() {
		System.out.print("tree1 = ");
		for (Iterator<String> it = tree1.values().iterator(); it.hasNext(); ) {
			String value = it.next();
			System.out.print(value + ", ");
		}
		System.out.println();
	}
	
	public void run() {
		
		String o20_20 = "20/20";
		String o30_30 = "30/30";
		String o90_10 = "90/10";
		String o75_75 = "75/75";
		String o50_10 = "50/10";
		String o75_50 = "75/50";
		String o80_73a = "80/73/a";
		String o80_73b = "80/73/b";
		String o20_60 = "20/60";
		
		add(20, 20, o20_20);
		add(30, 30, o30_30);
		add(90, 10, o90_10);
		add(75, 75, o75_75);
		add(50, 10, o50_10);
		add(75, 50, o75_50);
		add(80, 73, o80_73a);
		add(80, 73, o80_73b);
		add(80, 73, o80_73b);
		add(20, 60, o20_60);
		
		print();
		
		get(76, 74);
		get(74, 76);
		get(74, 74);
		get(76, 76);
		get(75, 75);
		
		get(25, 25);
		get(24.9, 25);
		get(25, 24.9);
		get(25.1, 25);
		get(25, 25.1);
		
		get(80, 73);
		
		remove(75, 50, o75_50);
		get(75, 50);
		print();

		remove(80, 73, o80_73b);
		get(80, 73);
		add(80, 73, o80_73b);
		remove(80, 73, o80_73a);

		get(80, 73);
		
		remove(80, 73, o80_73b);
		get(80, 73);
		remove(80, 73, o80_73b);
		get(80, 73);
		
		print();
		
		remove(20, 20, o20_20);
		print();
		remove(75, 75, o75_75);
		print();
	}
	
	public static void main(String[] args) {
		new QuadTreeTest().run();
	}
}
