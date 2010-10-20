/* *********************************************************************** *
 * project: org.matsim.*
 * AcceptanceProbability.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import gnu.trove.TDoubleDoubleHashMap;

import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.OrthodromicDistanceCalculator;
import playground.johannes.socialnetworks.statistics.Discretizer;
import playground.johannes.socialnetworks.statistics.LinearDiscretizer;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 * 
 */
public class AcceptanceProbability {

	private Discretizer discretizer = new LinearDiscretizer(1000);

	private DistanceCalculator distanceCalculator = new CartesianDistanceCalculator();// OrthodromicDistanceCalculator();

	public void setDistanceCalculator(DistanceCalculator calculator) {
		this.distanceCalculator = calculator;
	}

	public Distribution distribution(Set<? extends SpatialVertex> vertices, Set<Point> choiceSet) {
		// CoordinateReferenceSystem crs =
		// CRSUtils.getCRS(21781);//DefaultEngineeringCRS.CARTESIAN_2D; FIXME

//		int srid1 = vertices.iterator().next().getPoint().getSRID();
//		int srid2 = choiceSet.iterator().next().getSRID();
//
//		if (srid1 != srid2)
//			throw new RuntimeException(
//					"Vertices and points of choice set do not have the same coordinate reference system.");

		Distribution distribution = new Distribution();
		Set<SpatialEdge> touched = new HashSet<SpatialEdge>();

		// try {
		// MathTransform t1 = CRS.findMathTransform(CRSUtils.getCRS(srid1),
		// crs);
		// MathTransform t2 = CRS.findMathTransform(CRSUtils.getCRS(srid2),
		// crs);

//		DistanceCalculator calc2 = new CartesianDistanceCalculator();
		for (SpatialVertex vertex : vertices) {
			Point p1 = vertex.getPoint();
//			if (p1.getSRID() != srid1)
//				throw new RuntimeException("Vertices do not have the same coordinate reference system.");

			// double p1[] = new double[]{vertex.getPoint().getCoordinate().x,
			// vertex.getPoint().getCoordinate().y};
			// t1.transform(p1, 0, p1, 0, 1);

			TDoubleDoubleHashMap n_d = new TDoubleDoubleHashMap();
			for (Point p2 : choiceSet) {

//				if (p2.getSRID() != srid2)
//					throw new RuntimeException("Points do not have the same coordinate reference system.");

				// double p2[] = new double[]{p.getCoordinate().x,
				// p.getCoordinate().y};
				// t2.transform(p2, 0, p2, 0, 1);

				// double dx = p2[0] - p1[0];
				// double dy = p2[1] - p1[1];
				// double d = Math.sqrt(dx*dx + dy*dy);
				if (p1 != null && p2 != null) {
					double d = distanceCalculator.distance(p1, p2);
//					double d = calc2.distance(p1, p2);
					n_d.adjustOrPutValue(discretizer.discretize(d), 1, 1);
				}
			}

			for (int i = 0; i < vertex.getEdges().size(); i++) {
				SpatialEdge e = vertex.getEdges().get(i);
				if (touched.add(e)) {
					SpatialVertex neighbor = e.getOpposite(vertex);

//					if (neighbor.getPoint().getSRID() != srid1)
//						throw new RuntimeException("Vertices do not have the same coordinate reference system.");

					// double p2[] = new
					// double[]{neighbor.getPoint().getCoordinate().x,
					// neighbor.getPoint().getCoordinate().y};
					// t1.transform(p2, 0, p2, 0, 1);
					//						
					// double dx = p2[0] - p1[0];
					// double dy = p2[1] - p1[1];
					// double d = Math.sqrt(dx*dx + dy*dy);

					if (p1 != null && neighbor.getPoint() != null) {
						double d = distanceCalculator.distance(p1, neighbor.getPoint());

						double n = n_d.get(discretizer.discretize(d));
						if (n > 0)
							distribution.add(d, 1 / n);
					}
				}
			}
		}
		// } catch (FactoryException e) {
		// e.printStackTrace();
		// return null;
		// } catch (TransformException e) {
		// e.printStackTrace();
		// return null;
		// }

		return distribution;
	}
}
