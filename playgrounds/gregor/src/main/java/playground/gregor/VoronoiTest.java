/* *********************************************************************** *
 * project: org.matsim.*
 * VoronoiTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor;

import java.util.List;

import be.humphreys.simplevoronoi.GraphEdge;
import be.humphreys.simplevoronoi.Voronoi;

public class VoronoiTest {
public static void main(String[]args) {
	Voronoi v = new Voronoi(.1);
	
	double [] x = {1,2,3,4};
	double [] y = {0,1,0,3,2};
	
	List<GraphEdge> gg = v.generateVoronoi(x, y, -1, 5, -1, 5);
	System.out.println(gg.size());
}
}
