/* *********************************************************************** *
 * project: org.matsim.*
 * Graph.java
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

package playground.gregor.sim2denvironment.approxdecomp;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

public class Graph {

	/*package*/ static class Node {

		/*package*/ final Coordinate c;
		/*package*/ final int id;
		/*package*/ final List<Link> outLinks = new ArrayList<Link>();

		public Node(Coordinate c, int id) {
			this.c = c;
			this.id = id;
		}
		
		public void addOutLink(Link l) {
			this.outLinks.add(l);
		}
		

		
	}
	
	/*package*/ static class Link {

		/*package*/ final Node n0;
		/*package*/ final Node n1;
		/*package*/ double length;

		public Link(Node n0, Node n1) {
			this.n0 = n0;
			this.n1 = n1;
			this.length = n0.c.distance(n1.c);
		}
		
		public void setLength(double length) {
			this.length = length;
		}
		
	}
	
}
