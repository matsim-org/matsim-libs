/* *********************************************************************** *
 * project: org.matsim.*
 * MyLink.java
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

package teach.multiagent07;
import java.util.ArrayList;
import java.util.List;

public class MyLink {

	class Veh{};
	
	private final double CELLSIZE = 7.5;
	
	private double length;
	
	private int nCells;
	private List cells;


	public static void main(String[] args) {

		// CreateLink
		MyLink link = new MyLink();
		
		// Prepare Link
		link.setLength(375);
		link.build();
		link.randomFill(0.5);
		
		//Simulation Run 
		for(int step = 0; step < 30; step++) {
			link.move(step);
			link.tty();
		}
	}

	public void addVeh(Veh veh) {
		cells.set(0,veh);
	}

	public void removeFirstVeh() {
		cells.set(nCells-1, null);
	}

	public Veh getFirstVeh() {
		return (Veh) cells.get(nCells-1);
	}

	public boolean hasSpace() {
		return cells.get(0) == null;
	}

	public void doBoundary() {
		if (getFirstVeh() != null && hasSpace()) {
			Veh veh = getFirstVeh();
			removeFirstVeh();
			addVeh(veh);
		}
	}

	public void setLength(double l) {
		length = l;		
	}

	public void randomFill(double d) {
		for (int i=0; i< nCells; i++) {
			if (Math.random() >= d ) cells.set(i, new Veh());
		}
	}

	public void build() {
		// calc number of cells
		nCells = (int)(length /CELLSIZE);
		cells = new ArrayList();
		// Fill the cells up to nCells
		for (int i = 0; i < nCells; i++) cells.add(null);
	}

	public void tty() {
		for (int i=0; i< nCells; i++) {
			if (cells.get(i) != null) System.out.print("X");
			else System.out.print(".");
		}
		System.out.println("");
	}

	public void move(int step) {
		for (int i=0; i< nCells -1; i++) {
			if (cells.get(i) != null && cells.get(i+1) == null) {
				Veh veh = (Veh) cells.get(i);
				cells.set(i+1, veh);
				cells.set(i, null);
				i++; // Avoid multiple moves of the same vehicle
			}
		}
	}
}

