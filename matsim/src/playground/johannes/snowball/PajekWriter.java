/* *********************************************************************** *
 * project: org.matsim.*
 * PajekWriter.java
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
package playground.johannes.snowball;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.matsim.plans.Person;
import org.matsim.utils.geometry.CoordI;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.NumberEdgeValue;
import edu.uci.ics.jung.graph.decorators.VertexStringer;
import edu.uci.ics.jung.io.PajekNetWriter;
import edu.uci.ics.jung.visualization.VertexLocationFunction;

/**
 * @author illenberger
 *
 */
public class PajekWriter {

	public void write(Graph g, String filename) {
		Set<Vertex> vertices = g.getVertices();
		
		double minX = Double.MAX_VALUE;
		double minY = Double.MIN_VALUE;
		for(Vertex v : vertices) {
			CoordI c = ((Person)v.getUserDatum(Sampler.PERSON_KEY)).getSelectedPlan().getFirstActivity().getCoord();
			minX = Math.min(minX, c.getX());
			minY = Math.min(minY, c.getY());
		}
		
		PajekNetWriter writer = new PajekNetWriter();
		try {
			writer.save(g, filename, new PersonIdStringer(), new EdgeValue(), new VertexLocation(minX, minY));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static class PersonIdStringer implements VertexStringer {

		public String getLabel(ArchetypeVertex arg0) {
			return ((Person)arg0.getUserDatum(Sampler.PERSON_KEY)).getId().toString();
		}
		
	}
	
	private static class EdgeValue implements NumberEdgeValue {

		public Number getNumber(ArchetypeEdge arg0) {
			return 1;
		}

		public void setNumber(ArchetypeEdge arg0, Number arg1) {
			throw new UnsupportedOperationException();
		}
		
	}

	private static class VertexLocation implements VertexLocationFunction {

		private double minX;
		
		private double minY;
		
		public VertexLocation(double minX, double minY) {
			this.minX = minX;
			this.minY = minY;
		}
		
		public Point2D getLocation(ArchetypeVertex arg0) {
			CoordI c = ((Person)arg0.getUserDatum(Sampler.PERSON_KEY)).getSelectedPlan().getFirstActivity().getCoord();
			return new Point((int)(c.getX() - minX), (int)(c.getY() - minY));
		}

		public Iterator getVertexIterator() {
			throw new UnsupportedOperationException();
		}
		
	}
	
}
